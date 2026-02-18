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

    suspend fun handleTonConnectRequest(
        messageId: String,
        method: String,
        paramsJson: String?,
        url: String?,
        responseCallback: (JSONObject) -> Unit,
    ) {
        try {
            ensureInitialized()

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

            val result = rpcClient.call(BridgeMethodConstants.METHOD_PROCESS_INTERNAL_BROWSER_REQUEST, argsArray)
            responseCallback(result)
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

        // Send array [event, response] - walletkit expects: approveConnectRequest(event, response?)
        val argsArray = JSONArray().apply {
            put(json.toJSONObject(event))
            put(if (response != null) json.toJSONObject(response) else JSONObject.NULL)
        }

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
        val items = result.optJSONArray(ResponseConstants.KEY_ITEMS) ?: JSONArray()

        return buildList(items.length()) {
            for (index in 0 until items.length()) {
                val entry = items.optJSONObject(index) ?: continue

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
