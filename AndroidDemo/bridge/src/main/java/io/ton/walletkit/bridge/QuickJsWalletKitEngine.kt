package io.ton.walletkit.bridge

import android.content.Context
import android.util.Base64
import android.util.Log
import app.cash.quickjs.QuickJs
import io.ton.walletkit.bridge.config.WalletKitBridgeConfig
import io.ton.walletkit.bridge.listener.WalletKitEngineListener
import io.ton.walletkit.bridge.model.WalletAccount
import io.ton.walletkit.bridge.model.WalletKitEvent
import io.ton.walletkit.bridge.model.WalletSession
import io.ton.walletkit.bridge.model.WalletState
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.Closeable
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * QuickJS-backed implementation of [WalletKitEngine]. Executes the WalletKit JavaScript bundle
 * inside the embedded QuickJS runtime and bridges JSON-RPC calls/events to Kotlin.
 */
class QuickJsWalletKitEngine(
    context: Context,
    private val assetPath: String = "walletkit/quickjs/index.js",
    private val httpClient: OkHttpClient = defaultHttpClient(),
) : WalletKitEngine {
    override val kind: WalletKitEngineKind = WalletKitEngineKind.QUICKJS

    private val logTag = "QuickJsWalletKitEngine"
    private val appContext = context.applicationContext
    private val assetManager = appContext.assets
    private val listeners = CopyOnWriteArraySet<WalletKitEngineListener>()
    private val ready = CompletableDeferred<Unit>()
    private val pending = ConcurrentHashMap<String, CompletableDeferred<BridgeResponse>>()
    private val timerIdGenerator = AtomicInteger(1)
    private val timers = ConcurrentHashMap<Int, TimerHandle>()
    private val fetchIdGenerator = AtomicInteger(1)
    private val activeFetchCalls = ConcurrentHashMap<Int, Call>()

    private val jsExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "WalletKitQuickJs").apply { isDaemon = true }
    }
    private val jsDispatcher = jsExecutor.asCoroutineDispatcher()
    private val jsScope = CoroutineScope(jsDispatcher + SupervisorJob())
    private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mainScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private val timerExecutor = ScheduledThreadPoolExecutor(1) { runnable ->
        Thread(runnable, "WalletKitQuickJsTimer").apply { isDaemon = true }
    }.apply {
        removeOnCancelPolicy = true
    }
    private val quickJsDeferred = CompletableDeferred<QuickJs>()

    @Volatile private var quickJsInstance: QuickJs? = null
    private val random = SecureRandom()
    private val jsEvaluateMutex = Mutex()

    @Volatile private var currentNetwork: String = "testnet"

    @Volatile private var apiBaseUrl: String = "https://testnet.tonapi.io"

    @Volatile private var tonApiKey: String? = null

    init {
        jsScope.launch {
            try {
                val quickJs = QuickJs.create()
                installNativeBindings(quickJs)
                evaluateBootstrap(quickJs)
                loadBundle(quickJs)
                quickJsInstance = quickJs
                quickJsDeferred.complete(quickJs)
            } catch (err: Throwable) {
                Log.e(logTag, "Failed to initialise QuickJS runtime", err)
                quickJsDeferred.completeExceptionally(err)
                if (!ready.isCompleted) {
                    ready.completeExceptionally(
                        WalletKitBridgeException(
                            err.message ?: "Failed to initialise QuickJS runtime",
                        ),
                    )
                }
            }
        }
    }

    override fun addListener(listener: WalletKitEngineListener): Closeable {
        listeners.add(listener)
        return Closeable { listeners.remove(listener) }
    }

    override suspend fun init(config: WalletKitBridgeConfig): JSONObject {
        currentNetwork = config.network
        val tonClientEndpoint =
            config.tonClientEndpoint?.ifBlank { null }
                ?: config.apiUrl?.ifBlank { null }
                ?: defaultTonClientEndpoint(config.network)
        apiBaseUrl = config.tonApiUrl?.ifBlank { null } ?: defaultTonApiBase(config.network)
        tonApiKey = config.apiKey
        val payload =
            JSONObject().apply {
                put("network", config.network)
                put("apiUrl", tonClientEndpoint)
                config.apiUrl?.let { put("apiBaseUrl", it) }
                config.tonApiUrl?.let { put("tonApiUrl", it) }
                config.bridgeUrl?.let { put("bridgeUrl", it) }
                config.bridgeName?.let { put("bridgeName", it) }
                config.allowMemoryStorage?.let { put("allowMemoryStorage", it) }
                tonApiKey?.let { put("apiKey", it) }
            }
        return call("init", payload)
    }

    override suspend fun addWalletFromMnemonic(
        words: List<String>,
        version: String,
        network: String?,
    ): JSONObject {
        val params =
            JSONObject().apply {
                put("words", JSONArray(words))
                put("version", version)
                network?.let { put("network", it) }
            }
        return call("addWalletFromMnemonic", params)
    }

    override suspend fun getWallets(): List<WalletAccount> {
        val result = call("getWallets")
        val items = result.optJSONArray("items") ?: JSONArray()
        return buildList(items.length()) {
            for (index in 0 until items.length()) {
                val entry = items.optJSONObject(index) ?: continue
                add(
                    WalletAccount(
                        address = entry.optString("address"),
                        publicKey = entry.optNullableString("publicKey"),
                        version = entry.optString("version", "unknown"),
                        network = entry.optString("network", currentNetwork),
                        index = entry.optInt("index", index),
                    ),
                )
            }
        }
    }

    override suspend fun getWalletState(address: String): WalletState {
        val params = JSONObject().apply { put("address", address) }
        val result = call("getWalletState", params)
        return WalletState(
            balance =
            when {
                result.has("balance") -> result.optString("balance")
                result.has("value") -> result.optString("value")
                else -> null
            },
            transactions = result.optJSONArray("transactions") ?: JSONArray(),
        )
    }

    override suspend fun handleTonConnectUrl(url: String): JSONObject {
        val params = JSONObject().apply { put("url", url) }
        return call("handleTonConnectUrl", params)
    }

    override suspend fun approveConnect(
        requestId: Any,
        walletAddress: String,
    ): JSONObject {
        val params =
            JSONObject().apply {
                put("requestId", requestId)
                put("walletAddress", walletAddress)
            }
        return call("approveConnectRequest", params)
    }

    override suspend fun rejectConnect(
        requestId: Any,
        reason: String?,
    ): JSONObject {
        val params =
            JSONObject().apply {
                put("requestId", requestId)
                reason?.let { put("reason", it) }
            }
        return call("rejectConnectRequest", params)
    }

    override suspend fun approveTransaction(requestId: Any): JSONObject {
        val params = JSONObject().apply { put("requestId", requestId) }
        return call("approveTransactionRequest", params)
    }

    override suspend fun rejectTransaction(
        requestId: Any,
        reason: String?,
    ): JSONObject {
        val params =
            JSONObject().apply {
                put("requestId", requestId)
                reason?.let { put("reason", it) }
            }
        return call("rejectTransactionRequest", params)
    }

    override suspend fun approveSignData(requestId: Any): JSONObject {
        val params = JSONObject().apply { put("requestId", requestId) }
        return call("approveSignDataRequest", params)
    }

    override suspend fun rejectSignData(
        requestId: Any,
        reason: String?,
    ): JSONObject {
        val params =
            JSONObject().apply {
                put("requestId", requestId)
                reason?.let { put("reason", it) }
            }
        return call("rejectSignDataRequest", params)
    }

    override suspend fun listSessions(): List<WalletSession> {
        val result = call("listSessions")
        val items = result.optJSONArray("items") ?: JSONArray()
        return buildList(items.length()) {
            for (index in 0 until items.length()) {
                val entry = items.optJSONObject(index) ?: continue
                add(
                    WalletSession(
                        sessionId = entry.optString("sessionId"),
                        dAppName = entry.optString("dAppName"),
                        walletAddress = entry.optString("walletAddress"),
                        dAppUrl = entry.optNullableString("dAppUrl"),
                        manifestUrl = entry.optNullableString("manifestUrl"),
                        iconUrl = entry.optNullableString("iconUrl"),
                        createdAtIso = entry.optNullableString("createdAt"),
                        lastActivityIso = entry.optNullableString("lastActivity"),
                    ),
                )
            }
        }
    }

    override suspend fun disconnectSession(sessionId: String?): JSONObject {
        val params = JSONObject()
        sessionId?.let { params.put("sessionId", it) }
        return call("disconnectSession", if (params.length() == 0) null else params)
    }

    override suspend fun destroy() {
        withContext(jsDispatcher) {
            quickJsInstance?.close()
            quickJsInstance = null
            if (!quickJsDeferred.isCompleted) {
                quickJsDeferred.cancel()
            }
        }
        timers.values.forEach { handle ->
            handle.future?.cancel(true)
        }
        timers.clear()
        timerExecutor.shutdownNow()
        activeFetchCalls.values.forEach { call ->
            call.cancel()
        }
        activeFetchCalls.clear()
        ioScope.cancel()
        mainScope.cancel()
        jsScope.cancel()
        jsExecutor.shutdownNow()
    }

    private suspend fun call(
        method: String,
        params: JSONObject? = null,
    ): JSONObject {
        ready.await()
        val callId = UUID.randomUUID().toString()
        val deferred = CompletableDeferred<BridgeResponse>()
        pending[callId] = deferred
        val payload = params?.toString()
        val idLiteral = JSONObject.quote(callId)
        val methodLiteral = JSONObject.quote(method)
        val script =
            if (payload == null) {
                "globalThis.__walletkitCall($idLiteral,$methodLiteral,null)"
            } else {
                val payloadBase64 = Base64.encodeToString(payload.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
                val payloadLiteral = JSONObject.quote(payloadBase64)
                "globalThis.__walletkitCall($idLiteral,$methodLiteral, atob($payloadLiteral))"
            }
        evaluate(script, "walletkit-call.js")
        val response = deferred.await()
        return response.result
    }

    private suspend fun evaluate(script: String, filename: String = "walletkit-eval.js") {
        val quickJs = quickJsDeferred.await()
        withContext(jsDispatcher) {
            jsEvaluateMutex.withLock {
                quickJs.evaluate(script, filename)
            }
        }
    }

    private fun installNativeBindings(quickJs: QuickJs) {
        quickJs.set("WalletKitNative", NativeBridge::class.java, NativeBridge())
        quickJs.set("WalletKitConsole", NativeConsole::class.java, NativeConsole())
        quickJs.set("WalletKitBase64", NativeBase64::class.java, NativeBase64())
        quickJs.set("WalletKitCrypto", NativeCrypto::class.java, NativeCrypto())
        quickJs.set("WalletKitTimers", NativeTimers::class.java, NativeTimers())
        quickJs.set("WalletKitFetch", NativeFetch::class.java, NativeFetch())
    }

    private fun evaluateBootstrap(quickJs: QuickJs) {
        quickJs.evaluate(JS_BOOTSTRAP, "walletkit-bootstrap.js")
    }

    private fun loadBundle(quickJs: QuickJs) {
        val source = assetManager.open(assetPath).bufferedReader(Charsets.UTF_8).use { it.readText() }
        quickJs.evaluate(source, assetPath)
    }

    private fun handleReady() {
        if (!ready.isCompleted) {
            ready.complete(Unit)
            Log.d(logTag, "QuickJS bridge ready")
        }
    }

    private fun handleResponse(
        id: String,
        payload: JSONObject,
    ) {
        val deferred = pending.remove(id) ?: return
        val error = payload.optJSONObject("error")
        if (error != null) {
            val message = error.optString("message", "WalletKit bridge error")
            deferred.completeExceptionally(WalletKitBridgeException(message))
            return
        }
        val result = payload.opt("result")
        val data =
            when (result) {
                is JSONObject -> result
                is JSONArray -> JSONObject().put("items", result)
                null -> JSONObject()
                else -> JSONObject().put("value", result)
            }
        deferred.complete(BridgeResponse(data))
    }

    private fun handleEvent(event: JSONObject) {
        val type = event.optString("type", "unknown")
        val walletEvent = WalletKitEvent(type, event.optJSONObject("data") ?: JSONObject(), event)
        listeners.forEach { listener ->
            mainScope.launch { listener.onEvent(walletEvent) }
        }
    }

    private fun JSONObject.optNullableString(key: String): String? {
        val value = opt(key)
        return when (value) {
            null, JSONObject.NULL -> null
            else -> value.toString()
        }
    }

    private fun defaultTonClientEndpoint(network: String): String = if (network.equals("mainnet", ignoreCase = true)) {
        "https://toncenter.com/api/v2/jsonRPC"
    } else {
        "https://testnet.toncenter.com/api/v2/jsonRPC"
    }

    private fun defaultTonApiBase(network: String): String = if (network.equals("mainnet", ignoreCase = true)) {
        "https://tonapi.io"
    } else {
        "https://testnet.tonapi.io"
    }

    private inner class NativeBridge {
        fun postMessage(json: String) {
            try {
                val payload = JSONObject(json)
                when (payload.optString("kind")) {
                    "ready" -> handleReady()
                    "event" -> payload.optJSONObject("event")?.let { handleEvent(it) }
                    "response" -> handleResponse(payload.optString("id"), payload)
                }
            } catch (err: JSONException) {
                Log.e(logTag, "Malformed payload from QuickJS", err)
                pending.values.forEach { deferred ->
                    if (!deferred.isCompleted) {
                        deferred.completeExceptionally(
                            WalletKitBridgeException("Malformed payload: ${err.message}"),
                        )
                    }
                }
            }
        }
    }

    private inner class NativeConsole {
        fun log(level: String, message: String) {
            when (level.lowercase()) {
                "error" -> Log.e(logTag, message)
                "warn" -> Log.w(logTag, message)
                else -> Log.d(logTag, message)
            }
        }
    }

    private inner class NativeBase64 {
        fun encode(value: String): String {
            val bytes = value.toByteArray(Charsets.ISO_8859_1)
            return Base64.encodeToString(bytes, Base64.NO_WRAP)
        }

        fun decode(value: String): String {
            val bytes = Base64.decode(value, Base64.NO_WRAP)
            return String(bytes, Charsets.ISO_8859_1)
        }
    }

    private inner class NativeCrypto {
        fun randomBytes(length: Int): String {
            val buffer = ByteArray(length.coerceAtLeast(0))
            random.nextBytes(buffer)
            return Base64.encodeToString(buffer, Base64.NO_WRAP)
        }

        fun randomUuid(): String = UUID.randomUUID().toString()
    }

    private inner class NativeTimers {
        fun request(delayMs: Double, repeat: Boolean): Int {
            val id = timerIdGenerator.getAndIncrement()
            val delay =
                when {
                    delayMs.isNaN() || delayMs.isInfinite() -> 0L
                    delayMs < 0 -> 0L
                    else -> delayMs.toLong()
                }
            createTimer(id, delay, repeat)
            return id
        }

        fun clear(id: Int) {
            cancelTimer(id)
        }
    }

    private inner class NativeFetch {
        fun perform(requestJson: String): Int {
            val requestId = fetchIdGenerator.getAndIncrement()
            try {
                val json = JSONObject(requestJson)
                val url = json.optString("url")
                if (url.isNullOrBlank()) {
                    deliverFetchError(requestId, "Missing URL")
                    return requestId
                }
                val method = json.optString("method", "GET").ifBlank { "GET" }
                val headers = json.optJSONArray("headers")
                val headersBuilder = Headers.Builder()
                if (headers != null) {
                    for (index in 0 until headers.length()) {
                        val pair = headers.optJSONArray(index) ?: continue
                        val name = pair.optString(0)
                        val value = pair.optString(1)
                        if (name.isNotBlank()) {
                            headersBuilder.add(name, value)
                        }
                    }
                }
                val headersBuilt = headersBuilder.build()
                val bodyBase64 = json.optString("bodyBase64", "").takeIf { it.isNotEmpty() }
                val requestBody = createRequestBody(method.uppercase(), headersBuilt, bodyBase64)
                val request =
                    Request.Builder()
                        .url(url)
                        .headers(headersBuilt)
                        .method(method.uppercase(), requestBody)
                        .build()
                ioScope.launch {
                    executeFetch(requestId, request)
                }
            } catch (err: Throwable) {
                deliverFetchError(requestId, err.message ?: "Fetch request failed")
            }
            return requestId
        }

        fun abort(id: Int) {
            activeFetchCalls.remove(id)?.cancel()
            deliverFetchError(id, "Aborted")
        }
    }

    private fun createRequestBody(
        method: String,
        headers: Headers,
        bodyBase64: String?,
    ): RequestBody? {
        val requiresBody = method !in listOf("GET", "HEAD")
        if (bodyBase64 == null) {
            return if (requiresBody) {
                ByteArray(0).toRequestBody(null)
            } else {
                null
            }
        }
        val bodyBytes = Base64.decode(bodyBase64, Base64.NO_WRAP)
        val mediaType = headers["Content-Type"]?.toMediaTypeOrNull()
        return bodyBytes.toRequestBody(mediaType)
    }

    private suspend fun executeFetch(
        id: Int,
        request: Request,
    ) {
        val call = httpClient.newCall(request)
        activeFetchCalls[id] = call
        try {
            val response = suspendCancellableCall(call)
            val bodyBytes = response.body?.bytes()
            val base64Body =
                if (bodyBytes != null && bodyBytes.isNotEmpty()) {
                    Base64.encodeToString(bodyBytes, Base64.NO_WRAP)
                } else {
                    null
                }
            val headersArray = JSONArray()
            for ((name, value) in response.headers) {
                headersArray.put(JSONArray().put(name).put(value))
            }
            val meta =
                JSONObject().apply {
                    put("status", response.code)
                    put("statusText", response.message)
                    put("headers", headersArray)
                }
            deliverFetchSuccess(id, meta.toString(), base64Body)
            response.close()
        } catch (err: Throwable) {
            deliverFetchError(id, err.message ?: "Network error")
        } finally {
            activeFetchCalls.remove(id)
        }
    }

    private suspend fun suspendCancellableCall(call: Call): okhttp3.Response = suspendCancellableCoroutine { continuation: CancellableContinuation<okhttp3.Response> ->
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(
            object : okhttp3.Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isCancelled) {
                        return
                    }
                    continuation.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: okhttp3.Response) {
                    continuation.resume(response)
                }
            },
        )
    }

    private fun deliverFetchSuccess(
        id: Int,
        metaJson: String,
        bodyBase64: String?,
    ) {
        jsScope.launch {
            val metaLiteral = JSONObject.quote(metaJson)
            val bodyLiteral = if (bodyBase64 != null) JSONObject.quote(bodyBase64) else "null"
            val script =
                "globalThis.__walletkitResolveFetch($id,$metaLiteral,$bodyLiteral)"
            try {
                evaluate(script, "walletkit-fetch-success.js")
            } catch (err: Throwable) {
                Log.e(logTag, "Failed to deliver fetch success", err)
            }
        }
    }

    private fun deliverFetchError(
        id: Int,
        message: String,
    ) {
        jsScope.launch {
            val errorLiteral = JSONObject.quote(message)
            val script = "globalThis.__walletkitRejectFetch($id,$errorLiteral)"
            try {
                evaluate(script, "walletkit-fetch-error.js")
            } catch (err: Throwable) {
                Log.e(logTag, "Failed to deliver fetch error", err)
            }
        }
    }

    private fun createTimer(
        id: Int,
        delay: Long,
        repeat: Boolean,
    ) {
        lateinit var handle: TimerHandle
        val task = Runnable {
            jsScope.launch {
                try {
                    evaluate("globalThis.__walletkitRunTimer($id)", "walletkit-timer.js")
                } catch (err: Throwable) {
                    Log.e(logTag, "Timer $id execution failed", err)
                }
            }
            if (repeat) {
                rescheduleTimer(handle)
            } else {
                timers.remove(id)
            }
        }
        handle = TimerHandle(id, repeat, delay, task)
        handle.future = timerExecutor.schedule(task, delay.coerceAtLeast(0L), TimeUnit.MILLISECONDS)
        timers[id] = handle
    }

    private fun rescheduleTimer(handle: TimerHandle) {
        handle.future?.cancel(false)
        handle.future = timerExecutor.schedule(handle.task, handle.delayMillis, TimeUnit.MILLISECONDS)
    }

    private fun cancelTimer(id: Int) {
        timers.remove(id)?.future?.cancel(true)
    }

    private data class TimerHandle(
        val id: Int,
        val repeat: Boolean,
        val delayMillis: Long,
        val task: Runnable,
        @Volatile var future: ScheduledFuture<*>? = null,
    )

    private data class BridgeResponse(val result: JSONObject)

    companion object {
        private val JS_BOOTSTRAP: String =
            """
			(function(global) {
			  if (typeof global.window === 'undefined') global.window = global;
			  if (typeof global.self === 'undefined') global.self = global;
			  if (typeof global.global === 'undefined') global.global = global;

			  const consoleLevels = ['log', 'info', 'warn', 'error', 'debug'];
			  global.console = global.console || {};
			  consoleLevels.forEach(level => {
				global.console[level] = (...args) => {
				  const message = args.map(item => {
					try {
					  if (typeof item === 'string') return item;
					  return JSON.stringify(item);
					} catch (_) {
					  return String(item);
					}
				  }).join(' ');
				  WalletKitConsole.log(level, message);
				};
			  });
			  global.console.trace = global.console.trace || global.console.debug;

			  global.btoa = global.btoa || ((value) => WalletKitBase64.encode(value));
			  global.atob = global.atob || ((value) => WalletKitBase64.decode(value));

			  const timerCallbacks = new Map();
			  global.__walletkitRunTimer = (id) => {
				const entry = timerCallbacks.get(id);
				if (!entry) return;
				try {
				  entry.callback(...entry.args);
				} finally {
				  if (!entry.repeat) {
					timerCallbacks.delete(id);
				  }
				}
			  };
			  global.setTimeout = (cb, delay = 0, ...args) => {
				if (typeof cb !== 'function') throw new TypeError('setTimeout expects a function');
				const id = WalletKitTimers.request(delay, false);
				timerCallbacks.set(id, { callback: cb, args, repeat: false });
				return id;
			  };
			  global.clearTimeout = (id) => {
				if (timerCallbacks.delete(id)) WalletKitTimers.clear(id);
			  };
			  global.setInterval = (cb, delay = 0, ...args) => {
				if (typeof cb !== 'function') throw new TypeError('setInterval expects a function');
				const id = WalletKitTimers.request(delay, true);
				timerCallbacks.set(id, { callback: cb, args, repeat: true });
				return id;
			  };
			  global.clearInterval = (id) => {
				if (timerCallbacks.delete(id)) WalletKitTimers.clear(id);
			  };
			  global.queueMicrotask = global.queueMicrotask || (fn => Promise.resolve().then(fn));

			  const fetchResolvers = new Map();
			  global.__walletkitResolveFetch = (id, metaJson, bodyBase64) => {
				const entry = fetchResolvers.get(id);
				if (!entry) return;
				fetchResolvers.delete(id);
				if (!metaJson) {
				  entry.reject(new Error('Fetch failed'));
				  return;
				}
				const meta = JSON.parse(metaJson);
				const headers = new Map((meta.headers || []).map(([name, value]) => [String(name).toLowerCase(), String(value)]));
				const bodyString = bodyBase64 ? atob(bodyBase64) : '';
				const bodyBytes = new Uint8Array(bodyString.length);
				for (let i = 0; i < bodyString.length; i++) {
				  bodyBytes[i] = bodyString.charCodeAt(i) & 0xff;
				}
				const response = {
				  ok: meta.status >= 200 && meta.status < 300,
				  status: meta.status,
				  statusText: meta.statusText || '',
				  headers: {
					get(name) { return headers.get(String(name).toLowerCase()) ?? null; },
					has(name) { return headers.has(String(name).toLowerCase()); },
					forEach(callback) { headers.forEach((value, key) => callback(value, key)); },
					entries() { return headers.entries(); },
					[Symbol.iterator]() { return headers.entries(); },
				  },
				  clone() { return this; },
				  text: async () => bodyString,
				  json: async () => {
					if (!bodyString) return null;
					return JSON.parse(bodyString);
				  },
				  arrayBuffer: async () => bodyBytes.buffer.slice(bodyBytes.byteOffset, bodyBytes.byteOffset + bodyBytes.byteLength),
				};
				entry.resolve(response);
			  };
			  global.__walletkitRejectFetch = (id, message) => {
				const entry = fetchResolvers.get(id);
				if (!entry) return;
				fetchResolvers.delete(id);
				entry.reject(new Error(message || 'Fetch request failed'));
			  };

			  global.fetch = (input, init = {}) => {
				const request = { headers: [], method: 'GET' };
				if (typeof input === 'string') {
				  request.url = input;
				} else if (input && typeof input === 'object') {
				  request.url = input.url;
				  if (input.method) init.method = input.method;
				  if (input.headers && !init.headers) init.headers = input.headers;
				  if (input.body && init.body === undefined) init.body = input.body;
				}
				if (!request.url) throw new TypeError('fetch requires a URL');
				if (init.method) request.method = init.method;
				const headersInit = init.headers;
				if (headersInit) {
				  if (Array.isArray(headersInit)) {
					headersInit.forEach(([name, value]) => request.headers.push([String(name), String(value)]));
				  } else if (typeof headersInit.forEach === 'function') {
					headersInit.forEach((value, name) => request.headers.push([String(name), String(value)]));
				  } else {
					Object.entries(headersInit).forEach(([name, value]) => request.headers.push([String(name), String(value)]));
				  }
				}
				if (init.body != null) {
				  if (init.body instanceof Uint8Array || ArrayBuffer.isView(init.body)) {
					let binary = '';
					for (let i = 0; i < init.body.length; i++) {
					  binary += String.fromCharCode(init.body[i]);
					}
					request.bodyBase64 = btoa(binary);
				  } else if (init.body instanceof ArrayBuffer) {
					const view = new Uint8Array(init.body);
					let binary = '';
					for (let i = 0; i < view.length; i++) {
					  binary += String.fromCharCode(view[i]);
					}
					request.bodyBase64 = btoa(binary);
				  } else if (typeof init.body === 'string') {
					request.bodyBase64 = btoa(init.body);
				  } else {
					request.bodyBase64 = btoa(JSON.stringify(init.body));
					const hasContentType = request.headers.some(([name]) => name.toLowerCase() === 'content-type');
					if (!hasContentType) {
					  request.headers.push(['Content-Type', 'application/json']);
					}
				  }
				}
								const id = WalletKitFetch.perform(JSON.stringify(request));
								if (init.signal && typeof init.signal === 'object') {
									if (init.signal.aborted) {
										WalletKitFetch.abort(id);
										return Promise.reject(new Error('Aborted'));
									}
									if (typeof init.signal.addEventListener === 'function') {
										init.signal.addEventListener('abort', () => WalletKitFetch.abort(id));
									}
								}
								return new Promise((resolve, reject) => {
									fetchResolvers.set(id, { resolve, reject });
								});
			  };

			  if (!global.crypto) {
				global.crypto = {};
			  }
			  global.crypto.getRandomValues = (array) => {
				const length = array.length >>> 0;
				const base64 = WalletKitCrypto.randomBytes(length);
				const decoded = atob(base64);
				for (let i = 0; i < length && i < decoded.length; i++) {
				  array[i] = decoded.charCodeAt(i) & 0xff;
				}
				return array;
			  };
			  global.crypto.randomUUID = global.crypto.randomUUID || (() => WalletKitCrypto.randomUuid());

			})(typeof globalThis !== 'undefined' ? globalThis : this);
            """.trimIndent()

        private fun defaultHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .build()
    }
}
