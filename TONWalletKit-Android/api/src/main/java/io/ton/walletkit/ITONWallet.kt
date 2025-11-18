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
package io.ton.walletkit

import io.ton.walletkit.config.SignDataType
import io.ton.walletkit.model.*

/**
 * TON wallet instance for transaction management and dApp interactions.
 *
 * Mirrors the JavaScript WalletKit API for cross-platform consistency.
 */
interface ITONWallet {
    /**
     * Wallet address in user-friendly format (UQ... or EQ...).
     */
    val address: String?

    /**
     * Public key as hex string.
     */
    val publicKey: String?

    /**
     * Get wallet balance in nanoTON.
     */
    suspend fun getBalance(): String

    /**
     * Create a TON transfer transaction.
     *
     * @param params Recipient address, amount, optional comment/body/stateInit
     * @return Transaction with optional preview
     */
    suspend fun createTransferTonTransaction(params: TONTransferParams): TONTransactionWithPreview

    /**
     * Create a multi-recipient TON transfer transaction.
     *
     * @param messages List of transfer messages
     * @return Transaction with optional preview
     */
    suspend fun createTransferMultiTonTransaction(messages: List<TONTransferParams>): TONTransactionWithPreview

    /**
     * Get NFT items owned by this wallet.
     *
     * @param limit Maximum number of NFTs (default: 100)
     * @param offset Pagination offset (default: 0)
     * @param collectionAddress Filter by collection (optional)
     * @param indirectOwnership Include indirect ownership (optional)
     */
    suspend fun getNFTItems(
        limit: Int? = null,
        offset: Int? = null,
        collectionAddress: String? = null,
        indirectOwnership: Boolean? = null,
    ): TONNFTItems

    /**
     * Get a single NFT by address.
     *
     * @param nftAddress NFT contract address (user-friendly format)
     * @return NFT item or null if not found
     */
    suspend fun getNFT(nftAddress: String): TONNFTItem?

    /**
     * Get jetton wallets owned by this wallet.
     *
     * Returns all jettons held by this wallet with embedded metadata.
     *
     * @param limit Maximum number of jettons (default: 100)
     * @param offset Pagination offset (default: 0)
     */
    suspend fun getJettons(
        limit: Int? = null,
        offset: Int? = null,
    ): TONJettonWallets

    /**
     * Get balance of a specific jetton.
     *
     * @param jettonAddress Jetton master contract address (user-friendly format)
     * @return Balance in jetton units (not considering decimals)
     */
    suspend fun getJettonBalance(jettonAddress: String): String

    /**
     * Get jetton wallet address for a specific jetton.
     *
     * Each user has a unique jetton wallet contract for each jetton they hold.
     *
     * @param jettonAddress Jetton master contract address (user-friendly format)
     * @return Jetton wallet contract address (user-friendly format)
     */
    suspend fun getJettonWalletAddress(jettonAddress: String): String

    /**
     * Get active TON Connect sessions.
     *
     * @return List of connected dApps for this wallet
     */
    suspend fun sessions(): List<WalletSession>

    /**
     * Create an NFT transfer transaction.
     *
     * @param params NFT address, recipient, transfer amount, optional comment
     * @return Transaction content as JSON string for [sendTransaction]
     */
    suspend fun createTransferNFTTransaction(params: TONNFTTransferParamsHuman): String

    /**
     * Create an NFT transfer transaction with raw parameters.
     *
     * @param params Raw transfer message with full control
     * @return Transaction content as JSON string for [sendTransaction]
     */
    suspend fun createTransferNftRawTransaction(params: TONNFTTransferParamsRaw): String

    /**
     * Create a jetton transfer transaction.
     *
     * @param params Recipient, jetton address, amount, optional comment
     * @return Transaction content as JSON string for [sendTransaction]
     */
    suspend fun createTransferJettonTransaction(params: TONJettonTransferParams): String

    /**
     * Send a transaction to the blockchain.
     *
     * @param transactionContent Transaction from createTransfer* methods
     * @return Transaction hash (BOC) after successful broadcast
     */
    suspend fun sendTransaction(transactionContent: String): String

    /**
     * Get transaction preview with fee estimation.
     *
     * @param transactionContent Transaction from createTransfer* methods
     * @return Preview with estimated fees
     */
    suspend fun getTransactionPreview(transactionContent: String): TONTransactionPreview

    /**
     * Handle a TON Connect URL (QR code or deep link).
     *
     * Triggers connect/transaction/sign events delivered to your event handler.
     *
     * @param url TON Connect URL (tc://...)
     */
    suspend fun connect(url: String)

    /**
     * Sign arbitrary data with this wallet.
     *
     * Triggers sign request event flow (not direct signing).
     *
     * @param data Bytes to sign
     * @param type Sign data type (BINARY or TEXT)
     */
    suspend fun signData(
        data: ByteArray,
        type: SignDataType = SignDataType.BINARY,
    ): SignDataResult

    /**
     * Disconnect from all connected dApps.
     */
    suspend fun disconnect()

    /**
     * Remove this wallet from the SDK.
     *
     * Permanently deletes the wallet. Ensure backup before calling.
     */
    suspend fun remove()
}
