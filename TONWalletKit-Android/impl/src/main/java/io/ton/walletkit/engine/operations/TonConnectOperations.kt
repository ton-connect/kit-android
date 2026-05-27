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
import io.ton.walletkit.api.generated.TONEmbeddedRequestEvent
import io.ton.walletkit.api.generated.TONSendTransactionApprovalResponse
import io.ton.walletkit.api.generated.TONSendTransactionRequestEvent
import io.ton.walletkit.api.generated.TONSignDataApprovalResponse
import io.ton.walletkit.api.generated.TONSignDataRequestEvent
import io.ton.walletkit.api.generated.TONSignMessageApprovalResponse
import io.ton.walletkit.api.generated.TONSignMessageRequestEvent
import io.ton.walletkit.engine.infrastructure.BridgeRpcClient
import io.ton.walletkit.engine.infrastructure.callTyped
import io.ton.walletkit.engine.infrastructure.callTypedOrNull
import io.ton.walletkit.internal.constants.BridgeMethodConstants
import io.ton.walletkit.internal.constants.LogConstants
import io.ton.walletkit.internal.constants.ResponseConstants
import io.ton.walletkit.internal.util.Logger
import io.ton.walletkit.session.TONConnectSession
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.net.URL

private const val TAG = "${LogConstants.TAG_WEBVIEW_ENGINE}:TonConnectOps"
private const val ERROR_WALLET_ADDRESS_REQUIRED = "walletAddress is required for TonConnect approval"
private const val ERROR_WALLET_ID_REQUIRED = "walletId is required for TonConnect approval"

internal suspend fun BridgeRpcClient.handleTonConnectUrl(url: String) {
    send(BridgeMethodConstants.METHOD_HANDLE_TON_CONNECT_URL, url)
}

internal suspend fun BridgeRpcClient.connectionEventFromUrl(url: String): TONConnectionRequestEvent =
    callTyped(BridgeMethodConstants.METHOD_CONNECTION_EVENT_FROM_URL, url)

internal suspend fun BridgeRpcClient.handleTonConnectRequest(
    messageId: String,
    method: String,
    paramsJson: String?,
    url: String?,
    responseCallback: (JsonObject) -> Unit,
    walletId: String? = null,
) {
    try {
        val params: JsonElement = paramsJson?.let { Json.parseToJsonElement(it) }
            ?: JsonArray(emptyList())

        val messageInfo = buildJsonObject {
            put("messageId", messageId)
            put("tabId", messageId)
            put("domain", url?.let(::extractOriginFromUrl) ?: "internal-browser")
            walletId?.let { put("walletId", it) }
        }

        val request = buildJsonObject {
            put("id", messageId)
            put("method", method)
            put("params", params)
        }

        val argsArray = buildJsonArray {
            add(messageInfo)
            add(request)
        }

        responseCallback(call(BridgeMethodConstants.METHOD_PROCESS_INTERNAL_BROWSER_REQUEST, argsArray))
    } catch (e: Exception) {
        Logger.e(TAG, "Failed to process internal browser request", e)
        responseCallback(
            buildJsonObject {
                put(
                    ResponseConstants.KEY_ERROR,
                    buildJsonObject {
                        put(ResponseConstants.KEY_MESSAGE, e.message ?: "Failed to process request")
                        put(ResponseConstants.KEY_CODE, 500)
                    },
                )
            },
        )
    }
}

// Returns scheme://host[:port]. The protocol is required: walletkit's ConnectHandler
// passes this through `new URL(event.domain)` to validate the dApp URL.
private fun extractOriginFromUrl(url: String): String = runCatching {
    val parsed = URL(url)
    val hasExplicitPort = parsed.port != -1 && parsed.port != parsed.defaultPort
    "${parsed.protocol}://${parsed.host}" + if (hasExplicitPort) ":${parsed.port}" else ""
}.getOrDefault("internal-browser")

internal suspend fun BridgeRpcClient.approveConnect(
    event: TONConnectionRequestEvent,
    response: TONConnectionApprovalResponse? = null,
): TONEmbeddedRequestEvent? {
    event.walletAddress ?: throw WalletKitBridgeException(ERROR_WALLET_ADDRESS_REQUIRED)
    event.walletId ?: throw WalletKitBridgeException(ERROR_WALLET_ID_REQUIRED)
    return callTypedOrNull<TONEmbeddedRequestEvent>(
        BridgeMethodConstants.METHOD_APPROVE_CONNECT_REQUEST,
        listOf(event, response),
    )
}

internal suspend fun BridgeRpcClient.rejectConnect(
    event: TONConnectionRequestEvent,
    reason: String?,
    errorCode: Int? = null,
) {
    send(BridgeMethodConstants.METHOD_REJECT_CONNECT_REQUEST, listOf(event, reason, errorCode))
}

internal suspend fun BridgeRpcClient.approveTransaction(
    event: TONSendTransactionRequestEvent,
    response: TONSendTransactionApprovalResponse? = null,
) {
    event.walletAddress ?: throw WalletKitBridgeException(ERROR_WALLET_ADDRESS_REQUIRED)
    event.walletId ?: throw WalletKitBridgeException(ERROR_WALLET_ID_REQUIRED)
    send(BridgeMethodConstants.METHOD_APPROVE_TRANSACTION_REQUEST, listOf(event, response))
}

internal suspend fun BridgeRpcClient.rejectTransaction(
    event: TONSendTransactionRequestEvent,
    reason: String?,
    errorCode: Int? = null,
) {
    val reasonValue: Any? = errorCode?.let { mapOf("code" to it, "message" to (reason ?: "")) } ?: reason
    send(BridgeMethodConstants.METHOD_REJECT_TRANSACTION_REQUEST, listOf(event, reasonValue))
}

internal suspend fun BridgeRpcClient.approveSignData(
    event: TONSignDataRequestEvent,
    response: TONSignDataApprovalResponse? = null,
) {
    event.walletAddress ?: throw WalletKitBridgeException(ERROR_WALLET_ADDRESS_REQUIRED)
    event.walletId ?: throw WalletKitBridgeException(ERROR_WALLET_ID_REQUIRED)
    send(BridgeMethodConstants.METHOD_APPROVE_SIGN_DATA_REQUEST, listOf(event, response))
}

internal suspend fun BridgeRpcClient.rejectSignData(
    event: TONSignDataRequestEvent,
    reason: String?,
    @Suppress("UNUSED_PARAMETER") errorCode: Int? = null,
) {
    send(BridgeMethodConstants.METHOD_REJECT_SIGN_DATA_REQUEST, listOf(event, reason))
}

internal suspend fun BridgeRpcClient.approveSignMessage(
    event: TONSignMessageRequestEvent,
    response: TONSignMessageApprovalResponse? = null,
) {
    event.walletAddress ?: throw WalletKitBridgeException(ERROR_WALLET_ADDRESS_REQUIRED)
    event.walletId ?: throw WalletKitBridgeException(ERROR_WALLET_ID_REQUIRED)
    send(BridgeMethodConstants.METHOD_APPROVE_SIGN_MESSAGE_REQUEST, listOf(event, response))
}

internal suspend fun BridgeRpcClient.rejectSignMessage(
    event: TONSignMessageRequestEvent,
    reason: String?,
    @Suppress("UNUSED_PARAMETER") errorCode: Int? = null,
) {
    send(BridgeMethodConstants.METHOD_REJECT_SIGN_MESSAGE_REQUEST, listOf(event, reason))
}

internal suspend fun BridgeRpcClient.listSessions(): List<TONConnectSession> =
    callTyped(BridgeMethodConstants.METHOD_LIST_SESSIONS)

internal suspend fun BridgeRpcClient.disconnectSession(sessionId: String?) {
    send(BridgeMethodConstants.METHOD_DISCONNECT_SESSION, sessionId)
}
