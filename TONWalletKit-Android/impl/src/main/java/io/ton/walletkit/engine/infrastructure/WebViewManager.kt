/*
 * Copyright (c) 2025 TonTech
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.ton.walletkit.engine.infrastructure

import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewAssetLoader
import io.ton.walletkit.WalletKitBridgeException
import io.ton.walletkit.api.generated.TONDAppInfo
import io.ton.walletkit.api.generated.TONNetwork
import io.ton.walletkit.api.generated.TONRawStackItem
import io.ton.walletkit.api.isTestnet
import io.ton.walletkit.bridge.BuildConfig
import io.ton.walletkit.bridge.optString
import io.ton.walletkit.bridge.optStringOrNull
import io.ton.walletkit.bridge.transport.BridgeTransport
import io.ton.walletkit.bridge.transport.WebMessagePortBridgeTransport
import io.ton.walletkit.client.TONAPIClient
import io.ton.walletkit.config.SignDataType
import io.ton.walletkit.config.TONWalletKitConfiguration
import io.ton.walletkit.engine.state.AdapterManager
import io.ton.walletkit.internal.constants.JsonConstants
import io.ton.walletkit.internal.constants.LogConstants
import io.ton.walletkit.internal.constants.ResponseConstants
import io.ton.walletkit.internal.constants.WebViewConstants
import io.ton.walletkit.internal.util.Logger
import io.ton.walletkit.model.TONBase64
import io.ton.walletkit.model.TONUserFriendlyAddress
import io.ton.walletkit.session.SessionFilter
import io.ton.walletkit.session.TONConnectSessionManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

/**
 * Owns the WebView lifecycle, asset loading, and JavaScript bridge integration.
 *
 * @suppress Internal component. Use through [WebViewWalletKitEngine].
 */
internal class WebViewManager(
    context: Context,
    private val assetPath: String,
    private val storageManager: StorageManager,
    private val sessionManager: TONConnectSessionManager?,
    private val apiClients: Map<TONNetwork, TONAPIClient>,
    private val adapterManager: AdapterManager,
    private val json: Json,
    private val onMessage: (JsonObject) -> Unit,
    private val onBridgeError: (WalletKitBridgeException, String?) -> Unit,
) {
    private val appContext = context.applicationContext
    private val assetLoader =
        WebViewAssetLoader
            .Builder()
            .setDomain(WebViewConstants.ASSET_LOADER_DOMAIN)
            .addPathHandler(WebViewConstants.ASSET_LOADER_PATH, WebViewAssetLoader.AssetsPathHandler(appContext))
            .build()
    private val mainHandler = Handler(Looper.getMainLooper())
    private lateinit var webView: WebView

    val webViewInitialized = CompletableDeferred<Unit>()

    private lateinit var transportImpl: WebMessagePortBridgeTransport
    val transport: BridgeTransport
        get() = transportImpl

    init {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            initializeWebView()
        } else {
            mainHandler.post { initializeWebView() }
        }
    }

    fun getMainHandler(): Handler = mainHandler

    fun attachTo(parent: ViewGroup) {
        if (::webView.isInitialized && webView.parent !== parent) {
            (webView.parent as? ViewGroup)?.removeView(webView)
            parent.addView(webView)
        }
    }

    fun asView(): WebView = webView

    fun destroy() {
        if (!::webView.isInitialized) return
        if (::transportImpl.isInitialized) transportImpl.close()
        (webView.parent as? ViewGroup)?.removeView(webView)
        webView.removeJavascriptInterface(WebViewConstants.JS_INTERFACE_NAME)
        webView.stopLoading()
        webView.destroy()
    }

    private fun initializeWebView() {
        try {
            Logger.d(TAG, "Initializing WebView on thread: ${Thread.currentThread().name}")
            webView = WebView(appContext)
            WebView.setWebContentsDebuggingEnabled(BuildConfig.LOG_LEVEL != "OFF")
            webView.settings.javaScriptEnabled = true
            webView.settings.domStorageEnabled = true
            webView.settings.cacheMode = WebSettings.LOAD_NO_CACHE
            webView.settings.allowFileAccess = true
            webView.settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            webView.addJavascriptInterface(JsBinding(), WebViewConstants.JS_INTERFACE_NAME)

            transportImpl = WebMessagePortBridgeTransport(
                webView = webView,
                mainHandler = mainHandler,
                callbackHandler = mainHandler,
            )
            transportImpl.setOnMessage { jsonString ->
                try {
                    onMessage(json.parseToJsonElement(jsonString).jsonObject)
                } catch (err: SerializationException) {
                    Logger.e(TAG, "SerializationException: " + LogConstants.MSG_MALFORMED_PAYLOAD, err)
                    onBridgeError(
                        WalletKitBridgeException(LogConstants.ERROR_MALFORMED_PAYLOAD_PREFIX + err.message),
                        jsonString,
                    )
                }
            }

            webView.webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                    if (BuildConfig.LOG_LEVEL != "OFF") {
                        Logger.d(TAG, "[JS Console] ${consoleMessage.message()} (${consoleMessage.sourceId()}:${consoleMessage.lineNumber()})")
                    }
                    return true
                }
            }

            webView.webViewClient =
                object : WebViewClient() {
                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?,
                    ) {
                        super.onReceivedError(view, request, error)
                        val description = error?.description?.toString() ?: ResponseConstants.VALUE_UNKNOWN
                        val failingUrl = request?.url?.toString()
                        Logger.e(TAG, WebViewConstants.ERROR_WEBVIEW_LOAD_PREFIX + description + MSG_URL_SEPARATOR + (failingUrl ?: ResponseConstants.VALUE_UNKNOWN))
                        if (request?.isForMainFrame == true) {
                            val exception =
                                WalletKitBridgeException(
                                    WebViewConstants.ERROR_BUNDLE_LOAD_FAILED + MSG_OPEN_PAREN + description + MSG_CLOSE_PAREN_PERIOD_SPACE + WebViewConstants.BUILD_INSTRUCTION,
                                )
                            failBridgeFutures(exception)
                            onBridgeError(exception, null)
                        }
                    }

                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        Logger.d(TAG, "WebView page started loading: $url")
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        Logger.d(TAG, "WebView page finished loading: $url")

                        val logLevel = BuildConfig.LOG_LEVEL
                        view?.evaluateJavascript("window.__WALLETKIT_LOG_LEVEL__ = '$logLevel';") {
                            Logger.d(TAG, "Log level set: __WALLETKIT_LOG_LEVEL__ = $logLevel")
                        }

                        try {
                            transportImpl.handOffPortToJs()
                        } catch (err: Throwable) {
                            Logger.e(TAG, "Failed to hand off bridge port to JS", err)
                            val exception = WalletKitBridgeException(
                                "Failed to hand off bridge port: ${err.message}",
                            )
                            transportImpl.fail(exception)
                            onBridgeError(exception, null)
                        }
                    }

                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?,
                    ): WebResourceResponse? {
                        val url = request?.url ?: return super.shouldInterceptRequest(view, request)
                        Logger.d(TAG, "WebView intercepting request: $url")
                        return assetLoader.shouldInterceptRequest(url)
                            ?: super.shouldInterceptRequest(view, request)
                    }
                }
            val safeAssetPath = assetPath.trimStart('/')
            val fullUrl = WebViewConstants.URL_PREFIX_HTTPS + WebViewConstants.ASSET_LOADER_DOMAIN + WebViewConstants.ASSET_LOADER_PATH + safeAssetPath
            Logger.d(TAG, "Loading WebView URL: $fullUrl")
            webView.loadUrl(fullUrl)
            Logger.d(TAG, "WebView initialization completed, webViewInitialized completing")
            webViewInitialized.complete(Unit)
        } catch (e: Exception) {
            Logger.e(TAG, MSG_FAILED_INITIALIZE_WEBVIEW, e)
            webViewInitialized.completeExceptionally(e)
            if (::transportImpl.isInitialized) transportImpl.fail(e)
            onBridgeError(
                WalletKitBridgeException(
                    WebViewConstants.ERROR_BUNDLE_LOAD_FAILED + MSG_OPEN_PAREN + (e.message ?: ResponseConstants.VALUE_UNKNOWN) + MSG_CLOSE_PAREN_PERIOD_SPACE + WebViewConstants.BUILD_INSTRUCTION,
                ),
                null,
            )
        }
    }

    private fun failBridgeFutures(exception: WalletKitBridgeException) {
        if (::transportImpl.isInitialized) transportImpl.fail(exception)
    }

    private inner class JsBinding {
        @JavascriptInterface
        fun storageGet(key: String): String? {
            return runBlocking {
                storageManager.get(key)
            }
        }

        @JavascriptInterface
        fun storageSet(
            key: String,
            value: String,
        ) {
            runBlocking {
                storageManager.set(key, value)
            }
        }

        @JavascriptInterface
        fun storageRemove(key: String) {
            runBlocking {
                storageManager.remove(key)
            }
        }

        @JavascriptInterface
        fun storageClear() {
            runBlocking {
                storageManager.clear()
            }
        }

        @JavascriptInterface
        fun adapterCallSync(method: String, paramsJson: String): String = runBlocking {
            try {
                val params = json.parseToJsonElement(paramsJson).jsonObject
                if (method.startsWith("api.")) {
                    dispatchApi(method, params)
                } else {
                    withTimeout(1000) { dispatchAdapter(method, params) }
                }
            } catch (e: Exception) {
                Logger.e(TAG, "adapterCallSync($method) failed", e)
                throw e
            }
        }

        private fun dispatchAdapter(method: String, params: JsonObject): String {
            val adapterId = params.optString("adapterId")
            val adapter = adapterManager.getAdapter(adapterId)
                ?: throw IllegalArgumentException("Adapter not found: $adapterId")
            return when (method) {
                "getPublicKey" -> adapter.publicKey().value
                "getNetwork" -> buildJsonObject { put("chainId", adapter.network().chainId) }.toString()
                "getAddress" -> adapter.address(adapter.network().isTestnet).value
                "getWalletId" -> adapter.identifier()
                "getSupportedFeatures" -> {
                    val features = adapter.supportedFeatures() ?: return "null"
                    featuresToJson(features).toString()
                }
                else -> throw IllegalArgumentException("Unknown sync adapter method: $method")
            }
        }

        /**
         * Wire seam for the `api.*` namespace. Every param that maps to a typed value
         * class is validated here ([TONBase64.parse], [TONUserFriendlyAddress.parse]) so
         * bad input from JS surfaces as a structured BridgeError at the boundary instead
         * of leaking through to the http layer with no context.
         */
        private suspend fun dispatchApi(method: String, params: JsonObject): String {
            if (method == "api.getNetworks") return json.encodeToString(apiClients.keys.toList())
            val client = clientForParams(params)
            return when (method) {
                "api.sendBoc" -> client.sendBoc(TONBase64.parse(params.optString("boc")))
                "api.runGetMethod" -> {
                    val address = TONUserFriendlyAddress.parse(params.optString("address"))
                    val methodName = params.optString("method")
                    val stack = params["stack"]
                        ?.takeIf { it !is JsonNull }
                        ?.let { json.decodeFromJsonElement<List<TONRawStackItem>>(it) }
                    val seqno = (params["seqno"] as? JsonPrimitive)?.content?.toUIntOrNull()
                    json.encodeToString(client.runGetMethod(address, methodName, stack, seqno))
                }
                "api.getMasterchainInfo" -> json.encodeToString(client.getMasterchainInfo())
                else -> throw IllegalArgumentException("api method not implemented on host: $method")
            }
        }

        private fun clientForParams(params: JsonObject): TONAPIClient {
            val chainId = params.optString("chainId")
            return apiClients[TONNetwork(chainId)]
                ?: throw IllegalArgumentException("No API client configured for chainId=$chainId")
        }

        // ======== Session Manager Methods ========
        // These methods are only available when a custom session manager is configured.
        // The JS bridge checks hasSessionManager to determine if native session manager is available.

        @JavascriptInterface
        fun hasSessionManager(): Boolean = sessionManager != null

        @JavascriptInterface
        fun sessionCreate(
            sessionId: String,
            dAppInfoJson: String,
            walletId: String,
            walletAddress: String,
            isJsBridge: Boolean,
        ): String {
            val manager = sessionManager
                ?: throw IllegalStateException("Session manager not configured")

            return runBlocking {
                try {
                    Logger.d(TAG, "sessionCreate: sessionId=$sessionId, dAppInfo=$dAppInfoJson")

                    val dAppInfoObj = json.parseToJsonElement(dAppInfoJson).jsonObject
                    val dAppInfo = TONDAppInfo(
                        name = dAppInfoObj.optString("name", ""),
                        url = dAppInfoObj.optStringOrNull("url"),
                        iconUrl = dAppInfoObj.optStringOrNull("iconUrl"),
                        description = dAppInfoObj.optStringOrNull("description"),
                    )

                    val session = manager.createSession(
                        sessionId = sessionId,
                        dAppInfo = dAppInfo,
                        walletId = walletId,
                        walletAddress = walletAddress,
                        isJsBridge = isJsBridge,
                    )

                    json.encodeToString(session)
                } catch (e: Exception) {
                    Logger.e(TAG, "Failed to create session: $sessionId", e)
                    throw e
                }
            }
        }

        @JavascriptInterface
        fun sessionGet(sessionId: String): String? {
            val manager = sessionManager
                ?: throw IllegalStateException("Session manager not configured")

            return runBlocking {
                try {
                    Logger.d(TAG, "sessionGet: sessionId=$sessionId")
                    val session = manager.getSession(sessionId)
                    session?.let { json.encodeToString(it) }
                } catch (e: Exception) {
                    Logger.e(TAG, "Failed to get session: $sessionId", e)
                    null
                }
            }
        }

        @JavascriptInterface
        fun sessionGetFiltered(filterJson: String): String {
            val manager = sessionManager
                ?: throw IllegalStateException("Session manager not configured")

            return runBlocking {
                try {
                    val filter = parseSessionFilter(filterJson)
                    val sessions = manager.getSessions(filter)
                    json.encodeToString(sessions)
                } catch (e: Exception) {
                    Logger.e(TAG, "Failed to get filtered sessions", e)
                    "[]"
                }
            }
        }

        @JavascriptInterface
        fun sessionRemove(sessionId: String) {
            val manager = sessionManager
                ?: throw IllegalStateException("Session manager not configured")

            runBlocking {
                try {
                    manager.removeSession(sessionId)
                } catch (e: Exception) {
                    Logger.e(TAG, "Failed to remove session: $sessionId", e)
                }
            }
        }

        @JavascriptInterface
        fun sessionRemoveFiltered(filterJson: String) {
            val manager = sessionManager
                ?: throw IllegalStateException("Session manager not configured")

            runBlocking {
                try {
                    val filter = parseSessionFilter(filterJson)
                    manager.removeSessions(filter)
                } catch (e: Exception) {
                    Logger.e(TAG, "Failed to remove filtered sessions", e)
                }
            }
        }

        @JavascriptInterface
        fun sessionClear() {
            val manager = sessionManager
                ?: throw IllegalStateException("Session manager not configured")

            runBlocking {
                try {
                    Logger.d(TAG, "sessionClear")
                    manager.clearSessions()
                } catch (e: Exception) {
                    Logger.e(TAG, "Failed to clear sessions", e)
                }
            }
        }

        private fun parseSessionFilter(filterJson: String): SessionFilter? {
            return try {
                val jsonObj = json.parseToJsonElement(filterJson).jsonObject
                if (jsonObj.isEmpty()) {
                    null
                } else {
                    SessionFilter(
                        walletId = jsonObj.optStringOrNull("walletId"),
                        domain = jsonObj.optStringOrNull("domain"),
                        isJsBridge = (jsonObj["isJsBridge"] as? kotlinx.serialization.json.JsonPrimitive)?.let {
                            it.content.toBooleanStrictOrNull()
                        },
                    )
                }
            } catch (e: Exception) {
                Logger.w(TAG, "Failed to parse session filter: $filterJson", e)
                null
            }
        }

        private fun featuresToJson(features: List<TONWalletKitConfiguration.Feature>): JsonArray =
            buildJsonArray {
                for (feature in features) {
                    when (feature) {
                        is TONWalletKitConfiguration.SendTransactionFeature -> {
                            add(
                                buildJsonObject {
                                    put(JsonConstants.KEY_NAME, JsonConstants.FEATURE_SEND_TRANSACTION)
                                    feature.maxMessages?.let { put(JsonConstants.KEY_MAX_MESSAGES, it) }
                                    feature.extraCurrencySupported?.let { put("extraCurrencySupported", it) }
                                },
                            )
                        }
                        is TONWalletKitConfiguration.SignDataFeature -> {
                            add(
                                buildJsonObject {
                                    put(JsonConstants.KEY_NAME, JsonConstants.FEATURE_SIGN_DATA)
                                    put(
                                        JsonConstants.KEY_TYPES,
                                        buildJsonArray {
                                            for (type in feature.types) {
                                                add(
                                                    when (type) {
                                                        SignDataType.TEXT -> JsonConstants.VALUE_SIGN_DATA_TEXT
                                                        SignDataType.BINARY -> JsonConstants.VALUE_SIGN_DATA_BINARY
                                                        SignDataType.CELL -> JsonConstants.VALUE_SIGN_DATA_CELL
                                                    },
                                                )
                                            }
                                        },
                                    )
                                },
                            )
                        }
                    }
                }
            }
    }

    private companion object {
        private const val TAG = LogConstants.TAG_WEBVIEW_ENGINE
        private const val MSG_FAILED_INITIALIZE_WEBVIEW = "Failed to initialize WebView"
        private const val MSG_FAILED_EVALUATE_JS_BRIDGE = "Failed to evaluate JS bridge readiness"
        private const val MSG_URL_SEPARATOR = " url="
        private const val MSG_OPEN_PAREN = " ("
        private const val MSG_CLOSE_PAREN_PERIOD_SPACE = "). "
    }
}
