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
package io.ton.walletkit.engine.parsing

import io.ton.walletkit.api.generated.TONConnectionRequestEvent
import io.ton.walletkit.api.generated.TONDisconnectionEvent
import io.ton.walletkit.api.generated.TONDisconnectionEventPreview
import io.ton.walletkit.api.generated.TONRequestErrorEvent
import io.ton.walletkit.api.generated.TONSendTransactionRequestEvent
import io.ton.walletkit.api.generated.TONSignDataRequestEvent
import io.ton.walletkit.engine.WalletKitEngine
import io.ton.walletkit.event.TONWalletKitEvent
import io.ton.walletkit.exceptions.JSValueConversionException
import io.ton.walletkit.internal.constants.EventTypeConstants
import io.ton.walletkit.internal.constants.JsonConstants
import io.ton.walletkit.internal.constants.LogConstants
import io.ton.walletkit.internal.constants.ResponseConstants
import io.ton.walletkit.internal.util.Logger
import io.ton.walletkit.request.TONWalletConnectionRequest
import io.ton.walletkit.request.TONWalletSignDataRequest
import io.ton.walletkit.request.TONWalletTransactionRequest
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.json.JSONObject

/**
 * Parses raw JSON event payloads emitted by the JavaScript bridge into strongly typed SDK events.
 *
 * The parser encapsulates all JSON access patterns and preserves the legacy logging to keep
 * diagnostics identical to the monolithic implementation.
 *
 * @suppress Internal component. Use through [WebViewWalletKitEngine].
 */
internal class EventParser(
    private val json: Json,
    private val engine: WalletKitEngine,
) {
    fun parseEvent(type: String, data: JSONObject, raw: JSONObject): TONWalletKitEvent? =
        when (type) {
            EventTypeConstants.EVENT_CONNECT_REQUEST -> {
                try {
                    val rawJson = data.toString()
                    Logger.d(TAG, "Parsing connect request - raw JSON: $rawJson")
                    val event = json.decodeFromString<TONConnectionRequestEvent>(rawJson)
                    Logger.d(TAG, "Parsing connect request - preview: ${event.preview}, dAppInfo: ${event.dAppInfo}")
                    Logger.d(TAG, "Parsing connect request - requestedItems: ${event.requestedItems}")
                    val request = TONWalletConnectionRequest(
                        event = event,
                        handler = engine,
                    )
                    TONWalletKitEvent.ConnectRequest(request)
                } catch (e: SerializationException) {
                    Logger.e(TAG, ERROR_FAILED_PARSE_CONNECT_REQUEST, e)
                    throw JSValueConversionException.DecodingError(
                        message = "Failed to decode TONConnectionRequestEvent: ${e.message}",
                        cause = e,
                    )
                } catch (e: Exception) {
                    Logger.e(TAG, ERROR_FAILED_PARSE_CONNECT_REQUEST, e)
                    throw JSValueConversionException.Unknown(
                        message = "Failed to parse connect request: ${e.message}",
                        cause = e,
                    )
                }
            }

            EventTypeConstants.EVENT_TRANSACTION_REQUEST -> {
                try {
                    val event = json.decodeFromString<TONSendTransactionRequestEvent>(data.toString())
                    val request = TONWalletTransactionRequest(
                        event = event,
                        handler = engine,
                    )
                    TONWalletKitEvent.TransactionRequest(request)
                } catch (e: SerializationException) {
                    Logger.e(TAG, ERROR_FAILED_PARSE_TRANSACTION_REQUEST, e)
                    throw JSValueConversionException.DecodingError(
                        message = "Failed to decode TONSendTransactionRequestEvent: ${e.message}",
                        cause = e,
                    )
                } catch (e: Exception) {
                    Logger.e(TAG, ERROR_FAILED_PARSE_TRANSACTION_REQUEST, e)
                    throw JSValueConversionException.Unknown(
                        message = "Failed to parse transaction request: ${e.message}",
                        cause = e,
                    )
                }
            }

            EventTypeConstants.EVENT_SIGN_DATA_REQUEST -> {
                try {
                    val event = json.decodeFromString<TONSignDataRequestEvent>(data.toString())
                    val request = TONWalletSignDataRequest(
                        event = event,
                        handler = engine,
                    )
                    TONWalletKitEvent.SignDataRequest(request)
                } catch (e: SerializationException) {
                    Logger.e(TAG, ERROR_FAILED_PARSE_SIGN_DATA_REQUEST, e)
                    throw JSValueConversionException.DecodingError(
                        message = "Failed to decode TONSignDataRequestEvent: ${e.message}",
                        cause = e,
                    )
                } catch (e: Exception) {
                    Logger.e(TAG, ERROR_FAILED_PARSE_SIGN_DATA_REQUEST, e)
                    throw JSValueConversionException.Unknown(
                        message = "Failed to parse sign data request: ${e.message}",
                        cause = e,
                    )
                }
            }

            EventTypeConstants.EVENT_DISCONNECT -> {
                val sessionId =
                    data.optNullableString(ResponseConstants.KEY_SESSION_ID)
                        ?: data.optNullableString(JsonConstants.KEY_ID)
                        ?: return null
                Logger.d(TAG, "Disconnect event received. sessionId=$sessionId, dataKeys=${data.keys().asSequence().toList()}")
                TONWalletKitEvent.Disconnect(
                    TONDisconnectionEvent(
                        id = sessionId,
                        sessionId = sessionId,
                        preview = TONDisconnectionEventPreview(),
                    ),
                )
            }

            EventTypeConstants.EVENT_REQUEST_ERROR -> {
                Logger.d(TAG, "🔴 Parsing EVENT_REQUEST_ERROR event. Data keys: ${data.keys().asSequence().toList()}")
                try {
                    val event = json.decodeFromString<TONRequestErrorEvent>(data.toString())
                    Logger.d(TAG, "✅ Successfully parsed RequestError event: method=${event.data["method"]}, code=${event.error.code}, message=${event.error.message}")
                    TONWalletKitEvent.RequestError(event)
                } catch (e: SerializationException) {
                    Logger.e(TAG, "Failed to decode RequestErrorEvent", e)
                    throw JSValueConversionException.DecodingError(
                        message = "Failed to decode RequestErrorEvent: ${e.message}",
                        cause = e,
                    )
                } catch (e: Exception) {
                    Logger.e(TAG, "Failed to parse RequestErrorEvent", e)
                    throw JSValueConversionException.Unknown(
                        message = "Failed to parse RequestErrorEvent: ${e.message}",
                        cause = e,
                    )
                }
            }

            EventTypeConstants.EVENT_INTENT_REQUEST -> {
                try {
                    Logger.d(TAG, "Parsing intent event - raw JSON: $data")
                    val intentEvent = parseIntentEvent(data)
                    TONWalletKitEvent.IntentRequest(intentEvent)
                } catch (e: SerializationException) {
                    Logger.e(TAG, "Failed to parse IntentEvent", e)
                    throw JSValueConversionException.DecodingError(
                        message = "Failed to decode IntentEvent: ${e.message}",
                        cause = e,
                    )
                } catch (e: Exception) {
                    Logger.e(TAG, "Failed to parse IntentEvent", e)
                    throw JSValueConversionException.Unknown(
                        message = "Failed to parse IntentEvent: ${e.message}",
                        cause = e,
                    )
                }
            }

            // Internal browser events - not exposed to public API
            EventTypeConstants.EVENT_BROWSER_PAGE_STARTED,
            EventTypeConstants.EVENT_BROWSER_PAGE_FINISHED,
            EventTypeConstants.EVENT_BROWSER_ERROR,
            EventTypeConstants.EVENT_BROWSER_BRIDGE_REQUEST,
            EventTypeConstants.EVENT_STATE_CHANGED,
            EventTypeConstants.EVENT_WALLET_STATE_CHANGED,
            EventTypeConstants.EVENT_SESSIONS_CHANGED,
            -> null

            else -> null
        }

    /**
     * Parse intent event from JSON data.
     * The intent type determines the structure of the event.
     */
    private fun parseIntentEvent(data: JSONObject): io.ton.walletkit.event.TONIntentEvent {
        val id = data.optString("id", "")
        val clientId = data.optString("clientId", "")
        val hasConnectRequest = data.optBoolean("hasConnectRequest", false)
        val type = data.optString("type", "")

        // Parse connect request if present
        val connectRequest = parseConnectRequestFromIntent(data.optJSONObject("connectRequest"))

        return when (type) {
            "txIntent", "signMsg" -> {
                val itemsArray = data.optJSONArray("items")
                val items = mutableListOf<io.ton.walletkit.event.TONIntentItem>()
                if (itemsArray != null) {
                    for (i in 0 until itemsArray.length()) {
                        val itemJson = itemsArray.getJSONObject(i)
                        val itemType = itemJson.optString("t", "")
                        val item = when (itemType) {
                            "ton" -> io.ton.walletkit.event.TONIntentItem.SendTon(
                                address = itemJson.optString("a", ""),
                                amount = itemJson.optString("am", ""),
                                payload = itemJson.optNullableString("p"),
                                stateInit = itemJson.optNullableString("si"),
                            )
                            "jetton" -> io.ton.walletkit.event.TONIntentItem.SendJetton(
                                masterAddress = itemJson.optString("ma", ""),
                                jettonAmount = itemJson.optString("ja", ""),
                                destination = itemJson.optString("d", ""),
                                queryId = if (itemJson.has("qi")) itemJson.optLong("qi") else null,
                                responseDestination = itemJson.optNullableString("rd"),
                                customPayload = itemJson.optNullableString("cp"),
                                forwardTonAmount = itemJson.optNullableString("fta"),
                                forwardPayload = itemJson.optNullableString("fp"),
                            )
                            "nft" -> io.ton.walletkit.event.TONIntentItem.SendNft(
                                nftAddress = itemJson.optString("na", ""),
                                newOwner = itemJson.optString("no", ""),
                                queryId = if (itemJson.has("qi")) itemJson.optLong("qi") else null,
                                responseDestination = itemJson.optNullableString("rd"),
                                customPayload = itemJson.optNullableString("cp"),
                                forwardTonAmount = itemJson.optNullableString("fta"),
                                forwardPayload = itemJson.optNullableString("fp"),
                            )
                            else -> throw IllegalArgumentException("Unknown intent item type: $itemType")
                        }
                        items.add(item)
                    }
                }
                io.ton.walletkit.event.TONIntentEvent.TransactionIntent(
                    id = id,
                    clientId = clientId,
                    hasConnectRequest = hasConnectRequest,
                    type = type,
                    connectRequest = connectRequest,
                    network = data.optNullableString("network"),
                    validUntil = if (data.has("validUntil")) data.optLong("validUntil") else null,
                    items = items,
                )
            }
            "signIntent" -> {
                val payloadJson = data.optJSONObject("payload")
                val payloadType = payloadJson?.optString("type", "") ?: ""
                val payload = when (payloadType) {
                    "text" -> io.ton.walletkit.event.TONSignDataPayload.Text(
                        text = payloadJson?.optString("text", "") ?: "",
                    )
                    "binary" -> io.ton.walletkit.event.TONSignDataPayload.Binary(
                        bytes = payloadJson?.optString("bytes", "") ?: "",
                    )
                    "cell" -> io.ton.walletkit.event.TONSignDataPayload.Cell(
                        schema = payloadJson?.optString("schema", "") ?: "",
                        cell = payloadJson?.optString("cell", "") ?: "",
                    )
                    else -> throw IllegalArgumentException("Unknown sign data payload type: $payloadType")
                }
                io.ton.walletkit.event.TONIntentEvent.SignDataIntent(
                    id = id,
                    clientId = clientId,
                    hasConnectRequest = hasConnectRequest,
                    connectRequest = connectRequest,
                    manifestUrl = data.optString("manifestUrl", ""),
                    payload = payload,
                )
            }
            "actionIntent" -> {
                io.ton.walletkit.event.TONIntentEvent.ActionIntent(
                    id = id,
                    clientId = clientId,
                    hasConnectRequest = hasConnectRequest,
                    connectRequest = connectRequest,
                    actionUrl = data.optString("actionUrl", ""),
                )
            }
            else -> throw IllegalArgumentException("Unknown intent type: $type")
        }
    }

    private fun JSONObject.optNullableString(key: String): String? {
        val value = opt(key)
        return when (value) {
            null, JSONObject.NULL -> null
            else -> value.toString()
        }
    }

    /**
     * Parse connect request from intent event JSON.
     */
    private fun parseConnectRequestFromIntent(json: JSONObject?): io.ton.walletkit.event.TONIntentConnectRequest? {
        if (json == null) return null
        val manifestUrl = json.optString("manifestUrl", "")
        if (manifestUrl.isEmpty()) return null

        val itemsArray = json.optJSONArray("items")
        val items = if (itemsArray != null) {
            (0 until itemsArray.length()).map { i ->
                val itemJson = itemsArray.getJSONObject(i)
                io.ton.walletkit.event.TONIntentConnectItem(
                    name = itemJson.optString("name", ""),
                    payload = itemJson.optNullableString("payload"),
                )
            }
        } else {
            null
        }

        return io.ton.walletkit.event.TONIntentConnectRequest(
            manifestUrl = manifestUrl,
            items = items,
        )
    }

    private companion object {
        private const val TAG = LogConstants.TAG_WEBVIEW_ENGINE
        private const val ERROR_FAILED_PARSE_CONNECT_REQUEST = "Failed to parse TONConnectionRequestEvent"
        private const val ERROR_FAILED_PARSE_TRANSACTION_REQUEST = "Failed to parse TONSendTransactionRequestEvent"
        private const val ERROR_FAILED_PARSE_SIGN_DATA_REQUEST = "Failed to parse TONSignDataRequestEvent"
    }
}
