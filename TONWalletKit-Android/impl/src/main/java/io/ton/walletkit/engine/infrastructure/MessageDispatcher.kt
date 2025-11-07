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
        Logger.d(TAG, "📨 Message kind: $kind")

        when (kind) {
            ResponseConstants.VALUE_KIND_READY -> handleReady(payload)
            ResponseConstants.VALUE_KIND_EVENT -> {
                val event = payload.optJSONObject(ResponseConstants.KEY_EVENT)
                if (event != null) {
                    handleEvent(event)
                } else {
                    Logger.w(TAG, "⚠️ EVENT kind but no event object in payload")
                }
            }
            ResponseConstants.VALUE_KIND_RESPONSE -> {
                val id = payload.optString(ResponseConstants.KEY_ID)
                rpcClient.handleResponse(id, payload)
            }
            ResponseConstants.VALUE_KIND_JS_BRIDGE_EVENT -> handleJsBridgeEvent(payload)
            else -> Logger.w(TAG, "⚠️ Unknown message kind: $kind")
        }
    }

    suspend fun ensureEventListenersSetUp() {
        Logger.d(TAG, "🔵 ensureEventListenersSetUp() called, areEventListenersSetUp=$areEventListenersSetUp")
        if (areEventListenersSetUp) {
            Logger.d(TAG, "⚡ Event listeners already set up, skipping")
            return
        }

        Logger.d(TAG, "🔵 Acquiring eventListenersSetupMutex...")
        eventListenersSetupMutex.withLock {
            Logger.d(TAG, "🔵 eventListenersSetupMutex acquired")

            if (areEventListenersSetUp) {
                Logger.d(TAG, "⚡ Event listeners already set up (double-check), skipping")
                return@withLock
            }

            try {
                Logger.d(TAG, "🔵 Waiting for WalletKit initialization...")
                initManager.ensureInitialized()
                onInitialized()
                Logger.d(TAG, "✅ WalletKit initialization complete")

                Logger.d(TAG, "🔵 Calling JS setEventsListeners()...")
                rpcClient.call(BridgeMethodConstants.METHOD_SET_EVENTS_LISTENERS, JSONObject())
                areEventListenersSetUp = true
                Logger.d(TAG, "✅✅✅ Event listeners set up successfully! areEventListenersSetUp=true")
            } catch (err: Throwable) {
                Logger.e(TAG, "❌ Failed to set up event listeners", err)
                throw WalletKitBridgeException(ERROR_FAILED_SET_UP_EVENT_LISTENERS + err.message)
            }
        }
        Logger.d(TAG, "🔵 eventListenersSetupMutex released")
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
        Logger.d(TAG, "🚀 handleReady() called")

        val network = payload.optNullableString(ResponseConstants.KEY_NETWORK)
        initManager.updateNetwork(network)
        onNetworkChanged(network)
        val apiBaseUrl = payload.optNullableString(ResponseConstants.KEY_TON_API_URL)
        initManager.updateApiBaseUrl(apiBaseUrl)
        onApiBaseUrlChanged(apiBaseUrl)
        if (!webViewManager.bridgeLoaded.isCompleted) {
            webViewManager.bridgeLoaded.complete(Unit)
            Logger.d(TAG, "🚀 bridgeLoaded completed")
        }
        webViewManager.markJsBridgeReady()

        val wasAlreadyReady = rpcClient.isReady()
        Logger.d(TAG, "🚀 wasAlreadyReady=$wasAlreadyReady, ready.isCompleted=${rpcClient.isReady()}")

        if (!rpcClient.isReady()) {
            Logger.d(TAG, "🚀 Completing ready for the first time")
            rpcClient.markReady()
        }

        if (wasAlreadyReady && areEventListenersSetUp) {
            Logger.w(TAG, "⚠️⚠️⚠️ Bridge ready event received again - JavaScript context was lost! Re-setting up event listeners...")
            areEventListenersSetUp = false
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    Logger.d(TAG, "🔵 Re-setting up event listeners after JS context loss...")
                    ensureEventListenersSetUp()
                    Logger.d(TAG, "✅ Event listeners re-established after JS context loss")
                } catch (e: Exception) {
                    Logger.e(TAG, "❌ Failed to re-setup event listeners after JS context loss", e)
                }
            }
        } else {
            Logger.d(TAG, "🚀 Normal ready event (wasAlreadyReady=$wasAlreadyReady, areEventListenersSetUp=$areEventListenersSetUp)")
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
        Logger.d(TAG, "🚀 Calling handleEvent for ready event")
        handleEvent(readyEvent)
        Logger.d(TAG, "🚀 handleReady() complete")
    }

    private fun handleEvent(event: JSONObject) {
        val type = event.optString(JsonConstants.KEY_TYPE, EventTypeConstants.EVENT_TYPE_UNKNOWN)
        val data = event.optJSONObject(ResponseConstants.KEY_DATA) ?: JSONObject()
        val eventId = event.optString(JsonConstants.KEY_ID, java.util.UUID.randomUUID().toString())

        Logger.d(TAG, "🟢🟢🟢 === handleEvent called ===")
        Logger.d(TAG, "🟢 Event type: $type")
        Logger.d(TAG, "🟢 Event ID: $eventId")
        Logger.d(TAG, "🟢 Event data keys: ${data.keys().asSequence().toList()}")
        Logger.d(TAG, "🟢 Thread: ${Thread.currentThread().name}")

        val typedEvent = eventParser.parseEvent(type, data, event)
        Logger.d(TAG, "🟢 Parsed typed event: ${(typedEvent?.javaClass?.simpleName ?: ResponseConstants.VALUE_UNKNOWN)}")

        if (typedEvent != null) {
            Logger.d(TAG, "🟢 Typed event is NOT null, posting to main handler...")

            mainHandler.post {
                Logger.d(TAG, "🟢 Main handler runnable executing for event $type")
                runBlocking {
                    eventRouter.dispatchEvent(eventId, type, typedEvent)
                }
            }
        } else {
            Logger.w(TAG, "⚠️ " + MSG_FAILED_PARSE_TYPED_EVENT_PREFIX + type + " - event will be ignored")
        }
    }

    private fun handleJsBridgeEvent(payload: JSONObject) {
        val sessionId = payload.optString("sessionId")
        val event = payload.optJSONObject("event")

        Logger.d(TAG, "📤 handleJsBridgeEvent called")
        Logger.d(TAG, "📤 sessionId: $sessionId")
        Logger.d(TAG, "📤 event: $event")
        Logger.d(TAG, "📤 Full payload: $payload")

        if (event == null) {
            Logger.e(TAG, "❌ No event object in ${ResponseConstants.VALUE_KIND_JS_BRIDGE_EVENT} payload")
            return
        }

        mainHandler.post {
            try {
                Logger.d(TAG, "📤 Looking up WebView for session: $sessionId")
                val targetWebView = TonConnectInjector.getWebViewForSession(sessionId)

                if (targetWebView != null) {
                    Logger.d(TAG, "✅ Found WebView for session: $sessionId")
                    val injector = targetWebView.getTonConnectInjector()
                    if (injector != null) {
                        Logger.d(TAG, "✅ Found TonConnectInjector, sending event to WebView")
                        Logger.d(TAG, "📤 Event being sent: $event")
                        injector.sendEvent(event)
                        Logger.d(TAG, "✅ Event sent successfully")
                    } else {
                        Logger.w(TAG, "⚠️  WebView found but no TonConnectInjector attached for session: $sessionId")
                    }
                } else {
                    Logger.w(TAG, "⚠️  No WebView found for session: $sessionId (browser may have been closed)")
                    Logger.w(TAG, "⚠️  This means the WebView was never registered or was garbage collected")
                }
            } catch (e: Exception) {
                Logger.e(TAG, "❌ Failed to send JS Bridge event", e)
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
