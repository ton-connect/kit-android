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
import io.ton.walletkit.config.SignDataType
import io.ton.walletkit.engine.WalletKitEngine
import io.ton.walletkit.model.KeyPair
import io.ton.walletkit.model.SignDataResult
import io.ton.walletkit.model.TONJettonTransferParams
import io.ton.walletkit.model.TONJettonWallets
import io.ton.walletkit.model.TONNFTItem
import io.ton.walletkit.model.TONNFTItems
import io.ton.walletkit.model.TONNFTTransferParamsHuman
import io.ton.walletkit.model.TONNFTTransferParamsRaw
import io.ton.walletkit.model.TONTransactionPreview
import io.ton.walletkit.model.TONTransferParams
import io.ton.walletkit.model.Transaction
import io.ton.walletkit.model.WalletAccount
import io.ton.walletkit.model.WalletSession

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
 * @property address Wallet address (null if not yet created)
 * @property publicKey Public key of the wallet (null if not available)
 */
internal class TONWallet internal constructor(
    override val address: String?,
    private val engine: WalletKitEngine,
    private val account: WalletAccount?,
) : ITONWallet {
    /**
     * Public key of the wallet.
     */
    override val publicKey: String? get() = account?.publicKey
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

    /**
     * Get the current balance of this wallet.
     *
     * @return Balance in nanoTON as a string, or null if not available
     * @throws io.ton.walletkit.WalletKitBridgeException if balance retrieval fails
     */
    suspend fun balance(): String? {
        val addr = address ?: return null
        return engine.getBalance(addr)
    }

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
    override suspend fun connect(url: String) {
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
    override suspend fun remove() {
        val addr = address ?: return
        engine.removeWallet(addr)
    }

    /**
     * Get recent transactions for this wallet.
     *
     * This is an Android-specific convenience method; other platforms typically manage
     * transaction data separately using their own storage.
     *
     * @param limit Maximum number of transactions to return (default 10)
     * @return List of recent transactions
     * @throws io.ton.walletkit.WalletKitBridgeException if transaction retrieval fails
     */
    override suspend fun transactions(limit: Int): List<Transaction> {
        val addr = address ?: return emptyList()
        return engine.getRecentTransactions(addr, limit)
    }

    /**
     * Get active TON Connect sessions for this wallet.
     *
     * This is an Android-specific convenience method for viewing which dApps
     * are connected to this specific wallet.
     *
     * @return List of active sessions associated with this wallet
     * @throws io.ton.walletkit.WalletKitBridgeException if session retrieval fails
     */
    override suspend fun sessions(): List<WalletSession> {
        val addr = address ?: return emptyList()
        // Get all sessions and filter by wallet address
        return engine.listSessions().filter { session ->
            session.walletAddress == addr
        }
    }

    /**
     * Create a TON transfer transaction.
     *
     * This method creates transaction content that can be passed to `kit.handleNewTransaction()`
     * to trigger the approval flow. This matches the JS WalletKit API:
     *
     * ```typescript
     * const tx = await wallet.createTransferTonTransaction(params);
     * await kit.handleNewTransaction(wallet, tx);
     * ```
     *
     * @param params Transfer parameters (recipient address, amount, optional comment/body/stateInit)
     * @return Transaction with optional preview
     * @throws io.ton.walletkit.WalletKitBridgeException if transaction creation fails
     */
    override suspend fun createTransferTonTransaction(params: TONTransferParams): io.ton.walletkit.model.TONTransactionWithPreview {
        val addr = address ?: throw WalletKitBridgeException("Wallet address is null")
        return engine.createTransferTonTransaction(addr, params)
    }

    /**
     * Create a multi-recipient TON transfer transaction.
     * Matches the JS API `wallet.createTransferMultiTonTransaction()` function.
     *
     * Allows sending TON to multiple recipients in a single transaction.
     *
     * @param messages List of transfer messages (each with recipient address, amount, optional comment)
     * @return Transaction with optional preview
     * @throws WalletKitBridgeException if transaction creation fails
     */
    override suspend fun createTransferMultiTonTransaction(messages: List<TONTransferParams>): io.ton.walletkit.model.TONTransactionWithPreview {
        val addr = address ?: throw WalletKitBridgeException("Wallet address is null")
        return engine.createTransferMultiTonTransaction(addr, messages)
    }

    /**
     * Get transaction preview with fee estimation.
     * Matches the JS API `wallet.getTransactionPreview()` function.
     *
     * @param transactionContent Transaction content (from createTransfer* methods)
     * @return Transaction preview with fee information
     * @throws WalletKitBridgeException if preview generation fails
     */
    override suspend fun getTransactionPreview(transactionContent: String): TONTransactionPreview {
        val addr = address ?: throw WalletKitBridgeException("Wallet address is null")
        return engine.getTransactionPreview(addr, transactionContent)
    }

    /**
     * Get NFT items owned by this wallet.
     *
     * Matches the shared API surface for cross-platform consistency.
     *
     * @param limit Maximum number of NFTs to return (default: 100)
     * @param offset Offset for pagination (default: 0)
     * @return NFT items with pagination information
     * @throws WalletKitBridgeException if NFT retrieval fails
     */
    override suspend fun nfts(limit: Int, offset: Int): TONNFTItems {
        val addr = address ?: throw WalletKitBridgeException("Wallet address is null")
        return engine.getNfts(addr, limit, offset)
    }

    /**
     * Get a single NFT by its address.
     *
     * Matches the shared API surface for cross-platform consistency.
     *
     * @param nftAddress NFT contract address
     * @return NFT item or null if not found
     * @throws WalletKitBridgeException if NFT retrieval fails
     */
    override suspend fun getNFT(nftAddress: String): TONNFTItem? {
        return engine.getNft(nftAddress)
    }

    /**
     * Create an NFT transfer transaction with human-friendly parameters.
     * Matches the JS API `wallet.createTransferNftTransaction()` function.
     *
     * @param params Transfer parameters (NFT address, recipient, amount, optional comment)
     * @return Transaction content that can be sent via sendTransaction()
     * @throws WalletKitBridgeException if transaction creation fails
     */
    override suspend fun createTransferNFTTransaction(params: TONNFTTransferParamsHuman): String {
        val addr = address ?: throw WalletKitBridgeException("Wallet address is null")
        return engine.createTransferNftTransaction(addr, params)
    }

    /**
     * Create an NFT transfer transaction with raw parameters.
     * Matches the JS API `wallet.createTransferNftRawTransaction()` function.
     *
     * @param params Raw transfer parameters with full control over transfer message
     * @return Transaction content that can be sent via sendTransaction()
     * @throws WalletKitBridgeException if transaction creation fails
     */
    override suspend fun createTransferNftRawTransaction(params: TONNFTTransferParamsRaw): String {
        val addr = address ?: throw WalletKitBridgeException("Wallet address is null")
        return engine.createTransferNftRawTransaction(addr, params)
    }

    /**
     * Send a transaction to the blockchain.
     * Matches the JS API `wallet.sendTransaction()` function.
     *
     * This method takes transaction content (usually created by createTransferNftTransaction,
     * createTransferTonTransaction, createTransferJettonTransaction, etc.) and actually sends
     * it to the blockchain, returning the transaction hash.
     *
     * Example:
     * ```kotlin
     * // Create NFT transfer transaction
     * val txContent = wallet.createTransferNftTransaction(params)
     * // Send it to blockchain
     * val txHash = wallet.sendTransaction(txContent)
     * ```
     *
     * @param transactionContent Transaction content JSON (from createTransfer* methods)
     * @return Transaction hash (boc) after successful broadcast
     * @throws WalletKitBridgeException if sending fails
     */
    override suspend fun sendTransaction(transactionContent: String): String {
        val addr = address ?: throw WalletKitBridgeException("Wallet address is null")
        return engine.sendTransaction(addr, transactionContent)
    }

    /**
     * Get jetton wallets owned by this wallet with pagination.
     * Matches the JS API `wallet.getJettons()` function.
     *
     * @param limit Maximum number of jetton wallets to return (default: 100)
     * @param offset Offset for pagination (default: 0)
     * @return Jetton wallets with pagination information
     * @throws WalletKitBridgeException if jetton retrieval fails
     */
    override suspend fun jettons(limit: Int, offset: Int): TONJettonWallets {
        val addr = address ?: throw WalletKitBridgeException("Wallet address is null")
        return engine.getJettons(addr, limit, offset)
    }

    /**
     * Create a jetton transfer transaction.
     * Matches the JS API `wallet.createTransferJettonTransaction()` function.
     *
     * @param params Transfer parameters (recipient address, jetton address, amount, optional comment)
     * @return Transaction content that can be sent via sendTransaction()
     * @throws WalletKitBridgeException if transaction creation fails
     */
    override suspend fun createTransferJettonTransaction(params: TONJettonTransferParams): String {
        val addr = address ?: throw WalletKitBridgeException("Wallet address is null")
        return engine.createTransferJettonTransaction(addr, params)
    }

    /**
     * Get the balance of a specific jetton for this wallet.
     *
     * Matches the shared API surface for cross-platform consistency.
     *
     * @param jettonAddress Jetton master contract address
     * @return Balance as a string (in jetton units, not considering decimals)
     * @throws WalletKitBridgeException if balance retrieval fails
     */
    override suspend fun getJettonBalance(jettonAddress: String): String {
        val addr = address ?: throw WalletKitBridgeException("Wallet address is null")
        return engine.getJettonBalance(addr, jettonAddress)
    }

    /**
     * Get the jetton wallet address for a specific jetton master contract.
     *
     * Each user has a unique jetton wallet contract for each jetton they hold.
     * This method returns the address of this wallet's jetton wallet for the given jetton.
     *
     * Matches the shared API surface for cross-platform consistency.
     *
     * @param jettonAddress Jetton master contract address
     * @return Jetton wallet contract address
     * @throws WalletKitBridgeException if address retrieval fails
     */
    override suspend fun getJettonWalletAddress(jettonAddress: String): String {
        val addr = address ?: throw WalletKitBridgeException("Wallet address is null")
        return engine.getJettonWalletAddress(addr, jettonAddress)
    }

    // ========== Interface Implementations ==========

    override suspend fun getBalance(): String {
        return balance() ?: "0"
    }

    override suspend fun getRecentTransactions(limit: Int): List<Transaction> {
        val addr = address ?: throw WalletKitBridgeException("Wallet address is null")
        return engine.getRecentTransactions(addr, limit)
    }

    override suspend fun getNFTItems(
        limit: Int?,
        offset: Int?,
        collectionAddress: String?,
        indirectOwnership: Boolean?,
    ): TONNFTItems {
        val addr = address ?: throw WalletKitBridgeException("Wallet address is null")
        return engine.getNfts(addr, limit ?: 100, offset ?: 0)
    }

    override suspend fun getJettons(
        limit: Int?,
        offset: Int?,
    ): TONJettonWallets {
        val addr = address ?: throw WalletKitBridgeException("Wallet address is null")
        return engine.getJettons(addr, limit ?: 100, offset ?: 0)
    }

    override suspend fun signData(
        data: ByteArray,
        type: SignDataType,
    ): SignDataResult {
        // Sign data is handled via request/response flow, not direct signing
        // Use TONWallet.mnemonicToKeyPair and TONWallet.sign for low-level signing
        throw UnsupportedOperationException("Direct signing not supported. Use request/response flow via events.")
    }

    override suspend fun disconnect() {
        val addr = address ?: return
        engine.disconnectSession(null) // Disconnect all sessions for this wallet
    }
}
