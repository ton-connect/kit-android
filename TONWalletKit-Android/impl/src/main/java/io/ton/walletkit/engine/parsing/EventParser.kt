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
                    val event = json.decodeFromString<TONConnectionRequestEvent>(rawJson)
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
                    TONWalletKitEvent.SendTransactionRequest(request)
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
                try {
                    val event = json.decodeFromString<TONRequestErrorEvent>(data.toString())
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

    private fun JSONObject.optNullableString(key: String): String? {
        val value = opt(key)
        return when (value) {
            null, JSONObject.NULL -> null
            else -> value.toString()
        }
    }

    private companion object {
        private const val TAG = LogConstants.TAG_WEBVIEW_ENGINE
        private const val ERROR_FAILED_PARSE_CONNECT_REQUEST = "Failed to parse TONConnectionRequestEvent"
        private const val ERROR_FAILED_PARSE_TRANSACTION_REQUEST = "Failed to parse TONSendTransactionRequestEvent"
        private const val ERROR_FAILED_PARSE_SIGN_DATA_REQUEST = "Failed to parse TONSignDataRequestEvent"
    }
}
