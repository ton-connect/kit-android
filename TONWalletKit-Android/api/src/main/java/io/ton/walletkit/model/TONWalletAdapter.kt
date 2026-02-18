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
package io.ton.walletkit.model

import io.ton.walletkit.api.generated.TONNetwork
import io.ton.walletkit.api.generated.TONPreparedSignData
import io.ton.walletkit.api.generated.TONProofMessage
import io.ton.walletkit.api.generated.TONTransactionRequest

/**
 * Wallet adapter interface for wrapping existing wallet implementations.
 *
 * Implement this interface to integrate existing wallet entities with WalletKit.
 * This allows wallet apps to wrap their own wallet types (e.g., `WalletEntity`)
 * and use them directly with the SDK without going through the 3-step signer/adapter pattern.
 *
 * Mirrors iOS `TONWalletAdapterProtocol` for cross-platform consistency.
 *
 * **Example usage:**
 * ```kotlin
 * class MyWalletAdapter(private val wallet: WalletEntity) : TONWalletAdapter {
 *     override fun identifier(): String = wallet.id
 *     override fun publicKey(): TONHex = TONHex("0x" + wallet.publicKey.hex())
 *     override fun network(): TONNetwork = if (wallet.testnet) TONNetwork.TESTNET else TONNetwork.MAINNET
 *     override fun address(testnet: Boolean): TONUserFriendlyAddress = TONUserFriendlyAddress(wallet.address)
 *     override suspend fun stateInit(): TONBase64 = TONBase64(wallet.computeStateInit())
 *     // ... signing methods
 * }
 *
 * // Use with WalletKit
 * val tonWallet = walletKit.addWalletFromAdapter(myWalletAdapter)
 * ```
 *
 * @see WalletSigner For custom signing-only integration
 */
interface TONWalletAdapter {
    /**
     * Get the unique identifier for this wallet.
     *
     * This should be a stable identifier that uniquely identifies the wallet.
     * Typically the wallet ID or a derived identifier.
     *
     * @return Unique wallet identifier string
     */
    fun identifier(): String

    /**
     * Get the wallet's public key.
     *
     * @return Public key as hex string (with or without 0x prefix)
     */
    fun publicKey(): TONHex

    /**
     * Get the network this wallet operates on.
     *
     * @return TONNetwork.MAINNET or TONNetwork.TESTNET
     */
    fun network(): TONNetwork

    /**
     * Get the wallet's user-friendly address.
     *
     * @param testnet Whether to format for testnet (affects bounceable flag display)
     * @return User-friendly wallet address
     */
    fun address(testnet: Boolean): TONUserFriendlyAddress

    /**
     * Get the wallet's state init for deployment.
     *
     * This is the initial code and data of the wallet contract, encoded as base64 BOC.
     * Used when connecting to dApps that request stateInit.
     *
     * @return State init as base64-encoded BOC
     */
    suspend fun stateInit(): TONBase64

    /**
     * Sign a transaction and return the signed BOC.
     *
     * This method is called when the SDK needs to sign a transaction.
     * Implementations should sign the transaction using their secure key storage.
     *
     * @param input Transaction to sign
     * @param fakeSignature If true, return a fake signature (for emulation)
     * @return Signed transaction as base64-encoded BOC
     * @throws UnsupportedOperationException if not implemented
     */
    suspend fun signedSendTransaction(
        input: TONTransactionRequest,
        fakeSignature: Boolean? = null,
    ): TONBase64

    /**
     * Sign data and return the signature.
     *
     * This method is called when the SDK needs to sign arbitrary data (sign_data RPC).
     *
     * @param input Prepared sign data payload
     * @param fakeSignature If true, return a fake signature (for emulation)
     * @return Signature as hex string
     * @throws UnsupportedOperationException if not implemented
     */
    suspend fun signedSignData(
        input: TONPreparedSignData,
        fakeSignature: Boolean? = null,
    ): TONHex

    /**
     * Sign a TON proof message and return the signature.
     *
     * This method is called during TonConnect authentication to prove wallet ownership.
     *
     * @param input Proof message to sign
     * @param fakeSignature If true, return a fake signature (for emulation)
     * @return Signature as hex string
     * @throws UnsupportedOperationException if not implemented
     */
    suspend fun signedTonProof(
        input: TONProofMessage,
        fakeSignature: Boolean? = null,
    ): TONHex
}
