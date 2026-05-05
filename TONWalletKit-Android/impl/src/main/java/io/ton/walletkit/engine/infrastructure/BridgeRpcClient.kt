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

import io.ton.walletkit.WalletKitBridgeException
import io.ton.walletkit.internal.constants.BridgeMethodConstants
import io.ton.walletkit.internal.constants.LogConstants
import io.ton.walletkit.internal.constants.ResponseConstants
import io.ton.walletkit.internal.util.Logger
import kotlinx.coroutines.CompletableDeferred
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

internal class BridgeRpcClient(
    private val webViewManager: WebViewManager,
) {
    private val pending = ConcurrentHashMap<String, CompletableDeferred<BridgeResponse>>()
    private val ready = CompletableDeferred<Unit>()

    suspend fun call(
        method: String,
        params: Any? = null,
    ): JSONObject {
        webViewManager.webViewInitialized.await()
        webViewManager.transport.awaitReady()
        if (method != BridgeMethodConstants.METHOD_INIT) {
            ready.await()
        }

        val callId = UUID.randomUUID().toString()
        val deferred = CompletableDeferred<BridgeResponse>()
        pending[callId] = deferred

        val envelope = JSONObject().apply {
            put(ResponseConstants.KEY_KIND, ResponseConstants.VALUE_KIND_CALL)
            put(ResponseConstants.KEY_ID, callId)
            put(ResponseConstants.KEY_METHOD, method)
            when (params) {
                null -> {}
                is JSONObject -> put(ResponseConstants.KEY_PARAMS, params)
                is JSONArray -> put(ResponseConstants.KEY_PARAMS, params)
                is String -> put(ResponseConstants.KEY_PARAMS, params)
                else -> put(ResponseConstants.KEY_PARAMS, params.toString())
            }
        }

        webViewManager.transport.send(envelope.toString())

        val response = deferred.await()
        return response.result
    }

    fun handleResponse(
        id: String,
        response: JSONObject,
    ) {
        val deferred = pending.remove(id)
        if (deferred == null) {
            Logger.w(TAG, "handleResponse: No deferred found for id: $id")
            return
        }
        val error = response.optJSONObject(ResponseConstants.KEY_ERROR)
        if (error != null) {
            val message = error.optString(ResponseConstants.KEY_MESSAGE, ResponseConstants.ERROR_MESSAGE_DEFAULT)
            Logger.e(TAG, ERROR_CALL_FAILED + id + ERROR_FAILED_SUFFIX + message)
            deferred.completeExceptionally(WalletKitBridgeException(message))
            return
        }
        val result = response.opt(ResponseConstants.KEY_RESULT)
        val payload =
            when (result) {
                is JSONObject -> result
                is JSONArray -> JSONObject().put(ResponseConstants.KEY_ITEMS, result)
                null -> JSONObject()
                else -> JSONObject().put(ResponseConstants.KEY_VALUE, result)
            }
        deferred.complete(BridgeResponse(payload))
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
        private const val ERROR_CALL_FAILED = "call["
        private const val ERROR_FAILED_SUFFIX = "] failed: "
    }
}
