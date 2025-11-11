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

import io.ton.walletkit.model.WalletSigner
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages external signer callbacks used by the JavaScript bridge.
 *
 * This registry keeps track of Kotlin-side [WalletSigner] instances that can be invoked by the
 * bridge when a dApp requests a signature. Each signer is assigned a unique ID that is passed to
 * the bridge and later used to locate the signer when a sign request is received.
 *
 * @suppress Internal component. Exposed via [WebViewWalletKitEngine] only.
 */
internal class SignerManager {
    private val signerCallbacks = ConcurrentHashMap<String, WalletSigner>()
    private val mutex = Mutex()

    /**
     * Register a signer instance and obtain a unique ID that can be shared with the bridge.
     *
     * The ID format matches the legacy implementation to guarantee interoperability with
     * persisted bridge state.
     */
    suspend fun registerSigner(signer: WalletSigner): String = mutex.withLock {
        val signerId = buildString {
            append("signer_")
            append(System.currentTimeMillis())
            append('_')
            append((Math.random() * 1_000_000).toInt())
        }
        signerCallbacks[signerId] = signer
        signerId
    }

    /**
     * Look up a signer by ID.
     */
    fun getSigner(signerId: String): WalletSigner? = signerCallbacks[signerId]

    /**
     * Check if a signer exists (for determining if it's a custom signer).
     */
    fun hasCustomSigner(signerId: String): Boolean = signerCallbacks.containsKey(signerId)

    /**
     * Remove a signer from the registry.
     */
    suspend fun removeSigner(signerId: String) {
        mutex.withLock {
            signerCallbacks.remove(signerId)
        }
    }

    /**
     * Expose current signer IDs for debugging.
     */
    fun currentSignerIds(): Set<String> = signerCallbacks.keys
}
