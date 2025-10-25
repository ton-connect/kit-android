package io.ton.walletkit.presentation.browser.internal

import android.annotation.SuppressLint
import android.util.Log
import android.view.View
import android.webkit.WebView
import io.ton.walletkit.data.browser.PendingRequest
import io.ton.walletkit.domain.constants.BrowserConstants
import io.ton.walletkit.domain.constants.ResponseConstants
import io.ton.walletkit.presentation.TONWalletKit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
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
    private val walletKit: TONWalletKit,
) {
    companion object {
        private const val TAG = "TonConnectInjector"
        
        // Registry of active WebViews for JS Bridge sessions
        // Maps sessionId -> WeakReference<WebView> to allow garbage collection
        private val activeWebViews = ConcurrentHashMap<String, WeakReference<WebView>>()
        
        /**
         * Register a WebView for a JS Bridge session.
         * Called internally when a session is created.
         */
        @JvmStatic
        internal fun registerWebView(sessionId: String, webView: WebView) {
            Log.d(TAG, "Registering WebView for session: $sessionId")
            activeWebViews[sessionId] = WeakReference(webView)
            cleanupStaleReferences()
        }
        
        /**
         * Unregister a WebView for a JS Bridge session.
         * Called internally when a session is disconnected or WebView is destroyed.
         */
        @JvmStatic
        internal fun unregisterWebView(sessionId: String) {
            Log.d(TAG, "Unregistering WebView for session: $sessionId")
            activeWebViews.remove(sessionId)
        }
        
        /**
         * Get the WebView associated with a JS Bridge session.
         * Returns null if the WebView has been garbage collected or was never registered.
         */
        @JvmStatic
        internal fun getWebViewForSession(sessionId: String): WebView? {
            Log.d(TAG, "🔍 getWebViewForSession called for: $sessionId")
            Log.d(TAG, "🔍 Total registered sessions: ${activeWebViews.size}")
            Log.d(TAG, "🔍 Registered session IDs: ${activeWebViews.keys.joinToString(", ")}")
            
            val webViewRef = activeWebViews[sessionId]
            val webView = webViewRef?.get()
            
            Log.d(TAG, "🔍 WebView reference found: ${webViewRef != null}")
            Log.d(TAG, "🔍 WebView still alive: ${webView != null}")
            
            // Clean up stale reference if WebView was garbage collected
            if (webView == null && webViewRef != null) {
                activeWebViews.remove(sessionId)
                Log.d(TAG, "Removed stale WebView reference for session: $sessionId")
            }
            
            return webView?.also {
                Log.d(TAG, "✅ Found WebView for session: $sessionId")
            }
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
                Log.d(TAG, "Cleaned up ${staleKeys.size} stale WebView references")
            }
        }
        
        /**
         * Clear all WebView registrations.
         * Called internally during cleanup.
         */
        @JvmStatic
        internal fun clearAllRegistrations() {
            Log.d(TAG, "Clearing all WebView registrations")
            activeWebViews.clear()
        }
    }
    // Use application context to avoid leaking Activity context
    private val context = webView.context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val pendingRequests = ConcurrentHashMap<String, PendingRequest>()
    private var isCleanedUp = false

    // Listener to automatically cleanup when WebView is detached
    // NOTE: Disabled automatic cleanup on detach because WebView may be temporarily
    // detached when showing Connect/Transaction sheets. Cleanup is now manual via
    // BrowserSheet's DisposableEffect or when the parent screen is destroyed.
    private val detachListener = object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {
            Log.d(TAG, "WebView attached to window")
        }

        override fun onViewDetachedFromWindow(v: View) {
            Log.d(TAG, "WebView detached from window (NOT cleaning up - may be temporary)")
            // Do NOT call cleanup() here - the WebView may just be temporarily hidden
            // when showing Connect/Transaction request sheets
        }
    }

    /**
     * Set up TonConnect support with default WebViewClient.
     * This will replace any existing WebViewClient and WebChromeClient.
     */
    @SuppressLint("SetJavaScriptEnabled")
    internal fun setup() {
        // Register automatic cleanup listener
        webView.addOnAttachStateChangeListener(detachListener)

        // Add JavaScript interface for bridge communication
        webView.addJavascriptInterface(
            BridgeInterface(
                onMessage = { json, type -> handleBridgeMessage(json, type) },
                onError = { error -> Log.e(TAG, "Bridge error: $error") },
            ),
            BrowserConstants.JS_INTERFACE_NAME,
        )

        // Set custom WebViewClient to inject bridge on page load
        webView.webViewClient = TonConnectWebViewClient(
            onPageStarted = { url ->
                // Emit page started event
                scope.launch {
                    try {
                        walletKit.engine?.callBridgeMethod(
                            method = "emitBrowserPageStarted",
                            params = JSONObject().apply {
                                put("url", url)
                            },
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to emit page started event", e)
                    }
                }
            },
            onPageFinished = { url ->
                // Emit page finished event
                scope.launch {
                    try {
                        walletKit.engine?.callBridgeMethod(
                            method = "emitBrowserPageFinished",
                            params = JSONObject().apply {
                                put("url", url)
                            },
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to emit page finished event", e)
                    }
                }
            },
            onError = { error ->
                Log.e(TAG, "WebView error: $error")
                // Emit error event
                scope.launch {
                    try {
                        walletKit.engine?.callBridgeMethod(
                            method = "emitBrowserError",
                            params = JSONObject().apply {
                                put("message", error)
                            },
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to emit error event", e)
                    }
                }
            },
            injectBridge = { view -> injectBridgeIntoAllFrames(view) },
            onTonConnectUrl = { url ->
                // Intercept deep link navigation to prevent ERR_UNKNOWN_URL_SCHEME
                // The dApp should use the injected bridge instead (embedded: true tells it to)
                Log.d(TAG, "Intercepted deep link navigation (prevented): $url")
            },
        )

        // Set custom WebChromeClient to track dynamic iframes
        webView.webChromeClient = TonConnectWebChromeClient(
            injectBridge = { view -> injectBridgeIntoAllFrames(view) },
        )
    }

    /**
     * Send a response back to a dApp for a specific request.
     *
     * @param messageId The ID from the original request
     * @param response The response data to send back
     */
    fun sendResponse(messageId: String, response: JSONObject) {
        Log.d(TAG, "🔵 sendResponse called for messageId: $messageId")
        Log.d(TAG, "🔵 Pending requests: ${pendingRequests.keys}")
        Log.d(TAG, "🔵 Response: $response")
        
        val pending = pendingRequests.remove(messageId)
        if (pending == null) {
            Log.w(TAG, "⚠️ No pending request found for messageId: $messageId")
            Log.w(TAG, "⚠️ Available pending requests: ${pendingRequests.keys}")
            return
        }

        // If this is a successful connect response, register this WebView for the session
        if (pending.method == "connect" && response.has(ResponseConstants.KEY_PAYLOAD)) {
            try {
                // Register with messageId (for immediate responses)
                registerWebView(messageId, webView)
                Log.d(TAG, "✅ Registered WebView with messageId: $messageId")
                
                // CRITICAL FIX: After sending the response, query WalletKit for the sessionId
                // and register the WebView with it too (for disconnect later)
                scope.launch {
                    try {
                        // Give WalletKit time to create the session
                        kotlinx.coroutines.delay(500)
                        
                        // Get all sessions from WalletKit
                        val sessions = walletKit.engine?.callBridgeMethod("listSessions", null)
                        Log.d(TAG, "🔍 Got sessions for WebView re-registration: $sessions")
                        
                        // Find the session that was just created (by messageId/tabId)
                        // The session will have tabId = messageId initially
                        // We need to register the WebView with the session's sessionId
                        
                        // Parse sessions and find matching one
                        if (sessions is JSONObject && sessions.has("items")) {
                            val items = sessions.getJSONArray("items")
                            for (i in 0 until items.length()) {
                                val session = items.getJSONObject(i)
                                val sessionId = session.optString("sessionId")
                                // Check if this session matches our messageId
                                // We can't directly check, so register ALL JS Bridge sessions
                                // This is safe because we're using the same WebView
                                Log.d(TAG, "🔄 Registering WebView for sessionId: $sessionId")
                                registerWebView(sessionId, webView)
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "⚠️ Failed to re-register WebView with sessionId", e)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to register WebView for session", e)
            }
        }

        Log.d(TAG, "✅ Found pending request, calling sendResponseToFrame")
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
        Log.d(TAG, "🔔 sendEvent called")
        Log.d(TAG, "🔔 Event to send: $event")
        
        scope.launch {
            webView.post {
                // CRITICAL FIX: The event from Engine already has the correct structure:
                // { type: "TONCONNECT_BRIDGE_EVENT", source: "...", event: {...} }
                // We should NOT wrap it again - just extract the inner event
                val actualEvent = if (event.has("event")) {
                    // Extract the actual disconnect/connect event from the wrapper
                    event.getJSONObject("event")
                } else {
                    // Fallback if event is already unwrapped
                    event
                }
                
                // Now create the message for window.postMessage with correct structure
                val eventMessage = JSONObject().apply {
                    put(BrowserConstants.KEY_TYPE, BrowserConstants.MESSAGE_TYPE_BRIDGE_EVENT)
                    put(BrowserConstants.KEY_EVENT, actualEvent)  // ✅ Now this is the actual disconnect event
                }

                Log.d(TAG, "🔔 Extracted actual event: $actualEvent")
                Log.d(TAG, "🔔 Formatted event message: $eventMessage")
                Log.d(TAG, "🔔 Broadcasting event to all frames")

                // Send to main window
                val jsCode = """
                    ${BrowserConstants.JS_WINDOW_POST_MESSAGE}($eventMessage, '*');
                    """.trimIndent()
                    
                Log.d(TAG, "🔔 Executing JS to post message to main window")
                Log.d(TAG, "🔔 JS code: $jsCode")
                
                webView.evaluateJavascript(jsCode) { result ->
                    Log.d(TAG, "✅ JS executed for main window, result: $result")
                }

                // Send to all iframes
                webView.evaluateJavascript(
                    """
                    (function() {
                        const iframes = document.querySelectorAll('${BrowserConstants.JS_SELECTOR_IFRAMES}');
                        for (const iframe of iframes) {
                            try {
                                iframe.${BrowserConstants.JS_PROPERTY_CONTENT_WINDOW}.postMessage($eventMessage, '*');
                            } catch (e) {
                                // Cross-origin iframe, skip
                            }
                        }
                    })();
                    """.trimIndent(),
                    null,
                )
            }
        }
    }

    /**
     * Clean up resources.
     *
     * This is called automatically when the WebView is detached from the window.
     * You can also call this manually if needed (e.g., when reusing a WebView),
     * but it's safe to call multiple times.
     */
    fun cleanup() {
        if (isCleanedUp) {
            return // Already cleaned up
        }
        isCleanedUp = true

        Log.d(TAG, "Cleaning up TonConnect injector")

        // Unregister all sessions for this WebView
        // We need to iterate through all sessions and remove ones pointing to this WebView
        val sessionsToRemove = mutableListOf<String>()
        activeWebViews.forEach { (sessionId, webViewRef) ->
            if (webViewRef.get() == webView) {
                sessionsToRemove.add(sessionId)
            }
        }
        sessionsToRemove.forEach { sessionId ->
            unregisterWebView(sessionId)
        }

        // Remove attach state listener to prevent memory leak
        webView.removeOnAttachStateChangeListener(detachListener)

        scope.cancel()
        pendingRequests.clear()
    }

    private fun injectBridgeIntoAllFrames(webView: WebView?) {
        scope.launch {
            BridgeInjector.injectIntoAllFrames(
                context = context,
                webView = webView,
                onError = { error -> Log.e(TAG, "Bridge injection error: $error") },
            )
        }
    }

    private fun handleBridgeMessage(json: JSONObject, type: String) {
        scope.launch {
            when (type) {
                BrowserConstants.MESSAGE_TYPE_BRIDGE_REQUEST -> handleBridgeRequest(json)
                else -> Log.w(TAG, "Unknown message type: $type")
            }
        }
    }

    private fun handleBridgeRequest(json: JSONObject) {
        val frameId = json.optString(BrowserConstants.KEY_FRAME_ID, BrowserConstants.DEFAULT_FRAME_ID)
        val messageId = json.optString(BrowserConstants.KEY_MESSAGE_ID)
        val method = json.optString(BrowserConstants.KEY_METHOD, BrowserConstants.DEFAULT_METHOD)

        Log.d(TAG, "🟢 TonConnect request: frameId=$frameId, messageId=$messageId, method=$method")

        if (messageId.isEmpty()) {
            Log.e(TAG, "Bridge request missing messageId")
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

        Log.d(TAG, "✅ Request stored: $method from '$frameId' (ID: $messageId)")

        // Get the engine from the provided TONWalletKit instance
        val engine = walletKit.engine
        if (engine == null) {
            Log.e(TAG, "❌ WalletKit engine not available!")
            // Send error response back to dApp
            val errorResponse = JSONObject().apply {
                put(
                    ResponseConstants.KEY_ERROR,
                    JSONObject().apply {
                        put(ResponseConstants.KEY_MESSAGE, "Wallet engine not initialized")
                        put(ResponseConstants.KEY_CODE, 500)
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
                    method = "emitBrowserBridgeRequest",
                    params = JSONObject().apply {
                        put("messageId", messageId)
                        put("method", method)
                        put("request", json.toString())
                    },
                )
            } catch (e: Exception) {
                Log.w(TAG, "Failed to emit browser bridge request event", e)
            }
        }

        // Forward to TONWalletKit engine - it handles everything internally!
        scope.launch {
            try {
                Log.d(TAG, "🔄 Forwarding request to WalletKit engine: $method")

                // CRITICAL FIX: For 'send' method, params is an ARRAY containing the actual request
                // The dApp sends: { method: 'send', params: [{ method: 'signData', params: [...] }] }
                // We need to extract params[0] to get the actual request params
                val paramsToSend = if (method == "send") {
                    val paramsArray = json.optJSONArray(ResponseConstants.KEY_PARAMS)
                    if (paramsArray != null && paramsArray.length() > 0) {
                        paramsArray.optJSONObject(0) // Extract first element
                    } else {
                        json.optJSONObject(ResponseConstants.KEY_PARAMS) // Fallback to object
                    }
                } else {
                    json.optJSONObject(ResponseConstants.KEY_PARAMS)
                }
                
                Log.d(TAG, "🔄 Original params: ${json.opt(ResponseConstants.KEY_PARAMS)}")
                Log.d(TAG, "🔄 Extracted params: $paramsToSend")

                engine.handleTonConnectRequest(
                    messageId = messageId,
                    method = method,
                    params = paramsToSend,
                    responseCallback = { response ->
                        Log.d(TAG, "🟣 responseCallback invoked by engine!")
                        Log.d(TAG, "🟣 Response for messageId: $messageId")
                        Log.d(TAG, "🟣 Response data: $response")
                        Log.d(TAG, "🟣 About to call sendResponse...")
                        sendResponse(messageId, response)
                        Log.d(TAG, "🟣 sendResponse call completed")
                    },
                )
                Log.d(TAG, "✅ Request forwarded successfully to WalletKit engine")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to forward request to WalletKit engine", e)
                // Send error response back to dApp
                val errorResponse = JSONObject().apply {
                    put(
                        ResponseConstants.KEY_ERROR,
                        JSONObject().apply {
                            put(ResponseConstants.KEY_MESSAGE, e.message ?: "Failed to process request")
                            put(ResponseConstants.KEY_CODE, 500)
                        },
                    )
                }
                sendResponse(messageId, errorResponse)
            }
        }
    }

    private fun sendResponseToFrame(pending: PendingRequest, response: JSONObject) {
        Log.d(TAG, "📤 sendResponseToFrame called for messageId: ${pending.messageId}, method: ${pending.method}")
        Log.d(TAG, "📤 Response payload: $response")
        Log.d(TAG, "📤 WebView attached: ${webView.isAttachedToWindow}, WebView parent: ${webView.parent}")
        
        scope.launch {
            webView.post {
                val responseJson = JSONObject().apply {
                    put(BrowserConstants.KEY_TYPE, BrowserConstants.MESSAGE_TYPE_BRIDGE_RESPONSE)
                    put(BrowserConstants.KEY_MESSAGE_ID, pending.messageId)
                    put(BrowserConstants.KEY_SUCCESS, true)
                    put(BrowserConstants.KEY_PAYLOAD, response)
                }

                Log.d(TAG, "📤 Injecting response into WebView for '${pending.frameId}' for ${pending.method} (ID: ${pending.messageId})")
                Log.d(TAG, "📤 Response JSON: $responseJson")

                if (pending.frameId == BrowserConstants.DEFAULT_FRAME_ID) {
                    // Send to main window
                    val jsCode = """
                        ${BrowserConstants.JS_WINDOW_POST_MESSAGE}($responseJson, '*');
                        """.trimIndent()
                    Log.d(TAG, "📤 Executing JS to post message to main window")
                    webView.evaluateJavascript(jsCode) { result ->
                        Log.d(TAG, "✅ JS executed, result: $result")
                    }
                } else {
                    // Find iframe by frameId and send to it
                    webView.evaluateJavascript(
                        """
                        (function() {
                            const iframes = document.querySelectorAll('${BrowserConstants.JS_SELECTOR_IFRAMES}');
                            for (const iframe of iframes) {
                                try {
                                    if (iframe.${BrowserConstants.JS_PROPERTY_CONTENT_WINDOW}.${BrowserConstants.JS_PROPERTY_FRAME_ID} === '${pending.frameId}') {
                                        iframe.${BrowserConstants.JS_PROPERTY_CONTENT_WINDOW}.postMessage($responseJson, '*');
                                        console.log('${BrowserConstants.CONSOLE_PREFIX_NATIVE} ${BrowserConstants.CONSOLE_MSG_RESPONSE_SENT} ${pending.frameId}');
                                        return;
                                    }
                                } catch (e) {
                                    // Cross-origin iframe, skip
                                }
                            }
                            console.warn('${BrowserConstants.CONSOLE_PREFIX_NATIVE} ${BrowserConstants.CONSOLE_MSG_FRAME_NOT_FOUND}: ${pending.frameId}');
                        })();
                        """.trimIndent(),
                        null,
                    )
                }
            }
        }
    }
}
