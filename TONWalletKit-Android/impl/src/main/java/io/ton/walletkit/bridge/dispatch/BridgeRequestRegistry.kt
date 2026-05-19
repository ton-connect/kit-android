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
package io.ton.walletkit.bridge.dispatch

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.serializer

internal class BridgeRequestRegistry(private val json: Json) {
    private val handlers = HashMap<String, suspend (JsonElement) -> String>()

    fun register(method: String, handler: suspend (JsonElement) -> String) {
        require(handlers.put(method, handler) == null) {
            "Duplicate reverse-RPC handler registration for method: $method"
        }
    }

    inline fun <reified T> registerTyped(method: String, crossinline handler: suspend (T) -> String) {
        // Hoist serializer to inline call-site: the lambda below isn't inlined, so reified T isn't usable inside it.
        val serializer = json.serializersModule.serializer<T>()
        register(method) { raw -> handler(json.decodeFromJsonElement(serializer, raw)) }
    }

    /**
     * Typed-in, typed-out variant. The handler returns a strongly-typed [Res] which is encoded
     * to JSON for the wire — callers don't have to think about serialization.
     */
    inline fun <reified Req, reified Res> registerTypedJson(
        method: String,
        crossinline handler: suspend (Req) -> Res,
    ) {
        val reqSerializer = json.serializersModule.serializer<Req>()
        val resSerializer = json.serializersModule.serializer<Res>()
        register(method) { raw ->
            val req = json.decodeFromJsonElement(reqSerializer, raw)
            json.encodeToString(resSerializer, handler(req))
        }
    }

    suspend fun dispatch(method: String, params: JsonElement): String {
        val handler = handlers[method]
            ?: throw IllegalArgumentException("Unknown reverse-RPC method: $method")
        return handler(params)
    }
}
