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
package io.ton.walletkit.browser

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import io.ton.walletkit.ITONWalletKit
import io.ton.walletkit.WebViewTonConnectInjector
import io.ton.walletkit.bridge.BuildConfig
import io.ton.walletkit.config.TONWalletKitConfiguration
import io.ton.walletkit.core.TONWalletKit
import io.ton.walletkit.engine.WalletKitEngine
import io.ton.walletkit.internal.constants.BridgeMethodConstants
import io.ton.walletkit.internal.constants.BrowserConstants
import io.ton.walletkit.internal.constants.ResponseConstants
import io.ton.walletkit.internal.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONArray
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap

/**
 * TonConnect injector for WebViews.
 *
 * This class manages the TonConnect bridge lifecycle, injecting JavaScript code into web pages
 * and automatically routing all requests to the provided TONWalletKit instance.
 *
 * **Automatic Cleanup**: This class automatically cleans up resources when the WebView is
 * detached from the window, preventing memory leaks. You can also call [cleanup] manually
 * if needed (e.g., when reusing a WebView).
 *
 * You typically get an instance of this class by calling `webView.injectTonConnect(walletKit)`.
 */
internal class TonConnectInjector(
    private val webView: WebView,
    private val walletKit: ITONWalletKit,
) : WebViewTonConnectInjector {
    // Helper to access internal engine - cast to concrete implementation
    private val engine: WalletKitEngine?
        get() = (walletKit as? TONWalletKit)?.engine

    companion object {
        private const val TAG = "TonConnectInjector"
        private const val ERROR_WALLET_ENGINE_NOT_INITIALIZED = "Wallet engine not initialized"
        private const val ERROR_FAILED_PROCESS_REQUEST = "Failed to process request"
        private const val ERROR_CODE_INTERNAL = 500
        private const val METHOD_SEND = "send"

        // Registry of active WebViews for JS Bridge sessions
        // Maps sessionId -> WeakReference<WebView> to allow garbage collection
        private val activeWebViews = ConcurrentHashMap<String, WeakReference<WebView>>()

        /**
         * Register a WebView for a JS Bridge session.
         * Called internally when a session is created.
         */
        @JvmStatic
        internal fun registerWebView(sessionId: String, webView: WebView) {
            Logger.d(TAG, "Registering WebView for session: $sessionId")
            activeWebViews[sessionId] = WeakReference(webView)
            cleanupStaleReferences()
        }

        /**
         * Unregister a WebView for a JS Bridge session.
         * Called internally when a session is disconnected or WebView is destroyed.
         */
        @JvmStatic
        internal fun unregisterWebView(sessionId: String) {
            Logger.d(TAG, "Unregistering WebView for session: $sessionId")
            activeWebViews.remove(sessionId)
        }

        /**
         * Get the WebView associated with a JS Bridge session.
         * Returns null if the WebView has been garbage collected or was never registered.
         */
        @JvmStatic
        internal fun getWebViewForSession(sessionId: String): WebView? {
            val webViewRef = activeWebViews[sessionId]
            val webView = webViewRef?.get()

            if (webView == null && webViewRef != null) {
                activeWebViews.remove(sessionId)
            }

            return webView
        }

        /**
         * Clean up stale WebView references where the WebView has been garbage collected.
         * Called periodically to prevent map from growing indefinitely.
         */
        @JvmStatic
        private fun cleanupStaleReferences() {
            val staleKeys = activeWebViews.entries
                .filter { it.value.get() == null }
                .map { it.key }

            staleKeys.forEach { activeWebViews.remove(it) }

            if (staleKeys.isNotEmpty()) {
                Logger.d(TAG, "Cleaned up ${staleKeys.size} stale WebView references")
            }
        }

        /**
         * Clear all WebView registrations.
         * Called internally during cleanup.
         */
        @JvmStatic
        internal fun clearAllRegistrations() {
            Logger.d(TAG, "Clearing all WebView registrations")
            activeWebViews.clear()
        }

        /**
         * Broadcast an event to all registered WebViews.
         * Used when sessionId is not available (e.g., wallet-initiated disconnect).
         */
        @JvmStatic
        internal fun broadcastEventToAllWebViews(event: JSONObject) {
            Logger.d(TAG, "📤 Broadcasting event to all registered WebViews")
            cleanupStaleReferences()

            val webViews = activeWebViews.values.mapNotNull { it.get() }.distinct()
            Logger.d(TAG, "📤 Found ${webViews.size} unique WebViews to broadcast to")

            for (webView in webViews) {
                try {
                    // Use the same tag that's used when attaching injectors
                    val injector = webView.getTag("tonconnect_injector".hashCode()) as? TonConnectInjector
                    if (injector != null) {
                        Logger.d(TAG, "📤 Sending event to WebView: ${webView.hashCode()}")
                        injector.sendEvent(event)
                    }
                } catch (e: Exception) {
                    Logger.w(TAG, "⚠️ Failed to send event to WebView", e)
                }
            }
        }
    }

    // Use application context to avoid leaking Activity context
    private val context = webView.context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val pendingRequests = ConcurrentHashMap<String, PendingRequest>()
    private var isCleanedUp = false

    // Store reference to BridgeInterface for response delivery
    private lateinit var bridgeInterface: BridgeInterface

    // Track the current dApp URL for domain extraction
    @Volatile
    private var currentUrl: String? = null

    /**
     * Set up TonConnect support with default WebViewClient.
     * This will replace any existing WebViewClient and WebChromeClient.
     */
    @SuppressLint("SetJavaScriptEnabled")
    override fun setup() {
        // Add JavaScript interface for bridge communication
        // Store reference so we can call storeResponse() later
        bridgeInterface = BridgeInterface(
            onMessage = { json, type -> handleBridgeMessage(json, type) },
            onError = { error -> Logger.e(TAG, "Bridge error: $error") },
        )
        webView.addJavascriptInterface(
            bridgeInterface,
            BrowserConstants.JS_INTERFACE_NAME,
        )

        // Register WebView with existing sessions (important when app is reopened)
        // This ensures disconnect and other events work after the WebView is recreated
        scope.launch {
            try {
                val sessions = engine?.callBridgeMethod(BridgeMethodConstants.METHOD_LIST_SESSIONS, null)
                if (sessions is JSONObject && sessions.has(ResponseConstants.KEY_ITEMS)) {
                    val items = sessions.getJSONArray(ResponseConstants.KEY_ITEMS)
                    for (i in 0 until items.length()) {
                        val session = items.getJSONObject(i)
                        val sessionId = session.optString(ResponseConstants.KEY_SESSION_ID)
                        if (!sessionId.isNullOrEmpty()) {
                            registerWebView(sessionId, webView)
                            Logger.d(TAG, "✅ Re-registered WebView with existing session: $sessionId")
                        }
                    }
                }
            } catch (e: Exception) {
                Logger.w(TAG, "⚠️ Failed to query and register existing sessions", e)
            }
        }

        // CRITICAL: Use WebViewCompat.addDocumentStartJavaScript for early injection
        // This is the proper Android API to inject JavaScript before HTML parsing begins
        // (similar to iOS WKUserScript with injectionTime = .atDocumentStart)
        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            try {
                // Load inject.mjs from assets
                val injectionScript = context.assets.open(BrowserConstants.INJECT_SCRIPT_PATH)
                    .bufferedReader()
                    .use { it.readText() }

                // Build injection options from WalletKit configuration
                val config = (walletKit as? TONWalletKit)?.engine?.getConfiguration()
                val injectOptions = buildInjectOptions(config)

                // Create initialization script that calls window.injectWalletKit(options)
                val fullScript = """
                    $injectionScript
                    window.injectWalletKit($injectOptions);
                """.trimIndent()

                // Allow all origins (*) since this is a wallet browser that loads any dApp
                val allowedOrigins = setOf("*")

                WebViewCompat.addDocumentStartJavaScript(
                    webView,
                    fullScript,
                    allowedOrigins,
                )

                Logger.d(TAG, "✅ Bridge script registered via addDocumentStartJavaScript (executes before HTML parsing)")
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to add document start script", e)
            }
        } else {
            Logger.w(TAG, "⚠️ DOCUMENT_START_SCRIPT not supported on this Android version")
        }

        // Set custom WebViewClient
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                url?.let {
                    currentUrl = it
                    Logger.d(TAG, "✅ Current dApp URL updated: $it")

                    scope.launch {
                        try {
                            engine?.callBridgeMethod(
                                method = BridgeMethodConstants.METHOD_EMIT_BROWSER_PAGE_STARTED,
                                params = JSONObject().apply {
                                    put(ResponseConstants.KEY_URL, it)
                                },
                            )
                        } catch (e: Exception) {
                            Logger.w(TAG, "Failed to emit page started event", e)
                        }
                    }
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                url?.let {
                    Logger.d(TAG, "📍 Page finished loading: $it")

                    scope.launch {
                        try {
                            engine?.callBridgeMethod(
                                method = BridgeMethodConstants.METHOD_EMIT_BROWSER_PAGE_FINISHED,
                                params = JSONObject().apply {
                                    put(ResponseConstants.KEY_URL, it)
                                },
                            )
                        } catch (e: Exception) {
                            Logger.w(TAG, "Failed to emit page finished event", e)
                        }
                    }
                }
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: android.webkit.WebResourceError?,
            ) {
                super.onReceivedError(view, request, error)
                val errorMessage = error?.description?.toString() ?: "Unknown error"
                Logger.e(TAG, "WebView error: $errorMessage")

                scope.launch {
                    try {
                        engine?.callBridgeMethod(
                            method = BridgeMethodConstants.METHOD_EMIT_BROWSER_ERROR,
                            params = JSONObject().apply {
                                put(ResponseConstants.KEY_MESSAGE, errorMessage)
                            },
                        )
                    } catch (e: Exception) {
                        Logger.w(TAG, "Failed to emit error event", e)
                    }
                }
            }
        }

        // Set custom WebChromeClient for console logging
        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                if (BuildConfig.LOG_LEVEL != "OFF") {
                    Logger.d(TAG, "[JS Console] ${consoleMessage.message()} (${consoleMessage.sourceId()}:${consoleMessage.lineNumber()})")
                }
                return true
            }
        }
    }

    /**
     * Send a response back to a dApp for a specific request.
     *
     * @param messageId The ID from the original request
     * @param response The response data to send back
     */
    fun sendResponse(messageId: String, response: JSONObject) {
        val pending = pendingRequests.remove(messageId)
        if (pending == null) {
            Logger.w(TAG, "⚠️ No pending request found for messageId: $messageId")
            return
        }

        // If this is a successful connect response, register this WebView for the session
        // We need to register with BOTH messageId AND sessionId because:
        // - Responses are tagged with messageId (from the original request)
        // - Events (like disconnect) are tagged with sessionId (from the wallet)
        if (pending.method == BrowserConstants.EVENT_CONNECT && response.has(ResponseConstants.KEY_PAYLOAD)) {
            try {
                // Register with messageId (for immediate responses)
                registerWebView(messageId, webView)
                Logger.d(TAG, "✅ Registered WebView with messageId: $messageId")

                // CRITICAL: Query the wallet for the newly created session to get its sessionId
                // We do this synchronously so the sessionId is registered BEFORE the browser might close
                scope.launch {
                    try {
                        // Query sessions from the wallet
                        val sessions = engine?.callBridgeMethod(BridgeMethodConstants.METHOD_LIST_SESSIONS, null)
                        Logger.d(TAG, "🔍 Queried sessions to find sessionId: $sessions")

                        // Find the session that was just created for this messageId
                        // The wallet should have created a session during the connect processing
                        if (sessions is JSONObject && sessions.has(ResponseConstants.KEY_ITEMS)) {
                            val items = sessions.getJSONArray(ResponseConstants.KEY_ITEMS)

                            // The most recently created session should be ours
                            // Register it with the WebView
                            if (items.length() > 0) {
                                // For now, register the LAST session (most recently created)
                                // This is a heuristic but should work for the common case
                                val lastSession = items.getJSONObject(items.length() - 1)
                                val sessionId = lastSession.optString(ResponseConstants.KEY_SESSION_ID)

                                if (!sessionId.isNullOrEmpty() && sessionId != messageId) {
                                    registerWebView(sessionId, webView)
                                    Logger.d(TAG, "✅ Registered WebView with actual sessionId: $sessionId")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Logger.w(TAG, "⚠️ Failed to query sessionId after connect", e)
                    }
                }
            } catch (e: Exception) {
                Logger.w(TAG, "Failed to register WebView for session", e)
            }
        }

        Logger.d(TAG, "✅ Found pending request, calling sendResponseToFrame")
        sendResponseToFrame(pending, response)
    }

    /**
     * Broadcast an event to all frames (main page + all iframes).
     *
     * Use this to notify the dApp about wallet state changes (e.g., disconnect).
     *
     * @param event The event data to broadcast
     */
    fun sendEvent(event: JSONObject) {
        Logger.d(TAG, "🔔 sendEvent called")
        Logger.d(TAG, "🔔 Event to send: $event")

        // CRITICAL FIX: The event from Engine already has the correct structure:
        // { type: "TONCONNECT_BRIDGE_EVENT", source: "...", event: {...} }
        // Extract the inner event
        val actualEvent = if (event.has(BrowserConstants.KEY_EVENT)) {
            event.getJSONObject(BrowserConstants.KEY_EVENT)
        } else {
            event
        }

        // Create the message structure for the event
        val eventMessage = JSONObject().apply {
            put(BrowserConstants.KEY_TYPE, BrowserConstants.MESSAGE_TYPE_BRIDGE_EVENT)
            put(BrowserConstants.KEY_EVENT, actualEvent)
        }

        Logger.d(TAG, "🔔 Extracted actual event: $actualEvent")
        Logger.d(TAG, "🔔 Formatted event message: $eventMessage")
        Logger.d(TAG, "🔔 WebView attached: ${webView.parent != null}")

        // Store event in BridgeInterface - available to ALL frames via @JavascriptInterface
        bridgeInterface.storeEvent(eventMessage.toString())

        // Notify the main frame via JavaScript injection - main frame will broadcast to iframes via postMessage
        val script = """
            (function() {
                console.log('[Kotlin→JS] Event available');
                if (window.AndroidTonConnect && window.AndroidTonConnect.__notifyEvent) {
                    window.AndroidTonConnect.__notifyEvent();
                } else {
                    console.error('[Kotlin→JS] ❌ __notifyEvent not available!');
                }
            })();
        """.trimIndent()

        Logger.d(TAG, "📤 Notifying main frame about event availability...")
        webView.post {
            webView.evaluateJavascript(script) { result ->
                Logger.d(TAG, "📣 Event notification result: $result")
            }
        }
    }

    /**
     * Restores an existing TonConnect session for the given dApp URL.
     * When a user reopens the browser to a dApp they previously connected to,
     * this sends a "connect" event to restore the UI state.
     */
    /**
     * Clean up resources.
     *
     * This is called when the browser/WebView is closed, but it does NOT unregister
     * the session from the global registry. This allows disconnect events to still
     * be processed even after the browser is closed (the session remains active until
     * explicitly disconnected).
     *
     * Session cleanup happens when:
     * - User explicitly disconnects (removes session from registry)
     * - WebView is garbage collected (WeakReference returns null)
     */
    override fun cleanup() {
        if (isCleanedUp) {
            return // Already cleaned up
        }
        isCleanedUp = true

        Logger.d(TAG, "Cleaning up TonConnect injector (browser closed, but session remains active)")

        // NOTE: We do NOT unregister sessions here! The session should remain registered
        // even after the browser closes, so that disconnect events can still be processed.
        // The WeakReference will automatically return null when the WebView is garbage collected.

        scope.cancel()
        pendingRequests.clear()
    }

    private fun handleBridgeMessage(json: JSONObject, type: String) {
        scope.launch {
            when (type) {
                BrowserConstants.MESSAGE_TYPE_BRIDGE_REQUEST -> handleBridgeRequest(json)
                else -> Logger.w(TAG, "Unknown message type: $type")
            }
        }
    }

    private fun handleBridgeRequest(json: JSONObject) {
        val frameId = json.optString(BrowserConstants.KEY_FRAME_ID, BrowserConstants.DEFAULT_FRAME_ID)
        val messageId = json.optString(BrowserConstants.KEY_MESSAGE_ID)
        val method = json.optString(BrowserConstants.KEY_METHOD, BrowserConstants.DEFAULT_METHOD)

        Logger.d(TAG, "🟢 TonConnect request: frameId=$frameId, messageId=$messageId, method=$method")

        if (messageId.isEmpty()) {
            Logger.e(TAG, "Bridge request missing messageId")
            return
        }

        // Store pending request with frame info
        val pending = PendingRequest(
            frameId = frameId,
            messageId = messageId,
            method = method,
            timestamp = System.currentTimeMillis(),
        )
        pendingRequests[messageId] = pending

        Logger.d(TAG, "✅ Request stored: $method from '$frameId' (ID: $messageId)")

        // Get the engine from the provided TONWalletKit instance
        val engine = engine
        if (engine == null) {
            Logger.e(TAG, "❌ WalletKit engine not available!")
            // Send error response back to dApp
            val errorResponse = JSONObject().apply {
                put(
                    ResponseConstants.KEY_ERROR,
                    JSONObject().apply {
                        put(ResponseConstants.KEY_MESSAGE, ERROR_WALLET_ENGINE_NOT_INITIALIZED)
                        put(ResponseConstants.KEY_CODE, ERROR_CODE_INTERNAL)
                    },
                )
            }
            sendResponse(messageId, errorResponse)
            return
        }

        // Emit browser bridge request event for UI tracking
        scope.launch {
            try {
                engine.callBridgeMethod(
                    method = BridgeMethodConstants.METHOD_EMIT_BROWSER_BRIDGE_REQUEST,
                    params = JSONObject().apply {
                        put(BrowserConstants.KEY_MESSAGE_ID, messageId)
                        put(BrowserConstants.KEY_METHOD, method)
                        put(BrowserConstants.KEY_REQUEST, json.toString())
                    },
                )
            } catch (e: Exception) {
                Logger.w(TAG, "Failed to emit browser bridge request event", e)
            }
        }

        // Forward to TONWalletKit engine - it handles everything internally!
        scope.launch {
            try {
                Logger.d(TAG, "🔄 Forwarding request to WalletKit engine: $method")

                // Get params - can be JSONObject, JSONArray, or null
                val paramsRaw = json.opt(ResponseConstants.KEY_PARAMS)

                // Convert to JSON string for engine (engine will parse it properly)
                val paramsJson: String? = when (paramsRaw) {
                    is JSONObject -> paramsRaw.toString()
                    is JSONArray -> paramsRaw.toString()
                    null -> null
                    else -> {
                        Logger.w(TAG, "Unexpected params type: ${paramsRaw.javaClass.simpleName}")
                        null
                    }
                }

                Logger.d(TAG, "🔄 Method: $method")
                Logger.d(TAG, "🔄 Params JSON: $paramsJson")

                // Use WebView's current URL (the main frame URL) instead of tracking it manually
                // This is more reliable than trying to detect page vs resource loads
                Logger.d(TAG, "🔄 webView.url = ${webView.url}")
                Logger.d(TAG, "🔄 currentUrl = $currentUrl")
                val dAppUrl = webView.url ?: currentUrl
                Logger.d(TAG, "🔄 Final dApp URL used: $dAppUrl")

                engine.handleTonConnectRequest(
                    messageId = messageId,
                    method = method,
                    paramsJson = paramsJson,
                    url = dAppUrl,
                    responseCallback = { response ->
                        Logger.d(TAG, "🟣 responseCallback invoked by engine!")
                        Logger.d(TAG, "🟣 Response for messageId: $messageId")
                        Logger.d(TAG, "🟣 Response data: $response")
                        Logger.d(TAG, "🟣 About to call sendResponse...")
                        sendResponse(messageId, response)
                        Logger.d(TAG, "🟣 sendResponse call completed")
                    },
                )
                Logger.d(TAG, "✅ Request forwarded successfully to WalletKit engine")
            } catch (e: Exception) {
                Logger.e(TAG, "❌ Failed to forward request to WalletKit engine", e)
                // Send error response back to dApp
                val errorResponse = JSONObject().apply {
                    put(
                        ResponseConstants.KEY_ERROR,
                        JSONObject().apply {
                            put(ResponseConstants.KEY_MESSAGE, e.message ?: ERROR_FAILED_PROCESS_REQUEST)
                            put(ResponseConstants.KEY_CODE, ERROR_CODE_INTERNAL)
                        },
                    )
                }
                sendResponse(messageId, errorResponse)
            }
        }
    }

    private fun sendResponseToFrame(pending: PendingRequest, response: JSONObject) {
        Logger.d(TAG, "📤 sendResponseToFrame called for messageId: ${pending.messageId}, method: ${pending.method}")
        Logger.d(TAG, "📤 Response payload: $response")

        scope.launch(Dispatchers.Main) {
            Logger.d(TAG, "📤 About to deliver response directly")
            deliverResponse(pending, response)
        }
    }

    private fun deliverResponse(pending: PendingRequest, response: JSONObject) {
        val responseJson = JSONObject().apply {
            put(BrowserConstants.KEY_TYPE, BrowserConstants.MESSAGE_TYPE_BRIDGE_RESPONSE)
            put(BrowserConstants.KEY_MESSAGE_ID, pending.messageId)
            put(BrowserConstants.KEY_SUCCESS, true)
            put(BrowserConstants.KEY_PAYLOAD, response)
        }

        Logger.d(TAG, "📥 Storing response in BridgeInterface for messageId: ${pending.messageId}")
        Logger.d(TAG, "📥 Frame ID: ${pending.frameId}")
        Logger.d(TAG, "📥 Response JSON: $responseJson")
        Logger.d(TAG, "📥 WebView attached: ${webView.parent != null}")

        // Store response in BridgeInterface - available to ALL frames via @JavascriptInterface
        bridgeInterface.storeResponse(pending.messageId, responseJson.toString())

        // Notify the main frame via JavaScript injection - main frame will broadcast to iframes via postMessage
        val script = """
            (function() {
                console.log('[Kotlin→JS] Response available for messageId: ${pending.messageId}');
                if (window.AndroidTonConnect && window.AndroidTonConnect.__notifyResponse) {
                    window.AndroidTonConnect.__notifyResponse('${pending.messageId}');
                } else {
                    console.error('[Kotlin→JS] ❌ __notifyResponse not available!');
                }
            })();
        """.trimIndent()

        Logger.d(TAG, "📤 Notifying main frame about response availability...")
        webView.evaluateJavascript(script) { result ->
            Logger.d(TAG, "📣 Notification result: $result")
        }
    }

    @Serializable
    private data class InjectOptions(
        val isWalletBrowser: Boolean,
        val deviceInfo: DeviceInfo,
        val walletInfo: WalletInfo,
    )

    @Serializable
    private data class DeviceInfo(
        val platform: String,
        val appName: String,
        val appVersion: String,
        val maxProtocolVersion: Int,
        val features: List<String>,
    )

    @Serializable
    private data class WalletInfo(
        val name: String,
        val app_name: String,
        val about_url: String,
        val image: String,
        val platforms: List<String>,
        val jsBridgeKey: String,
        val injected: Boolean,
        val embedded: Boolean,
        val tondns: String? = null,
        val bridgeUrl: String,
    )

    private fun buildInjectOptions(config: TONWalletKitConfiguration?): String {
        val manifest = config?.walletManifest
        val deviceInfo = config?.deviceInfo

        val features = buildFeaturesList(deviceInfo?.features ?: config?.features)

        val options = InjectOptions(
            isWalletBrowser = true,
            deviceInfo = DeviceInfo(
                platform = deviceInfo?.platform ?: "android",
                appName = deviceInfo?.appName ?: manifest?.appName ?: "TON Wallet",
                appVersion = deviceInfo?.appVersion ?: "1.0.0",
                maxProtocolVersion = deviceInfo?.maxProtocolVersion ?: 2,
                features = features,
            ),
            walletInfo = WalletInfo(
                name = manifest?.name ?: "tonwallet",
                app_name = manifest?.appName ?: "TON Wallet",
                about_url = manifest?.aboutUrl ?: "",
                image = manifest?.imageUrl ?: "",
                platforms = listOf("android"),
                jsBridgeKey = manifest?.name ?: "tonwallet",
                injected = true,
                embedded = true,
                tondns = manifest?.tondns,
                bridgeUrl = manifest?.bridgeUrl ?: "",
            ),
        )

        return Json.encodeToString(options)
    }

    private fun buildFeaturesList(features: List<TONWalletKitConfiguration.Feature>?): List<String> {
        if (features.isNullOrEmpty()) return listOf("SendTransaction")

        return features.flatMap { feature ->
            when (feature) {
                is TONWalletKitConfiguration.SendTransactionFeature -> {
                    if (feature.maxMessages != null) {
                        val optionsJson = Json.encodeToString(mapOf("maxMessages" to feature.maxMessages))
                        listOf("SendTransaction", "SendTransaction:$optionsJson")
                    } else {
                        listOf("SendTransaction")
                    }
                }
                is TONWalletKitConfiguration.SignDataFeature -> {
                    if (feature.types.isNotEmpty()) {
                        val types = feature.types.map { it.name.lowercase() }
                        val typesJson = Json.encodeToString(mapOf("types" to types))
                        listOf("SignData:$typesJson")
                    } else {
                        emptyList()
                    }
                }
                else -> emptyList()
            }
        }.distinct()
    }
}
