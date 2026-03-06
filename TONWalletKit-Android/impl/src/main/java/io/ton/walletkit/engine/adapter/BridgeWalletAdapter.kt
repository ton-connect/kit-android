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
import io.ton.walletkit.engine.infrastructure.BridgeRpcClient
import io.ton.walletkit.internal.constants.BridgeMethodConstants
import io.ton.walletkit.internal.util.Logger
import io.ton.walletkit.model.TONBase64
import io.ton.walletkit.model.TONHex
import io.ton.walletkit.model.TONUserFriendlyAddress
import io.ton.walletkit.model.TONWalletAdapter
import io.ton.walletkit.model.WalletAdapterInfo
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Internal adapter wrapping a JS-side wallet adapter.
 * Holds cached metadata; signing is handled by the JS engine.
 */
internal class BridgeWalletAdapter(
    internal val adapterId: String,
    private val cachedPublicKey: TONHex,
    private val cachedNetwork: TONNetwork,
    private val cachedAddress: TONUserFriendlyAddress,
    private val cachedWalletVersion: String,
    private val rpcClient: BridgeRpcClient,
) : TONWalletAdapter {

    override fun identifier(): String = adapterId

    override fun publicKey(): TONHex = cachedPublicKey

    override fun network(): TONNetwork = cachedNetwork

    override fun address(testnet: Boolean): TONUserFriendlyAddress = cachedAddress

    override fun walletVersion(): String = cachedWalletVersion

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

    @Suppress("removal")
    @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
    protected fun finalize() {
        GlobalScope.launch {
            try {
                val request = JSONObject().apply { put("id", adapterId) }
                rpcClient.call(BridgeMethodConstants.METHOD_RELEASE_REF, request)
            } catch (_: Exception) {
                Logger.w(TAG, "Failed to release adapter $adapterId during finalization")
            }
        }
    }

    internal fun toWalletAdapterInfo(): WalletAdapterInfo = WalletAdapterInfo(
        adapterId = adapterId,
        address = cachedAddress,
        network = cachedNetwork,
    )

    private companion object {
        private const val TAG = "BridgeWalletAdapter"
    }
}
