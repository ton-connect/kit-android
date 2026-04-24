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
package io.ton.walletkit.engine.state

import io.ton.walletkit.internal.util.Logger
import java.util.concurrent.ConcurrentHashMap

/**
 * Reverse-RPC registry for Kotlin-implemented domain providers (swap, staking, …).
 *
 * Each domain subclasses this to add its own `suspend` RPC methods on top of the shared
 * register/unregister/getProvider/clear/require plumbing. [TAG] is supplied by the subclass so
 * "no provider registered" warnings identify the specific domain in logcat.
 */
internal abstract class KotlinProviderRegistry<T : Any> {
    private val providers = ConcurrentHashMap<String, T>()

    protected abstract val tag: String

    fun register(providerId: String, provider: T) {
        providers[providerId] = provider
    }

    fun unregister(providerId: String) {
        providers.remove(providerId)
    }

    fun getProvider(providerId: String): T? = providers[providerId]

    fun clear() {
        providers.clear()
    }

    /**
     * Look up a provider or throw. The warning log matches the pre-extraction behaviour so the
     * reverse-RPC failure path stays diagnosable without adding JS-side noise.
     */
    protected fun require(providerId: String): T = providers[providerId] ?: run {
        Logger.w(tag, "No Kotlin provider registered for id=$providerId")
        throw IllegalStateException("No Kotlin provider registered for id=$providerId")
    }
}
