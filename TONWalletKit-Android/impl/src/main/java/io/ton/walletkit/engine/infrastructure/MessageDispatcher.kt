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
import android.webkit.WebView
import io.ton.walletkit.WalletKitBridgeException
import io.ton.walletkit.api.generated.TONPreparedSignData
import io.ton.walletkit.api.generated.TONProofMessage
import io.ton.walletkit.api.generated.TONStakeParams
import io.ton.walletkit.api.generated.TONStakingQuoteParams
import io.ton.walletkit.api.generated.TONSwapParams
import io.ton.walletkit.api.generated.TONSwapQuoteParams
import io.ton.walletkit.api.generated.TONTransactionRequest
import io.ton.walletkit.bridge.dispatch.AdapterByIdRequest
import io.ton.walletkit.bridge.dispatch.AdapterSignDataRequest
import io.ton.walletkit.bridge.dispatch.AdapterSignTonProofRequest
import io.ton.walletkit.bridge.dispatch.AdapterSignTransactionRequest
import io.ton.walletkit.bridge.dispatch.BridgeRequestRegistry
import io.ton.walletkit.bridge.dispatch.CallByReferenceRequest
import io.ton.walletkit.bridge.dispatch.KotlinProviderBuildRequest
import io.ton.walletkit.bridge.dispatch.KotlinProviderIdRequest
import io.ton.walletkit.bridge.dispatch.KotlinProviderQuoteRequest
import io.ton.walletkit.bridge.dispatch.KotlinProviderUnwatchRequest
import io.ton.walletkit.bridge.dispatch.KotlinProviderWatchRequest
import io.ton.walletkit.bridge.dispatch.KotlinStakingGetProviderInfoRequest
import io.ton.walletkit.bridge.dispatch.KotlinStakingGetStakedBalanceRequest
import io.ton.walletkit.bridge.dispatch.SignWithCustomSignerRequest
import io.ton.walletkit.bridge.optJsonObject
import io.ton.walletkit.bridge.optString
import io.ton.walletkit.bridge.optStringOrNull
import io.ton.walletkit.browser.TonConnectInjector
import io.ton.walletkit.core.streaming.StreamingEvent
import io.ton.walletkit.engine.parsing.EventParser
import io.ton.walletkit.engine.state.AdapterManager
import io.ton.walletkit.engine.state.EventRouter
import io.ton.walletkit.engine.state.KotlinStakingProviderManager
import io.ton.walletkit.engine.state.KotlinStreamingProviderManager
import io.ton.walletkit.engine.state.KotlinSwapProviderManager
import io.ton.walletkit.engine.state.SignerManager
import io.ton.walletkit.internal.constants.BridgeMethodConstants
import io.ton.walletkit.internal.constants.EventTypeConstants
import io.ton.walletkit.internal.constants.JsonConstants
import io.ton.walletkit.internal.constants.LogConstants
import io.ton.walletkit.internal.constants.ResponseConstants
import io.ton.walletkit.internal.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID

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
    private val adapterManager: AdapterManager,
    private val signerManager: SignerManager,
    private val kotlinSwapProviderManager: KotlinSwapProviderManager,
    private val kotlinStakingProviderManager: KotlinStakingProviderManager,
    private val kotlinStreamingProviderManager: KotlinStreamingProviderManager,
    private val json: Json,
    private val onInitialized: () -> Unit,
) {
    private val mainHandler: Handler = webViewManager.getMainHandler()
    private val eventListenersSetupMutex = Mutex()

    private val _streamingEvents = MutableSharedFlow<StreamingEvent>(extraBufferCapacity = 64)
    val streamingEvents: SharedFlow<StreamingEvent> = _streamingEvents.asSharedFlow()

    @Volatile private var areEventListenersSetUp = false

    private val requestRegistry: BridgeRequestRegistry = BridgeRequestRegistry(json).apply {
        registerTypedJson<SignWithCustomSignerRequest, String>(REQUEST_METHOD_SIGN_WITH_CUSTOM_SIGNER) { req ->
            val signer = signerManager.getSigner(req.signerId)
                ?: throw IllegalArgumentException("Custom signer not found: ${req.signerId}")
            signer.sign(req.data).value
        }

        registerTypedJson<AdapterByIdRequest, String>(REQUEST_METHOD_ADAPTER_GET_STATE_INIT) { req ->
            requireAdapter(req.adapterId).stateInit().value
        }

        registerTypedJson<AdapterSignTransactionRequest, String>(REQUEST_METHOD_ADAPTER_SIGN_TRANSACTION) { req ->
            val request = json.decodeFromString<TONTransactionRequest>(req.input)
            requireAdapter(req.adapterId).signedSendTransaction(request, req.fakeSignature ?: false).value
        }

        registerTypedJson<AdapterSignTransactionRequest, String>(REQUEST_METHOD_ADAPTER_SIGN_MESSAGE) { req ->
            val request = json.decodeFromString<TONTransactionRequest>(req.input)
            requireAdapter(req.adapterId).signedSignMessage(request, req.fakeSignature ?: false).value
        }

        registerTypedJson<AdapterSignDataRequest, String>(REQUEST_METHOD_ADAPTER_SIGN_DATA) { req ->
            val request = json.decodeFromString<TONPreparedSignData>(req.input)
            requireAdapter(req.adapterId).signedSignData(request, req.fakeSignature ?: false).value
        }

        registerTypedJson<AdapterSignTonProofRequest, String>(REQUEST_METHOD_ADAPTER_SIGN_TON_PROOF) { req ->
            val request = json.decodeFromString<TONProofMessage>(req.input)
            requireAdapter(req.adapterId).signedTonProof(request, req.fakeSignature ?: false).value
        }

        registerTypedJson<KotlinProviderQuoteRequest, _>(REQUEST_METHOD_KOTLIN_SWAP_PROVIDER_QUOTE) { req ->
            kotlinSwapProviderManager.quote(req.providerId, decodeParams<TONSwapQuoteParams<JsonElement>>(req.params))
        }

        registerTypedJson<KotlinProviderBuildRequest, _>(REQUEST_METHOD_KOTLIN_SWAP_PROVIDER_BUILD_SWAP_TRANSACTION) { req ->
            kotlinSwapProviderManager.buildSwapTransaction(req.providerId, decodeParams<TONSwapParams<JsonElement>>(req.params))
        }

        registerTyped<KotlinProviderIdRequest>(REQUEST_METHOD_KOTLIN_SWAP_PROVIDER_RELEASE) { req ->
            kotlinSwapProviderManager.unregister(req.providerId)
            EMPTY_JSON_OBJECT
        }

        registerTypedJson<KotlinProviderQuoteRequest, _>(REQUEST_METHOD_KOTLIN_STAKING_PROVIDER_GET_QUOTE) { req ->
            kotlinStakingProviderManager.getQuote(req.providerId, decodeParams<TONStakingQuoteParams<JsonElement>>(req.params))
        }

        registerTypedJson<KotlinProviderBuildRequest, _>(REQUEST_METHOD_KOTLIN_STAKING_PROVIDER_BUILD_STAKE_TRANSACTION) { req ->
            kotlinStakingProviderManager.buildStakeTransaction(req.providerId, decodeParams<TONStakeParams<JsonElement>>(req.params))
        }

        registerTypedJson<KotlinStakingGetStakedBalanceRequest, _>(REQUEST_METHOD_KOTLIN_STAKING_PROVIDER_GET_STAKED_BALANCE) { req ->
            kotlinStakingProviderManager.getStakedBalance(req.providerId, req.userAddress, req.networkChainId)
        }

        registerTypedJson<KotlinStakingGetProviderInfoRequest, _>(REQUEST_METHOD_KOTLIN_STAKING_PROVIDER_GET_STAKING_PROVIDER_INFO) { req ->
            kotlinStakingProviderManager.info(req.providerId, req.networkChainId)
        }

        registerTyped<KotlinProviderIdRequest>(REQUEST_METHOD_KOTLIN_STAKING_PROVIDER_RELEASE) { req ->
            kotlinStakingProviderManager.unregister(req.providerId)
            EMPTY_JSON_OBJECT
        }

        registerTyped<KotlinProviderWatchRequest>(REQUEST_METHOD_KOTLIN_PROVIDER_WATCH) { req ->
            kotlinStreamingProviderManager.watch(req.providerId, req.subscriptionId, req.type, req.address)
            EMPTY_JSON_OBJECT
        }

        registerTyped<KotlinProviderUnwatchRequest>(REQUEST_METHOD_KOTLIN_PROVIDER_UNWATCH) { req ->
            kotlinStreamingProviderManager.unwatch(req.subscriptionId)
            EMPTY_JSON_OBJECT
        }

        registerTyped<KotlinProviderIdRequest>(REQUEST_METHOD_KOTLIN_PROVIDER_CONNECT) { req ->
            kotlinStreamingProviderManager.getProvider(req.providerId)?.connect()
            EMPTY_JSON_OBJECT
        }

        registerTyped<KotlinProviderIdRequest>(REQUEST_METHOD_KOTLIN_PROVIDER_DISCONNECT) { req ->
            kotlinStreamingProviderManager.getProvider(req.providerId)?.disconnect()
            EMPTY_JSON_OBJECT
        }

        registerTyped<KotlinProviderIdRequest>(REQUEST_METHOD_KOTLIN_PROVIDER_RELEASE) { req ->
            kotlinStreamingProviderManager.unregister(req.providerId)
            EMPTY_JSON_OBJECT
        }

        registerTyped<CallByReferenceRequest>(REQUEST_METHOD_CALL_BY_REFERENCE) { req ->
            rpcClient.wrappedFunctions.invoke(req.refId, req.args)
        }
    }

    fun dispatchMessage(payload: JsonObject) {
        val kind = payload.optString(ResponseConstants.KEY_KIND)

        when (kind) {
            ResponseConstants.VALUE_KIND_READY -> handleReady(payload)
            ResponseConstants.VALUE_KIND_EVENT -> {
                val event = payload.optJsonObject(ResponseConstants.KEY_EVENT)
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
            ResponseConstants.VALUE_KIND_REQUEST -> handleRequest(payload)
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

                rpcClient.send(BridgeMethodConstants.METHOD_SET_EVENTS_LISTENERS, JsonObject(emptyMap()))
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
            rpcClient.send(BridgeMethodConstants.METHOD_REMOVE_EVENT_LISTENERS, JsonObject(emptyMap()))
            areEventListenersSetUp = false
            Logger.d(TAG, "Event listeners removed from JS bridge (no handlers remaining)")
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to remove event listeners from JS bridge", e)
        }
    }

    fun areEventListenersSetUp(): Boolean = areEventListenersSetUp

    private fun handleRequest(payload: JsonObject) {
        val id = payload.optString(ResponseConstants.KEY_ID)
        val method = payload.optString(ResponseConstants.KEY_METHOD)
        val params = payload.optJsonObject(ResponseConstants.KEY_PARAMS) ?: JsonObject(emptyMap())

        if (id.isEmpty() || method.isEmpty()) {
            Logger.e(TAG, "Reverse-RPC request missing id or method")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = executeNativeRequest(method, params)
                respondToJs(id, result, null)
            } catch (e: Exception) {
                Logger.e(TAG, "Reverse-RPC request failed: method=$method", e)
                respondToJs(id, null, e.message ?: "Unknown error")
            }
        }
    }

    private suspend fun executeNativeRequest(method: String, params: JsonObject): String {
        return requestRegistry.dispatch(method, params)
    }

    /** JS pre-stringifies generic provider params; decode them before handing off to the manager. */
    private inline fun <reified T> decodeParams(paramsJson: String): T = json.decodeFromString(paramsJson)

    private fun requireAdapter(adapterId: String) =
        adapterManager.getAdapter(adapterId)
            ?: throw IllegalArgumentException("Adapter not found: $adapterId")

    private fun respondToJs(id: String, result: String?, errorMessage: String?) {
        val envelope = buildJsonObject {
            put(ResponseConstants.KEY_KIND, ResponseConstants.VALUE_KIND_RESPONSE)
            put(ResponseConstants.KEY_ID, id)
            if (errorMessage != null) {
                put(
                    ResponseConstants.KEY_ERROR,
                    buildJsonObject { put(ResponseConstants.KEY_MESSAGE, errorMessage) },
                )
            } else if (result != null) {
                put(ResponseConstants.KEY_RESULT, result)
            }
        }
        webViewManager.transport.send(envelope.toString())
    }

    private fun handleReady(payload: JsonObject) {
        initManager.updateNetwork(payload.optStringOrNull(ResponseConstants.KEY_NETWORK))
        initManager.updateApiBaseUrl(payload.optStringOrNull(ResponseConstants.KEY_TON_API_URL))

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

        val data = JsonObject(payload.filterKeys { it != ResponseConstants.KEY_KIND })
        val readyEvent = buildJsonObject {
            put(ResponseConstants.KEY_TYPE, ResponseConstants.VALUE_KIND_READY)
            put(ResponseConstants.KEY_DATA, data)
        }
        handleEvent(readyEvent)
    }

    private fun handleEvent(event: JsonObject) {
        val type = event.optString(JsonConstants.KEY_TYPE, EventTypeConstants.EVENT_TYPE_UNKNOWN)
        val data = event.optJsonObject(ResponseConstants.KEY_DATA) ?: JsonObject(emptyMap())
        val eventId = event.optString(JsonConstants.KEY_ID, UUID.randomUUID().toString())

        // Streaming events are routed through the dedicated streaming channel
        val streamingEvent = eventParser.parseStreamingEvent(type, data)
        if (streamingEvent != null) {
            mainHandler.post { _streamingEvents.tryEmit(streamingEvent) }
            return
        }

        val typedEvent = try {
            eventParser.parseEvent(type, data)
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

    private fun handleJsBridgeEvent(payload: JsonObject) {
        val sessionId = payload.optString("sessionId")
        val event = payload.optJsonObject("event")

        if (event == null) {
            Logger.e(TAG, "No event object in ${ResponseConstants.VALUE_KIND_JS_BRIDGE_EVENT} payload")
            return
        }

        mainHandler.post {
            try {
                // If sessionId is empty (e.g., wallet-initiated disconnect), broadcast to all WebViews
                if (sessionId.isEmpty()) {
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
            val errorResponse = buildJsonObject {
                put(ResponseConstants.KEY_KIND, ResponseConstants.VALUE_KIND_RESPONSE)
                put(ResponseConstants.KEY_ID, callId)
                put(
                    ResponseConstants.KEY_ERROR,
                    buildJsonObject {
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

    companion object {
        private const val TAG = LogConstants.TAG_WEBVIEW_ENGINE
        private const val MSG_FAILED_PARSE_TYPED_EVENT_PREFIX = "Failed to parse typed event for type: "
        private const val ERROR_FAILED_SET_UP_EVENT_LISTENERS = "Failed to set up event listeners: "

        private const val EMPTY_JSON_OBJECT = "{}"

        // Reverse-RPC method names (must match the JS bridgeRequest() method strings)
        private const val REQUEST_METHOD_SIGN_WITH_CUSTOM_SIGNER = "signWithCustomSigner"
        private const val REQUEST_METHOD_ADAPTER_GET_STATE_INIT = "adapterGetStateInit"
        private const val REQUEST_METHOD_ADAPTER_SIGN_TRANSACTION = "adapterSignTransaction"
        private const val REQUEST_METHOD_ADAPTER_SIGN_MESSAGE = "adapterSignMessage"
        private const val REQUEST_METHOD_ADAPTER_SIGN_DATA = "adapterSignData"
        private const val REQUEST_METHOD_ADAPTER_SIGN_TON_PROOF = "adapterSignTonProof"
        private const val REQUEST_METHOD_KOTLIN_SWAP_PROVIDER_QUOTE = "kotlinSwapProviderQuote"
        private const val REQUEST_METHOD_KOTLIN_SWAP_PROVIDER_BUILD_SWAP_TRANSACTION = "kotlinSwapProviderBuildSwapTransaction"
        private const val REQUEST_METHOD_KOTLIN_SWAP_PROVIDER_RELEASE = "kotlinSwapProviderRelease"
        private const val REQUEST_METHOD_KOTLIN_STAKING_PROVIDER_GET_QUOTE = "kotlinStakingProviderGetQuote"
        private const val REQUEST_METHOD_KOTLIN_STAKING_PROVIDER_BUILD_STAKE_TRANSACTION = "kotlinStakingProviderBuildStakeTransaction"
        private const val REQUEST_METHOD_KOTLIN_STAKING_PROVIDER_GET_STAKED_BALANCE = "kotlinStakingProviderGetStakedBalance"
        private const val REQUEST_METHOD_KOTLIN_STAKING_PROVIDER_GET_STAKING_PROVIDER_INFO = "kotlinStakingProviderGetStakingProviderInfo"
        private const val REQUEST_METHOD_KOTLIN_STAKING_PROVIDER_RELEASE = "kotlinStakingProviderRelease"
        private const val REQUEST_METHOD_KOTLIN_PROVIDER_WATCH = "kotlinProviderWatch"
        private const val REQUEST_METHOD_KOTLIN_PROVIDER_UNWATCH = "kotlinProviderUnwatch"
        private const val REQUEST_METHOD_KOTLIN_PROVIDER_CONNECT = "kotlinProviderConnect"
        private const val REQUEST_METHOD_KOTLIN_PROVIDER_DISCONNECT = "kotlinProviderDisconnect"
        private const val REQUEST_METHOD_KOTLIN_PROVIDER_RELEASE = "kotlinProviderRelease"
        private const val REQUEST_METHOD_CALL_BY_REFERENCE = "callByReference"
    }
}

private const val TAG_TON_CONNECT_INJECTOR = "tonconnect_injector"

private fun WebView.getTonConnectInjector(): TonConnectInjector? {
    return getTag(TAG_TON_CONNECT_INJECTOR.hashCode()) as? TonConnectInjector
}
