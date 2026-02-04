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
package io.ton.walletkit.engine.operations

import io.ton.walletkit.WalletKitBridgeException
import io.ton.walletkit.api.generated.TONConnectionApprovalResponse
import io.ton.walletkit.api.generated.TONConnectionRequestEvent
import io.ton.walletkit.api.generated.TONSendTransactionApprovalResponse
import io.ton.walletkit.api.generated.TONSendTransactionRequestEvent
import io.ton.walletkit.api.generated.TONSignDataApprovalResponse
import io.ton.walletkit.api.generated.TONSignDataRequestEvent
import io.ton.walletkit.engine.infrastructure.BridgeRpcClient
import io.ton.walletkit.engine.infrastructure.toJSONObject
import io.ton.walletkit.engine.operations.requests.ActionIntentEventBridgeDto
import io.ton.walletkit.engine.operations.requests.ApproveActionIntentRequest
import io.ton.walletkit.engine.operations.requests.ApproveSignDataIntentRequest
import io.ton.walletkit.engine.operations.requests.ApproveTransactionIntentRequest
import io.ton.walletkit.engine.operations.requests.ConnectItemBridgeDto
import io.ton.walletkit.engine.operations.requests.ConnectRequestBridgeDto
import io.ton.walletkit.engine.operations.requests.ConnectionApprovalProofBridgeDto
import io.ton.walletkit.engine.operations.requests.HandleIntentUrlRequest
import io.ton.walletkit.engine.operations.requests.IntentEventBridgeDto
import io.ton.walletkit.engine.operations.requests.IntentEventRefBridgeDto
import io.ton.walletkit.engine.operations.requests.IntentEventWithConnectBridgeDto
import io.ton.walletkit.engine.operations.requests.IntentItemBridgeDto
import io.ton.walletkit.engine.operations.requests.IntentItemsToTransactionRequestRequest
import io.ton.walletkit.engine.operations.requests.IsIntentUrlRequest
import io.ton.walletkit.engine.operations.requests.ProcessConnectAfterIntentRequest
import io.ton.walletkit.engine.operations.requests.RejectIntentRequest
import io.ton.walletkit.engine.operations.requests.SignDataIntentEventBridgeDto
import io.ton.walletkit.engine.operations.requests.SignDataIntentPayloadBridgeDto
import io.ton.walletkit.engine.operations.requests.TransactionIntentEventBridgeDto
import io.ton.walletkit.event.TONIntentConnectRequest
import io.ton.walletkit.event.TONIntentEvent
import io.ton.walletkit.event.TONIntentItem
import io.ton.walletkit.event.TONSignDataPayload
import io.ton.walletkit.internal.constants.BridgeMethodConstants
import io.ton.walletkit.internal.constants.JsonConstants
import io.ton.walletkit.internal.constants.LogConstants
import io.ton.walletkit.internal.constants.ResponseConstants
import io.ton.walletkit.internal.util.Logger
import io.ton.walletkit.model.TONUserFriendlyAddress
import io.ton.walletkit.session.TONConnectSession
import kotlinx.serialization.json.Json
import org.json.JSONArray
import org.json.JSONObject

/**
 * Wraps TON Connect bridge calls such as processing URLs, responding to connect/sign
 * requests, and session lifecycle management.
 *
 * @property ensureInitialized Suspended callback that ensures the bridge is ready.
 * @property rpcClient Bridge RPC client.
 * @property json Serializer used for transforming Kotlin data classes to JSON payloads.
 *
 * @suppress Internal component used by [WebViewWalletKitEngine].
 */
internal class TonConnectOperations(
    private val ensureInitialized: suspend () -> Unit,
    private val rpcClient: BridgeRpcClient,
    private val json: Json,
) {

    suspend fun handleTonConnectUrl(url: String) {
        ensureInitialized()

        // Send just the URL string - walletkit expects: handleTonConnectUrl(url: string)
        rpcClient.call(BridgeMethodConstants.METHOD_HANDLE_TON_CONNECT_URL, url)
    }

    /**
     * Handle an intent URL (tc://intent_inline?...).
     * Parses the URL and emits an intent event for the wallet UI.
     */
    suspend fun handleIntentUrl(url: String) {
        ensureInitialized()

        val request = HandleIntentUrlRequest(url = url)
        rpcClient.call(BridgeMethodConstants.METHOD_HANDLE_INTENT_URL, json.toJSONObject(request))
    }

    /**
     * Check if a URL is an intent URL (tc://intent_inline?... or tc://intent?...).
     */
    suspend fun isIntentUrl(url: String): Boolean {
        ensureInitialized()

        val request = IsIntentUrlRequest(url = url)
        val result = rpcClient.call(BridgeMethodConstants.METHOD_IS_INTENT_URL, json.toJSONObject(request))
        return result.optBoolean(ResponseConstants.KEY_VALUE, false)
    }

    /**
     * Convert intent items to a transaction request.
     * Used when approving an intent to build the actual transaction.
     */
    suspend fun intentItemsToTransactionRequest(
        intentType: String,
        items: List<IntentItemBridgeDto>,
        walletId: String,
        network: String? = null,
        validUntil: Long? = null,
    ): JSONObject {
        ensureInitialized()

        val event = IntentEventBridgeDto(
            id = System.currentTimeMillis().toString(),
            type = intentType,
            network = network,
            validUntil = validUntil,
            items = items,
        )

        val request = IntentItemsToTransactionRequestRequest(
            event = event,
            walletId = walletId,
        )

        return rpcClient.call(
            BridgeMethodConstants.METHOD_INTENT_ITEMS_TO_TRANSACTION_REQUEST,
            json.toJSONObject(request),
        )
    }

    /**
     * Approve a transaction intent (txIntent or signMsg).
     *
     * For txIntent: Signs and sends the transaction to the blockchain.
     * For signMsg: Signs but does NOT send (for gasless transactions).
     *
     * @param event The transaction intent event to approve.
     * @param walletId The wallet ID to use for signing.
     * @return The signed BoC as base64 string.
     */
    suspend fun approveTransactionIntent(
        event: TONIntentEvent.TransactionIntent,
        walletId: String,
    ): String {
        ensureInitialized()

        val itemDtos = event.items.map { item ->
            when (item) {
                is TONIntentItem.SendTon -> IntentItemBridgeDto(
                    t = "ton",
                    a = item.address,
                    am = item.amount.toString(),
                    p = item.payload,
                    si = item.stateInit,
                )
                is TONIntentItem.SendJetton -> IntentItemBridgeDto(
                    t = "jetton",
                    ma = item.masterAddress,
                    qi = item.queryId,
                    ja = item.jettonAmount.toString(),
                    d = item.destination,
                    rd = item.responseDestination,
                    cp = item.customPayload,
                    fta = item.forwardTonAmount?.toString(),
                    fp = item.forwardPayload,
                )
                is TONIntentItem.SendNft -> IntentItemBridgeDto(
                    t = "nft",
                    na = item.nftAddress,
                    qi = item.queryId,
                    no = item.newOwner,
                    rd = item.responseDestination,
                    cp = item.customPayload,
                    fta = item.forwardTonAmount?.toString(),
                    fp = item.forwardPayload,
                )
            }
        }

        val request = ApproveTransactionIntentRequest(
            event = TransactionIntentEventBridgeDto(
                id = event.id,
                clientId = event.clientId,
                hasConnectRequest = event.hasConnectRequest,
                type = event.type,
                network = event.network,
                validUntil = event.validUntil,
                items = itemDtos,
            ),
            walletId = walletId,
        )

        val result = rpcClient.call(
            BridgeMethodConstants.METHOD_APPROVE_TRANSACTION_INTENT,
            json.toJSONObject(request),
        )
        return result.getString("result")
    }

    /**
     * Approve a sign data intent (signIntent).
     *
     * @param event The sign data intent event to approve.
     * @param walletId The wallet ID to use for signing.
     * @return The signature result as JSONObject.
     */
    suspend fun approveSignDataIntent(
        event: TONIntentEvent.SignDataIntent,
        walletId: String,
    ): JSONObject {
        ensureInitialized()

        val payloadDto = when (val payload = event.payload) {
            is TONSignDataPayload.Text -> SignDataIntentPayloadBridgeDto(
                type = "text",
                text = payload.text,
            )
            is TONSignDataPayload.Binary -> SignDataIntentPayloadBridgeDto(
                type = "binary",
                bytes = payload.bytes,
            )
            is TONSignDataPayload.Cell -> SignDataIntentPayloadBridgeDto(
                type = "cell",
                schema = payload.schema,
                cell = payload.cell,
            )
        }

        val request = ApproveSignDataIntentRequest(
            event = SignDataIntentEventBridgeDto(
                id = event.id,
                clientId = event.clientId,
                hasConnectRequest = event.hasConnectRequest,
                type = "signIntent",
                network = event.network,
                manifestUrl = event.manifestUrl,
                payload = payloadDto,
            ),
            walletId = walletId,
        )

        return rpcClient.call(
            BridgeMethodConstants.METHOD_APPROVE_SIGN_DATA_INTENT,
            json.toJSONObject(request),
        )
    }

    /**
     * Reject an intent request.
     *
     * @param event The intent event to reject.
     * @param reason Optional rejection reason.
     * @param errorCode Optional error code (defaults to 300 = USER_DECLINED).
     * @return The rejection response.
     */
    suspend fun rejectIntent(
        event: TONIntentEvent,
        reason: String? = null,
        errorCode: Int? = null,
    ): JSONObject {
        ensureInitialized()

        val (id, clientId, type) = when (event) {
            is TONIntentEvent.TransactionIntent -> Triple(event.id, event.clientId, event.type)
            is TONIntentEvent.SignDataIntent -> Triple(event.id, event.clientId, "signIntent")
            is TONIntentEvent.ActionIntent -> Triple(event.id, event.clientId, "actionIntent")
        }

        val request = RejectIntentRequest(
            event = IntentEventRefBridgeDto(id = id, clientId = clientId, type = type),
            reason = reason,
            errorCode = errorCode,
        )

        return rpcClient.call(
            BridgeMethodConstants.METHOD_REJECT_INTENT,
            json.toJSONObject(request),
        )
    }

    /**
     * Approve an action intent (actionIntent).
     *
     * The wallet fetches action details from the action URL and executes the action.
     *
     * @param event The action intent event to approve.
     * @param walletId The wallet ID to use for signing.
     * @return The action result as JSONObject.
     */
    suspend fun approveActionIntent(
        event: TONIntentEvent.ActionIntent,
        walletId: String,
    ): JSONObject {
        ensureInitialized()

        val request = ApproveActionIntentRequest(
            event = ActionIntentEventBridgeDto(
                id = event.id,
                clientId = event.clientId,
                hasConnectRequest = event.hasConnectRequest,
                type = "actionIntent",
                actionUrl = event.actionUrl,
            ),
            walletId = walletId,
        )

        return rpcClient.call(
            BridgeMethodConstants.METHOD_APPROVE_ACTION_INTENT,
            json.toJSONObject(request),
        )
    }

    /**
     * Process connect request after intent approval.
     *
     * Creates a proper session for the dApp after intent approval.
     *
     * @param event The intent event with connect request.
     * @param walletId The wallet ID to use for the connection.
     * @param proof Optional proof for ton_proof.
     */
    suspend fun processConnectAfterIntent(
        event: TONIntentEvent,
        walletId: String,
        proof: ConnectionApprovalProofBridgeDto? = null,
    ) {
        ensureInitialized()

        val (id, clientId, hasConnectRequest, type, connectReq) = when (event) {
            is TONIntentEvent.TransactionIntent -> listOf(event.id, event.clientId, event.hasConnectRequest, event.type, event.connectRequest)
            is TONIntentEvent.SignDataIntent -> listOf(event.id, event.clientId, event.hasConnectRequest, "signIntent", event.connectRequest)
            is TONIntentEvent.ActionIntent -> listOf(event.id, event.clientId, event.hasConnectRequest, "actionIntent", event.connectRequest)
        }

        // Convert TONIntentConnectRequest to bridge DTO
        val connectRequest = (connectReq as? TONIntentConnectRequest)?.let { req ->
            ConnectRequestBridgeDto(
                manifestUrl = req.manifestUrl,
                items = req.items?.map { item ->
                    ConnectItemBridgeDto(
                        name = item.name,
                        payload = item.payload,
                    )
                },
            )
        }

        val request = ProcessConnectAfterIntentRequest(
            event = IntentEventWithConnectBridgeDto(
                id = id as String,
                clientId = clientId as String,
                hasConnectRequest = hasConnectRequest as Boolean,
                type = type as String,
                connectRequest = connectRequest,
            ),
            walletId = walletId,
            proof = proof,
        )

        rpcClient.call(
            BridgeMethodConstants.METHOD_PROCESS_CONNECT_AFTER_INTENT,
            json.toJSONObject(request),
        )
    }

    suspend fun handleTonConnectRequest(
        messageId: String,
        method: String,
        paramsJson: String?,
        url: String?,
        responseCallback: (JSONObject) -> Unit,
    ) {
        try {
            ensureInitialized()

            Logger.d(TAG, "Processing internal browser request: $method (messageId: $messageId)")

            // Parse params - could be either JSONObject (for connect) or JSONArray (for other methods)
            val params: Any = paramsJson?.let {
                try {
                    // Try as JSONObject first (for connect method which has {manifestUrl, items, ...})
                    JSONObject(it)
                } catch (e: Exception) {
                    try {
                        // Fall back to JSONArray (for other methods)
                        JSONArray(it)
                    } catch (e2: Exception) {
                        // Last resort - empty array
                        JSONArray()
                    }
                }
            } ?: JSONArray()

            // Build messageInfo object
            // Note: domain should be origin (protocol + host) like "https://example.com", not just host
            val messageInfo = JSONObject().apply {
                put("messageId", messageId)
                put("tabId", messageId)
                put(
                    "domain",
                    url?.let {
                        try {
                            val parsedUrl = java.net.URL(it)
                            "${parsedUrl.protocol}://${parsedUrl.host}" + (if (parsedUrl.port != -1 && parsedUrl.port != parsedUrl.defaultPort) ":${parsedUrl.port}" else "")
                        } catch (e: Exception) {
                            "internal-browser"
                        }
                    } ?: "internal-browser",
                )
            }

            // Build request object
            val request = JSONObject().apply {
                put("id", messageId)
                put("method", method)
                put("params", params)
            }

            // Send array [messageInfo, request]
            val argsArray = JSONArray().apply {
                put(messageInfo)
                put(request)
            }

            Logger.d(TAG, "🔵 Calling processInternalBrowserRequest via bridge...")
            val result = rpcClient.call(BridgeMethodConstants.METHOD_PROCESS_INTERNAL_BROWSER_REQUEST, argsArray)

            Logger.d(TAG, "🟢 Bridge call returned, result: $result")
            responseCallback(result)

            Logger.d(TAG, "✅ Internal browser request processed: $method")
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to process internal browser request", e)
            val errorResponse =
                JSONObject().apply {
                    put(
                        ResponseConstants.KEY_ERROR,
                        JSONObject().apply {
                            put(ResponseConstants.KEY_MESSAGE, e.message ?: ERROR_FAILED_PROCESS_REQUEST)
                            put(ResponseConstants.KEY_CODE, 500)
                        },
                    )
                }
            responseCallback(errorResponse)
        }
    }

    suspend fun approveConnect(
        event: TONConnectionRequestEvent,
        response: TONConnectionApprovalResponse? = null,
    ) {
        ensureInitialized()

        val walletAddress = event.walletAddress ?: throw WalletKitBridgeException(ERROR_WALLET_ADDRESS_REQUIRED)
        val walletId = event.walletId ?: throw WalletKitBridgeException("Wallet ID is required")

        Logger.d(TAG, "approveConnect - event.requestedItems: ${event.requestedItems}")

        // Send array [event, response] - walletkit expects: approveConnectRequest(event, response?)
        val argsArray = JSONArray().apply {
            put(json.toJSONObject(event))
            put(if (response != null) json.toJSONObject(response) else JSONObject.NULL)
        }

        Logger.d(TAG, "approveConnect - serialized JSON array: $argsArray")

        rpcClient.call(BridgeMethodConstants.METHOD_APPROVE_CONNECT_REQUEST, argsArray)
    }

    suspend fun rejectConnect(event: TONConnectionRequestEvent, reason: String?, errorCode: Int? = null) {
        ensureInitialized()

        // Send array [event, reason, errorCode] - walletkit expects: rejectConnectRequest(event, reason?, errorCode?)
        val argsArray = JSONArray().apply {
            put(json.toJSONObject(event))
            put(reason ?: JSONObject.NULL)
            put(errorCode ?: JSONObject.NULL)
        }
        rpcClient.call(BridgeMethodConstants.METHOD_REJECT_CONNECT_REQUEST, argsArray)
    }

    suspend fun approveTransaction(
        event: TONSendTransactionRequestEvent,
        response: TONSendTransactionApprovalResponse? = null,
    ) {
        ensureInitialized()

        val walletAddress = event.walletAddress ?: throw WalletKitBridgeException(ERROR_WALLET_ADDRESS_REQUIRED)
        val walletId = event.walletId ?: throw WalletKitBridgeException(ERROR_WALLET_ID_REQUIRED)

        // Send array [event, response] - walletkit expects: approveTransactionRequest(event, response?)
        val argsArray = JSONArray().apply {
            put(json.toJSONObject(event))
            put(if (response != null) json.toJSONObject(response) else JSONObject.NULL)
        }
        rpcClient.call(BridgeMethodConstants.METHOD_APPROVE_TRANSACTION_REQUEST, argsArray)
    }

    suspend fun rejectTransaction(event: TONSendTransactionRequestEvent, reason: String?, errorCode: Int? = null) {
        ensureInitialized()

        // Send array [event, reason] - walletkit expects: rejectTransactionRequest(event, reason?)
        // reason can be string or {code, message} object
        val reasonValue = if (errorCode != null) {
            JSONObject().apply {
                put("code", errorCode)
                put("message", reason ?: "")
            }
        } else {
            reason ?: JSONObject.NULL
        }
        val argsArray = JSONArray().apply {
            put(json.toJSONObject(event))
            put(reasonValue)
        }
        rpcClient.call(BridgeMethodConstants.METHOD_REJECT_TRANSACTION_REQUEST, argsArray)
    }

    suspend fun approveSignData(
        event: TONSignDataRequestEvent,
        response: TONSignDataApprovalResponse? = null,
    ) {
        ensureInitialized()

        val walletAddress = event.walletAddress ?: throw WalletKitBridgeException(ERROR_WALLET_ADDRESS_REQUIRED)
        val walletId = event.walletId ?: throw WalletKitBridgeException(ERROR_WALLET_ID_REQUIRED)

        // Send array [event, response] - walletkit expects: approveSignDataRequest(event, response?)
        val argsArray = JSONArray().apply {
            put(json.toJSONObject(event))
            put(if (response != null) json.toJSONObject(response) else JSONObject.NULL)
        }
        rpcClient.call(BridgeMethodConstants.METHOD_APPROVE_SIGN_DATA_REQUEST, argsArray)
    }

    suspend fun rejectSignData(event: TONSignDataRequestEvent, reason: String?, errorCode: Int? = null) {
        ensureInitialized()

        // Send array [event, reason] - walletkit expects: rejectSignDataRequest(event, reason?)
        val argsArray = JSONArray().apply {
            put(json.toJSONObject(event))
            put(reason ?: JSONObject.NULL)
        }
        rpcClient.call(BridgeMethodConstants.METHOD_REJECT_SIGN_DATA_REQUEST, argsArray)
    }

    suspend fun listSessions(): List<TONConnectSession> {
        ensureInitialized()

        val result = rpcClient.call(BridgeMethodConstants.METHOD_LIST_SESSIONS)
        Logger.d(TAG, "listSessions raw result: $result")
        val items = result.optJSONArray(ResponseConstants.KEY_ITEMS) ?: JSONArray()

        return buildList(items.length()) {
            for (index in 0 until items.length()) {
                val entry = items.optJSONObject(index) ?: continue
                Logger.d(
                    TAG,
                    "listSessions entry[$index]: keys=${entry.keys().asSequence().toList()}, sessionId=${entry.optString(ResponseConstants.KEY_SESSION_ID)}",
                )

                // Parse dAppInfo from the session entry (for backwards compatibility)
                val dAppInfoJson = entry.optJSONObject(JsonConstants.KEY_DAPP_INFO)

                add(
                    TONConnectSession(
                        sessionId = entry.optString(ResponseConstants.KEY_SESSION_ID),
                        walletId = entry.optString(JsonConstants.KEY_WALLET_ID),
                        walletAddress = TONUserFriendlyAddress(entry.optString(ResponseConstants.KEY_WALLET_ADDRESS)),
                        createdAt = entry.optString(ResponseConstants.KEY_CREATED_AT),
                        lastActivityAt = entry.optString(ResponseConstants.KEY_LAST_ACTIVITY),
                        privateKey = entry.optString(JsonConstants.KEY_PRIVATE_KEY),
                        publicKey = entry.optString(JsonConstants.KEY_PUBLIC_KEY),
                        domain = entry.optString(JsonConstants.KEY_DOMAIN),
                        schemaVersion = entry.optInt("schemaVersion", 1),
                        dAppName = dAppInfoJson?.optString("name") ?: entry.optNullableString("dAppName"),
                        dAppDescription = dAppInfoJson?.optNullableString("description") ?: entry.optNullableString("dAppDescription"),
                        dAppUrl = dAppInfoJson?.optNullableString("url") ?: entry.optNullableString("dAppUrl"),
                        dAppIconUrl = dAppInfoJson?.optNullableString("iconUrl") ?: entry.optNullableString("dAppIconUrl"),
                        isJsBridge = entry.optBoolean(JsonConstants.KEY_IS_JS_BRIDGE, false),
                    ),
                )
            }
        }
    }

    suspend fun disconnectSession(sessionId: String?) {
        ensureInitialized()

        // Send just the sessionId string - walletkit expects: disconnect(sessionId?: string)
        rpcClient.call(BridgeMethodConstants.METHOD_DISCONNECT_SESSION, sessionId)
    }

    private fun JSONObject.optNullableString(key: String): String? {
        val value = opt(key)
        return when (value) {
            null, JSONObject.NULL -> null
            else -> value.toString()
        }
    }

    companion object {
        private const val TAG = "${LogConstants.TAG_WEBVIEW_ENGINE}:TonConnectOps"

        internal const val ERROR_FAILED_PROCESS_REQUEST = "Failed to process request"
        internal const val ERROR_WALLET_ADDRESS_REQUIRED = "walletAddress is required for TonConnect approval"
        internal const val ERROR_WALLET_ID_REQUIRED = "walletId is required for TonConnect approval"
    }
}
