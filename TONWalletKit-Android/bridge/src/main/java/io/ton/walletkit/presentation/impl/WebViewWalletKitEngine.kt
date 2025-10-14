package io.ton.walletkit.presentation.impl

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewAssetLoader
import io.ton.walletkit.domain.model.DAppInfo
import io.ton.walletkit.domain.model.SignDataRequest
import io.ton.walletkit.domain.model.SignDataResult
import io.ton.walletkit.domain.model.Transaction
import io.ton.walletkit.domain.model.TransactionRequest
import io.ton.walletkit.domain.model.TransactionType
import io.ton.walletkit.domain.model.WalletAccount
import io.ton.walletkit.domain.model.WalletSession
import io.ton.walletkit.domain.model.WalletState
import io.ton.walletkit.presentation.WalletKitBridgeException
import io.ton.walletkit.presentation.WalletKitEngine
import io.ton.walletkit.presentation.WalletKitEngineKind
import io.ton.walletkit.presentation.config.WalletKitBridgeConfig
import io.ton.walletkit.presentation.event.ConnectRequestEvent
import io.ton.walletkit.presentation.event.SignDataRequestEvent
import io.ton.walletkit.presentation.event.TransactionRequestEvent
import io.ton.walletkit.presentation.event.WalletKitEvent
import io.ton.walletkit.presentation.listener.WalletKitEventHandler
import io.ton.walletkit.presentation.request.ConnectRequest
import io.ton.walletkit.presentation.storage.bridge.BridgeStorageAdapter
import io.ton.walletkit.presentation.storage.bridge.SecureBridgeStorageAdapter
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.Closeable
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet

/**
 * WebView-backed WalletKit engine. Hosts the WalletKit bundle inside a hidden WebView and uses the
 * established JS bridge to communicate with the Kotlin layer.
 *
 * **Persistent Storage**: By default, this engine persists wallet and session data
 * using secure encrypted storage. Data is automatically restored on app restart.
 * Storage can be disabled via config for testing or privacy-focused use cases.
 */
class WebViewWalletKitEngine(
    context: Context,
    private val assetPath: String = "walletkit/index.html",
) : WalletKitEngine {
    override val kind: WalletKitEngineKind = WalletKitEngineKind.WEBVIEW

    private val logTag = "WebViewWalletKitEngine"
    private val appContext = context.applicationContext

    // Json instance configured to ignore unknown keys (bridge may send extra fields)
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    // Secure storage adapter for the bridge (conditionally enabled based on config)
    private val storageAdapter: BridgeStorageAdapter = SecureBridgeStorageAdapter(appContext)

    // Whether persistent storage is enabled (set during init)
    @Volatile private var persistentStorageEnabled: Boolean = true

    private val assetLoader =
        WebViewAssetLoader
            .Builder()
            .setDomain(ASSET_LOADER_DOMAIN)
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(appContext))
            .build()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val webView: WebView = WebView(appContext)
    private val ready = CompletableDeferred<Unit>()
    private val pending = ConcurrentHashMap<String, CompletableDeferred<BridgeResponse>>()
    private val eventHandlers = CopyOnWriteArraySet<WalletKitEventHandler>()

    @Volatile private var currentNetwork: String = "testnet"

    @Volatile private var apiBaseUrl: String = "https://testnet.tonapi.io"

    @Volatile private var tonApiKey: String? = null

    // Auto-initialization state
    @Volatile private var isWalletKitInitialized = false
    private val walletKitInitMutex = Mutex()
    private var pendingInitConfig: WalletKitBridgeConfig? = null

    init {
        mainHandler.post {
            WebView.setWebContentsDebuggingEnabled(true)
            webView.settings.javaScriptEnabled = true
            webView.settings.domStorageEnabled = true
            webView.settings.cacheMode = WebSettings.LOAD_NO_CACHE
            webView.settings.allowFileAccess = true
            webView.addJavascriptInterface(JsBinding(), "WalletKitNative")
            webView.webViewClient =
                object : WebViewClient() {
                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?,
                    ) {
                        super.onReceivedError(view, request, error)
                        val description = error?.description?.toString() ?: "unknown"
                        val failingUrl = request?.url?.toString()
                        Log.e(logTag, "WebView load failed: $description url=$failingUrl")
                        if (request?.isForMainFrame == true && !ready.isCompleted) {
                            ready.completeExceptionally(
                                WalletKitBridgeException(
                                    "Failed to load WalletKit bundle ($description). Run `pnpm -w --filter androidkit build` and recompile.",
                                ),
                            )
                        }
                    }

                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?,
                    ): WebResourceResponse? {
                        val url = request?.url ?: return super.shouldInterceptRequest(view, request)
                        return assetLoader.shouldInterceptRequest(url)
                            ?: super.shouldInterceptRequest(view, request)
                    }
                }
            val safeAssetPath = assetPath.trimStart('/')
            webView.loadUrl("https://$ASSET_LOADER_DOMAIN/assets/$safeAssetPath")
            Log.d(logTag, "WebView bridge loading asset: $assetPath")
        }
    }

    /** Attach the WebView to a parent view so it can be inspected/debugged if needed. */
    fun attachTo(parent: ViewGroup) {
        if (webView.parent !== parent) {
            (webView.parent as? ViewGroup)?.removeView(webView)
            parent.addView(webView)
        }
    }

    fun asView(): WebView = webView

    override fun addEventHandler(handler: WalletKitEventHandler): Closeable {
        eventHandlers.add(handler)
        return Closeable { eventHandlers.remove(handler) }
    }

    /**
     * Ensures WalletKit is initialized. If not already initialized, performs initialization
     * with the provided config or defaults. This is called automatically by all public methods
     * that require initialization, enabling auto-init behavior.
     *
     * @param config Configuration to use for initialization if not already initialized
     */
    private suspend fun ensureWalletKitInitialized(config: WalletKitBridgeConfig = WalletKitBridgeConfig()) {
        // Fast path: already initialized
        if (isWalletKitInitialized) {
            return
        }

        walletKitInitMutex.withLock {
            // Double-check after acquiring lock
            if (isWalletKitInitialized) {
                return@withLock
            }

            Log.d(logTag, "Auto-initializing WalletKit with config: network=${config.network}")

            // Use pending config if init was called explicitly, otherwise use provided config
            val effectiveConfig = pendingInitConfig ?: config
            pendingInitConfig = null

            try {
                performInitialization(effectiveConfig)
                isWalletKitInitialized = true
                Log.d(logTag, "WalletKit auto-initialization completed successfully")
            } catch (err: Throwable) {
                Log.e(logTag, "WalletKit auto-initialization failed", err)
                throw WalletKitBridgeException(
                    "Failed to auto-initialize WalletKit: ${err.message}",
                )
            }
        }
    }

    /**
     * Performs the actual initialization by calling the JavaScript init method.
     */
    private suspend fun performInitialization(config: WalletKitBridgeConfig) {
        currentNetwork = config.network
        persistentStorageEnabled = config.enablePersistentStorage

        // Only use explicitly provided URLs; JS side handles defaults based on network
        val tonClientEndpoint = config.tonClientEndpoint?.ifBlank { null }
            ?: config.apiUrl?.ifBlank { null }
        apiBaseUrl = config.tonApiUrl?.ifBlank { null } ?: ""
        tonApiKey = config.apiKey

        // Get app version from PackageManager
        val appVersion = config.appVersion ?: try {
            val packageInfo = appContext.packageManager.getPackageInfo(appContext.packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            Log.w(logTag, "Failed to get app version, using default", e)
            "1.0.0"
        }

        // Get app name from config or use application label
        val appName = config.appName ?: try {
            val applicationInfo = appContext.applicationInfo
            val stringId = applicationInfo.labelRes
            if (stringId == 0) {
                applicationInfo.nonLocalizedLabel?.toString() ?: appContext.packageName
            } else {
                appContext.getString(stringId)
            }
        } catch (e: Exception) {
            Log.w(logTag, "Failed to get app name, using package name", e)
            appContext.packageName
        }

        val payload =
            JSONObject().apply {
                put("network", config.network)
                // Only include URLs if explicitly provided; JS will use defaults otherwise
                tonClientEndpoint?.let { put("apiUrl", it) }
                config.tonApiUrl?.let { put("tonApiUrl", it) }
                config.bridgeUrl?.let { put("bridgeUrl", it) }
                config.bridgeName?.let { put("bridgeName", it) }

                // Add walletManifest so dApp can recognize the wallet
                put(
                    "walletManifest",
                    JSONObject().apply {
                        put("name", appName)
                        put("appName", appName)
                        put("imageUrl", config.walletImageUrl ?: "https://wallet.ton.org/assets/ui/qr-logo.png")
                        put("aboutUrl", config.walletAboutUrl ?: "https://wallet.ton.org")
                        config.walletUniversalUrl?.let { put("universalUrl", it) }
                        put(
                            "platforms",
                            JSONArray().apply {
                                put("android")
                            },
                        )
                    },
                )

                // Add deviceInfo with SendTransaction and SignData features
                put(
                    "deviceInfo",
                    JSONObject().apply {
                        put("platform", "android")
                        put("appName", appName)
                        put("appVersion", appVersion)
                        put("maxProtocolVersion", 2)
                        put(
                            "features",
                            JSONArray().apply {
                                // Add SendTransaction feature (detailed form matching iOS)
                                put(
                                    JSONObject().apply {
                                        put("name", "SendTransaction")
                                        put("maxMessages", config.maxMessages)
                                    },
                                )
                                // Add SignData feature with supported types
                                put(
                                    JSONObject().apply {
                                        put("name", "SignData")
                                        put("types", JSONArray(config.signDataTypes))
                                    },
                                )
                            },
                        )
                    },
                )

                // Note: Persistent storage is controlled by enablePersistentStorage flag
                // When disabled, storage operations return immediately without persisting
            }

        Log.d(logTag, "Initializing WalletKit with persistent storage: $persistentStorageEnabled, app: $appName v$appVersion")
        call("init", payload)
    }

    override suspend fun init(config: WalletKitBridgeConfig) {
        // Store config for use during auto-init if this is called before any other method
        walletKitInitMutex.withLock {
            if (!isWalletKitInitialized) {
                pendingInitConfig = config
            }
        }

        // Ensure initialization happens with this config
        ensureWalletKitInitialized(config)
    }

    override suspend fun addWalletFromMnemonic(
        words: List<String>,
        name: String?,
        version: String,
        network: String?,
    ): WalletAccount {
        ensureWalletKitInitialized()
        val params =
            JSONObject().apply {
                put("words", JSONArray(words))
                put("version", version)
                network?.let { put("network", it) }
                name?.let { put("name", it) }
            }
        val result = call("addWalletFromMnemonic", params)

        // Parse the result into WalletAccount
        return WalletAccount(
            address = result.optString("address"),
            publicKey = result.optNullableString("publicKey"),
            name = result.optNullableString("name") ?: name,
            version = result.optString("version", version),
            network = result.optString("network", network ?: currentNetwork),
            index = result.optInt("index", 0),
        )
    }

    override suspend fun getWallets(): List<WalletAccount> {
        ensureWalletKitInitialized()
        val result = call("getWallets")
        val items = result.optJSONArray("items") ?: JSONArray()
        return buildList(items.length()) {
            for (index in 0 until items.length()) {
                val entry = items.optJSONObject(index) ?: continue
                add(
                    WalletAccount(
                        address = entry.optString("address"),
                        publicKey = entry.optNullableString("publicKey"),
                        name = entry.optNullableString("name"),
                        version = entry.optString("version", "unknown"),
                        network = entry.optString("network", currentNetwork),
                        index = entry.optInt("index", index),
                    ),
                )
            }
        }
    }

    override suspend fun removeWallet(address: String) {
        ensureWalletKitInitialized()
        val params = JSONObject().apply { put("address", address) }
        val result = call("removeWallet", params)
        Log.d(logTag, "removeWallet result: $result")

        // Check if removal was successful
        val removed = when {
            result.has("removed") -> result.optBoolean("removed", false)
            result.has("ok") -> result.optBoolean("ok", true)
            result.has("value") -> result.optBoolean("value", true)
            else -> true
        }

        if (!removed) {
            throw WalletKitBridgeException("Failed to remove wallet: $address")
        }
    }

    override suspend fun getWalletState(address: String): WalletState {
        ensureWalletKitInitialized()
        val params = JSONObject().apply { put("address", address) }
        val result = call("getWalletState", params)
        return WalletState(
            balance =
            when {
                result.has("balance") -> result.optString("balance")
                result.has("value") -> result.optString("value")
                else -> null
            },
            transactions = parseTransactions(result.optJSONArray("transactions")),
        )
    }

    override suspend fun getRecentTransactions(address: String, limit: Int): List<Transaction> {
        ensureWalletKitInitialized()
        val params = JSONObject().apply {
            put("address", address)
            put("limit", limit)
        }
        val result = call("getRecentTransactions", params)
        return parseTransactions(result.optJSONArray("items"))
    }

    /**
     * Parse JSONArray of transactions into typed Transaction list.
     * Filters out jetton/token transactions, only showing native TON transfers.
     */
    private fun parseTransactions(jsonArray: JSONArray?): List<Transaction> {
        if (jsonArray == null) return emptyList()

        return buildList(jsonArray.length()) {
            for (i in 0 until jsonArray.length()) {
                val txJson = jsonArray.optJSONObject(i) ?: continue

                // Get messages
                val inMsg = txJson.optJSONObject("in_msg")
                val outMsgs = txJson.optJSONArray("out_msgs")

                // Filter out jetton/token transactions
                // Jetton transactions have op_code in their messages (like 0xf8a7ea5 for transfer)
                // or have a message body/payload
                val isJettonOrTokenTx = when {
                    // Check incoming message for jetton markers
                    inMsg != null -> {
                        val opCode = inMsg.optString("op_code")?.takeIf { it.isNotEmpty() }
                        val body = inMsg.optString("body")?.takeIf { it.isNotEmpty() }
                        val message = inMsg.optString("message")?.takeIf { it.isNotEmpty() }
                        // Has op_code or has complex body (not just a comment)
                        opCode != null || (body != null && body != "te6ccgEBAQEAAgAAAA==") ||
                            (message != null && message.length > 200)
                    }
                    // Check outgoing messages for jetton markers
                    outMsgs != null && outMsgs.length() > 0 -> {
                        var hasJettonMarkers = false
                        for (j in 0 until outMsgs.length()) {
                            val msg = outMsgs.optJSONObject(j) ?: continue
                            val opCode = msg.optString("op_code")?.takeIf { it.isNotEmpty() }
                            val body = msg.optString("body")?.takeIf { it.isNotEmpty() }
                            val message = msg.optString("message")?.takeIf { it.isNotEmpty() }
                            if (opCode != null || (body != null && body != "te6ccgEBAQEAAgAAAA==") ||
                                (message != null && message.length > 200)
                            ) {
                                hasJettonMarkers = true
                                break
                            }
                        }
                        hasJettonMarkers
                    }
                    else -> false
                }

                // Skip non-TON transactions
                if (isJettonOrTokenTx) {
                    Log.d(logTag, "Skipping jetton/token transaction: ${txJson.optString("hash_hex", "unknown")}")
                    continue
                }

                // Determine transaction type based on incoming/outgoing value
                // Check if incoming message has value (meaning we received funds)
                val incomingValue = inMsg?.optString("value")?.toLongOrNull() ?: 0L
                val hasIncomingValue = incomingValue > 0

                // Check if we have outgoing messages with value
                var outgoingValue = 0L
                if (outMsgs != null) {
                    for (j in 0 until outMsgs.length()) {
                        val msg = outMsgs.optJSONObject(j)
                        val value = msg?.optString("value")?.toLongOrNull() ?: 0L
                        outgoingValue += value
                    }
                }
                val hasOutgoingValue = outgoingValue > 0

                // Transaction is INCOMING if we received value, OUTGOING if we only sent value
                // Note: Many incoming transactions also have outgoing messages (fees, change, etc.)
                val type = when {
                    hasIncomingValue -> TransactionType.INCOMING
                    hasOutgoingValue -> TransactionType.OUTGOING
                    else -> TransactionType.UNKNOWN
                }

                // Get amount based on transaction type (already calculated above)
                val amount = when (type) {
                    TransactionType.INCOMING -> incomingValue.toString()
                    TransactionType.OUTGOING -> outgoingValue.toString()
                    else -> "0"
                }

                // Get fee from total_fees field
                val fee = txJson.optString("total_fees")?.takeIf { it.isNotEmpty() }

                // Get comment from messages
                val comment = when (type) {
                    TransactionType.INCOMING -> inMsg?.optString("comment")?.takeIf { it.isNotEmpty() }
                    TransactionType.OUTGOING -> outMsgs?.optJSONObject(0)?.optString("comment")?.takeIf { it.isNotEmpty() }
                    else -> null
                }

                // Get sender - prefer friendly address
                val sender = if (type == TransactionType.INCOMING) {
                    inMsg?.optString("source_friendly")?.takeIf { it.isNotEmpty() }
                        ?: inMsg?.optString("source")
                } else {
                    null
                }

                // Get recipient - prefer friendly address
                val recipient = if (type == TransactionType.OUTGOING) {
                    outMsgs?.optJSONObject(0)?.let { msg ->
                        msg.optString("destination_friendly")?.takeIf { it.isNotEmpty() }
                            ?: msg.optString("destination")
                    }
                } else {
                    null
                }

                // Get hash - prefer hex format
                val hash = txJson.optString("hash_hex")?.takeIf { it.isNotEmpty() }
                    ?: txJson.optString("hash", "")

                // Get timestamp - use 'now' field and convert to milliseconds
                val timestamp = txJson.optLong("now", 0L) * 1000

                // Get logical time and block sequence number
                val lt = txJson.optString("lt")?.takeIf { it.isNotEmpty() }
                val blockSeqno = txJson.optInt("mc_block_seqno", -1).takeIf { it >= 0 }

                add(
                    Transaction(
                        hash = hash,
                        timestamp = timestamp,
                        amount = amount,
                        fee = fee,
                        comment = comment,
                        sender = sender,
                        recipient = recipient,
                        type = type,
                        lt = lt,
                        blockSeqno = blockSeqno,
                    ),
                )
            }
        }
    }

    override suspend fun handleTonConnectUrl(url: String) {
        ensureWalletKitInitialized()
        val params = JSONObject().apply { put("url", url) }
        call("handleTonConnectUrl", params)
    }

    override suspend fun sendTransaction(
        walletAddress: String,
        recipient: String,
        amount: String,
        comment: String?,
    ) {
        ensureWalletKitInitialized()
        val params =
            JSONObject().apply {
                put("walletAddress", walletAddress)
                put("toAddress", recipient)
                put("amount", amount)
                if (!comment.isNullOrBlank()) {
                    put("comment", comment)
                }
            }
        call("sendTransaction", params)
    }

    override suspend fun approveConnect(event: ConnectRequestEvent) {
        ensureWalletKitInitialized()
        val eventJson = JSONObject(json.encodeToString(event))
        val params =
            JSONObject().apply {
                put("event", eventJson)
                put("walletAddress", event.walletAddress ?: throw WalletKitBridgeException("walletAddress is required for connect approval"))
            }
        call("approveConnectRequest", params)
    }

    override suspend fun rejectConnect(
        event: ConnectRequestEvent,
        reason: String?,
    ) {
        ensureWalletKitInitialized()
        val eventJson = JSONObject(json.encodeToString(event))
        val params =
            JSONObject().apply {
                put("event", eventJson)
                reason?.let { put("reason", it) }
            }
        call("rejectConnectRequest", params)
    }

    override suspend fun approveTransaction(event: TransactionRequestEvent) {
        ensureWalletKitInitialized()
        val eventJson = JSONObject(json.encodeToString(event))
        val params = JSONObject().apply { put("event", eventJson) }
        call("approveTransactionRequest", params)
    }

    override suspend fun rejectTransaction(
        event: TransactionRequestEvent,
        reason: String?,
    ) {
        ensureWalletKitInitialized()
        val eventJson = JSONObject(json.encodeToString(event))
        val params =
            JSONObject().apply {
                put("event", eventJson)
                reason?.let { put("reason", it) }
            }
        call("rejectTransactionRequest", params)
    }

    override suspend fun approveSignData(event: SignDataRequestEvent): SignDataResult {
        ensureWalletKitInitialized()
        val eventJson = JSONObject(json.encodeToString(event))
        val params = JSONObject().apply { put("event", eventJson) }
        val result = call("approveSignDataRequest", params)

        Log.d(logTag, "approveSignData raw result: $result")

        // Extract signature from the response
        // The result might be nested in a 'result' object or directly accessible
        val signature = result.optNullableString("signature")
            ?: result.optJSONObject("result")?.optNullableString("signature")
            ?: result.optJSONObject("data")?.optNullableString("signature")

        if (signature.isNullOrEmpty()) {
            throw WalletKitBridgeException("No signature in approveSignData response: $result")
        }

        return SignDataResult(signature = signature)
    }

    override suspend fun rejectSignData(
        event: SignDataRequestEvent,
        reason: String?,
    ) {
        ensureWalletKitInitialized()
        val eventJson = JSONObject(json.encodeToString(event))
        val params =
            JSONObject().apply {
                put("event", eventJson)
                reason?.let { put("reason", it) }
            }
        call("rejectSignDataRequest", params)
    }

    override suspend fun listSessions(): List<WalletSession> {
        ensureWalletKitInitialized()
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

    override suspend fun disconnectSession(sessionId: String?) {
        ensureWalletKitInitialized()
        val params = JSONObject()
        sessionId?.let { params.put("sessionId", it) }
        call("disconnectSession", if (params.length() == 0) null else params)
    }

    override suspend fun destroy() {
        withContext(Dispatchers.Main) {
            (webView.parent as? ViewGroup)?.removeView(webView)
            webView.removeJavascriptInterface("WalletKitNative")
            webView.stopLoading()
            webView.destroy()
        }
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
                "window.__walletkitCall($idLiteral,$methodLiteral,null)"
            } else {
                val payloadBase64 = Base64.encodeToString(payload.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
                val payloadLiteral = JSONObject.quote(payloadBase64)
                "window.__walletkitCall($idLiteral,$methodLiteral, atob($payloadLiteral))"
            }

        withContext(Dispatchers.Main) {
            webView.evaluateJavascript(script, null)
        }

        val response = deferred.await()
        Log.d(logTag, "call[$method] completed")
        return response.result
    }

    private fun handleResponse(
        id: String,
        response: JSONObject,
    ) {
        val deferred = pending.remove(id) ?: return
        val error = response.optJSONObject("error")
        if (error != null) {
            val message = error.optString("message", "WalletKit bridge error")
            Log.e(logTag, "call[$id] failed: $message")
            deferred.completeExceptionally(WalletKitBridgeException(message))
            return
        }
        val result = response.opt("result")
        val payload =
            when (result) {
                is JSONObject -> result
                is JSONArray -> JSONObject().put("items", result)
                null -> JSONObject()
                else -> JSONObject().put("value", result)
            }
        deferred.complete(BridgeResponse(payload))
    }

    private fun handleEvent(event: JSONObject) {
        val type = event.optString("type", "unknown")
        val data = event.optJSONObject("data") ?: JSONObject()
        Log.d(logTag, "event[$type]")

        // Typed event handlers (sealed class)
        val typedEvent = parseTypedEvent(type, data, event)
        if (typedEvent != null) {
            eventHandlers.forEach { handler ->
                mainHandler.post { handler.handleEvent(typedEvent) }
            }
        }
    }

    private fun parseTypedEvent(type: String, data: JSONObject, raw: JSONObject): WalletKitEvent? {
        return when (type) {
            "connectRequest" -> {
                try {
                    // Deserialize JSON into typed event
                    val event = json.decodeFromString<ConnectRequestEvent>(data.toString())
                    val requestId = event.id
                    val dAppInfo = parseDAppInfo(data) // Keep existing parser for now
                    val permissions = event.preview?.permissions ?: emptyList()
                    val request = ConnectRequest(
                        requestId = requestId,
                        dAppInfo = dAppInfo,
                        permissions = permissions,
                        event = event,
                        engine = this,
                    )
                    WalletKitEvent.ConnectRequestEvent(request)
                } catch (e: Exception) {
                    Log.e(logTag, "Failed to parse ConnectRequestEvent", e)
                    null
                }
            }

            "transactionRequest" -> {
                try {
                    // Deserialize JSON into typed event
                    val event = json.decodeFromString<TransactionRequestEvent>(data.toString())
                    val requestId = event.id ?: return null
                    val dAppInfo = parseDAppInfo(data) // Keep existing parser for now
                    val txRequest = parseTransactionRequest(data) // Keep existing parser for now

                    // Extract preview data if available
                    val preview = data.optJSONObject("preview")?.toString()

                    val request = io.ton.walletkit.presentation.request.TransactionRequest(
                        requestId = requestId,
                        dAppInfo = dAppInfo,
                        request = txRequest,
                        preview = preview,
                        event = event,
                        engine = this,
                    )
                    WalletKitEvent.TransactionRequestEvent(request)
                } catch (e: Exception) {
                    Log.e(logTag, "Failed to parse TransactionRequestEvent", e)
                    null
                }
            }

            "signDataRequest" -> {
                try {
                    // Deserialize JSON into typed event
                    val event = json.decodeFromString<SignDataRequestEvent>(data.toString())
                    val requestId = event.id ?: return null
                    val dAppInfo = parseDAppInfo(data) // Keep existing parser for now
                    val signRequest = parseSignDataRequest(data) // Keep existing parser for now
                    val request = io.ton.walletkit.presentation.request.SignDataRequest(
                        requestId = requestId,
                        dAppInfo = dAppInfo,
                        request = signRequest,
                        event = event,
                        engine = this,
                    )
                    WalletKitEvent.SignDataRequestEvent(request)
                } catch (e: Exception) {
                    Log.e(logTag, "Failed to parse SignDataRequestEvent", e)
                    null
                }
            }

            "disconnect" -> {
                val sessionId = data.optNullableString("sessionId")
                    ?: data.optNullableString("id")
                    ?: return null
                WalletKitEvent.DisconnectEvent(sessionId)
            }

            "stateChanged", "walletStateChanged" -> {
                val address = data.optNullableString("address")
                    ?: data.optJSONObject("wallet")?.optNullableString("address")
                    ?: return null
                WalletKitEvent.StateChangedEvent(address)
            }

            "sessionsChanged" -> {
                WalletKitEvent.SessionsChangedEvent
            }

            else -> null // Unknown event type
        }
    }

    private fun parseDAppInfo(data: JSONObject): DAppInfo? {
        // Try to get dApp name from multiple sources
        val dAppName = data.optNullableString("dAppName")
            ?: data.optJSONObject("manifest")?.optNullableString("name")
            ?: data.optJSONObject("preview")?.optJSONObject("manifest")?.optNullableString("name")

        // Try to get URLs from multiple sources
        val manifest = data.optJSONObject("preview")?.optJSONObject("manifest")
            ?: data.optJSONObject("manifest")

        val dAppUrl = data.optNullableString("dAppUrl")
            ?: manifest?.optNullableString("url")
            ?: ""

        val iconUrl = data.optNullableString("dAppIconUrl")
            ?: manifest?.optNullableString("iconUrl")

        val manifestUrl = data.optNullableString("manifestUrl")
            ?: manifest?.optNullableString("url")

        // Only return null if we have absolutely no dApp information
        if (dAppName == null && dAppUrl.isEmpty()) {
            return null
        }

        return DAppInfo(
            name = dAppName ?: dAppUrl.takeIf { it.isNotEmpty() } ?: "Unknown dApp",
            url = dAppUrl,
            iconUrl = iconUrl,
            manifestUrl = manifestUrl,
        )
    }

    private fun parsePermissions(data: JSONObject): List<String> {
        val permissions = data.optJSONArray("permissions") ?: return emptyList()
        return List(permissions.length()) { i ->
            permissions.optString(i)
        }
    }

    private fun parseTransactionRequest(data: JSONObject): TransactionRequest {
        // Check if data is nested under "request" field
        val requestData = data.optJSONObject("request") ?: data

        // Try to parse from messages array first (TON Connect format)
        val messages = requestData.optJSONArray("messages")
        if (messages != null && messages.length() > 0) {
            val firstMessage = messages.optJSONObject(0)
            if (firstMessage != null) {
                return TransactionRequest(
                    recipient = firstMessage.optNullableString("address")
                        ?: firstMessage.optNullableString("to")
                        ?: "",
                    amount = firstMessage.optNullableString("amount")
                        ?: firstMessage.optNullableString("value")
                        ?: "0",
                    comment = firstMessage.optNullableString("comment")
                        ?: firstMessage.optNullableString("text"),
                    payload = firstMessage.optNullableString("payload"),
                )
            }
        }

        // Fallback to direct fields (legacy format or direct send)
        return TransactionRequest(
            recipient = requestData.optNullableString("to") ?: requestData.optNullableString("recipient") ?: "",
            amount = requestData.optNullableString("amount") ?: requestData.optNullableString("value") ?: "0",
            comment = requestData.optNullableString("comment") ?: requestData.optNullableString("text"),
            payload = requestData.optNullableString("payload"),
        )
    }

    private fun parseSignDataRequest(data: JSONObject): SignDataRequest {
        // Parse params array - params[0] contains stringified JSON with schema_crc and payload
        var payload = data.optNullableString("payload") ?: data.optNullableString("data") ?: ""
        var schema: String? = data.optNullableString("schema")

        // Check if params array exists (newer format from bridge)
        val paramsArray = data.optJSONArray("params")
        if (paramsArray != null && paramsArray.length() > 0) {
            val paramsString = paramsArray.optString(0)
            if (paramsString.isNotEmpty()) {
                try {
                    val paramsObj = JSONObject(paramsString)
                    payload = paramsObj.optNullableString("payload") ?: payload

                    // Convert schema_crc to human-readable schema type
                    val schemaCrc = paramsObj.optInt("schema_crc", -1)
                    schema = when (schemaCrc) {
                        0 -> "text"
                        1 -> "binary"
                        2 -> "cell"
                        else -> schema
                    }
                } catch (e: Exception) {
                    Log.e(logTag, "Failed to parse params for sign data", e)
                }
            }
        }

        return SignDataRequest(
            payload = payload,
            schema = schema,
        )
    }

    private fun handleReady(payload: JSONObject) {
        payload.optNullableString("network")?.let { currentNetwork = it }
        payload.optNullableString("tonApiUrl")?.let { apiBaseUrl = it }
        if (!ready.isCompleted) {
            Log.d(logTag, "bridge ready")
            ready.complete(Unit)
        }
        val data = JSONObject()
        val keys = payload.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            if (key == "kind") continue
            if (payload.isNull(key)) {
                data.put(key, JSONObject.NULL)
            } else {
                data.put(key, payload.get(key))
            }
        }
        val readyEvent = JSONObject().apply {
            put("type", "ready")
            put("data", data)
        }
        handleEvent(readyEvent)
    }

    private fun JSONObject.optNullableString(key: String): String? {
        val value = opt(key)
        return when (value) {
            null, JSONObject.NULL -> null
            else -> value.toString()
        }
    }

    private inner class JsBinding {
        @JavascriptInterface
        fun postMessage(json: String) {
            try {
                val payload = JSONObject(json)
                when (payload.optString("kind")) {
                    "ready" -> handleReady(payload)
                    "event" -> payload.optJSONObject("event")?.let { handleEvent(it) }
                    "response" -> handleResponse(payload.optString("id"), payload)
                }
            } catch (err: JSONException) {
                Log.e(logTag, "Malformed payload from JS", err)
                pending.values.forEach { deferred ->
                    if (!deferred.isCompleted) {
                        deferred.completeExceptionally(WalletKitBridgeException("Malformed payload: ${err.message}"))
                    }
                }
            }
        }

        /**
         * Storage adapter methods called by JavaScript bundle to persist data.
         * These methods enable the JS bundle to use Android secure storage instead of
         * ephemeral WebView LocalStorage.
         *
         * If persistent storage is disabled, these methods become no-ops (return null/empty).
         */
        @JavascriptInterface
        fun storageGet(key: String): String? {
            if (!persistentStorageEnabled) {
                return null // Return null when storage is disabled
            }

            return try {
                // Note: This is synchronous from JS perspective but async in Kotlin
                // We use runBlocking here as JavascriptInterface requires synchronous return
                runBlocking {
                    storageAdapter.get(key)
                }
            } catch (e: Exception) {
                Log.e(logTag, "Storage get failed for key: $key", e)
                null
            }
        }

        @JavascriptInterface
        fun storageSet(
            key: String,
            value: String,
        ) {
            if (!persistentStorageEnabled) {
                return // No-op when storage is disabled
            }

            try {
                runBlocking {
                    storageAdapter.set(key, value)
                }
            } catch (e: Exception) {
                Log.e(logTag, "Storage set failed for key: $key", e)
            }
        }

        @JavascriptInterface
        fun storageRemove(key: String) {
            if (!persistentStorageEnabled) {
                return // No-op when storage is disabled
            }

            try {
                runBlocking {
                    storageAdapter.remove(key)
                }
            } catch (e: Exception) {
                Log.e(logTag, "Storage remove failed for key: $key", e)
            }
        }

        @JavascriptInterface
        fun storageClear() {
            if (!persistentStorageEnabled) {
                return // No-op when storage is disabled
            }

            try {
                runBlocking {
                    storageAdapter.clear()
                }
            } catch (e: Exception) {
                Log.e(logTag, "Storage clear failed", e)
            }
        }
    }

    override suspend fun injectSignDataRequest(requestData: JSONObject): JSONObject {
        ensureWalletKitInitialized()
        return call("injectSignDataRequest", requestData)
    }

    private data class BridgeResponse(
        val result: JSONObject,
    )

    private companion object {
        private const val ASSET_LOADER_DOMAIN = "appassets.androidplatform.net"
    }
}

/** Legacy alias retained for callers that still reference the old class name. */
typealias WalletKitBridge = WebViewWalletKitEngine
