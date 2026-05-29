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
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.serializer
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Lets a native (Kotlin) callback be invoked from JS without passing the function across the
 * bridge — which isn't possible, only serializable data is. Each callback is registered under a
 * generated UUID "reference"; the reference travels to JS (e.g. in the init config), and JS calls
 * back through the `callByReference` reverse-RPC method, which resolves the reference here and runs
 * the callback over the async reverse-RPC channel.
 *
 * @suppress Internal component.
 */
internal class WrappedFunctionRegistry(
    @PublishedApi internal val json: Json,
) {
    private val functions = ConcurrentHashMap<String, suspend (JsonArray) -> String>()

    /**
     * Typed single-argument registration. The JSON in/out marshalling lives here (decode the
     * positional arg, encode the result) so callers hand over a strongly-typed callback instead
     * of repeating the serialization dance at every registration site.
     */
    inline fun <reified A, reified R> registerTyped(crossinline fn: suspend (A) -> R): String {
        val argSerializer = json.serializersModule.serializer<A>()
        val resSerializer = json.serializersModule.serializer<R>()
        return register { args ->
            json.encodeToString(resSerializer, fn(json.decodeFromJsonElement(argSerializer, args[0])))
        }
    }

    /**
     * Raw registration escape hatch: [fn] receives the positional args array and must return an
     * already-JSON-encoded result. Prefer [registerTyped] for the common single-arg case.
     */
    fun register(fn: suspend (JsonArray) -> String): String {
        var id = UUID.randomUUID().toString()
        // Guard against an id collision: putIfAbsent is atomic and returns non-null only if the
        // key was already taken, so regenerate until we claim a free reference.
        while (functions.putIfAbsent(id, fn) != null) {
            id = UUID.randomUUID().toString()
        }
        return id
    }

    /** Invokes the callback bound to [refId]. The returned String is already JSON-encoded. */
    suspend fun invoke(refId: String, args: JsonArray): String {
        val fn = functions[refId]
            ?: throw IllegalArgumentException("No wrapped function registered for reference: $refId")
        return fn(args)
    }
}
