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
import io.ton.walletkit.bridge.BridgeCodec
import io.ton.walletkit.bridge.decodeFromBridge
import io.ton.walletkit.bridge.decodeFromBridgeOrNull
import io.ton.walletkit.bridge.dispatch.WrappedFunctionRegistry
import io.ton.walletkit.bridge.optString
import io.ton.walletkit.internal.constants.BridgeMethodConstants
import io.ton.walletkit.internal.constants.LogConstants
import io.ton.walletkit.internal.constants.ResponseConstants
import io.ton.walletkit.internal.util.Logger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

internal class BridgeRpcClient(
    private val webViewManager: WebViewManager,
    private val codec: BridgeCodec,
    private val ensureInitialized: suspend () -> Unit,
    @PublishedApi internal val json: Json,
) {
    private val pending = ConcurrentHashMap<String, CompletableDeferred<BridgeResponse>>()
    private val ready = CompletableDeferred<Unit>()

    /**
     * Reverse-RPC channel: native callbacks JS can invoke by reference. Forward calls (Kotlin→JS,
     * tracked in [pending]) and these reverse callbacks (JS→Kotlin) are two halves of the same
     * bridge RPC, so the registry lives here rather than as a separate engine-level object.
     */
    internal val wrappedFunctions = WrappedFunctionRegistry(json)

    /**
     * Wraps [send] in a JsonObject envelope. Reserved for the [callBridgeMethod] escape hatch
     * and other callsites that explicitly need an opaque JsonObject result; prefer [callTyped].
     */
    suspend fun call(method: String, params: Any? = null): JsonObject = wrap(send(method, params))

    /** Send a request to JS and return the raw decoded result; callers may discard it. */
    suspend fun send(method: String, params: Any? = null): JsonElement {
        webViewManager.webViewInitialized.await()
        webViewManager.transport.awaitReady()
        if (method != BridgeMethodConstants.METHOD_INIT) {
            ready.await()
            ensureInitialized()
        }

        val callId = UUID.randomUUID().toString()
        val deferred = CompletableDeferred<BridgeResponse>()
        pending[callId] = deferred

        val envelope = buildJsonObject {
            put(ResponseConstants.KEY_KIND, ResponseConstants.VALUE_KIND_CALL)
            put(ResponseConstants.KEY_ID, callId)
            put(ResponseConstants.KEY_METHOD, method)
            val encoded = codec.encode(params)
            if (encoded !is JsonNull) {
                put(ResponseConstants.KEY_PARAMS, encoded)
            }
        }

        webViewManager.transport.send(envelope.toString())
        return deferred.await().raw
    }

    fun handleResponse(id: String, response: JsonObject) {
        val deferred = pending.remove(id)
        if (deferred == null) {
            Logger.w(TAG, "handleResponse: No deferred found for id: $id")
            return
        }
        val error = response[ResponseConstants.KEY_ERROR] as? JsonObject
        if (error != null) {
            val message = error.optString(ResponseConstants.KEY_MESSAGE, ResponseConstants.ERROR_MESSAGE_DEFAULT)
            Logger.e(TAG, ERROR_CALL_FAILED + id + ERROR_FAILED_SUFFIX + message)
            deferred.completeExceptionally(WalletKitBridgeException(message))
            return
        }
        val raw = response[ResponseConstants.KEY_RESULT] ?: JsonNull
        deferred.complete(BridgeResponse(raw))
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

    private fun wrap(raw: JsonElement): JsonObject = when (raw) {
        is JsonObject -> raw
        is JsonArray -> buildJsonObject { put(ResponseConstants.KEY_ITEMS, raw) }
        is JsonNull -> JsonObject(emptyMap())
        else -> buildJsonObject { put(ResponseConstants.KEY_VALUE, raw) }
    }

    private data class BridgeResponse(val raw: JsonElement)

    private companion object {
        private const val TAG = LogConstants.TAG_WEBVIEW_ENGINE
        private const val ERROR_CALL_FAILED = "call["
        private const val ERROR_FAILED_SUFFIX = "] failed: "
    }
}

internal suspend inline fun <reified T : Any> BridgeRpcClient.callTyped(
    method: String,
    params: Any? = null,
): T = json.decodeFromBridge(send(method, params))

internal suspend inline fun <reified T : Any> BridgeRpcClient.callTypedOrNull(
    method: String,
    params: Any? = null,
): T? = json.decodeFromBridgeOrNull(send(method, params))
