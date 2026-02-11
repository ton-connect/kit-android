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

import android.content.Context
import android.webkit.WebView
import io.ton.walletkit.ITONWallet
import io.ton.walletkit.ITONWalletKit
import io.ton.walletkit.WebViewTonConnectInjector
import io.ton.walletkit.api.generated.TONNetwork
import io.ton.walletkit.browser.TonConnectInjector
import io.ton.walletkit.config.TONWalletKitConfiguration
import io.ton.walletkit.engine.WalletKitEngine
import io.ton.walletkit.listener.TONBridgeEventsHandler
import io.ton.walletkit.model.KeyPair
import io.ton.walletkit.model.TONHex
import io.ton.walletkit.model.TONUserFriendlyAddress
import io.ton.walletkit.model.TONWalletAdapter
import io.ton.walletkit.model.WalletAdapterInfo
import io.ton.walletkit.model.WalletSigner
import io.ton.walletkit.model.WalletSignerInfo

/**
 * Main entry point for TON Wallet Kit SDK.
 *
 * Mirrors the canonical TON Wallet Kit specification for cross-platform consistency.
 *
 * Initialize the SDK by calling [initialize] with your configuration.
 * Then add event handlers using [addEventsHandler] when you're ready to receive events.
 *
 * **Important:** Unlike a singleton, each TONWalletKit instance is independent. When you're done
 * with an instance, call [destroy] or let it go out of scope to clean up resources and stop
 * receiving events.
 *
 * Example:
 * ```kotlin
 * val config = TONWalletKitConfiguration(
 *     network = TONNetwork.MAINNET,
 *     walletManifest = TONWalletKitConfiguration.Manifest(
 *         name = "My TON Wallet",
 *         appName = "Wallet",
 *         imageUrl = "https://example.com/icon.png",
 *         aboutUrl = "https://example.com",
 *         universalLink = "https://example.com/tc",
 *         bridgeUrl = "https://bridge.tonapi.io/bridge"
 *     ),
 *     bridge = TONWalletKitConfiguration.Bridge(
 *         bridgeUrl = "https://bridge.tonapi.io/bridge"
 *     ),
 *     features = listOf(
 *         TONWalletKitConfiguration.SendTransactionFeature(maxMessages = 4),
 *         TONWalletKitConfiguration.SignDataFeature(
 *             types = listOf(SignDataType.TEXT, SignDataType.BINARY, SignDataType.CELL)
 *         )
 *     ),
 *     storageType = TONWalletKitStorageType.Encrypted
 * )
 *
 * // Initialize SDK (returns instance)
 * val kit = TONWalletKit.initialize(context, config)
 *
 * // Later, add event handler when ready
 * val handler = object : TONBridgeEventsHandler {
 *     override fun handle(event: TONWalletKitEvent) {
 *         // Handle events
 *     }
 * }
 * kit.addEventsHandler(handler)
 *
 * // When done, destroy to stop receiving events and clean up
 * kit.destroy()
 * // or let kit = null (will auto-cleanup)
 * ```
 */
internal class TONWalletKit private constructor(
    /**
     * Internal engine instance. Exposed for TONWallet class access.
     * @suppress
     */
    @JvmSynthetic
    internal val engine: WalletKitEngine,
) : ITONWalletKit {

    companion object {
        /**
         * Initialize TON Wallet Kit with configuration.
         *
         * See class-level documentation for usage examples.
         */
        suspend fun initialize(
            context: Context,
            configuration: TONWalletKitConfiguration,
        ): ITONWalletKit {
            // Create engine with configuration using the WebView implementation
            val newEngine = WalletKitEngineFactory.create(
                kind = WalletKitEngineKind.WEBVIEW,
                context = context,
                configuration = configuration,
                eventsHandler = null,
            )

            return TONWalletKit(newEngine)
        }
    }

    @Volatile
    private var isDestroyed = false

    /**
     * Add an event handler to receive SDK events.
     *
     * This method can be called after initialization to start receiving events.
     * Multiple handlers can be added, and each will receive all events.
     * Events that occurred before the handler was added will be replayed if they
     * are still in the durable events queue.
     *
     * @param eventsHandler Handler for SDK events (connections, transactions, sign data, disconnects)
     * @throws IllegalStateException if SDK instance has been destroyed
     */
    override suspend fun addEventsHandler(eventsHandler: TONBridgeEventsHandler) {
        checkNotDestroyed()
        engine.addEventsHandler(eventsHandler)
    }

    /**
     * Remove a previously added event handler.
     *
     * @param eventsHandler Handler to remove
     */
    override suspend fun removeEventsHandler(eventsHandler: TONBridgeEventsHandler) {
        if (isDestroyed) return
        engine.removeEventsHandler(eventsHandler)
    }

    /**
     * Shut down the Wallet Kit instance and release all resources.
     *
     * This removes all event handlers, stops the WebView engine, and ensures
     * no more events will be received. After calling this, the instance cannot
     * be reused.
     *
     * This is called automatically when the instance is garbage collected.
     */
    override suspend fun destroy() {
        if (isDestroyed) return

        isDestroyed = true

        try {
            engine.destroy()
        } catch (e: Exception) {
            // Log but don't throw - cleanup should be best-effort
        }
    }

    private fun checkNotDestroyed() {
        if (isDestroyed) {
            throw IllegalStateException("TONWalletKit instance has been destroyed. Create a new instance.")
        }
    }

    // === Wallet Management Methods ===

    /**
     * Create a signer from mnemonic phrase.
     * Step 1 of the wallet creation pattern matching JS WalletKit.
     */
    override suspend fun createSignerFromMnemonic(
        mnemonic: List<String>,
        mnemonicType: String,
    ): WalletSignerInfo {
        checkNotDestroyed()
        return engine.createSignerFromMnemonic(mnemonic, mnemonicType)
    }

    /**
     * Create a signer from secret key (private key).
     * Step 1 of the wallet creation pattern matching JS WalletKit.
     */
    override suspend fun createSignerFromSecretKey(secretKey: ByteArray): WalletSignerInfo {
        checkNotDestroyed()
        return engine.createSignerFromSecretKey(secretKey)
    }

    /**
     * Create a signer from a custom WalletSigner implementation.
     * Step 1 of the wallet creation pattern, enabling hardware wallet integration.
     */
    override suspend fun createSignerFromCustom(signer: WalletSigner): WalletSignerInfo {
        checkNotDestroyed()
        return engine.createSignerFromCustom(signer)
    }

    /**
     * Create a V5R1 wallet adapter from a signer.
     * Step 2 of the wallet creation pattern matching JS WalletKit.
     */
    override suspend fun createV5R1Adapter(
        signer: WalletSignerInfo,
        network: TONNetwork,
        workchain: Int,
        walletId: Long,
    ): WalletAdapterInfo {
        checkNotDestroyed()
        val isCustom = engine.isCustomSigner(signer.signerId)
        return engine.createV5R1Adapter(
            signerId = signer.signerId,
            network = network,
            workchain = workchain,
            walletId = walletId,
            publicKey = if (isCustom) signer.publicKey?.value else null,
            isCustom = isCustom,
        )
    }

    /**
     * Create a V4R2 wallet adapter from a signer.
     * Step 2 of the wallet creation pattern matching JS WalletKit.
     */
    override suspend fun createV4R2Adapter(
        signer: WalletSignerInfo,
        network: TONNetwork,
        workchain: Int,
        walletId: Long,
    ): WalletAdapterInfo {
        checkNotDestroyed()
        val isCustom = engine.isCustomSigner(signer.signerId)
        return engine.createV4R2Adapter(
            signerId = signer.signerId,
            network = network,
            workchain = workchain,
            walletId = walletId,
            publicKey = if (isCustom) signer.publicKey?.value else null,
            isCustom = isCustom,
        )
    }

    /**
     * Add a wallet to the kit using an adapter.
     * Step 3 of the wallet creation pattern matching JS WalletKit.
     */
    override suspend fun addWallet(adapterId: String): ITONWallet {
        checkNotDestroyed()

        val account = engine.addWallet(adapterId)

        return TONWallet(
            id = account.walletId,
            address = account.address,
            engine = engine,
            account = account,
        )
    }

    /**
     * Add a wallet to the kit using a custom TONWalletAdapter.
     *
     * This wraps the adapter's WalletSigner-like signing into the 3-step pattern internally.
     * Mirrors iOS's `add(walletAdapter:)` method for cross-platform consistency.
     */
    override suspend fun addWallet(adapter: TONWalletAdapter): ITONWallet {
        checkNotDestroyed()

        // Create a WalletSigner wrapper around the adapter
        val walletSigner = AdapterBackedWalletSigner(adapter)

        // Step 1: Create signer from the adapter's signing capability
        val signerInfo = createSignerFromCustom(walletSigner)

        // Step 2: Create adapter based on wallet version
        val network = adapter.network()
        val version = adapter.walletVersion()
        val adapterInfo = when (version?.lowercase()) {
            "v4r2" -> createV4R2Adapter(signerInfo, network)
            else -> createV5R1Adapter(signerInfo, network)
        }

        // Step 3: Add wallet
        return addWallet(adapterInfo.adapterId)
    }

    /**
     * Get all wallets managed by this SDK instance.
     *
     * @return List of all wallets
     */
    override suspend fun getWallets(): List<ITONWallet> {
        checkNotDestroyed()

        val accounts = engine.getWallets()
        return accounts.map { account ->
            TONWallet(
                id = account.walletId,
                address = account.address,
                engine = engine,
                account = account,
            )
        }
    }

    /**
     * Get a single wallet by its address.
     */
    override suspend fun getWallet(address: TONUserFriendlyAddress): ITONWallet? {
        checkNotDestroyed()
        val account = engine.getWallet(address.value) ?: return null
        return TONWallet(
            id = account.walletId,
            address = account.address,
            engine = engine,
            account = account,
        )
    }

    /**
     * Remove a wallet by its address.
     */
    override suspend fun removeWallet(address: TONUserFriendlyAddress): Boolean {
        checkNotDestroyed()
        val wallet = getWallet(address)
        return if (wallet != null) {
            (wallet as TONWallet).remove()
            true
        } else {
            false
        }
    }

    /**
     * Clear all wallets from the SDK.
     */
    override suspend fun clearWallets() {
        checkNotDestroyed()
        val wallets = getWallets()
        wallets.forEach { (it as TONWallet).remove() }
    }

    /**
     * Generate a new TON mnemonic phrase.
     */
    override suspend fun createTonMnemonic(): List<String> {
        checkNotDestroyed()
        return engine.createTonMnemonic(wordCount = 24)
    }

    /**
     * Convert a mnemonic to an Ed25519 key pair.
     */
    override suspend fun mnemonicToKeyPair(
        mnemonic: List<String>,
        mnemonicType: String,
    ): KeyPair {
        checkNotDestroyed()
        return engine.mnemonicToKeyPair(mnemonic, mnemonicType)
    }

    /**
     * Sign arbitrary data using a secret key.
     */
    override suspend fun sign(
        data: ByteArray,
        secretKey: ByteArray,
    ): ByteArray {
        checkNotDestroyed()
        return engine.sign(data, secretKey)
    }

    /**
     * Handle a new transaction initiated from the wallet app.
     *
     * This method takes transaction content (created via wallet.createTransferTonTransaction,
     * wallet.transferJettonTransaction, etc.) and triggers the transaction approval flow.
     *
     * Matches the JS WalletKit API:
     * ```typescript
     * const tx = await wallet.createTransferTonTransaction(params);
     * await kit.handleNewTransaction(wallet, tx);
     * // This triggers onTransactionRequest event
     * ```
     *
     * The transaction will appear as a TransactionRequestEvent that can be approved or rejected
     * via the event handler.
     *
     * @param wallet The wallet that will sign and send the transaction
     * @param transactionContent Transaction content as JSON string (from createTransferTonTransaction, etc.)
     * @throws io.ton.walletkit.WalletKitBridgeException if transaction handling fails
     */
    override suspend fun handleNewTransaction(wallet: ITONWallet, transactionContent: String) {
        checkNotDestroyed()
        val addr = wallet.address?.value ?: throw IllegalArgumentException("Wallet address is null")
        engine.handleNewTransaction(addr, transactionContent)
    }

    /**
     * Disconnect a TON Connect session by session ID.
     *
     * @param sessionId The ID of the session to disconnect
     * @throws io.ton.walletkit.WalletKitBridgeException if session disconnection fails
     */
    override suspend fun disconnectSession(sessionId: String) {
        checkNotDestroyed()
        engine.disconnectSession(sessionId)
    }

    /**
     * List all active TON Connect sessions.
     *
     * @return List of all active sessions
     */
    override suspend fun listSessions(): List<io.ton.walletkit.session.TONConnectSession> {
        checkNotDestroyed()
        return engine.listSessions()
    }

    /**
     * Handle a TON Connect URL (deep link or QR code scan).
     *
     * This will parse the URL and trigger appropriate events through the events handler.
     *
     * @param url TON Connect URL (tc:// or https://)
     * @throws io.ton.walletkit.WalletKitBridgeException if URL handling fails
     */
    override suspend fun connect(url: String) {
        checkNotDestroyed()
        engine.handleTonConnectUrl(url)
    }

    /**
     * Create a WebView TonConnect injector for the given WebView.
     *
     * @param webView The WebView to inject TonConnect into
     * @return A WebViewTonConnectInjector that can setup and cleanup TonConnect
     */
    override fun createWebViewInjector(webView: WebView, walletId: String?): WebViewTonConnectInjector {
        return TonConnectInjector(webView, this, walletId)
    }
}

/**
 * Internal WalletSigner implementation that wraps a TONWalletAdapter.
 *
 * This bridges the TONWalletAdapter interface to the internal WalletSigner interface,
 * allowing adapters to provide their signing capabilities to the SDK engine.
 */
private class AdapterBackedWalletSigner(
    private val adapter: TONWalletAdapter,
) : WalletSigner {
    override fun publicKey(): TONHex = adapter.publicKey()

    override suspend fun sign(data: ByteArray): TONHex {
        // TODO: Implement direct signing through adapter's specific methods
        throw UnsupportedOperationException(
            "Direct signing through AdapterBackedWalletSigner is not supported. " +
                "The SDK should call the adapter's specific signing methods directly.",
        )
    }
}
