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

import android.os.Handler
import io.ton.walletkit.WalletKitBridgeException
import io.ton.walletkit.browser.TonConnectInjector
import io.ton.walletkit.engine.parsing.EventParser
import io.ton.walletkit.engine.state.EventRouter
import io.ton.walletkit.internal.constants.BridgeMethodConstants
import io.ton.walletkit.internal.constants.EventTypeConstants
import io.ton.walletkit.internal.constants.JsonConstants
import io.ton.walletkit.internal.constants.LogConstants
import io.ton.walletkit.internal.constants.ResponseConstants
import io.ton.walletkit.internal.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject

/**
 * Routes messages coming from the JavaScript bridge to the appropriate engine components.
 *
 * Responsibilities:
 * * Maintain bridge ready state and trigger reconfiguration when the JS context reloads.
 * * Parse and dispatch typed events to registered handlers.
 * * Coordinate JavaScript-side event listener setup/teardown.
 * * Forward RPC responses to [BridgeRpcClient].
 *
 * @suppress Internal component. Use through [WebViewWalletKitEngine].
 */
internal class MessageDispatcher(
    private val rpcClient: BridgeRpcClient,
    private val eventParser: EventParser,
    private val eventRouter: EventRouter,
    private val initManager: InitializationManager,
    private val webViewManager: WebViewManager,
    private val onInitialized: () -> Unit,
    private val onNetworkChanged: (String?) -> Unit,
    private val onApiBaseUrlChanged: (String?) -> Unit,
) {
    private val mainHandler: Handler = webViewManager.getMainHandler()
    private val eventListenersSetupMutex = Mutex()

    @Volatile private var areEventListenersSetUp = false

    fun dispatchMessage(payload: JSONObject) {
        val kind = payload.optString(ResponseConstants.KEY_KIND)

        when (kind) {
            ResponseConstants.VALUE_KIND_READY -> handleReady(payload)
            ResponseConstants.VALUE_KIND_EVENT -> {
                val event = payload.optJSONObject(ResponseConstants.KEY_EVENT)
                if (event != null) {
                    handleEvent(event)
                } else {
                    Logger.w(TAG, "EVENT kind but no event object in payload")
                }
            }
            ResponseConstants.VALUE_KIND_RESPONSE -> {
                val id = payload.optString(ResponseConstants.KEY_ID)
                rpcClient.handleResponse(id, payload)
            }
            ResponseConstants.VALUE_KIND_JS_BRIDGE_EVENT -> handleJsBridgeEvent(payload)
            else -> Logger.w(TAG, "Unknown message kind: $kind")
        }
    }

    suspend fun ensureEventListenersSetUp() {
        if (areEventListenersSetUp) {
            return
        }

        eventListenersSetupMutex.withLock {
            if (areEventListenersSetUp) {
                return@withLock
            }

            try {
                initManager.ensureInitialized()
                onInitialized()

                rpcClient.call(BridgeMethodConstants.METHOD_SET_EVENTS_LISTENERS, JSONObject())
                areEventListenersSetUp = true
            } catch (err: Throwable) {
                Logger.e(TAG, "Failed to set up event listeners", err)
                throw WalletKitBridgeException(ERROR_FAILED_SET_UP_EVENT_LISTENERS + err.message)
            }
        }
    }

    suspend fun removeEventListenersIfNeeded() {
        if (!areEventListenersSetUp) {
            return
        }
        try {
            rpcClient.call(BridgeMethodConstants.METHOD_REMOVE_EVENT_LISTENERS, JSONObject())
            areEventListenersSetUp = false
            Logger.d(TAG, "Event listeners removed from JS bridge (no handlers remaining)")
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to remove event listeners from JS bridge", e)
        }
    }

    fun areEventListenersSetUp(): Boolean = areEventListenersSetUp

    private fun handleReady(payload: JSONObject) {
        val network = payload.optNullableString(ResponseConstants.KEY_NETWORK)
        initManager.updateNetwork(network)
        onNetworkChanged(network)
        val apiBaseUrl = payload.optNullableString(ResponseConstants.KEY_TON_API_URL)
        initManager.updateApiBaseUrl(apiBaseUrl)
        onApiBaseUrlChanged(apiBaseUrl)
        if (!webViewManager.bridgeLoaded.isCompleted) {
            webViewManager.bridgeLoaded.complete(Unit)
        }
        webViewManager.markJsBridgeReady()

        val wasAlreadyReady = rpcClient.isReady()

        if (!wasAlreadyReady) {
            rpcClient.markReady()
        }

        if (wasAlreadyReady && areEventListenersSetUp) {
            Logger.w(TAG, "Bridge ready event received again - JavaScript context was lost! Re-setting up event listeners...")
            areEventListenersSetUp = false
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    ensureEventListenersSetUp()
                } catch (e: Exception) {
                    Logger.e(TAG, "Failed to re-setup event listeners after JS context loss", e)
                }
            }
        }

        val data = JSONObject()
        val keys = payload.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            if (key == ResponseConstants.KEY_KIND) continue
            if (payload.isNull(key)) {
                data.put(key, JSONObject.NULL)
            } else {
                data.put(key, payload.get(key))
            }
        }
        val readyEvent =
            JSONObject().apply {
                put(ResponseConstants.KEY_TYPE, ResponseConstants.VALUE_KIND_READY)
                put(ResponseConstants.KEY_DATA, data)
            }
        handleEvent(readyEvent)
    }

    private fun handleEvent(event: JSONObject) {
        val type = event.optString(JsonConstants.KEY_TYPE, EventTypeConstants.EVENT_TYPE_UNKNOWN)
        val data = event.optJSONObject(ResponseConstants.KEY_DATA) ?: JSONObject()
        val eventId = event.optString(JsonConstants.KEY_ID, java.util.UUID.randomUUID().toString())

        val typedEvent = try {
            eventParser.parseEvent(type, data, event)
        } catch (e: Exception) {
            Logger.e(TAG, "Exception thrown while parsing event type=$type", e)
            null
        }

        if (typedEvent != null) {
            mainHandler.post {
                runBlocking {
                    eventRouter.dispatchEvent(eventId, type, typedEvent)
                }
            }
        } else {
            Logger.w(TAG, MSG_FAILED_PARSE_TYPED_EVENT_PREFIX + type + " - event will be ignored")
        }
    }

    private fun handleJsBridgeEvent(payload: JSONObject) {
        val sessionId = payload.optString("sessionId")
        val event = payload.optJSONObject("event")

        if (event == null) {
            Logger.e(TAG, "No event object in ${ResponseConstants.VALUE_KIND_JS_BRIDGE_EVENT} payload")
            return
        }

        mainHandler.post {
            try {
                // If sessionId is empty (e.g., wallet-initiated disconnect), broadcast to all WebViews
                if (sessionId.isNullOrEmpty()) {
                    TonConnectInjector.broadcastEventToAllWebViews(event)
                    return@post
                }

                val targetWebView = TonConnectInjector.getWebViewForSession(sessionId)

                if (targetWebView != null) {
                    val injector = targetWebView.getTonConnectInjector()
                    if (injector != null) {
                        injector.sendEvent(event)
                    } else {
                        Logger.w(TAG, "WebView found but no TonConnectInjector attached for session: $sessionId")
                    }
                } else {
                    Logger.w(TAG, "No WebView found for session: $sessionId (browser may have been closed)")
                }
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to send JS Bridge event", e)
            }
        }
    }

    /**
     * Dispatches an error to fail a specific pending RPC call.
     *
     * When the bridge encounters an error (e.g., malformed JSON), this method:
     * 1. Attempts to extract the call ID from the malformed message
     * 2. Creates an error response for that specific call
     * 3. Dispatches the error response to fail only that call
     *
     * If the call ID cannot be extracted, the error is logged but no call is failed.
     */
    fun dispatchError(exception: WalletKitBridgeException, malformedJson: String?) {
        Logger.e(TAG, "Dispatching error for malformed message", exception)

        // Try to extract call ID from malformed JSON
        val callId = malformedJson?.let { tryExtractCallId(it) }

        if (callId != null) {
            // Create error response for this specific call
            val errorResponse = JSONObject().apply {
                put(ResponseConstants.KEY_KIND, ResponseConstants.VALUE_KIND_RESPONSE)
                put(ResponseConstants.KEY_ID, callId)
                put(
                    ResponseConstants.KEY_ERROR,
                    JSONObject().apply {
                        put(ResponseConstants.KEY_MESSAGE, exception.message ?: "Bridge error")
                    },
                )
            }

            // Dispatch the error response to fail the specific call
            rpcClient.handleResponse(callId, errorResponse)
        } else {
            Logger.w(TAG, "Could not extract call ID from malformed JSON, cannot fail specific call")
        }
    }

    /**
     * Attempts to extract the call ID from a malformed JSON string.
     * Uses regex to find the "id" field even if the JSON is incomplete or invalid.
     */
    private fun tryExtractCallId(malformedJson: String): String? {
        return try {
            // Try to find "id":"value" or "id": "value" pattern
            val regex = """"id"\s*:\s*"([^"]+)"""".toRegex()
            val matchResult = regex.find(malformedJson)
            matchResult?.groupValues?.get(1)
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to extract call ID from malformed JSON", e)
            null
        }
    }

    private fun JSONObject.optNullableString(key: String): String? {
        val value = opt(key)
        return when (value) {
            null, JSONObject.NULL -> null
            else -> value.toString()
        }
    }

    companion object {
        private const val TAG = LogConstants.TAG_WEBVIEW_ENGINE
        private const val MSG_FAILED_PARSE_TYPED_EVENT_PREFIX = "Failed to parse typed event for type: "
        private const val ERROR_FAILED_SET_UP_EVENT_LISTENERS = "Failed to set up event listeners: "
    }
}

private const val TAG_TON_CONNECT_INJECTOR = "tonconnect_injector"

private fun android.webkit.WebView.getTonConnectInjector(): TonConnectInjector? {
    return getTag(TAG_TON_CONNECT_INJECTOR.hashCode()) as? TonConnectInjector
}
