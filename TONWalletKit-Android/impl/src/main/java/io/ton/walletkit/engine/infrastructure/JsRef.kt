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

import io.ton.walletkit.engine.WalletKitEngine
import io.ton.walletkit.internal.constants.BridgeMethodConstants
import io.ton.walletkit.internal.util.Logger
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Ownership handle for a live JS object held in the bridge registry (registry.ts).
 *
 * The Android WebView bridge only passes strings across the JS/Kotlin boundary, so live
 * JS objects are kept in a JS-side [Map] indexed by a string [id]. A [JsRef] represents
 * Kotlin ownership of one such entry: calling [close] removes it from the registry
 * immediately; [finalize] acts as a safety-net in case [close] is never called.
 *
 * Create via the companion factories:
 * ```kotlin
 * JsRef(id, rpcClient)   // from WalletOperations / internal engine code
 * JsRef(id, engine)      // from higher-level components that hold a WalletKitEngine
 * ```
 *
 * Usage — attached to a longer-lived object:
 * ```kotlin
 * class MyWrapper(private val ref: JsRef) : AutoCloseable by ref { ... }
 * ```
 */
internal class JsRef private constructor(
    val id: String,
    private val onRelease: suspend () -> Unit,
) : AutoCloseable {

    private val released = AtomicBoolean(false)

    override fun close() {
        if (released.compareAndSet(false, true)) {
            @OptIn(DelicateCoroutinesApi::class)
            GlobalScope.launch { runCatching { onRelease() } }
        }
    }

    @Suppress("removal")
    protected fun finalize() {
        if (released.compareAndSet(false, true)) {
            @OptIn(DelicateCoroutinesApi::class)
            GlobalScope.launch { runCatching { onRelease() } }
        }
    }

    internal companion object {
        private const val TAG = "JsRef"

        operator fun invoke(id: String, rpcClient: BridgeRpcClient): JsRef = JsRef(id) {
            try {
                rpcClient.call(BridgeMethodConstants.METHOD_RELEASE_REF, JSONObject().apply { put("id", id) })
            } catch (_: Exception) {
                Logger.w(TAG, "Failed to release JS ref: $id")
            }
        }

        operator fun invoke(id: String, engine: WalletKitEngine): JsRef = JsRef(id) {
            try {
                engine.callBridgeMethod(BridgeMethodConstants.METHOD_RELEASE_REF, JSONObject().apply { put("id", id) })
            } catch (_: Exception) {
                Logger.w(TAG, "Failed to release JS ref: $id")
            }
        }
    }
}
