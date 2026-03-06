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

import io.ton.walletkit.model.TONWalletAdapter
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages native wallet adapter instances used by the JavaScript bridge.
 *
 * When a host app calls [addWallet(adapter)], the adapter is registered here
 * and an ID is passed to JavaScript. The JS proxy adapter delegates async methods
 * (getStateInit, getSignedSendTransaction, etc.) back to the Kotlin adapter
 * via WebView @JavascriptInterface bridge methods that look up the adapter by ID.
 *
 * This mirrors iOS's pattern where the Swift adapter is exposed to JS via JSExport,
 * except Android uses message-based bridge callbacks instead.
 *
 * @suppress Internal component. Exposed via [WebViewWalletKitEngine] only.
 */
internal class AdapterManager {
    private val adapters = ConcurrentHashMap<String, TONWalletAdapter>()
    private val mutex = Mutex()

    /**
     * Register a wallet adapter and obtain a unique ID for the JS bridge.
     */
    suspend fun registerAdapter(adapter: TONWalletAdapter): String = mutex.withLock {
        val adapterId = buildString {
            append("native_adapter_")
            append(System.currentTimeMillis())
            append('_')
            append((Math.random() * 1_000_000).toInt())
        }
        adapters[adapterId] = adapter
        adapterId
    }

    /**
     * Look up an adapter by ID.
     */
    fun getAdapter(adapterId: String): TONWalletAdapter? = adapters[adapterId]

    /**
     * Remove an adapter from the registry.
     */
    suspend fun removeAdapter(adapterId: String) {
        mutex.withLock {
            adapters.remove(adapterId)
        }
    }

    /**
     * Check if an adapter exists.
     */
    fun hasAdapter(adapterId: String): Boolean = adapters.containsKey(adapterId)
}
