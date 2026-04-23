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
package io.ton.walletkit.internal.constants

/**
 * Constants for JavaScript bridge method names.
 *
 * These constants define the method names used when calling JavaScript functions
 * in the WebView-based WalletKit engine. Centralizing these names ensures consistency
 * and makes refactoring easier.
 *
 * @suppress Internal implementation constants. Not part of public API.
 */
internal object BridgeMethodConstants {
    /**
     * Method name for initializing the WalletKit bridge.
     */
    const val METHOD_INIT = "init"

    /**
     * Method name for setting up event listeners.
     */
    const val METHOD_SET_EVENTS_LISTENERS = "setEventsListeners"

    /**
     * Method name for removing event listeners.
     */
    const val METHOD_REMOVE_EVENT_LISTENERS = "removeEventListeners"

    /**
     * Method name for getting all wallets.
     */
    const val METHOD_GET_WALLETS = "getWallets"

    const val METHOD_CREATE_SIGNER_FROM_MNEMONIC = "createSignerFromMnemonic"
    const val METHOD_CREATE_SIGNER_FROM_PRIVATE_KEY = "createSignerFromPrivateKey"
    const val METHOD_CREATE_SIGNER_FROM_CUSTOM = "createSignerFromCustom"
    const val METHOD_CREATE_V5R1_WALLET_ADAPTER = "createV5R1WalletAdapter"
    const val METHOD_CREATE_V4R2_WALLET_ADAPTER = "createV4R2WalletAdapter"
    const val METHOD_ADD_WALLET = "addWallet"
    const val METHOD_RELEASE_REF = "releaseRef"

    /**
     * Method name for getting a single wallet by address.
     */
    const val METHOD_GET_WALLET = "getWallet"

    /**
     * Method name for removing a wallet.
     */
    const val METHOD_REMOVE_WALLET = "removeWallet"

    /**
     * Method name for getting wallet balance.
     */
    const val METHOD_GET_BALANCE = "getBalance"

    /**
     * Method name for handling TON Connect URL.
     */
    const val METHOD_HANDLE_TON_CONNECT_URL = "handleTonConnectUrl"

    /**
     * Method name for parsing a TON Connect URL into a connection request event without routing to event handlers.
     */
    const val METHOD_CONNECTION_EVENT_FROM_URL = "connectionEventFromUrl"

    /**
     * Method name for creating a TON transfer transaction.
     * Matches JS API: wallet.createTransferTonTransaction()
     */
    const val METHOD_CREATE_TRANSFER_TON_TRANSACTION = "createTransferTonTransaction"

    /**
     * Method name for handling a new transaction.
     * Matches JS API: kit.handleNewTransaction()
     */
    const val METHOD_HANDLE_NEW_TRANSACTION = "handleNewTransaction"

    /**
     * Method name for sending a transaction to the blockchain.
     * This sends arbitrary transaction content (created by transferNFT, etc.)
     */
    const val METHOD_SEND_TRANSACTION = "sendTransaction"

    /**
     * Method name for approving a connect request.
     */
    const val METHOD_APPROVE_CONNECT_REQUEST = "approveConnectRequest"

    /**
     * Method name for rejecting a connect request.
     */
    const val METHOD_REJECT_CONNECT_REQUEST = "rejectConnectRequest"

    /**
     * Method name for approving a transaction request.
     */
    const val METHOD_APPROVE_TRANSACTION_REQUEST = "approveTransactionRequest"

    /**
     * Method name for rejecting a transaction request.
     */
    const val METHOD_REJECT_TRANSACTION_REQUEST = "rejectTransactionRequest"

    /**
     * Method name for approving a sign data request.
     */
    const val METHOD_APPROVE_SIGN_DATA_REQUEST = "approveSignDataRequest"

    /**
     * Method name for rejecting a sign data request.
     */
    const val METHOD_REJECT_SIGN_DATA_REQUEST = "rejectSignDataRequest"

    /**
     * Method name for listing all active sessions.
     */
    const val METHOD_LIST_SESSIONS = "listSessions"

    /**
     * Method name for disconnecting a session.
     */
    const val METHOD_DISCONNECT_SESSION = "disconnectSession"

    /**
     * Method name for converting mnemonic to key pair.
     */
    const val METHOD_MNEMONIC_TO_KEY_PAIR = "mnemonicToKeyPair"

    /**
     * Method name for signing arbitrary data with a secret key.
     */
    const val METHOD_SIGN = "sign"

    /**
     * Method name for generating a new TON mnemonic via the JS bundle.
     */
    const val METHOD_CREATE_TON_MNEMONIC = "createTonMnemonic"

    /**
     * Method name for processing an internal browser TonConnect request.
     */
    const val METHOD_PROCESS_INTERNAL_BROWSER_REQUEST = "processInternalBrowserRequest"

    /**
     * Method name for emitting a browser page started event to JavaScript.
     */
    const val METHOD_EMIT_BROWSER_PAGE_STARTED = "emitBrowserPageStarted"

    /**
     * Method name for emitting a browser page finished event to JavaScript.
     */
    const val METHOD_EMIT_BROWSER_PAGE_FINISHED = "emitBrowserPageFinished"

    /**
     * Method name for emitting a browser error event to JavaScript.
     */
    const val METHOD_EMIT_BROWSER_ERROR = "emitBrowserError"

    /**
     * Method name for emitting a browser bridge request diagnostic event.
     */
    const val METHOD_EMIT_BROWSER_BRIDGE_REQUEST = "emitBrowserBridgeRequest"

    /**
     * Method name for getting NFTs owned by a wallet.
     */
    const val METHOD_GET_NFTS = "getNfts"

    /**
     * Method name for getting a single NFT by address.
     */
    const val METHOD_GET_NFT = "getNft"

    /**
     * Method name for creating an NFT transfer transaction with human-friendly parameters.
     */
    const val METHOD_CREATE_TRANSFER_NFT_TRANSACTION = "createTransferNftTransaction"

    /**
     * Method name for creating an NFT transfer transaction with raw parameters.
     */
    const val METHOD_CREATE_TRANSFER_NFT_RAW_TRANSACTION = "createTransferNftRawTransaction"

    /**
     * Method name for getting jettons owned by a wallet.
     */
    const val METHOD_GET_JETTONS = "getJettons"

    /**
     * Method name for creating a jetton transfer transaction.
     */
    const val METHOD_CREATE_TRANSFER_JETTON_TRANSACTION = "createTransferJettonTransaction"

    /**
     * Method name for creating a multi-recipient TON transfer transaction.
     */
    const val METHOD_CREATE_TRANSFER_MULTI_TON_TRANSACTION = "createTransferMultiTonTransaction"

    /**
     * Method name for getting a transaction preview with fee estimation.
     */
    const val METHOD_GET_TRANSACTION_PREVIEW = "getTransactionPreview"

    /**
     * Method name for getting jetton balance for a wallet.
     */
    const val METHOD_GET_JETTON_BALANCE = "getJettonBalance"

    /**
     * Method name for getting jetton wallet address.
     */
    const val METHOD_GET_JETTON_WALLET_ADDRESS = "getJettonWalletAddress"

    // Swap methods

    /** Create an Omniston swap provider instance. */
    const val METHOD_CREATE_OMNISTON_SWAP_PROVIDER = "createOmnistonSwapProvider"

    /** Create a DeDust swap provider instance. */
    const val METHOD_CREATE_DEDUST_SWAP_PROVIDER = "createDeDustSwapProvider"

    /** Register a previously created swap provider with the swap manager. */
    const val METHOD_REGISTER_SWAP_PROVIDER = "registerSwapProvider"

    /** Set the default swap provider. */
    const val METHOD_SET_DEFAULT_SWAP_PROVIDER = "setDefaultSwapProvider"

    /** Get all registered swap provider IDs. */
    const val METHOD_GET_REGISTERED_SWAP_PROVIDERS = "getRegisteredSwapProviders"

    /** Check if a swap provider is registered. */
    const val METHOD_HAS_SWAP_PROVIDER = "hasSwapProvider"

    /** Get a swap quote from a registered provider. */
    const val METHOD_GET_SWAP_QUOTE = "getSwapQuote"

    /** Build a transaction for executing a swap. */
    const val METHOD_BUILD_SWAP_TRANSACTION = "buildSwapTransaction"

    /**
     * Method name for telling JS to create a ProxySwapProvider that forwards calls back to a
     * Kotlin-implemented [io.ton.walletkit.swap.ITONSwapProvider] via reverse-RPC.
     */
    const val METHOD_REGISTER_KOTLIN_SWAP_PROVIDER = "registerKotlinSwapProvider"

    // Staking methods

    /** Create a TonStakers staking provider instance. */
    const val METHOD_CREATE_TON_STAKERS_STAKING_PROVIDER = "createTonStakersStakingProvider"

    /** Register a previously created staking provider with the staking manager. */
    const val METHOD_REGISTER_STAKING_PROVIDER = "registerStakingProvider"

    /** Set the default staking provider. */
    const val METHOD_SET_DEFAULT_STAKING_PROVIDER = "setDefaultStakingProvider"

    /** Get a staking quote (stake or unstake) from a registered provider. */
    const val METHOD_GET_STAKING_QUOTE = "getStakingQuote"

    /** Build a stake or unstake transaction. */
    const val METHOD_BUILD_STAKE_TRANSACTION = "buildStakeTransaction"

    /** Get the user's staked balance. */
    const val METHOD_GET_STAKED_BALANCE = "getStakedBalance"

    /** Get staking provider information (APY, liquidity). */
    const val METHOD_GET_STAKING_PROVIDER_INFO = "getStakingProviderInfo"

    /** Get supported unstake modes for a staking provider. */
    const val METHOD_GET_SUPPORTED_UNSTAKE_MODES = "getSupportedUnstakeModes"

    /** Get all registered staking provider IDs. */
    const val METHOD_GET_REGISTERED_STAKING_PROVIDERS = "getRegisteredStakingProviders"

    /** Check if a staking provider is registered. */
    const val METHOD_HAS_STAKING_PROVIDER = "hasStakingProvider"

    /**
     * Method name for telling JS to create a ProxyStakingProvider that forwards calls back
     * to a Kotlin-implemented [io.ton.walletkit.staking.ITONStakingProvider] via reverse-RPC.
     */
    const val METHOD_REGISTER_KOTLIN_STAKING_PROVIDER = "registerKotlinStakingProvider"

    // Streaming methods

    const val METHOD_CREATE_TON_CENTER_STREAMING_PROVIDER = "createTonCenterStreamingProvider"
    const val METHOD_CREATE_TON_API_STREAMING_PROVIDER = "createTonApiStreamingProvider"
    const val METHOD_REGISTER_STREAMING_PROVIDER = "registerStreamingProvider"
    const val METHOD_STREAMING_HAS_PROVIDER = "streamingHasProvider"
    const val METHOD_STREAMING_WATCH = "streamingWatch"
    const val METHOD_STREAMING_UNWATCH = "streamingUnwatch"
    const val METHOD_STREAMING_CONNECT = "streamingConnect"
    const val METHOD_STREAMING_DISCONNECT = "streamingDisconnect"
    const val METHOD_STREAMING_WATCH_CONNECTION_CHANGE = "streamingWatchConnectionChange"
    const val METHOD_STREAMING_WATCH_BALANCE = "streamingWatchBalance"
    const val METHOD_STREAMING_WATCH_TRANSACTIONS = "streamingWatchTransactions"
    const val METHOD_STREAMING_WATCH_JETTONS = "streamingWatchJettons"

    /**
     * Method name for telling JS to create a ProxyStreamingProvider that forwards calls back
     * to a Kotlin-implemented [io.ton.walletkit.streaming.ITONStreamingProvider] via reverse-RPC.
     */
    const val METHOD_REGISTER_KOTLIN_STREAMING_PROVIDER = "registerKotlinStreamingProvider"

    const val METHOD_KOTLIN_PROVIDER_DISPATCH = "kotlinProviderDispatch"
}
