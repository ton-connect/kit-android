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

import android.util.Base64
import io.ton.walletkit.WalletKitBridgeException
import io.ton.walletkit.internal.constants.BridgeMethodConstants
import io.ton.walletkit.internal.constants.LogConstants
import io.ton.walletkit.internal.constants.MiscConstants
import io.ton.walletkit.internal.constants.ResponseConstants
import io.ton.walletkit.internal.constants.WebViewConstants
import io.ton.walletkit.internal.util.Logger
import kotlinx.coroutines.CompletableDeferred
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Performs RPC-style invocations against the JavaScript bridge and coordinates pending requests.
 *
 * The implementation mirrors the behaviour of the legacy `call`/`handleResponse` pair to ensure
 * identical logging, error handling, and payload normalisation.
 *
 * @suppress Internal component. Use through [WebViewWalletKitEngine].
 */
internal class BridgeRpcClient(
    private val webViewManager: WebViewManager,
) {
    private val pending = ConcurrentHashMap<String, CompletableDeferred<BridgeResponse>>()
    private val ready = CompletableDeferred<Unit>()

    suspend fun call(
        method: String,
        params: JSONObject? = null,
    ): JSONObject {
        webViewManager.webViewInitialized.await()
        webViewManager.bridgeLoaded.await()
        webViewManager.jsBridgeReady.await()
        if (method != BridgeMethodConstants.METHOD_INIT) {
            ready.await()
        }

        val callId = UUID.randomUUID().toString()
        val deferred = CompletableDeferred<BridgeResponse>()
        pending[callId] = deferred

        val payload = params?.toString()
        val idLiteral = JSONObject.quote(callId)
        val methodLiteral = JSONObject.quote(method)
        val script =
            if (payload == null) {
                buildString {
                    append(WebViewConstants.JS_FUNCTION_WALLETKIT_CALL)
                    append(JS_OPEN_PAREN)
                    append(idLiteral)
                    append(JS_PARAMETER_SEPARATOR)
                    append(methodLiteral)
                    append(JS_PARAMETER_SEPARATOR)
                    append(WebViewConstants.JS_NULL)
                    append(JS_CLOSE_PAREN)
                }
            } else {
                val payloadBase64 = Base64.encodeToString(payload.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
                val payloadLiteral = JSONObject.quote(payloadBase64)
                buildString {
                    append(WebViewConstants.JS_FUNCTION_WALLETKIT_CALL)
                    append(JS_OPEN_PAREN)
                    append(idLiteral)
                    append(JS_PARAMETER_SEPARATOR)
                    append(methodLiteral)
                    append(JS_PARAMETER_SEPARATOR)
                    append(MiscConstants.SPACE_DELIMITER)
                    append(WebViewConstants.JS_FUNCTION_ATOB)
                    append(JS_OPEN_PAREN)
                    append(payloadLiteral)
                    append(JS_CLOSE_PAREN)
                    append(JS_CLOSE_PAREN)
                }
            }

        webViewManager.executeJavaScript(script)

        val response = deferred.await()
        Logger.d(TAG, "call[$method] completed")
        return response.result
    }

    fun handleResponse(
        id: String,
        response: JSONObject,
    ) {
        Logger.d(TAG, "🟡 handleResponse called for id: $id")
        Logger.d(TAG, "🟡 response: $response")
        Logger.d(TAG, "🟡 pending.size before remove: ${pending.size}")
        val deferred = pending.remove(id)
        if (deferred == null) {
            Logger.w(TAG, "⚠️ handleResponse: No deferred found for id: $id")
            Logger.w(TAG, "⚠️ pending keys: ${pending.keys}")
            return
        }
        Logger.d(TAG, "✅ Found deferred for id: $id")
        val error = response.optJSONObject(ResponseConstants.KEY_ERROR)
        if (error != null) {
            val message = error.optString(ResponseConstants.KEY_MESSAGE, ResponseConstants.ERROR_MESSAGE_DEFAULT)
            Logger.e(TAG, ERROR_CALL_FAILED + id + ERROR_FAILED_SUFFIX + message)
            deferred.completeExceptionally(WalletKitBridgeException(message))
            return
        }
        val result = response.opt(ResponseConstants.KEY_RESULT)
        Logger.d(TAG, "🟡 result type: ${result?.javaClass?.simpleName}")
        Logger.d(TAG, "🟡 result: $result")
        val payload =
            when (result) {
                is JSONObject -> result
                is JSONArray -> JSONObject().put(ResponseConstants.KEY_ITEMS, result)
                null -> JSONObject()
                else -> JSONObject().put(ResponseConstants.KEY_VALUE, result)
            }
        Logger.d(TAG, "✅ Completing deferred with payload: $payload")
        deferred.complete(BridgeResponse(payload))
        Logger.d(TAG, "✅ Deferred completed for id: $id")
    }

    fun failAll(exception: WalletKitBridgeException) {
        pending.values.forEach { deferred ->
            if (!deferred.isCompleted) {
                deferred.completeExceptionally(exception)
            }
        }
        pending.clear()
        if (!ready.isCompleted) {
            ready.completeExceptionally(exception)
        }
    }

    fun markReady() {
        if (!ready.isCompleted) {
            ready.complete(Unit)
        }
    }

    fun isReady(): Boolean = ready.isCompleted

    private data class BridgeResponse(
        val result: JSONObject,
    )

    private companion object {
        private const val TAG = LogConstants.TAG_WEBVIEW_ENGINE
        private const val JS_OPEN_PAREN = "("
        private const val JS_CLOSE_PAREN = ")"
        private const val JS_PARAMETER_SEPARATOR = ","
        private const val ERROR_CALL_FAILED = "call["
        private const val ERROR_FAILED_SUFFIX = "] failed: "
    }
}
