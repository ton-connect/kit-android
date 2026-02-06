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
import io.ton.walletkit.bridge.BuildConfig
import io.ton.walletkit.client.TONAPIClient
import io.ton.walletkit.internal.constants.LogConstants
import io.ton.walletkit.internal.constants.MiscConstants
import io.ton.walletkit.internal.constants.ResponseConstants
import io.ton.walletkit.internal.constants.WebViewConstants
import io.ton.walletkit.internal.util.Logger
import io.ton.walletkit.model.TONBase64
import io.ton.walletkit.model.TONUserFriendlyAddress
import io.ton.walletkit.session.SessionFilter
import io.ton.walletkit.session.TONConnectSessionManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONException
import org.json.JSONObject

/**
 * Owns the WebView lifecycle, asset loading, and JavaScript bridge integration.
 *
 * The manager mirrors the legacy inline implementation to ensure behaviour (including logging)
 * remains unchanged while allowing the rest of the engine to interact through a focused API.
 *
 * @suppress Internal component. Use through [WebViewWalletKitEngine].
 */
internal class WebViewManager(
    context: Context,
    private val assetPath: String,
    private val storageManager: StorageManager,
    private val signerManager: io.ton.walletkit.engine.state.SignerManager,
    private val sessionManager: TONConnectSessionManager?,
    private val apiClients: List<TONAPIClient>,
    private val json: Json,
    private val onMessage: (JSONObject) -> Unit,
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
    val bridgeLoaded = CompletableDeferred<Unit>()
    val jsBridgeReady = CompletableDeferred<Unit>()

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

    suspend fun executeJavaScript(script: String) {
        webViewInitialized.await()
        withContext(Dispatchers.Main) {
            webView.evaluateJavascript(script, null)
        }
    }

    fun destroy() {
        if (!::webView.isInitialized) return
        (webView.parent as? ViewGroup)?.removeView(webView)
        webView.removeJavascriptInterface(WebViewConstants.JS_INTERFACE_NAME)
        webView.stopLoading()
        webView.destroy()
    }

    fun startJsBridgeReadyPolling() {
        if (jsBridgeReady.isCompleted) {
            return
        }
        mainHandler.post { pollJsBridgeReady() }
    }

    fun markJsBridgeReady() {
        if (!jsBridgeReady.isCompleted) {
            jsBridgeReady.complete(Unit)
        }
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

            // Set WebChromeClient to suppress console logs in release builds
            webView.webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                    // Only show console logs when logging is enabled
                    if (BuildConfig.LOG_LEVEL != "OFF") {
                        Logger.d(TAG, "[JS Console] ${consoleMessage.message()} (${consoleMessage.sourceId()}:${consoleMessage.lineNumber()})")
                    }
                    return true // Suppress default logging
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

                    override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        Logger.d(TAG, "WebView page started loading: $url")
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        Logger.d(TAG, "WebView page finished loading: $url")

                        // Set log level for JavaScript bridge
                        // Controls granular logging in the bridge JavaScript bundle
                        // Levels: OFF, ERROR, WARN, INFO, DEBUG
                        // Release: WARN (errors + warnings), Debug: DEBUG (everything)
                        val logLevel = BuildConfig.LOG_LEVEL
                        view?.evaluateJavascript("window.__WALLETKIT_LOG_LEVEL__ = '$logLevel';") { result ->
                            Logger.d(TAG, "Log level set: __WALLETKIT_LOG_LEVEL__ = $logLevel")
                        }

                        if (!bridgeLoaded.isCompleted) {
                            bridgeLoaded.complete(Unit)
                        }
                        startJsBridgeReadyPolling()
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
            bridgeLoaded.completeExceptionally(e)
            jsBridgeReady.completeExceptionally(e)
            onBridgeError(
                WalletKitBridgeException(
                    WebViewConstants.ERROR_BUNDLE_LOAD_FAILED + MSG_OPEN_PAREN + (e.message ?: ResponseConstants.VALUE_UNKNOWN) + MSG_CLOSE_PAREN_PERIOD_SPACE + WebViewConstants.BUILD_INSTRUCTION,
                ),
                null,
            )
        }
    }

    private fun pollJsBridgeReady() {
        if (jsBridgeReady.isCompleted) {
            return
        }
        try {
            webView.evaluateJavascript(WebViewConstants.JS_BRIDGE_READY_CHECK) { result ->
                val normalized = result?.trim()?.trim('"') ?: MiscConstants.EMPTY_STRING
                if (normalized.equals(WebViewConstants.JS_BOOLEAN_TRUE, ignoreCase = true)) {
                    markJsBridgeReady()
                } else {
                    mainHandler.postDelayed({ pollJsBridgeReady() }, WebViewConstants.JS_BRIDGE_POLL_DELAY_MS)
                }
            }
        } catch (err: Throwable) {
            Logger.e(TAG, MSG_FAILED_EVALUATE_JS_BRIDGE, err)
            val safeMessage = err.message ?: ResponseConstants.VALUE_UNKNOWN
            val exception =
                WalletKitBridgeException(
                    WebViewConstants.ERROR_BUNDLE_LOAD_FAILED + MSG_OPEN_PAREN + safeMessage + MSG_CLOSE_PAREN_PERIOD_SPACE + WebViewConstants.BUILD_INSTRUCTION,
                )
            failBridgeFutures(exception)
            onBridgeError(exception, null)
        }
    }

    private fun failBridgeFutures(exception: WalletKitBridgeException) {
        if (!bridgeLoaded.isCompleted) {
            bridgeLoaded.completeExceptionally(exception)
        }
        if (!jsBridgeReady.isCompleted) {
            jsBridgeReady.completeExceptionally(exception)
        }
    }

    private inner class JsBinding {
        @JavascriptInterface
        fun postMessage(json: String) {
            try {
                val payload = JSONObject(json)
                onMessage(payload)
            } catch (err: JSONException) {
                Logger.e(TAG, "JSONException: " + LogConstants.MSG_MALFORMED_PAYLOAD, err)
                onBridgeError(WalletKitBridgeException(LogConstants.ERROR_MALFORMED_PAYLOAD_PREFIX + err.message), json)
            }
        }

        @JavascriptInterface
        fun storageGet(key: String): String? {
            return kotlinx.coroutines.runBlocking {
                storageManager.get(key)
            }
        }

        @JavascriptInterface
        fun storageSet(
            key: String,
            value: String,
        ) {
            kotlinx.coroutines.runBlocking {
                storageManager.set(key, value)
            }
        }

        @JavascriptInterface
        fun storageRemove(key: String) {
            kotlinx.coroutines.runBlocking {
                storageManager.remove(key)
            }
        }

        @JavascriptInterface
        fun storageClear() {
            kotlinx.coroutines.runBlocking {
                storageManager.clear()
            }
        }

        @JavascriptInterface
        fun signWithCustomSigner(
            signerId: String,
            bytesJson: String,
        ): String {
            return kotlinx.coroutines.runBlocking {
                try {
                    val signer =
                        signerManager.getSigner(signerId)
                            ?: throw IllegalArgumentException("Custom signer not found: $signerId")

                    val bytesArray = org.json.JSONArray(bytesJson)
                    val bytes = ByteArray(bytesArray.length()) { i -> bytesArray.getInt(i).toByte() }

                    val signatureHex = signer.sign(bytes)
                    // Return hex string with 0x prefix as expected by JavaScript
                    signatureHex.value
                } catch (e: Exception) {
                    Logger.e(TAG, "Failed to sign with custom signer: $signerId", e)
                    throw e
                }
            }
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

            return kotlinx.coroutines.runBlocking {
                try {
                    Logger.d(TAG, "sessionCreate: sessionId=$sessionId, dAppInfo=$dAppInfoJson")

                    val dAppInfoObj = JSONObject(dAppInfoJson)
                    val dAppInfo = TONDAppInfo(
                        name = dAppInfoObj.optString("name", ""),
                        url = dAppInfoObj.optNullableString("url"),
                        iconUrl = dAppInfoObj.optNullableString("iconUrl"),
                        description = dAppInfoObj.optNullableString("description"),
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

            return kotlinx.coroutines.runBlocking {
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

            return kotlinx.coroutines.runBlocking {
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
        fun sessionRemove(sessionId: String): String? {
            val manager = sessionManager
                ?: throw IllegalStateException("Session manager not configured")

            return kotlinx.coroutines.runBlocking {
                try {
                    val session = manager.removeSession(sessionId)
                    session?.let { json.encodeToString(it) }
                } catch (e: Exception) {
                    Logger.e(TAG, "Failed to remove session: $sessionId", e)
                    null
                }
            }
        }

        @JavascriptInterface
        fun sessionRemoveFiltered(filterJson: String): String {
            val manager = sessionManager
                ?: throw IllegalStateException("Session manager not configured")

            return kotlinx.coroutines.runBlocking {
                try {
                    val filter = parseSessionFilter(filterJson)
                    val sessions = manager.removeSessions(filter)
                    json.encodeToString(sessions)
                } catch (e: Exception) {
                    Logger.e(TAG, "Failed to remove filtered sessions", e)
                    "[]"
                }
            }
        }

        @JavascriptInterface

        fun sessionClear() {
            val manager = sessionManager
                ?: throw IllegalStateException("Session manager not configured")

            kotlinx.coroutines.runBlocking {
                try {
                    Logger.d(TAG, "sessionClear")
                    manager.clearSessions()
                } catch (e: Exception) {
                    Logger.e(TAG, "Failed to clear sessions", e)
                }
            }
        }

        // ======== API Client Methods ========
        // These methods are only available when custom API clients are configured.
        // The JS bridge checks for apiGetNetworks to determine if native API clients are available.

        @JavascriptInterface
        fun apiGetNetworks(): String {
            if (apiClients.isEmpty()) {
                return "[]"
            }

            val networks = apiClients.map { client ->
                json.encodeToString(client.network)
            }
            return "[${ networks.joinToString(",") }]"
        }

        @JavascriptInterface
        fun apiSendBoc(networkJson: String, boc: String): String {
            val network = json.decodeFromString<TONNetwork>(networkJson)
            val client = apiClients.find { it.network == network }
                ?: throw IllegalArgumentException("No API client configured for network: $network")

            return kotlinx.coroutines.runBlocking {
                try {
                    Logger.d(TAG, "apiSendBoc: network=$network")
                    client.sendBoc(TONBase64(boc))
                } catch (e: Exception) {
                    Logger.e(TAG, "Failed to send BOC: $network", e)
                    throw e
                }
            }
        }

        @JavascriptInterface
        fun apiRunGetMethod(
            networkJson: String,
            address: String,
            method: String,
            stackJson: String?,
            seqno: Int,
        ): String {
            val network = json.decodeFromString<TONNetwork>(networkJson)
            val client = apiClients.find { it.network == network }
                ?: throw IllegalArgumentException("No API client configured for network: $network")

            return kotlinx.coroutines.runBlocking {
                try {
                    Logger.d(TAG, "apiRunGetMethod: network=$network, address=$address, method=$method")
                    val stack = stackJson?.let { json.decodeFromString<List<TONRawStackItem>>(it) }
                    val seqnoArg = if (seqno == -1) null else seqno
                    val result = client.runGetMethod(TONUserFriendlyAddress(address), method, stack, seqnoArg)
                    json.encodeToString(result)
                } catch (e: Exception) {
                    Logger.e(TAG, "Failed to run get method: $method on $address", e)
                    throw e
                }
            }
        }

        @JavascriptInterface
        fun apiGetBalance(
            networkJson: String,
            address: String,
            seqno: Int,
        ): String {
            val network = json.decodeFromString<TONNetwork>(networkJson)
            val client = apiClients.find { it.network == network }
                ?: throw IllegalArgumentException("No API client configured for network: $network")

            return kotlinx.coroutines.runBlocking {
                try {
                    Logger.d(TAG, "apiGetBalance: network=$network, address=$address")
                    val seqnoArg = if (seqno == -1) null else seqno
                    client.getBalance(TONUserFriendlyAddress(address), seqnoArg)
                } catch (e: Exception) {
                    Logger.e(TAG, "Failed to get balance for: $address", e)
                    throw e
                }
            }
        }

        private fun JSONObject.optNullableString(key: String): String? {
            val value = opt(key)
            return when (value) {
                null, JSONObject.NULL -> null
                else -> value.toString()
            }
        }

        private fun parseSessionFilter(filterJson: String): SessionFilter? {
            return try {
                val jsonObj = org.json.JSONObject(filterJson)
                if (jsonObj.length() == 0) {
                    null
                } else {
                    SessionFilter(
                        walletId = jsonObj.optNullableString("walletId"),
                        domain = jsonObj.optNullableString("domain"),
                        isJsBridge = if (jsonObj.has("isJsBridge")) jsonObj.getBoolean("isJsBridge") else null,
                    )
                }
            } catch (e: Exception) {
                Logger.w(TAG, "Failed to parse session filter: $filterJson", e)
                null
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
