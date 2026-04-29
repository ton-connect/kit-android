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

import io.ton.walletkit.api.generated.TONConnectionApprovalResponse
import io.ton.walletkit.api.generated.TONConnectionRequestEvent
import io.ton.walletkit.api.generated.TONDeDustSwapProviderConfig
import io.ton.walletkit.api.generated.TONJettonsResponse
import io.ton.walletkit.api.generated.TONJettonsTransferRequest
import io.ton.walletkit.api.generated.TONNFT
import io.ton.walletkit.api.generated.TONNFTRawTransferRequest
import io.ton.walletkit.api.generated.TONNFTTransferRequest
import io.ton.walletkit.api.generated.TONNFTsResponse
import io.ton.walletkit.api.generated.TONNetwork
import io.ton.walletkit.api.generated.TONOmnistonSwapProviderConfig
import io.ton.walletkit.api.generated.TONSendTransactionApprovalResponse
import io.ton.walletkit.api.generated.TONSendTransactionRequestEvent
import io.ton.walletkit.api.generated.TONSignDataApprovalResponse
import io.ton.walletkit.api.generated.TONSignDataRequestEvent
import io.ton.walletkit.api.generated.TONSignatureDomain
import io.ton.walletkit.api.generated.TONStakeParams
import io.ton.walletkit.api.generated.TONStakingBalance
import io.ton.walletkit.api.generated.TONStakingProviderInfo
import io.ton.walletkit.api.generated.TONStakingQuote
import io.ton.walletkit.api.generated.TONStakingQuoteParams
import io.ton.walletkit.api.generated.TONSwapParams
import io.ton.walletkit.api.generated.TONSwapQuote
import io.ton.walletkit.api.generated.TONSwapQuoteParams
import io.ton.walletkit.api.generated.TONTonStakersChainConfig
import io.ton.walletkit.api.generated.TONTransactionEmulatedPreview
import io.ton.walletkit.api.generated.TONTransferRequest
import io.ton.walletkit.api.generated.TONUnstakeMode
import io.ton.walletkit.config.TONWalletKitConfiguration
import io.ton.walletkit.core.streaming.StreamingEvent
import io.ton.walletkit.engine.model.WalletAccount
import io.ton.walletkit.engine.state.KotlinStreamingProviderManager
import io.ton.walletkit.model.KeyPair
import io.ton.walletkit.model.TONHex
import io.ton.walletkit.model.TONWalletAdapter
import io.ton.walletkit.model.WalletSigner
import io.ton.walletkit.model.WalletSignerInfo
import io.ton.walletkit.request.RequestHandler
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.serialization.json.JsonElement

/**
 * Abstraction over a runtime that can execute the WalletKit JavaScript bundle and expose
 * the wallet APIs to Android callers. The runtime is backed by an Android WebView.
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
    val streamingEvents: SharedFlow<StreamingEvent>
    val kotlinStreamingProviderManager: KotlinStreamingProviderManager

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
     *
     * @param mnemonic Mnemonic phrase as a list of words
     * @param mnemonicType Mnemonic type ("ton" or "bip39"), defaults to "ton"
     * @return Signer info with ID and public key
     */
    suspend fun createSignerFromMnemonic(
        mnemonic: List<String>,
        mnemonicType: String = "ton",
    ): WalletSignerInfo

    /**
     * Create a signer from a secret key hex string.
     *
     * @param secretKeyHex Private key as hex string
     * @return Signer info with ID and public key
     */
    suspend fun createSignerFromSecretKey(
        secretKeyHex: String,
    ): WalletSignerInfo

    suspend fun createSignerFromCustom(signer: WalletSigner): WalletSignerInfo

    suspend fun createAdapter(
        signerId: String,
        publicKey: TONHex,
        version: String,
        network: TONNetwork? = null,
        workchain: Int = 0,
        walletId: Long = 2147483409L,
        domain: TONSignatureDomain? = null,
    ): TONWalletAdapter

    suspend fun addWallet(adapter: io.ton.walletkit.model.TONWalletAdapter): WalletAccount

    suspend fun getWallets(): List<WalletAccount>

    suspend fun getWallet(walletId: String): WalletAccount?

    suspend fun removeWallet(walletId: String)

    /**
     * Get the current state of a wallet.
     *
     * @param walletId Wallet ID
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
     * Parse a TON Connect URL into a connection request without routing it to event handlers.
     * Allows inline handling of connection requests.
     *
     * @param url TON Connect URL (tc:// or https://)
     * @return A connection request that can be approved or rejected
     * @throws WalletKitBridgeException if URL parsing fails
     */
    suspend fun connectionEventFromUrl(url: String): io.ton.walletkit.request.TONWalletConnectionRequest

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
        walletId: String? = null,
    )

    /**
     * Create a TON transfer transaction.
     *
     * This method creates transaction content matching the JS WalletKit API wallet.createTransferTonTransaction().
     * The returned transaction content can be passed to handleNewTransaction() to trigger the approval flow.
     * Use getTransactionPreview() to get fee estimation.
     *
     * @param walletId Wallet ID
     * @param params Transfer parameters (recipient, amount, optional comment/body/stateInit)
     * @return Transaction content as JSON string
     * @throws WalletKitBridgeException if transaction creation fails
     */
    suspend fun createTransferTonTransaction(
        walletId: String,
        params: TONTransferRequest,
    ): String

    /**
     * Handle a new transaction initiated from the wallet app.
     *
     * This method matches the JS WalletKit API kit.handleNewTransaction() and triggers
     * a transaction request event that can be approved or rejected via the event handler.
     *
     * @param walletId Wallet ID
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
     * @param walletId Wallet ID
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
     * The event should have walletId and walletAddress set.
     *
     * @param event Typed event from the connect request with wallet info
     * @param response Optional pre-computed approval response
     * @throws WalletKitBridgeException if approval fails
     */
    override suspend fun approveConnect(
        event: TONConnectionRequestEvent,
        response: TONConnectionApprovalResponse?,
    )

    /**
     * Reject a connection request from a dApp.
     *
     * @param event Typed event from the connect request
     * @param reason Optional reason for rejection
     * @param errorCode Optional error code for TON Connect protocol
     * @throws WalletKitBridgeException if rejection fails
     */
    override suspend fun rejectConnect(
        event: TONConnectionRequestEvent,
        reason: String?,
        errorCode: Int?,
    )

    /**
     * Approve and sign a transaction request.
     *
     * @param event Typed event from the transaction request
     * @param response Optional pre-computed approval response
     * @throws WalletKitBridgeException if approval or signing fails
     */
    override suspend fun approveTransaction(
        event: TONSendTransactionRequestEvent,
        response: TONSendTransactionApprovalResponse?,
    )

    /**
     * Reject a transaction request.
     *
     * @param event Typed event from the transaction request
     * @param reason Optional reason for rejection
     * @param errorCode Optional error code (defaults to USER_REJECTS_ERROR=300)
     * @throws WalletKitBridgeException if rejection fails
     */
    override suspend fun rejectTransaction(
        event: TONSendTransactionRequestEvent,
        reason: String?,
        errorCode: Int?,
    )

    /**
     * Approve and sign a data signing request.
     *
     * @param event Typed event from the sign data request
     * @param response Optional pre-computed approval response
     * @throws WalletKitBridgeException if approval or signing fails
     */
    override suspend fun approveSignData(
        event: TONSignDataRequestEvent,
        response: TONSignDataApprovalResponse?,
    )

    /**
     * Reject a data signing request.
     *
     * @param event Typed event from the sign data request
     * @param reason Optional reason for rejection
     * @param errorCode Optional error code for rejection
     * @throws WalletKitBridgeException if rejection fails
     */
    override suspend fun rejectSignData(
        event: TONSignDataRequestEvent,
        reason: String?,
        errorCode: Int?,
    )

    /**
     * Get all active TON Connect sessions.
     *
     * @return List of active sessions
     */
    suspend fun listSessions(): List<io.ton.walletkit.session.TONConnectSession>

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
    suspend fun getNfts(walletId: String, limit: Int = 100, offset: Int = 0): TONNFTsResponse

    /**
     * Get a single NFT by its address.
     *
     * @param nftAddress NFT contract address
     * @return NFT item or null if not found
     * @throws WalletKitBridgeException if the request fails
     */
    suspend fun getNft(nftAddress: String): TONNFT?

    /**
     * Create an NFT transfer transaction with human-friendly parameters.
     *
     * @param walletId Wallet ID
     * @param params Transfer parameters
     * @return Transaction content as JSON string
     * @throws WalletKitBridgeException if transaction creation fails
     */
    suspend fun createTransferNftTransaction(
        walletId: String,
        params: TONNFTTransferRequest,
    ): String

    /**
     * Create an NFT transfer transaction with raw parameters.
     *
     * @param walletId Wallet ID
     * @param params Raw transfer parameters
     * @return Transaction content as JSON string
     * @throws WalletKitBridgeException if transaction creation fails
     */
    suspend fun createTransferNftRawTransaction(
        walletId: String,
        params: TONNFTRawTransferRequest,
    ): String

    /**
     * Get jetton wallets owned by a wallet with pagination.
     *
     * @param walletId Wallet ID
     * @param limit Maximum number of jetton wallets to return
     * @param offset Offset for pagination
     * @return Jetton wallets with pagination info
     * @throws WalletKitBridgeException if the request fails
     */
    suspend fun getJettons(walletId: String, limit: Int = 100, offset: Int = 0): TONJettonsResponse

    /**
     * Create a jetton transfer transaction.
     *
     * @param walletId Wallet ID
     * @param params Transfer parameters
     * @return Transaction content as JSON string
     * @throws WalletKitBridgeException if transaction creation fails
     */
    suspend fun createTransferJettonTransaction(
        walletId: String,
        params: TONJettonsTransferRequest,
    ): String

    /**
     * Create a multi-recipient TON transfer transaction.
     *
     * @param walletId Wallet ID
     * @param messages List of transfer parameters for each recipient
     * @return Transaction content as JSON string
     * @throws WalletKitBridgeException if transaction creation fails
     */
    suspend fun createTransferMultiTonTransaction(
        walletId: String,
        messages: List<TONTransferRequest>,
    ): String

    /**
     * Get a preview of a transaction including estimated fees.
     *
     * @param walletId Wallet ID
     * @param transactionContent Transaction content as JSON string
     * @return Transaction preview with fee estimation
     * @throws WalletKitBridgeException if preview generation fails
     */
    suspend fun getTransactionPreview(
        walletId: String,
        transactionContent: String,
    ): TONTransactionEmulatedPreview

    /**
     * Get the balance of a specific jetton for a wallet.
     *
     * @param walletId Wallet ID
     * @param jettonAddress Jetton master contract address
     * @return Balance as a string (in jetton units)
     * @throws WalletKitBridgeException if balance retrieval fails
     */
    suspend fun getJettonBalance(walletId: String, jettonAddress: String): String

    /**
     * Get the jetton wallet address for a specific jetton master contract.
     *
     * @param walletId Wallet ID
     * @param jettonAddress Jetton master contract address
     * @return Jetton wallet contract address
     * @throws WalletKitBridgeException if address retrieval fails
     */
    suspend fun getJettonWalletAddress(walletId: String, jettonAddress: String): String

    // ── Swap ──

    suspend fun createOmnistonSwapProvider(config: TONOmnistonSwapProviderConfig?): String

    suspend fun createDeDustSwapProvider(config: TONDeDustSwapProviderConfig?): String

    suspend fun registerSwapProvider(providerId: String)

    suspend fun setDefaultSwapProvider(providerId: String)

    suspend fun getRegisteredSwapProviders(): List<String>

    suspend fun hasSwapProvider(providerId: String): Boolean

    /**
     * Registry for Kotlin-implemented [io.ton.walletkit.swap.ITONSwapProvider] instances. Reverse-RPC
     * calls from JS's `ProxySwapProvider` are routed here by [io.ton.walletkit.engine.infrastructure.MessageDispatcher].
     */
    val kotlinSwapProviderManager: io.ton.walletkit.engine.state.KotlinSwapProviderManager

    /**
     * Tell the JS side to create a `ProxySwapProvider` bound to [providerId] and register it
     * with the JS swap manager. Called after [kotlinSwapProviderManager] has the Kotlin instance
     * so reverse-RPC calls can find it.
     */
    suspend fun registerKotlinSwapProvider(providerId: String)

    suspend fun getSwapQuote(params: TONSwapQuoteParams<JsonElement>, providerId: String?): TONSwapQuote

    suspend fun buildSwapTransaction(params: TONSwapParams<JsonElement>): String

    // ── Staking ──

    /**
     * Create a TonStakers staking provider in the JS bridge.
     *
     * @param chainConfig Chain-ID keyed config, e.g. { "-239" to TONTonStakersChainConfig(...) }
     * @return JS registry reference ID for the created provider
     */
    suspend fun createTonStakersStakingProvider(chainConfig: Map<String, TONTonStakersChainConfig>?): String

    /** Register a previously created staking provider with the staking manager. */
    suspend fun registerStakingProvider(providerId: String)

    /** Set the default staking provider used when no providerId is specified. */
    suspend fun setDefaultStakingProvider(providerId: String)

    /** Get the IDs of all registered staking providers. */
    suspend fun getRegisteredStakingProviders(): List<String>

    /** Check if a staking provider with the given ID is registered. */
    suspend fun hasStakingProvider(providerId: String): Boolean

    /**
     * Registry for Kotlin-implemented [io.ton.walletkit.staking.ITONStakingProvider] instances. Reverse-RPC
     * calls from JS's `ProxyStakingProvider` are routed here by [io.ton.walletkit.engine.infrastructure.MessageDispatcher].
     */
    val kotlinStakingProviderManager: io.ton.walletkit.engine.state.KotlinStakingProviderManager

    /**
     * Tell the JS side to create a `ProxyStakingProvider` bound to [providerId] and register it
     * with the JS staking manager. Called after [kotlinStakingProviderManager] has the Kotlin
     * instance so reverse-RPC calls can find it.
     *
     * @param supportedUnstakeModesJson JSON array of supported unstake modes, fetched eagerly so the
     *   JS proxy can satisfy the synchronous `getSupportedUnstakeModes()` contract without a round-trip.
     */
    suspend fun registerKotlinStakingProvider(providerId: String, supportedUnstakeModesJson: String)

    suspend fun getStakingQuote(
        params: TONStakingQuoteParams<JsonElement>,
        providerId: String?,
    ): TONStakingQuote

    suspend fun buildStakeTransaction(
        params: TONStakeParams<JsonElement>,
        providerId: String?,
    ): String

    suspend fun getStakedBalance(
        userAddress: String,
        network: TONNetwork?,
        providerId: String?,
    ): TONStakingBalance

    suspend fun getStakingProviderInfo(
        network: TONNetwork?,
        providerId: String?,
    ): TONStakingProviderInfo

    suspend fun getSupportedUnstakeModes(providerId: String?): List<TONUnstakeMode>

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
