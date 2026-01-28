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
package io.ton.walletkit.core

import io.ton.walletkit.ITONWallet
import io.ton.walletkit.WalletKitBridgeException
import io.ton.walletkit.api.generated.*
import io.ton.walletkit.engine.WalletKitEngine
import io.ton.walletkit.engine.model.WalletAccount
import io.ton.walletkit.engine.model.WalletSession
import io.ton.walletkit.model.KeyPair
import io.ton.walletkit.model.TONBalance
import io.ton.walletkit.model.TONUserFriendlyAddress
import kotlinx.serialization.json.Json

/**
 * Represents a TON wallet with balance and state management.
 *
 * Mirrors the canonical TON Wallet contract interface for cross-platform consistency.
 *
 * Use [TONWallet.add] to create a new wallet from mnemonic data.
 *
 * Example:
 * ```kotlin
 * val data = TONWalletData(
 *     mnemonic = listOf("word1", "word2", ...),
 *     name = "My Wallet",
 *     network = TONNetwork.MAINNET,
 *     version = "v5r1"
 * )
 * val wallet = TONWallet.add(data)
 * val balance = wallet.balance()
 * wallet.connect("tc://...")
 * wallet.remove()
 * ```
 *
 * @property id Unique wallet identifier (opaque hash from bridge)
 * @property address Wallet address in user-friendly format
 */
internal class TONWallet internal constructor(
    override val id: String,
    override val address: TONUserFriendlyAddress,
    private val engine: WalletKitEngine,
    private val account: WalletAccount,
) : ITONWallet {

    private val json = Json { ignoreUnknownKeys = true }
    companion object {
        /**
         * Convert a mnemonic phrase to a key pair.
         *
         * @param kit The TONWalletKit instance
         * @param mnemonic Mnemonic phrase (12 or 24 words)
         * @param mnemonicType Mnemonic type ("ton" by default)
         * @return KeyPair with publicKey and secretKey
         * @throws WalletKitBridgeException if conversion fails or SDK not initialized
         */
        suspend fun mnemonicToKeyPair(
            kit: TONWalletKit,
            mnemonic: List<String>,
            mnemonicType: String = "ton",
        ): KeyPair {
            return kit.engine.mnemonicToKeyPair(mnemonic, mnemonicType)
        }

        /**
         * Sign arbitrary data using a secret key.
         *
         * This is a low-level signing primitive. Typically used in conjunction with
         * [mnemonicToKeyPair] to derive keys and then sign data.
         *
         * @param kit The TONWalletKit instance
         * @param data Data bytes to sign
         * @param secretKey Secret key bytes for signing
         * @return Signature bytes
         */
        suspend fun sign(
            kit: TONWalletKit,
            data: ByteArray,
            secretKey: ByteArray,
        ): ByteArray {
            return kit.engine.sign(data, secretKey)
        }

        /**
         * Get all wallets managed by the SDK.
         *
         * **Note:** This method is deprecated. Use `kit.getWallets()` instead to stay aligned with the primary kit API.
         *
         * @param kit The TONWalletKit instance
         * @return List of all wallets
         */
        @Deprecated("Use kit.getWallets() instead", ReplaceWith("kit.getWallets()"))
        suspend fun wallets(kit: TONWalletKit): List<TONWallet> {
            return kit.getWallets().map { it as TONWallet }
        }
    }

    // ========================================================================
    // ITONWallet interface implementation
    // ========================================================================

    /**
     * Get wallet balance.
     *
     * @return Balance as TONBalance (wraps nanoTON value)
     * @throws io.ton.walletkit.WalletKitBridgeException if balance retrieval fails
     */
    override suspend fun balance(): TONBalance {
        val balanceString = engine.getBalance(id)
        return TONBalance(balanceString)
    }

    /**
     * Create a TON transfer transaction.
     *
     * @param request Transfer request with recipient, amount, and optional comment
     * @return Transaction request ready for sending or preview
     * @throws WalletKitBridgeException if transaction creation fails
     */
    override suspend fun transferTONTransaction(request: TONTransferRequest): TONTransactionRequest {
        val result = engine.createTransferTonTransaction(id, request)
        return json.decodeFromString<TONTransactionRequest>(result.transaction)
    }

    /**
     * Create a multi-recipient TON transfer transaction.
     *
     * @param requests List of transfer requests
     * @return Transaction request ready for sending or preview
     * @throws WalletKitBridgeException if transaction creation fails
     */
    override suspend fun transferTONTransaction(requests: List<TONTransferRequest>): TONTransactionRequest {
        val result = engine.createTransferMultiTonTransaction(id, requests)
        return json.decodeFromString<TONTransactionRequest>(result.transaction)
    }

    /**
     * Send a transaction to the blockchain.
     *
     * @param transactionRequest Transaction request from transferTON/NFT/Jetton methods
     * @throws WalletKitBridgeException if sending fails
     */
    override suspend fun send(transactionRequest: TONTransactionRequest) {
        val transactionContent = json.encodeToString(TONTransactionRequest.serializer(), transactionRequest)
        engine.sendTransaction(id, transactionContent)
    }

    /**
     * Get transaction preview with fee estimation.
     *
     * @param transactionRequest Transaction request to preview
     * @return Preview with estimated fees and trace
     * @throws WalletKitBridgeException if preview generation fails
     */
    override suspend fun preview(transactionRequest: TONTransactionRequest): TONTransactionEmulatedPreview {
        val transactionContent = json.encodeToString(TONTransactionRequest.serializer(), transactionRequest)
        return engine.getTransactionPreview(id, transactionContent)
    }

    /**
     * Create an NFT transfer transaction.
     *
     * @param request NFT transfer request with recipient and optional comment
     * @return Transaction request ready for sending or preview
     * @throws WalletKitBridgeException if transaction creation fails
     */
    override suspend fun transferNFTTransaction(request: TONNFTTransferRequest): TONTransactionRequest {
        val transactionContent = engine.createTransferNftTransaction(id, request)
        return json.decodeFromString<TONTransactionRequest>(transactionContent)
    }

    /**
     * Create an NFT transfer transaction with raw parameters.
     *
     * @param request Raw NFT transfer request with full control
     * @return Transaction request ready for sending or preview
     * @throws WalletKitBridgeException if transaction creation fails
     */
    override suspend fun transferNFTTransaction(request: TONNFTRawTransferRequest): TONTransactionRequest {
        val transactionContent = engine.createTransferNftRawTransaction(id, request)
        return json.decodeFromString<TONTransactionRequest>(transactionContent)
    }

    /**
     * Get NFTs owned by this wallet.
     *
     * @param request Request with pagination and optional filters
     * @return Response with NFTs and address book
     * @throws WalletKitBridgeException if NFT retrieval fails
     */
    override suspend fun nfts(request: TONNFTsRequest): TONNFTsResponse {
        val limit = request.pagination?.limit ?: 100
        val offset = request.pagination?.offset ?: 0
        return engine.getNfts(id, limit, offset)
    }

    /**
     * Get a single NFT by address.
     *
     * @param address NFT contract address
     * @return NFT details
     * @throws WalletKitBridgeException if NFT retrieval fails
     */
    override suspend fun nft(address: TONUserFriendlyAddress): TONNFT {
        return engine.getNft(address.value)
            ?: throw WalletKitBridgeException("NFT not found: ${address.value}")
    }

    /**
     * Get balance of a specific jetton.
     *
     * @param jettonAddress Jetton master contract address
     * @return Balance in jetton units
     * @throws WalletKitBridgeException if balance retrieval fails
     */
    override suspend fun jettonBalance(jettonAddress: TONUserFriendlyAddress): TONBalance {
        val balanceString = engine.getJettonBalance(id, jettonAddress.value)
        return TONBalance(balanceString)
    }

    /**
     * Get jetton wallet address for a specific jetton.
     *
     * @param jettonAddress Jetton master contract address
     * @return Jetton wallet contract address
     * @throws WalletKitBridgeException if address retrieval fails
     */
    override suspend fun jettonWalletAddress(jettonAddress: TONUserFriendlyAddress): TONUserFriendlyAddress {
        val addressString = engine.getJettonWalletAddress(id, jettonAddress.value)
        return TONUserFriendlyAddress(addressString)
    }

    /**
     * Create a jetton transfer transaction.
     *
     * @param request Jetton transfer request
     * @return Transaction request ready for sending or preview
     * @throws WalletKitBridgeException if transaction creation fails
     */
    override suspend fun transferJettonTransaction(request: TONJettonsTransferRequest): TONTransactionRequest {
        val transactionContent = engine.createTransferJettonTransaction(id, request)
        return json.decodeFromString<TONTransactionRequest>(transactionContent)
    }

    /**
     * Get jettons owned by this wallet.
     *
     * @param request Request with pagination
     * @return Response with jettons and address book
     * @throws WalletKitBridgeException if jetton retrieval fails
     */
    override suspend fun jettons(request: TONJettonsRequest): TONJettonsResponse {
        val limit = request.pagination?.limit ?: 100
        val offset = request.pagination?.offset ?: 0
        return engine.getJettons(id, limit, offset)
    }

    // ========================================================================
    // Additional methods (not in ITONWallet interface)
    // ========================================================================

    /**
     * Get the state init BOC for this wallet.
     *
     * The implementation currently returns null until state init retrieval is added to the engine.
     *
     * @return State init as base64-encoded BOC, or null if not available
     * @throws io.ton.walletkit.WalletKitBridgeException if state init retrieval fails
     */
    suspend fun stateInit(): String? {
        // TODO: Implement state init retrieval when available in engine
        // Reference implementation: wallet.getSateInit()?.toString()
        return null
    }

    /**
     * Handle a TON Connect URL (e.g., from QR code scan or deep link).
     *
     * This will trigger appropriate events (connect request, transaction request, etc.)
     * that will be delivered to your event handler.
     *
     * @param url TON Connect URL to handle
     * @throws io.ton.walletkit.WalletKitBridgeException if URL handling fails
     */
    suspend fun connect(url: String) {
        engine.handleTonConnectUrl(url)
    }

    /**
     * Remove this wallet from the wallet kit.
     *
     * This permanently deletes the wallet. Make sure the user has backed up
     * their mnemonic before calling this.
     *
     * @throws io.ton.walletkit.WalletKitBridgeException if removal fails
     */
    suspend fun remove() {
        engine.removeWallet(id)
    }

    /**
     * Get active TON Connect sessions for this wallet.
     *
     * @return List of active sessions associated with this wallet
     * @throws io.ton.walletkit.WalletKitBridgeException if session retrieval fails
     */
    suspend fun sessions(): List<WalletSession> {
        return engine.listSessions().filter { session ->
            session.walletAddress == address.value
        }
    }

    /**
     * Disconnect all TON Connect sessions for this wallet.
     *
     * @throws WalletKitBridgeException if disconnection fails
     */
    suspend fun disconnect() {
        engine.disconnectSession(null)
    }
}
