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

import io.ton.walletkit.engine.WalletKitEngine
import io.ton.walletkit.event.ConnectRequestEvent
import io.ton.walletkit.event.SignDataRequestEvent
import io.ton.walletkit.event.TONWalletKitEvent
import io.ton.walletkit.event.TransactionRequestEvent
import io.ton.walletkit.exceptions.JSValueConversionException
import io.ton.walletkit.internal.constants.EventTypeConstants
import io.ton.walletkit.internal.constants.JsonConstants
import io.ton.walletkit.internal.constants.LogConstants
import io.ton.walletkit.internal.constants.ResponseConstants
import io.ton.walletkit.internal.util.Logger
import io.ton.walletkit.model.DAppInfo
import io.ton.walletkit.model.TONNetwork
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
                    val event = json.decodeFromString<ConnectRequestEvent>(data.toString())
                    // Normalize manifest URL to include https:// if it's just a domain
                    val normalizedEvent = normalizeManifestUrl(event)
                    val dAppInfo = parseDAppInfo(data)
                    val permissions = normalizedEvent.preview?.permissions ?: emptyList()
                    val manifestFetchErrorCode = normalizedEvent.preview?.manifestFetchErrorCode
                    Logger.d(TAG, "Parsing connect request - preview: ${normalizedEvent.preview}, manifestFetchErrorCode: $manifestFetchErrorCode")
                    val request =
                        TONWalletConnectionRequest(
                            dAppInfo = dAppInfo,
                            permissions = permissions,
                            manifestFetchErrorCode = manifestFetchErrorCode,
                            tonNetwork = resolveNetwork(),
                            event = normalizedEvent,
                            handler = engine,
                        )
                    TONWalletKitEvent.ConnectRequest(request)
                } catch (e: SerializationException) {
                    Logger.e(TAG, ERROR_FAILED_PARSE_CONNECT_REQUEST, e)
                    throw JSValueConversionException.DecodingError(
                        message = "Failed to decode ConnectRequestEvent: ${e.message}",
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
                    val event = json.decodeFromString<TransactionRequestEvent>(data.toString())
                    val dAppInfo = parseDAppInfo(data)

                    val request =
                        TONWalletTransactionRequest(
                            dAppInfo = dAppInfo,
                            tonNetwork = resolveNetwork(),
                            event = event,
                            handler = engine,
                        )
                    TONWalletKitEvent.TransactionRequest(request)
                } catch (e: SerializationException) {
                    Logger.e(TAG, ERROR_FAILED_PARSE_TRANSACTION_REQUEST, e)
                    throw JSValueConversionException.DecodingError(
                        message = "Failed to decode TransactionRequestEvent: ${e.message}",
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
                    val event = json.decodeFromString<SignDataRequestEvent>(data.toString())
                    val dAppInfo = parseDAppInfo(data)
                    val request =
                        TONWalletSignDataRequest(
                            dAppInfo = dAppInfo,
                            walletAddress = event.walletAddress,
                            tonNetwork = resolveNetwork(),
                            event = event,
                            handler = engine,
                        )
                    TONWalletKitEvent.SignDataRequest(request)
                } catch (e: SerializationException) {
                    Logger.e(TAG, ERROR_FAILED_PARSE_SIGN_DATA_REQUEST, e)
                    throw JSValueConversionException.DecodingError(
                        message = "Failed to decode SignDataRequestEvent: ${e.message}",
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
                TONWalletKitEvent.Disconnect(io.ton.walletkit.event.DisconnectEvent(sessionId))
            }

            EventTypeConstants.EVENT_BROWSER_PAGE_STARTED -> {
                val url = data.optString("url", "")
                TONWalletKitEvent.BrowserPageStarted(url)
            }

            EventTypeConstants.EVENT_BROWSER_PAGE_FINISHED -> {
                val url = data.optString("url", "")
                TONWalletKitEvent.BrowserPageFinished(url)
            }

            EventTypeConstants.EVENT_BROWSER_ERROR -> {
                val message = data.optString("message", "Unknown error")
                TONWalletKitEvent.BrowserError(message)
            }

            EventTypeConstants.EVENT_BROWSER_BRIDGE_REQUEST -> {
                val messageId = data.optString("messageId", "")
                val method = data.optString("method", "")
                val request = data.optString("request", "")
                TONWalletKitEvent.BrowserBridgeRequest(messageId, method, request)
            }

            EventTypeConstants.EVENT_REQUEST_ERROR -> {
                // EventRequestError structure: { incomingEvent: { method }, result: { error: { code, message } } }
                val incomingEvent = data.optJSONObject("incomingEvent")
                val result = data.optJSONObject("result")
                val error = result?.optJSONObject("error")

                val method = incomingEvent?.optString("method", "unknown") ?: "unknown"
                val errorCode = error?.optInt("code", 0) ?: 0
                val errorMessage = error?.optString("message", "Unknown error") ?: "Unknown error"

                TONWalletKitEvent.RequestError(method, errorCode, errorMessage)
            }

            EventTypeConstants.EVENT_STATE_CHANGED,
            EventTypeConstants.EVENT_WALLET_STATE_CHANGED,
            EventTypeConstants.EVENT_SESSIONS_CHANGED,
            -> null

            else -> null
        }

    private fun resolveNetwork(): TONNetwork {
        val configNetwork = engine.getConfiguration()?.network
        if (configNetwork == null) {
            Logger.w(TAG, "WalletKit configuration missing network, defaulting to TESTNET")
            return TONNetwork.TESTNET
        }
        return configNetwork
    }

    private fun parseDAppInfo(data: JSONObject): DAppInfo? {
        // Try to get dApp name from multiple sources
        val dAppName =
            data.optNullableString(ResponseConstants.KEY_DAPP_NAME)
                ?: data.optJSONObject(ResponseConstants.KEY_MANIFEST)?.optNullableString(JsonConstants.KEY_NAME)
                ?: data.optJSONObject(ResponseConstants.KEY_PREVIEW)?.optJSONObject(ResponseConstants.KEY_MANIFEST)?.optNullableString(JsonConstants.KEY_NAME)

        // Try to get URLs from multiple sources
        val manifest =
            data.optJSONObject(ResponseConstants.KEY_PREVIEW)?.optJSONObject(ResponseConstants.KEY_MANIFEST)
                ?: data.optJSONObject(ResponseConstants.KEY_MANIFEST)

        val dAppUrl =
            data.optNullableString(ResponseConstants.KEY_DAPP_URL_ALT)
                ?: manifest?.optNullableString(ResponseConstants.KEY_URL) ?: ""

        val iconUrl =
            data.optNullableString(ResponseConstants.KEY_DAPP_ICON_URL)
                ?: manifest?.optNullableString(ResponseConstants.KEY_ICON_URL_ALT)

        val manifestUrl =
            data.optNullableString(ResponseConstants.KEY_MANIFEST_URL_ALT)
                ?: manifest?.optNullableString(ResponseConstants.KEY_URL)

        // Only return null if we have absolutely no dApp information
        if (dAppName == null && dAppUrl.isEmpty()) {
            return null
        }

        return DAppInfo(
            name = dAppName ?: dAppUrl.takeIf { it.isNotEmpty() } ?: "Unknown dApp",
            url = dAppUrl,
            iconUrl = iconUrl,
            manifestUrl = manifestUrl,
        )
    }

    private fun JSONObject.optNullableString(key: String): String? {
        val value = opt(key)
        return when (value) {
            null, JSONObject.NULL -> null
            else -> value.toString()
        }
    }

    /**
     * Normalizes the manifest URL in a ConnectRequestEvent to include https:// if it's just a domain.
     * The JS bridge may pass just the domain (e.g., "toncommunity.org") instead of a full URL.
     */
    private fun normalizeManifestUrl(event: ConnectRequestEvent): ConnectRequestEvent {
        val manifestUrl = event.preview?.manifest?.url
        if (manifestUrl.isNullOrEmpty()) return event
        if (manifestUrl.startsWith("http://") || manifestUrl.startsWith("https://")) return event

        val normalizedUrl = "https://$manifestUrl"
        val normalizedManifest = event.preview?.manifest?.copy(url = normalizedUrl)
        val normalizedPreview = event.preview?.copy(manifest = normalizedManifest)
        return event.copy(preview = normalizedPreview)
    }

    private companion object {
        private const val TAG = LogConstants.TAG_WEBVIEW_ENGINE
        private const val ERROR_FAILED_PARSE_CONNECT_REQUEST = "Failed to parse ConnectRequestEvent"
        private const val ERROR_FAILED_PARSE_TRANSACTION_REQUEST = "Failed to parse TransactionRequestEvent"
        private const val ERROR_FAILED_PARSE_SIGN_DATA_REQUEST = "Failed to parse SignDataRequestEvent"
    }
}
