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
package io.ton.walletkit.engine

import io.ton.walletkit.config.TONWalletKitConfiguration
import io.ton.walletkit.core.WalletKitEngineKind
import io.ton.walletkit.event.ConnectRequestEvent
import io.ton.walletkit.event.SignDataRequestEvent
import io.ton.walletkit.event.TransactionRequestEvent
import io.ton.walletkit.model.KeyPair
import io.ton.walletkit.model.TONNetwork
import io.ton.walletkit.model.WalletAccount
import io.ton.walletkit.model.WalletAdapterInfo
import io.ton.walletkit.model.WalletSession
import io.ton.walletkit.model.WalletSigner
import io.ton.walletkit.model.WalletSignerInfo
import io.ton.walletkit.request.RequestHandler

/**
 * Abstraction over a runtime that can execute the WalletKit JavaScript bundle and expose
 * the wallet APIs to Android callers. Implementations may back the runtime with a WebView or
 * an embedded JavaScript engine such as QuickJS. Every implementation must provide the same
 * JSON-RPC surface as the historical [WalletKitBridge] class.
 *
 * **Auto-Initialization:**
 * All methods that require WalletKit initialization will automatically initialize the SDK
 * if it hasn't been initialized yet. This means calling [init] explicitly is optional -
 * you can call any method and initialization will happen automatically with default settings.
 * If you need custom configuration, call [init] with your config before other methods.
 *
 * **Event Handling:**
 * Events are handled via the eventsHandler passed during engine creation.
 *
 * @suppress Internal engine abstraction. Use TONWalletKit and TONWallet public API instead.
 */
internal interface WalletKitEngine : RequestHandler {
    val kind: WalletKitEngineKind

    /**
     * Initialize WalletKit with custom configuration. This must be called before any other method;
     * subsequent calls are ignored once initialization succeeds.
     *
     * @param configuration Configuration for the WalletKit SDK
     * @throws WalletKitBridgeException if initialization fails
     */
    suspend fun init(configuration: TONWalletKitConfiguration)

    /**
     * Get the current WalletKit configuration.
     * Used by WebView injector to pass config to JavaScript.
     *
     * @return Current configuration, or null if not initialized
     */
    fun getConfiguration(): TONWalletKitConfiguration?

    /**
     * Convert a mnemonic phrase to an Ed25519 key pair.
     *
     * This matches the JS WalletKit's `MnemonicToKeyPair` utility function.
     *
     * @param words Mnemonic phrase as a list of words (12 or 24 words)
     * @param mnemonicType Derivation type: "ton" (default) or "bip39"
     * @return KeyPair containing public key (32 bytes) and secret key (64 bytes)
     * @throws WalletKitBridgeException if conversion fails
     */
    suspend fun mnemonicToKeyPair(words: List<String>, mnemonicType: String = "ton"): KeyPair

    /**
     * Sign arbitrary data using a secret key.
     *
     * @param data Data bytes to sign
     * @param secretKey Secret key bytes for signing
     * @return Signature bytes
     * @throws WalletKitBridgeException if signing fails
     */
    suspend fun sign(
        data: ByteArray,
        secretKey: ByteArray,
    ): ByteArray

    /**
     * Generate a new mnemonic phrase using the WalletKit JS utilities.
     *
     * @param wordCount Number of words to generate (12 or 24). Defaults to 24.
     * @return List of mnemonic words
     * @throws WalletKitBridgeException if generation fails
     */
    suspend fun createTonMnemonic(wordCount: Int = 24): List<String>

    /**
     * Create a signer from mnemonic phrase.
     * Step 1 of the wallet creation pattern matching JS WalletKit.
     *
     * @param mnemonic Mnemonic phrase as a list of words
     * @param mnemonicType Mnemonic type ("ton" or "bip39"), defaults to "ton"
     * @return Signer info with ID and public key
     * @throws WalletKitBridgeException if signer creation fails
     */
    suspend fun createSignerFromMnemonic(
        mnemonic: List<String>,
        mnemonicType: String = "ton",
    ): WalletSignerInfo

    /**
     * Create a signer from secret key (private key).
     * Step 1 of the wallet creation pattern matching JS WalletKit.
     *
     * @param secretKey Private key as byte array (32 bytes for Ed25519)
     * @return Signer info with ID and public key
     * @throws WalletKitBridgeException if signer creation fails
     */
    suspend fun createSignerFromSecretKey(secretKey: ByteArray): WalletSignerInfo

    /**
     * Create a signer from a custom WalletSigner implementation.
     * Step 1 of the wallet creation pattern, enabling hardware wallet integration.
     *
     * @param signer Custom wallet signer (e.g., hardware wallet)
     * @return Signer info with ID and public key
     * @throws WalletKitBridgeException if signer creation fails
     */
    suspend fun createSignerFromCustom(signer: WalletSigner): WalletSignerInfo

    /**
     * Check if a signer is a custom signer (registered in SignerManager).
     */
    fun isCustomSigner(signerId: String): Boolean

    /**
     * Create a V5R1 wallet adapter from a signer.
     * Step 2 of the wallet creation pattern matching JS WalletKit.
     *
     * @param signerId Signer ID from createSignerFromMnemonic or createSignerFromSecretKey
     * @param network Network to use (e.g., "mainnet", "testnet"), defaults to current network
     * @param workchain Workchain ID: 0 for basechain (default), -1 for masterchain
     * @param walletId Wallet ID for address uniqueness
     * @param publicKey Public key hex string (required for custom signers)
     * @param isCustom Whether this is a custom signer (hardware wallet)
     * @return Adapter info with ID and wallet address
     * @throws WalletKitBridgeException if adapter creation fails
     */
    suspend fun createV5R1Adapter(
        signerId: String,
        network: String? = null,
        workchain: Int = 0,
        walletId: Long = 2147483409L,
        publicKey: String? = null,
        isCustom: Boolean = false,
    ): WalletAdapterInfo

    /**
     * Create a V4R2 wallet adapter from a signer.
     * Step 2 of the wallet creation pattern matching JS WalletKit.
     *
     * @param signerId Signer ID from createSignerFromMnemonic or createSignerFromSecretKey
     * @param network Network to use (e.g., "mainnet", "testnet"), defaults to current network
     * @param workchain Workchain ID: 0 for basechain (default), -1 for masterchain
     * @param walletId Wallet ID for address uniqueness
     * @param publicKey Public key hex string (required for custom signers)
     * @param isCustom Whether this is a custom signer (hardware wallet)
     * @return Adapter info with ID and wallet address
     * @throws WalletKitBridgeException if adapter creation fails
     */
    suspend fun createV4R2Adapter(
        signerId: String,
        network: String? = null,
        workchain: Int = 0,
        walletId: Long = 698983191L,
        publicKey: String? = null,
        isCustom: Boolean = false,
    ): WalletAdapterInfo

    /**
     * Add a wallet to the kit using an adapter.
     * Step 3 of the wallet creation pattern matching JS WalletKit.
     *
     * @param adapterId Adapter ID from createV5R1Adapter or createV4R2Adapter
     * @return The newly added wallet account
     * @throws WalletKitBridgeException if wallet addition fails
     */
    suspend fun addWallet(adapterId: String): WalletAccount

    /**
     * Get all wallets managed by this engine.
     *
     * @return List of wallet accounts
     */
    suspend fun getWallets(): List<WalletAccount>

    /**
     * Get a single wallet by walletId using RPC call.
     *
     * @param walletId Wallet ID in format "chainId:address" (e.g., "-239:UQDtFp...")
     * @return Wallet account or null if not found
     */
    suspend fun getWallet(walletId: String): WalletAccount?

    /**
     * Remove a wallet by walletId.
     *
     * @param walletId Wallet ID in format "chainId:address" (e.g., "-239:UQDtFp...")
     * @throws WalletKitBridgeException if removal fails
     */
    suspend fun removeWallet(walletId: String)

    /**
     * Get the current state of a wallet.
     *
     * @param walletId Wallet ID in format "chainId:address" (e.g., "-239:UQDtFp...")
     * @return Current wallet balance in nanoTON as a string
     * @throws WalletKitBridgeException if balance retrieval fails
     */
    suspend fun getBalance(walletId: String): String

    /**
     * Handle a TON Connect URL (e.g., from QR code scan or deep link).
     * This will trigger appropriate events (connect request, transaction request, etc.)
     *
     * @param url TON Connect URL to handle
     * @throws WalletKitBridgeException if URL handling fails
     */
    suspend fun handleTonConnectUrl(url: String)

    /**
     * Handle a TonConnect request from a dApp (via internal browser or extension).
     * This processes the request and invokes the callback with the response.
     *
     * @param messageId Unique message ID from the dApp
     * @param method Request method (e.g., "connect", "sendTransaction", "signData", "send")
     * @param paramsJson Request parameters as JSON string (can be object or array for 'send' method)
     * @param url The current dApp URL (for extracting the domain)
     * @param responseCallback Callback to send response back to dApp
     */
    suspend fun handleTonConnectRequest(
        messageId: String,
        method: String,
        paramsJson: String?,
        url: String? = null,
        responseCallback: (org.json.JSONObject) -> Unit,
    )

    /**
     * Create a TON transfer transaction.
     *
     * This method creates transaction content matching the JS WalletKit API wallet.createTransferTonTransaction().
     * The returned transaction content can be passed to handleNewTransaction() to trigger the approval flow.
     *
     * @param walletId Wallet ID in format "chainId:address" (e.g., "-239:UQDtFp...")
     * @param params Transfer parameters (recipient, amount, optional comment/body/stateInit)
     * @return Transaction with optional preview
     * @throws WalletKitBridgeException if transaction creation fails
     */
    suspend fun createTransferTonTransaction(
        walletId: String,
        params: io.ton.walletkit.model.TONTransferParams,
    ): io.ton.walletkit.model.TONTransactionWithPreview

    /**
     * Handle a new transaction initiated from the wallet app.
     *
     * This method matches the JS WalletKit API kit.handleNewTransaction() and triggers
     * a transaction request event that can be approved or rejected via the event handler.
     *
     * @param walletId Wallet ID in format "chainId:address" (e.g., "-239:UQDtFp...")
     * @param transactionContent Transaction content as JSON (from createTransferTonTransaction, etc.)
     * @throws WalletKitBridgeException if transaction handling fails
     */
    suspend fun handleNewTransaction(
        walletId: String,
        transactionContent: String,
    )

    /**
     * Send a transaction to the blockchain.
     *
     * This method takes transaction content (as JSON) and sends it to the blockchain,
     * returning the transaction hash. This matches the iOS wallet.sendTransaction() behavior.
     *
     * @param walletId Wallet ID in format "chainId:address" (e.g., "-239:UQDtFp...")
     * @param transactionContent Transaction content as JSON (from transferNFT, createTransferJettonTransaction, etc.)
     * @return Transaction hash (signedBoc) after successful broadcast
     * @throws WalletKitBridgeException if sending fails
     */
    suspend fun sendTransaction(
        walletId: String,
        transactionContent: String,
    ): String

    /**
     * Approve a connection request from a dApp.
     *
     * @param event Typed event from the connect request
     * @throws WalletKitBridgeException if approval fails
     */
    override suspend fun approveConnect(event: ConnectRequestEvent, network: TONNetwork)

    /**
     * Reject a connection request from a dApp.
     *
     * @param event Typed event from the connect request
     * @param reason Optional reason for rejection
     * @param errorCode Optional error code for TON Connect protocol
     * @throws WalletKitBridgeException if rejection fails
     */
    override suspend fun rejectConnect(
        event: ConnectRequestEvent,
        reason: String?,
        errorCode: Int?,
    )

    /**
     * Approve and sign a transaction request.
     *
     * @param event Typed event from the transaction request
     * @throws WalletKitBridgeException if approval or signing fails
     */
    override suspend fun approveTransaction(event: TransactionRequestEvent, network: TONNetwork)

    /**
     * Reject a transaction request.
     *
     * @param event Typed event from the transaction request
     * @param reason Optional reason for rejection
     * @param errorCode Optional error code (defaults to USER_REJECTS_ERROR=300)
     * @throws WalletKitBridgeException if rejection fails
     */
    override suspend fun rejectTransaction(
        event: TransactionRequestEvent,
        reason: String?,
        errorCode: Int?,
    )

    /**
     * Approve and sign a data signing request.
     *
     * @param event Typed event from the sign data request
     * @throws WalletKitBridgeException if approval or signing fails
     */
    override suspend fun approveSignData(event: SignDataRequestEvent, network: TONNetwork)

    /**
     * Reject a data signing request.
     *
     * @param event Typed event from the sign data request
     * @param reason Optional reason for rejection
     * @throws WalletKitBridgeException if rejection fails
     */
    override suspend fun rejectSignData(
        event: SignDataRequestEvent,
        reason: String?,
    )

    /**
     * Get all active TON Connect sessions.
     *
     * @return List of active sessions
     */
    suspend fun listSessions(): List<WalletSession>

    /**
     * Disconnect a TON Connect session.
     *
     * @param sessionId Session ID to disconnect, or null to disconnect all sessions
     * @throws WalletKitBridgeException if disconnection fails
     */
    suspend fun disconnectSession(sessionId: String? = null)

    /**
     * Get NFTs owned by a wallet with pagination.
     *
     * @param walletAddress Wallet address to get NFTs for
     * @param limit Maximum number of NFTs to return
     * @param offset Offset for pagination
     * @return NFT items with pagination info
     * @throws WalletKitBridgeException if the request fails
     */
    suspend fun getNfts(walletId: String, limit: Int = 100, offset: Int = 0): io.ton.walletkit.model.TONNFTItems

    /**
     * Get a single NFT by its address.
     *
     * @param nftAddress NFT contract address
     * @return NFT item or null if not found
     * @throws WalletKitBridgeException if the request fails
     */
    suspend fun getNft(nftAddress: String): io.ton.walletkit.model.TONNFTItem?

    /**
     * Create an NFT transfer transaction with human-friendly parameters.
     *
     * @param walletId Wallet ID in format "chainId:address" (e.g., "-239:UQDtFp...")
     * @param params Transfer parameters
     * @return Transaction content as JSON string
     * @throws WalletKitBridgeException if transaction creation fails
     */
    suspend fun createTransferNftTransaction(
        walletId: String,
        params: io.ton.walletkit.model.TONNFTTransferParamsHuman,
    ): String

    /**
     * Create an NFT transfer transaction with raw parameters.
     *
     * @param walletId Wallet ID in format "chainId:address" (e.g., "-239:UQDtFp...")
     * @param params Raw transfer parameters
     * @return Transaction content as JSON string
     * @throws WalletKitBridgeException if transaction creation fails
     */
    suspend fun createTransferNftRawTransaction(
        walletId: String,
        params: io.ton.walletkit.model.TONNFTTransferParamsRaw,
    ): String

    /**
     * Get jetton wallets owned by a wallet with pagination.
     *
     * @param walletId Wallet ID in format "chainId:address" (e.g., "-239:UQDtFp...")
     * @param limit Maximum number of jetton wallets to return
     * @param offset Offset for pagination
     * @return Jetton wallets with pagination info
     * @throws WalletKitBridgeException if the request fails
     */
    suspend fun getJettons(walletId: String, limit: Int = 100, offset: Int = 0): io.ton.walletkit.model.TONJettonWallets

    /**
     * Create a jetton transfer transaction.
     *
     * @param walletId Wallet ID in format "chainId:address" (e.g., "-239:UQDtFp...")
     * @param params Transfer parameters
     * @return Transaction content as JSON string
     * @throws WalletKitBridgeException if transaction creation fails
     */
    suspend fun createTransferJettonTransaction(
        walletId: String,
        params: io.ton.walletkit.model.TONJettonTransferParams,
    ): String

    /**
     * Create a multi-recipient TON transfer transaction.
     *
     * @param walletId Wallet ID in format "chainId:address" (e.g., "-239:UQDtFp...")
     * @param messages List of transfer parameters for each recipient
     * @return Transaction with optional preview
     * @throws WalletKitBridgeException if transaction creation fails
     */
    suspend fun createTransferMultiTonTransaction(
        walletId: String,
        messages: List<io.ton.walletkit.model.TONTransferParams>,
    ): io.ton.walletkit.model.TONTransactionWithPreview

    /**
     * Get a preview of a transaction including estimated fees.
     *
     * @param walletId Wallet ID in format "chainId:address" (e.g., "-239:UQDtFp...")
     * @param transactionContent Transaction content as JSON string
     * @return Transaction preview with fee estimation
     * @throws WalletKitBridgeException if preview generation fails
     */
    suspend fun getTransactionPreview(
        walletId: String,
        transactionContent: String,
    ): io.ton.walletkit.model.TONTransactionPreview

    /**
     * Get the balance of a specific jetton for a wallet.
     *
     * @param walletId Wallet ID in format "chainId:address" (e.g., "-239:UQDtFp...")
     * @param jettonAddress Jetton master contract address
     * @return Balance as a string (in jetton units)
     * @throws WalletKitBridgeException if balance retrieval fails
     */
    suspend fun getJettonBalance(walletId: String, jettonAddress: String): String

    /**
     * Get the jetton wallet address for a specific jetton master contract.
     *
     * @param walletId Wallet ID in format "chainId:address" (e.g., "-239:UQDtFp...")
     * @param jettonAddress Jetton master contract address
     * @return Jetton wallet contract address
     * @throws WalletKitBridgeException if address retrieval fails
     */
    suspend fun getJettonWalletAddress(walletId: String, jettonAddress: String): String

    /**
     * Call a bridge method directly.
     *
     * This is used internally by browser extensions to emit events.
     *
     * @param method The bridge method name
     * @param params Optional JSON parameters
     * @return The JSON response from the bridge
     * @throws WalletKitBridgeException if the call fails
     */
    suspend fun callBridgeMethod(method: String, params: org.json.JSONObject? = null): org.json.JSONObject

    /**
     * Add an event handler to receive SDK events.
     *
     * This allows adding event handlers after initialization, enabling on-demand
     * event handling. Multiple handlers can be added, and each will receive all events.
     *
     * @param eventsHandler Handler for SDK events
     */
    suspend fun addEventsHandler(eventsHandler: io.ton.walletkit.listener.TONBridgeEventsHandler)

    /**
     * Remove a previously added event handler.
     *
     * @param eventsHandler Handler to remove
     */
    suspend fun removeEventsHandler(eventsHandler: io.ton.walletkit.listener.TONBridgeEventsHandler)

    /**
     * Destroy the engine and release all resources.
     */
    suspend fun destroy()
}
