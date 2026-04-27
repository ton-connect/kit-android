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
import android.webkit.WebView
import io.ton.walletkit.api.MAINNET
import io.ton.walletkit.api.TONTonStakersProviderConfig
import io.ton.walletkit.api.generated.TONDeDustSwapProviderConfig
import io.ton.walletkit.api.generated.TONNetwork
import io.ton.walletkit.api.generated.TONOmnistonSwapProviderConfig
import io.ton.walletkit.api.generated.TONSignatureDomain
import io.ton.walletkit.api.generated.TONTonApiStreamingProviderConfig
import io.ton.walletkit.api.generated.TONTonCenterStreamingProviderConfig
import io.ton.walletkit.config.TONWalletKitConfiguration
import io.ton.walletkit.internal.TONWalletKitFactory
import io.ton.walletkit.listener.TONBridgeEventsHandler
import io.ton.walletkit.model.KeyPair
import io.ton.walletkit.model.TONWalletAdapter
import io.ton.walletkit.model.WalletSigner
import io.ton.walletkit.model.WalletSignerInfo
import io.ton.walletkit.request.TONWalletConnectionRequest
import io.ton.walletkit.session.TONConnectSession
import io.ton.walletkit.staking.ITONStakingManager
import io.ton.walletkit.staking.tonstakers.TONTonStakersStakingProvider
import io.ton.walletkit.streaming.ITONStreamingManager
import io.ton.walletkit.streaming.ITONStreamingProvider
import io.ton.walletkit.swap.ITONSwapManager
import io.ton.walletkit.swap.dedust.TONDeDustSwapProvider
import io.ton.walletkit.swap.omniston.TONOmnistonSwapProvider

/**
 * TON Wallet Kit SDK for managing wallets and TON Connect.
 */
interface ITONWalletKit {
    companion object {
        suspend inline fun initialize(
            context: Context,
            config: TONWalletKitConfiguration,
        ): ITONWalletKit = TONWalletKitFactory.create(context, config)
    }

    /**
     * Add event handler for TON Connect and transaction events.
     */
    suspend fun addEventsHandler(eventsHandler: TONBridgeEventsHandler)

    suspend fun removeEventsHandler(eventsHandler: TONBridgeEventsHandler)

    suspend fun destroy()

    // ── Signer factory ──

    /**
     * Create a signer from a mnemonic phrase.
     */
    suspend fun createSignerFromMnemonic(
        mnemonic: List<String>,
        mnemonicType: String = "ton",
    ): WalletSignerInfo

    /**
     * Create a signer from a 32-byte secret key.
     */
    suspend fun createSignerFromSecretKey(
        secretKey: ByteArray,
    ): WalletSignerInfo

    /**
     * Create a signer from a custom [WalletSigner] (e.g. hardware wallet).
     */
    suspend fun createSignerFromCustom(signer: WalletSigner): WalletSignerInfo

    // ── Adapter factory ──

    /**
     * Create a V5R1 wallet adapter.
     *
     * @param domain Optional signature domain for L2 chains (e.g. Tetra).
     */
    suspend fun createV5R1Adapter(
        signer: WalletSignerInfo,
        network: TONNetwork = TONNetwork.MAINNET,
        workchain: Int = WalletKitConstants.DEFAULT_WORKCHAIN,
        walletId: Long = WalletKitConstants.DEFAULT_WALLET_ID_V5R1,
        domain: TONSignatureDomain? = null,
    ): TONWalletAdapter

    /**
     * Create a V4R2 wallet adapter.
     *
     * @param domain Optional signature domain for L2 chains (e.g. Tetra).
     */
    suspend fun createV4R2Adapter(
        signer: WalletSignerInfo,
        network: TONNetwork = TONNetwork.MAINNET,
        workchain: Int = WalletKitConstants.DEFAULT_WORKCHAIN,
        walletId: Long = WalletKitConstants.DEFAULT_WALLET_ID_V4R2,
        domain: TONSignatureDomain? = null,
    ): TONWalletAdapter

    // ── Add wallet ──

    /**
     * Add a wallet using a [TONWalletAdapter].
     */
    suspend fun addWallet(adapter: TONWalletAdapter): ITONWallet

    suspend fun getWallets(): List<ITONWallet>

    /**
     * Get a wallet by its ID (from [TONWalletAdapter.identifier]).
     */
    suspend fun getWallet(walletId: String): ITONWallet?

    suspend fun removeWallet(walletId: String): Boolean

    suspend fun clearWallets()

    /**
     * Generate a new 24-word TON mnemonic phrase.
     */
    suspend fun createTonMnemonic(): List<String>

    /**
     * Convert a mnemonic phrase to an Ed25519 key pair.
     */
    suspend fun mnemonicToKeyPair(
        mnemonic: List<String>,
        mnemonicType: String = "ton",
    ): KeyPair

    suspend fun sign(
        data: ByteArray,
        secretKey: ByteArray,
    ): ByteArray

    /**
     * Trigger transaction approval flow.
     */
    suspend fun handleNewTransaction(wallet: ITONWallet, transactionContent: String)

    /**
     * Handle a TON Connect URL (tc:// or https://).
     */
    suspend fun connect(url: String)

    /**
     * Parse a TON Connect URL into a connection request without routing it to event handlers.
     * Allows inline handling of connection requests instead of waiting for [TONBridgeEventsHandler.onConnectRequest].
     */
    suspend fun connectionEventFromUrl(url: String): TONWalletConnectionRequest

    suspend fun listSessions(): List<TONConnectSession>

    suspend fun disconnectSession(sessionId: String)

    /**
     * Create WebView TON Connect injector.
     */
    fun createWebViewInjector(webView: WebView, walletId: String? = null): WebViewTonConnectInjector

    // ── Swap ──

    /**
     * Create an Omniston (STON.fi) swap provider.
     *
     * Call [swap().registerProvider] with the returned handle before calling [swap().getQuote].
     */
    suspend fun omnistonSwapProvider(config: TONOmnistonSwapProviderConfig? = null): TONOmnistonSwapProvider

    /**
     * Create a DeDust swap provider.
     *
     * Call [swap().registerProvider] with the returned handle before calling [swap().getQuote].
     */
    suspend fun dedustSwapProvider(config: TONDeDustSwapProviderConfig? = null): TONDeDustSwapProvider

    /**
     * Get the swap manager for registering providers and executing swaps.
     */
    suspend fun swap(): ITONSwapManager

    // ── Staking ──

    /**
     * Access the staking manager for registering providers and performing staking operations.
     */
    fun staking(): ITONStakingManager

    /**
     * Create a TonStakers staking provider.
     *
     * Call [ITONStakingManager.register] with the returned provider to make it available for quotes.
     *
     * @param config Optional per-chain configuration (contract address, TonAPI key)
     * @return A provider that can be registered with [staking]
     */
    suspend fun tonStakersStakingProvider(
        config: TONTonStakersProviderConfig? = null,
    ): TONTonStakersStakingProvider

    // ── Streaming ──

    /**
     * Get the streaming manager.
     */
    fun streaming(): ITONStreamingManager

    suspend fun createStreamingProvider(
        config: TONTonCenterStreamingProviderConfig,
    ): ITONStreamingProvider

    suspend fun createStreamingProvider(
        config: TONTonApiStreamingProviderConfig,
    ): ITONStreamingProvider
}

interface WebViewTonConnectInjector {
    fun setup()
    fun cleanup()
}
