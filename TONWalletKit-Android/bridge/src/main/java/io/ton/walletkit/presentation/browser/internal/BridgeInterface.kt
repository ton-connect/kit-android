package io.ton.walletkit.presentation.browser.internal

import android.util.Log
import android.webkit.JavascriptInterface
import io.ton.walletkit.domain.constants.BrowserConstants
import org.json.JSONObject

/**
 * JavaScript interface for bridge communication.
 * Messages from injected bridge come through this interface.
 *
 * Each message includes a frameId to identify which window/iframe sent it.
 * 
 * This interface is automatically available in ALL frames (parent + iframes)
 * thanks to addJavascriptInterface() behavior.
 */
internal class BridgeInterface(
    private val onMessage: (message: JSONObject, type: String) -> Unit,
    private val onError: (error: String) -> Unit,
    private val onResponse: ((message: JSONObject) -> Unit)? = null,
) {
    // Thread-safe storage for responses that iframes can pull
    private val availableResponses = java.util.concurrent.ConcurrentHashMap<String, String>()
    
    @JavascriptInterface
    fun postMessage(message: String) {
        Log.d(TAG, "🔵 BridgeInterface.postMessage called with: $message")
        try {
            val json = JSONObject(message)
            val type = json.optString(BrowserConstants.KEY_TYPE)

            Log.d(TAG, "�� Message type: $type")

            onMessage(json, type)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to handle bridge message", e)
            onError("Invalid bridge message: ${e.message}")
        }
    }
    
    /**
     * Store a response that any iframe can pull.
     * Called from Kotlin when a response is ready.
     */
    fun storeResponse(messageId: String, response: String) {
        Log.d(TAG, "📥 Storing response for messageId: $messageId")
        availableResponses[messageId] = response
    }
    
    /**
     * Pull a response for a specific messageId.
     * Called from JavaScript in any frame (parent or iframe).
     * Returns the response JSON string or null if not available.
     */
    @JavascriptInterface
    fun pullResponse(messageId: String): String? {
        val response = availableResponses.remove(messageId)
        if (response != null) {
            Log.d(TAG, "📤 Pulled response for messageId: $messageId")
        }
        return response
    }
    
    /**
     * Check if a response is available for a messageId.
     * Called from JavaScript to poll for responses.
     */
    @JavascriptInterface
    fun hasResponse(messageId: String): Boolean {
        return availableResponses.containsKey(messageId)
    }

    /**
     * Send a TonConnect response from the RPC bridge back to the internal browser WebView.
     * This is called by bridge.ts jsBridgeTransport to deliver responses.
     */
    @JavascriptInterface
    fun postResponse(message: String) {
        Log.d(TAG, "🟣 BridgeInterface.postResponse called with response to inject into WebView")
        try {
            val json = JSONObject(message)
            onResponse?.invoke(json) ?: Log.w(TAG, "No response handler configured")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to handle bridge response", e)
            onError("Invalid bridge response: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "BridgeInterface"
    }
}
