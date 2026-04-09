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
package io.ton.walletkit.engine.adapter

import io.ton.walletkit.api.generated.TONNetwork
import io.ton.walletkit.api.generated.TONPreparedSignData
import io.ton.walletkit.api.generated.TONProofMessage
import io.ton.walletkit.api.generated.TONTransactionRequest
import io.ton.walletkit.engine.infrastructure.JsRef
import io.ton.walletkit.model.TONBase64
import io.ton.walletkit.model.TONHex
import io.ton.walletkit.model.TONUserFriendlyAddress
import io.ton.walletkit.model.TONWalletAdapter
import io.ton.walletkit.model.WalletAdapterInfo

/**
 * Internal adapter wrapping a JS-side wallet adapter.
 * Holds cached metadata; signing is handled by the JS engine.
 */
internal class BridgeWalletAdapter(
    private val ref: JsRef,
    private val cachedPublicKey: TONHex,
    private val cachedNetwork: TONNetwork,
    private val cachedAddress: TONUserFriendlyAddress,
) : TONWalletAdapter, AutoCloseable by ref {

    internal val adapterId: String get() = ref.id

    override fun identifier(): String = ref.id

    override fun publicKey(): TONHex = cachedPublicKey

    override fun network(): TONNetwork = cachedNetwork

    override fun address(testnet: Boolean): TONUserFriendlyAddress = cachedAddress

    override suspend fun stateInit(): TONBase64 {
        throw UnsupportedOperationException("BridgeWalletAdapter delegates to JS engine")
    }

    override suspend fun signedSendTransaction(
        input: TONTransactionRequest,
        fakeSignature: Boolean?,
    ): TONBase64 {
        throw UnsupportedOperationException("BridgeWalletAdapter delegates to JS engine")
    }

    override suspend fun signedSignData(
        input: TONPreparedSignData,
        fakeSignature: Boolean?,
    ): TONHex {
        throw UnsupportedOperationException("BridgeWalletAdapter delegates to JS engine")
    }

    override suspend fun signedTonProof(
        input: TONProofMessage,
        fakeSignature: Boolean?,
    ): TONHex {
        throw UnsupportedOperationException("BridgeWalletAdapter delegates to JS engine")
    }

    internal fun toWalletAdapterInfo(): WalletAdapterInfo = WalletAdapterInfo(
        adapterId = ref.id,
        address = cachedAddress,
        network = cachedNetwork,
    )
}
