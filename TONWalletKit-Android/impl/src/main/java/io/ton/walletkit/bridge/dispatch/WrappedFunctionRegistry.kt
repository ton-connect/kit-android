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

import kotlinx.serialization.json.JsonArray
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Lets a native (Kotlin) callback be invoked from JS without passing the function across the
 * bridge — which isn't possible, only serializable data is. Each callback is registered under a
 * generated UUID "reference"; the reference travels to JS (e.g. in the init config), and JS calls
 * back through the `callByReference` reverse-RPC method, which resolves the reference here and runs
 * the callback. Mirrors the iOS UUID-handler-map approach and reuses the async reverse-RPC channel.
 *
 * @suppress Internal component.
 */
internal class WrappedFunctionRegistry {
    private val functions = ConcurrentHashMap<String, suspend (JsonArray) -> String>()

    /**
     * Registers [fn] under a fresh reference id and returns it. The id is the opaque handle JS
     * uses to invoke the callback; [fn] receives the JSON-encoded call arguments and must return
     * an already-JSON-encoded result.
     */
    fun register(fn: suspend (JsonArray) -> String): String {
        val id = UUID.randomUUID().toString()
        functions[id] = fn
        return id
    }

    /** Invokes the callback bound to [refId]. The returned String is already JSON-encoded. */
    suspend fun invoke(refId: String, args: JsonArray): String {
        val fn = functions[refId]
            ?: throw IllegalArgumentException("No wrapped function registered for reference: $refId")
        return fn(args)
    }
}
