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
import io.ton.walletkit.engine.infrastructure.BridgeRpcClient
import io.ton.walletkit.engine.infrastructure.toJSONObject
import io.ton.walletkit.engine.operations.requests.ApproveConnectRequest
import io.ton.walletkit.engine.operations.requests.ApproveSignDataRequest
import io.ton.walletkit.engine.operations.requests.ApproveTransactionRequest
import io.ton.walletkit.engine.operations.requests.DisconnectSessionRequest
import io.ton.walletkit.engine.operations.requests.HandleTonConnectUrlRequest
import io.ton.walletkit.engine.operations.requests.ProcessInternalBrowserRequest
import io.ton.walletkit.engine.operations.requests.RejectConnectRequest
import io.ton.walletkit.engine.operations.requests.RejectSignDataRequest
import io.ton.walletkit.engine.operations.requests.RejectTransactionRequest
import io.ton.walletkit.event.ConnectRequestEvent
import io.ton.walletkit.event.SignDataRequestEvent
import io.ton.walletkit.event.TransactionRequestEvent
import io.ton.walletkit.internal.constants.BridgeMethodConstants
import io.ton.walletkit.internal.constants.JsonConstants
import io.ton.walletkit.internal.constants.LogConstants
import io.ton.walletkit.internal.constants.ResponseConstants
import io.ton.walletkit.internal.util.Logger
import io.ton.walletkit.model.WalletSession
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

        val request = HandleTonConnectUrlRequest(url = url)
        rpcClient.call(BridgeMethodConstants.METHOD_HANDLE_TON_CONNECT_URL, json.toJSONObject(request))
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
            Logger.d(TAG, "dApp URL: $url")

            // Parse params string to JsonElement to avoid double-encoding
            val paramsElement = paramsJson?.let {
                try {
                    json.parseToJsonElement(it)
                } catch (e: Exception) {
                    Logger.w(TAG, "Failed to parse params JSON, passing as null: ${e.message}")
                    null
                }
            }

            val request = ProcessInternalBrowserRequest(
                messageId = messageId,
                method = method,
                params = paramsElement,
                url = url,
            )

            Logger.d(TAG, "🔵 Calling processInternalBrowserRequest via bridge...")
            val result = rpcClient.call(BridgeMethodConstants.METHOD_PROCESS_INTERNAL_BROWSER_REQUEST, json.toJSONObject(request))

            Logger.d(TAG, "🟢 Bridge call returned, result: $result")
            Logger.d(TAG, "🟢 Calling responseCallback with result...")

            responseCallback(result)

            Logger.d(TAG, "✅ Internal browser request processed: $method, responseCallback invoked")
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

    suspend fun approveConnect(event: ConnectRequestEvent) {
        ensureInitialized()

        val request = ApproveConnectRequest(
            event = event,
            walletAddress = event.walletAddress ?: throw WalletKitBridgeException(ERROR_WALLET_ADDRESS_REQUIRED),
        )
        rpcClient.call(BridgeMethodConstants.METHOD_APPROVE_CONNECT_REQUEST, json.toJSONObject(request))
    }

    suspend fun rejectConnect(event: ConnectRequestEvent, reason: String?) {
        ensureInitialized()

        val request = RejectConnectRequest(event = event, reason = reason)
        rpcClient.call(BridgeMethodConstants.METHOD_REJECT_CONNECT_REQUEST, json.toJSONObject(request))
    }

    suspend fun approveTransaction(event: TransactionRequestEvent) {
        ensureInitialized()

        val request = ApproveTransactionRequest(event = event)
        rpcClient.call(BridgeMethodConstants.METHOD_APPROVE_TRANSACTION_REQUEST, json.toJSONObject(request))
    }

    suspend fun rejectTransaction(event: TransactionRequestEvent, reason: String?) {
        ensureInitialized()

        val request = RejectTransactionRequest(event = event, reason = reason)
        rpcClient.call(BridgeMethodConstants.METHOD_REJECT_TRANSACTION_REQUEST, json.toJSONObject(request))
    }

    suspend fun approveSignData(event: SignDataRequestEvent) {
        ensureInitialized()

        val request = ApproveSignDataRequest(event = event)
        rpcClient.call(BridgeMethodConstants.METHOD_APPROVE_SIGN_DATA_REQUEST, json.toJSONObject(request))
    }

    suspend fun rejectSignData(event: SignDataRequestEvent, reason: String?) {
        ensureInitialized()

        val request = RejectSignDataRequest(event = event, reason = reason)
        rpcClient.call(BridgeMethodConstants.METHOD_REJECT_SIGN_DATA_REQUEST, json.toJSONObject(request))
    }

    suspend fun listSessions(): List<WalletSession> {
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
                add(
                    WalletSession(
                        sessionId = entry.optString(ResponseConstants.KEY_SESSION_ID),
                        dAppName = entry.optString(ResponseConstants.KEY_DAPP_NAME),
                        walletAddress = entry.optString(ResponseConstants.KEY_WALLET_ADDRESS),
                        dAppUrl = entry.optNullableString(JsonConstants.KEY_DAPP_URL),
                        manifestUrl = entry.optNullableString(JsonConstants.KEY_MANIFEST_URL),
                        iconUrl = entry.optNullableString(JsonConstants.KEY_ICON_URL),
                        createdAtIso = entry.optNullableString(ResponseConstants.KEY_CREATED_AT),
                        lastActivityIso = entry.optNullableString(ResponseConstants.KEY_LAST_ACTIVITY),
                    ),
                )
            }
        }
    }

    suspend fun disconnectSession(sessionId: String?) {
        ensureInitialized()

        val request = DisconnectSessionRequest(sessionId = sessionId)
        rpcClient.call(BridgeMethodConstants.METHOD_DISCONNECT_SESSION, if (sessionId == null) null else json.toJSONObject(request))
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
        internal const val ERROR_WALLET_ADDRESS_REQUIRED = "walletAddress is required for connect approval"
    }
}
