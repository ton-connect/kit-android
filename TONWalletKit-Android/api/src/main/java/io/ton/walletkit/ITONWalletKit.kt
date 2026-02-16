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

import android.content.Context
import io.ton.walletkit.api.MAINNET
import io.ton.walletkit.api.generated.TONNetwork
import io.ton.walletkit.config.TONWalletKitConfiguration
import io.ton.walletkit.internal.TONWalletKitFactory
import io.ton.walletkit.listener.TONBridgeEventsHandler
import io.ton.walletkit.model.KeyPair
import io.ton.walletkit.model.TONWalletAdapter
import io.ton.walletkit.model.WalletAdapterInfo
import io.ton.walletkit.model.WalletSigner
import io.ton.walletkit.model.WalletSignerInfo

/**
 * TON Wallet Kit SDK for managing wallets and TON Connect.
 *
 * Create an instance via [initialize], add event handlers with [addEventsHandler],
 * then add wallets either with the factory methods (createSigner → createV5R1Adapter → addWallet)
 * or by providing a custom [TONWalletAdapter] implementation.
 */
interface ITONWalletKit {
    companion object {
        /**
         * Initialize the SDK.
         *
         * @param context Android application context
         * @param config SDK configuration (network, storage, engine)
         * @return Initialized SDK instance
         */
        suspend inline fun initialize(
            context: Context,
            config: TONWalletKitConfiguration,
        ): ITONWalletKit = TONWalletKitFactory.create(context, config)
    }

    /**
     * Add event handler for TON Connect and transaction events.
     */
    suspend fun addEventsHandler(eventsHandler: TONBridgeEventsHandler)

    /**
     * Remove event handler.
     */
    suspend fun removeEventsHandler(eventsHandler: TONBridgeEventsHandler)

    /**
     * Shut down SDK and release resources.
     */
    suspend fun destroy()

    // ── Signer factory (matches iOS signer(mnemonic:) / signer(privateKey:)) ──

    /**
     * Create a signer from a mnemonic phrase.
     *
     * @param mnemonic 24-word mnemonic phrase
     * @param mnemonicType Mnemonic derivation type: "ton" (default) or "bip39"
     * @return Signer info containing ID and public key
     */
    suspend fun createSignerFromMnemonic(
        mnemonic: List<String>,
        mnemonicType: String = "ton",
    ): WalletSignerInfo

    /**
     * Create a signer from a secret key (private key).
     *
     * @param secretKey 32-byte secret key
     * @return Signer info containing ID and public key
     */
    suspend fun createSignerFromSecretKey(secretKey: ByteArray): WalletSignerInfo

    /**
     * Create a signer from a custom [WalletSigner] implementation (e.g. hardware wallet).
     *
     * @param signer Custom WalletSigner implementation
     * @return Signer info containing ID and public key
     * @see WalletSigner
     */
    suspend fun createSignerFromCustom(signer: WalletSigner): WalletSignerInfo

    // ── Adapter factory (matches iOS walletV5R1Adapter / walletV4R2Adapter) ──

    /**
     * Create a V5R1 wallet adapter from a signer.
     *
     * @param signer Signer info from createSignerFrom*
     * @param network Network to use (MAINNET or TESTNET)
     * @param workchain Workchain ID: 0 for basechain (default), -1 for masterchain
     * @param walletId Wallet ID
     * @return Adapter info containing ID and wallet address
     */
    suspend fun createV5R1Adapter(
        signer: WalletSignerInfo,
        network: TONNetwork = TONNetwork.MAINNET,
        workchain: Int = WalletKitConstants.DEFAULT_WORKCHAIN,
        walletId: Long = WalletKitConstants.DEFAULT_WALLET_ID_V5R1,
    ): WalletAdapterInfo

    /**
     * Create a V4R2 wallet adapter from a signer.
     *
     * @param signer Signer info from createSignerFrom*
     * @param network Network to use (MAINNET or TESTNET)
     * @param workchain Workchain ID: 0 for basechain (default), -1 for masterchain
     * @param walletId Wallet ID
     * @return Adapter info containing ID and wallet address
     */
    suspend fun createV4R2Adapter(
        signer: WalletSignerInfo,
        network: TONNetwork = TONNetwork.MAINNET,
        workchain: Int = WalletKitConstants.DEFAULT_WORKCHAIN,
        walletId: Long = WalletKitConstants.DEFAULT_WALLET_ID_V4R2,
    ): WalletAdapterInfo

    // ── Add wallet ──

    /**
     * Add a wallet using an adapter ID from [createV5R1Adapter] or [createV4R2Adapter].
     *
     * @param adapterId Adapter ID
     * @return Created wallet instance
     */
    suspend fun addWallet(adapterId: String): ITONWallet

    /**
     * Add a wallet to the kit using a custom [TONWalletAdapter].
     *
     * This wraps existing wallet entities directly, matching iOS's `add(walletAdapter:)`.
     * Use this when the host app already manages wallets (e.g. Tonkeeper).
     *
     * **Example:**
     * ```kotlin
     * class MyWalletAdapter(wallet: WalletEntity) : TONWalletAdapter {
     *     // implement interface methods...
     * }
     *
     * val adapter = MyWalletAdapter(myWallet)
     * val tonWallet = walletKit.addWallet(adapter)
     * ```
     *
     * @param adapter Custom wallet adapter wrapping an existing wallet
     * @return Created wallet instance
     * @see TONWalletAdapter
     */
    suspend fun addWallet(adapter: TONWalletAdapter): ITONWallet

    /**
     * Get all wallets managed by SDK.
     */
    suspend fun getWallets(): List<ITONWallet>

    /**
     * Get a single wallet by its ID.
     *
     * The wallet ID is the value returned by [TONWalletAdapter.identifier],
     * which should be the host app's stable wallet identifier.
     *
     * @param walletId Wallet ID (from adapter's identifier())
     * @return Wallet instance or null if not found
     */
    suspend fun getWallet(walletId: String): ITONWallet?

    /**
     * Remove a wallet by its ID.
     *
     * @param walletId Wallet ID (from adapter's identifier())
     * @return True if wallet was found and removed, false otherwise
     */
    suspend fun removeWallet(walletId: String): Boolean

    /**
     * Clear all wallets from the SDK.
     *
     * Removes all wallets from storage. This action cannot be undone.
     */
    suspend fun clearWallets()

    /**
     * Generate a new TON mnemonic phrase.
     *
     * Creates a 24-word mnemonic using TON-specific derivation.
     * This function should only be called once per wallet and the result
     * must be stored securely.
     *
     * @return List of 24 mnemonic words
     */
    suspend fun createTonMnemonic(): List<String>

    /**
     * Convert a mnemonic phrase to an Ed25519 key pair.
     *
     * This matches the JS WalletKit's `MnemonicToKeyPair` utility function.
     * Use this to derive both public and secret keys from a mnemonic.
     *
     * @param mnemonic 12 or 24-word mnemonic phrase
     * @param mnemonicType Derivation type: "ton" (default) or "bip39"
     * @return KeyPair containing public key (32 bytes) and secret key (64 bytes)
     */
    suspend fun mnemonicToKeyPair(
        mnemonic: List<String>,
        mnemonicType: String = "ton",
    ): KeyPair

    /**
     * Sign arbitrary data using a secret key.
     *
     * @param data Data bytes to sign
     * @param secretKey Secret key bytes for signing
     * @return Signature bytes
     */
    suspend fun sign(
        data: ByteArray,
        secretKey: ByteArray,
    ): ByteArray

    /**
     * Trigger transaction approval flow.
     *
     * This triggers the onTransactionRequest event for user confirmation.
     *
     * @param wallet Wallet to send from
     * @param transactionContent Transaction from createTransfer* methods
     */
    suspend fun handleNewTransaction(wallet: ITONWallet, transactionContent: String)

    /**
     * Handle a TON Connect URL.
     *
     * Use this to process TON Connect deep links or QR code scans.
     * The URL will be parsed and appropriate events will be triggered.
     *
     * @param url TON Connect URL (tc:// or https://)
     */
    suspend fun connect(url: String)

    /**
     * List all active TON Connect sessions.
     *
     * @return List of all active sessions
     */
    suspend fun listSessions(): List<io.ton.walletkit.session.TONConnectSession>

    /**
     * Disconnect TON Connect session.
     *
     * @param sessionId Session ID to disconnect
     */
    suspend fun disconnectSession(sessionId: String)

    /**
     * Create WebView TON Connect injector.
     *
     * Used internally by WebView extension functions.
     *
     * @param webView WebView to inject into
     * @param walletId Optional SDK wallet ID to scope JS bridge events to a specific wallet
     * @return Injector for setup/cleanup
     */
    fun createWebViewInjector(webView: android.webkit.WebView, walletId: String? = null): WebViewTonConnectInjector
}

/**
 * WebView TON Connect injector for enabling TON Connect in WebView.
 *
 * Created by [ITONWalletKit.createWebViewInjector].
 */
interface WebViewTonConnectInjector {
    /**
     * Setup TON Connect in WebView.
     */
    fun setup()

    /**
     * Cleanup TON Connect resources.
     */
    fun cleanup()
}
