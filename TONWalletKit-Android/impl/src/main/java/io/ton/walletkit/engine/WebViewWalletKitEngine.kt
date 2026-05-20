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

import android.content.Context
import io.ton.walletkit.WalletKitBridgeException
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
import io.ton.walletkit.api.generated.TONGetMethodResult
import io.ton.walletkit.api.generated.TONMasterchainInfo
import io.ton.walletkit.api.generated.TONRawStackItem
import io.ton.walletkit.api.generated.TONSendTransactionResponse
import io.ton.walletkit.api.generated.TONTransactionEmulatedPreview
import io.ton.walletkit.api.generated.TONTransactionPreviewOptions
import io.ton.walletkit.api.generated.TONTransactionRequest
import io.ton.walletkit.api.generated.TONTransferRequest
import io.ton.walletkit.api.generated.TONUnstakeMode
import io.ton.walletkit.bridge.BridgeCodec
import io.ton.walletkit.client.TONAPIClient
import io.ton.walletkit.config.TONWalletKitConfiguration
import io.ton.walletkit.engine.adapter.BridgeWalletAdapter
import io.ton.walletkit.engine.infrastructure.BridgeRpcClient
import io.ton.walletkit.engine.infrastructure.InitializationManager
import io.ton.walletkit.engine.infrastructure.MessageDispatcher
import io.ton.walletkit.engine.infrastructure.StorageManager
import io.ton.walletkit.engine.infrastructure.WebViewManager
import io.ton.walletkit.engine.model.WalletAccount
import io.ton.walletkit.engine.operations.addWallet
import io.ton.walletkit.engine.operations.approveConnect
import io.ton.walletkit.engine.operations.approveSignData
import io.ton.walletkit.engine.operations.approveTransaction
import io.ton.walletkit.engine.operations.buildStakeTransaction
import io.ton.walletkit.engine.operations.buildSwapTransaction
import io.ton.walletkit.engine.operations.connectionEventFromUrl
import io.ton.walletkit.engine.operations.createDeDustSwapProvider
import io.ton.walletkit.engine.operations.createOmnistonSwapProvider
import io.ton.walletkit.engine.operations.createSignerFromCustom
import io.ton.walletkit.engine.operations.createSignerFromMnemonic
import io.ton.walletkit.engine.operations.createSignerFromSecretKey
import io.ton.walletkit.engine.operations.createTonMnemonic
import io.ton.walletkit.engine.operations.createTonStakersStakingProvider
import io.ton.walletkit.engine.operations.createTransferJettonTransaction
import io.ton.walletkit.engine.operations.createTransferMultiTonTransaction
import io.ton.walletkit.engine.operations.createTransferNftRawTransaction
import io.ton.walletkit.engine.operations.createTransferNftTransaction
import io.ton.walletkit.engine.operations.createTransferTonTransaction
import io.ton.walletkit.engine.operations.createWalletAdapter
import io.ton.walletkit.engine.operations.disconnectSession
import io.ton.walletkit.engine.operations.getBalance
import io.ton.walletkit.engine.operations.getJettonBalance
import io.ton.walletkit.engine.operations.getJettonWalletAddress
import io.ton.walletkit.engine.operations.getJettons
import io.ton.walletkit.engine.operations.getNft
import io.ton.walletkit.engine.operations.getNfts
import io.ton.walletkit.engine.operations.getRegisteredStakingProviders
import io.ton.walletkit.engine.operations.getRegisteredSwapProviders
import io.ton.walletkit.engine.operations.getStakedBalance
import io.ton.walletkit.engine.operations.getStakingProviderInfo
import io.ton.walletkit.engine.operations.getStakingQuote
import io.ton.walletkit.engine.operations.getSupportedUnstakeModes
import io.ton.walletkit.engine.operations.getSwapQuote
import io.ton.walletkit.engine.operations.getTransactionPreview
import io.ton.walletkit.engine.operations.getWallet
import io.ton.walletkit.engine.operations.getWalletAddress
import io.ton.walletkit.engine.operations.getWallets
import io.ton.walletkit.engine.operations.handleNewTransaction
import io.ton.walletkit.engine.operations.handleTonConnectRequest
import io.ton.walletkit.engine.operations.handleTonConnectUrl
import io.ton.walletkit.engine.operations.hasStakingProvider
import io.ton.walletkit.engine.operations.hasSwapProvider
import io.ton.walletkit.engine.operations.listSessions
import io.ton.walletkit.engine.operations.mnemonicToKeyPair
import io.ton.walletkit.engine.operations.registerStakingProvider
import io.ton.walletkit.engine.operations.registerSwapProvider
import io.ton.walletkit.engine.operations.rejectConnect
import io.ton.walletkit.engine.operations.rejectSignData
import io.ton.walletkit.engine.operations.rejectTransaction
import io.ton.walletkit.engine.operations.removeWallet
import io.ton.walletkit.engine.operations.responses.AddWalletResponse
import io.ton.walletkit.engine.operations.responses.SignerInfoResponse
import io.ton.walletkit.engine.operations.sendTransaction
import io.ton.walletkit.engine.operations.setDefaultStakingProvider
import io.ton.walletkit.engine.operations.setDefaultSwapProvider
import io.ton.walletkit.engine.operations.sign
import io.ton.walletkit.engine.operations.walletClientGetMasterchainInfo
import io.ton.walletkit.engine.operations.walletClientRunGetMethod
import io.ton.walletkit.engine.operations.walletClientSendBoc
import io.ton.walletkit.engine.parsing.EventParser
import io.ton.walletkit.engine.state.AdapterManager
import io.ton.walletkit.engine.state.EventRouter
import io.ton.walletkit.engine.state.KotlinStakingProviderManager
import io.ton.walletkit.engine.state.KotlinStreamingProviderManager
import io.ton.walletkit.engine.state.KotlinSwapProviderManager
import io.ton.walletkit.engine.state.SignerManager
import io.ton.walletkit.internal.constants.BridgeMethodConstants
import io.ton.walletkit.internal.constants.LogConstants
import io.ton.walletkit.internal.constants.WebViewConstants
import io.ton.walletkit.internal.util.Logger
import io.ton.walletkit.internal.util.WalletKitUtils
import io.ton.walletkit.listener.TONBridgeEventsHandler
import io.ton.walletkit.model.KeyPair
import io.ton.walletkit.model.TONHex
import io.ton.walletkit.model.TONUserFriendlyAddress
import io.ton.walletkit.model.TONWalletAdapter
import io.ton.walletkit.model.WalletSigner
import io.ton.walletkit.model.WalletSignerInfo
import io.ton.walletkit.request.TONWalletConnectionRequest
import io.ton.walletkit.session.TONConnectSession
import io.ton.walletkit.session.TONConnectSessionManager
import io.ton.walletkit.storage.BridgeStorageAdapter
import io.ton.walletkit.storage.CustomBridgeStorageAdapter
import io.ton.walletkit.storage.MemoryBridgeStorageAdapter
import io.ton.walletkit.storage.SecureBridgeStorageAdapter
import io.ton.walletkit.storage.TONWalletKitStorageType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * WebView-backed WalletKit engine. Orchestrates the WebView, JS bridge transport,
 * RPC dispatch, and event routing for a single network instance.
 *
 * @suppress Internal implementation. Created by [io.ton.walletkit.core.TONWalletKit.initialize].
 */
internal class WebViewWalletKitEngine private constructor(
    context: Context,
    eventsHandler: TONBridgeEventsHandler?,
    private val storageAdapter: BridgeStorageAdapter,
    private val sessionManager: TONConnectSessionManager?,
    private val apiClients: List<Pair<TONNetwork, TONAPIClient>>,
    private val assetPath: String = WebViewConstants.DEFAULT_ASSET_PATH,
) : WalletKitEngine {
    override val streamingEvents get() = messageDispatcher.streamingEvents

    private val appContext = context.applicationContext

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Volatile private var persistentStorageEnabled: Boolean = true

    @Volatile private var isDestroyed: Boolean = false

    private val adapterManager = AdapterManager()
    private val signerManager = SignerManager()
    override val kotlinStreamingProviderManager: KotlinStreamingProviderManager
    private val eventRouter = EventRouter()
    private val storageManager = StorageManager(storageAdapter) { persistentStorageEnabled }
    override val kotlinSwapProviderManager = KotlinSwapProviderManager()
    override val kotlinStakingProviderManager = KotlinStakingProviderManager()

    private val webViewManager: WebViewManager
    private val rpcClient: BridgeRpcClient
    private val initManager: InitializationManager
    private val eventParser: EventParser
    private val messageDispatcher: MessageDispatcher

    init {
        webViewManager =
            WebViewManager(
                context = appContext,
                assetPath = assetPath,
                storageManager = storageManager,
                sessionManager = sessionManager,
                apiClients = apiClients,
                adapterManager = adapterManager,
                json = json,
                onMessage = ::handleBridgeMessage,
                onBridgeError = ::handleBridgeError,
            )
        rpcClient = BridgeRpcClient(
            webViewManager = webViewManager,
            codec = BridgeCodec(json),
            ensureInitialized = { ensureWalletKitInitialized() },
            json = json,
        )
        kotlinStreamingProviderManager = KotlinStreamingProviderManager(rpcClient, json)
        initManager = InitializationManager(appContext, rpcClient)
        eventParser = EventParser(json, this)
        messageDispatcher =
            MessageDispatcher(
                rpcClient = rpcClient,
                eventParser = eventParser,
                eventRouter = eventRouter,
                initManager = initManager,
                webViewManager = webViewManager,
                adapterManager = adapterManager,
                signerManager = signerManager,
                kotlinSwapProviderManager = kotlinSwapProviderManager,
                kotlinStakingProviderManager = kotlinStakingProviderManager,
                kotlinStreamingProviderManager = kotlinStreamingProviderManager,
                json = json,
                onInitialized = ::refreshDerivedState,
            )

        if (eventsHandler != null) {
            runBlocking {
                eventRouter.addHandler(eventsHandler)
            }
        }
    }

    private suspend fun ensureWalletKitInitialized(configuration: TONWalletKitConfiguration? = null) {
        initManager.ensureInitialized(configuration)
        refreshDerivedState()
    }

    private fun refreshDerivedState() {
        persistentStorageEnabled = initManager.isPersistentStorageEnabled()
    }

    private fun handleBridgeMessage(payload: JsonObject) {
        messageDispatcher.dispatchMessage(payload)
    }

    private fun handleBridgeError(exception: WalletKitBridgeException, malformedJson: String? = null) {
        messageDispatcher.dispatchError(exception, malformedJson)
    }

    private suspend fun ensureEventListenersSetUp() {
        messageDispatcher.ensureEventListenersSetUp()
    }

    private suspend fun call(method: String, params: Any? = null): JsonObject {
        if (isDestroyed) {
            throw WalletKitBridgeException("Cannot call method '$method' - SDK has been destroyed")
        }
        return rpcClient.call(method, params)
    }

    override suspend fun init(configuration: TONWalletKitConfiguration) {
        initManager.initialize(configuration)
        refreshDerivedState()
    }

    override fun getConfiguration(): TONWalletKitConfiguration? =
        initManager.getConfiguration()

    override suspend fun mnemonicToKeyPair(words: List<String>, mnemonicType: String): KeyPair =
        rpcClient.mnemonicToKeyPair(words, mnemonicType)

    override suspend fun sign(data: ByteArray, secretKey: ByteArray): ByteArray =
        rpcClient.sign(data, secretKey)

    override suspend fun createTonMnemonic(wordCount: Int): List<String> =
        rpcClient.createTonMnemonic(wordCount)

    override suspend fun addWallet(adapter: TONWalletAdapter): WalletAccount {
        // BridgeWalletAdapter wraps a JS-side adapter; route through its stable adapterId so we don't
        // re-register in AdapterManager or create a duplicate proxy in JS.
        val adapterId = if (adapter is BridgeWalletAdapter) adapter.adapterId else adapterManager.registerAdapter(adapter)
        return rpcClient.addWallet(adapterId).toWalletAccount()
    }

    override suspend fun createSignerFromMnemonic(
        mnemonic: List<String>,
        mnemonicType: String,
    ): WalletSignerInfo = rpcClient.createSignerFromMnemonic(mnemonic, mnemonicType).toWalletSignerInfo()

    override suspend fun createSignerFromSecretKey(
        secretKeyHex: String,
    ): WalletSignerInfo = rpcClient.createSignerFromSecretKey(secretKeyHex).toWalletSignerInfo()

    override suspend fun createSignerFromCustom(signer: WalletSigner): WalletSignerInfo {
        val signerId = signerManager.registerSigner(signer)
        rpcClient.createSignerFromCustom(signerId, WalletKitUtils.ensureHexPrefix(signer.publicKey().value))
        return WalletSignerInfo(signerId = signerId, publicKey = signer.publicKey())
    }

    override suspend fun createAdapter(
        signerId: String,
        publicKey: TONHex,
        version: String,
        network: TONNetwork?,
        workchain: Int,
        walletId: Long,
        domain: TONSignatureDomain?,
    ): TONWalletAdapter {
        val resolvedNetwork = network ?: TONNetwork(chainId = "-239")
        val response = rpcClient.createWalletAdapter(version, signerId, resolvedNetwork, workchain, walletId, domain)
        val adapterId = response.adapterId?.takeIf { it.isNotEmpty() }
            ?: throw WalletKitBridgeException("JS did not return adapterId")
        return BridgeWalletAdapter(
            adapterId = adapterId,
            cachedPublicKey = publicKey,
            cachedNetwork = resolvedNetwork,
            cachedAddress = TONUserFriendlyAddress(response.address ?: ""),
            rpcClient = rpcClient,
        )
    }

    override suspend fun getWallets(): List<WalletAccount> =
        rpcClient.getWallets().mapNotNull { entry ->
            entry.walletId?.takeIf { it.isNotEmpty() }?.let { entry.toWalletAccount() }
        }

    override suspend fun getWallet(walletId: String): WalletAccount? {
        val response = rpcClient.getWallet(walletId) ?: return null
        val resolvedId = response.walletId?.takeIf { it.isNotEmpty() } ?: walletId
        val address = rpcClient.getWalletAddress(resolvedId)
        if (address.isEmpty()) return null
        return response.copy(walletId = resolvedId).toWalletAccount(address)
    }

    override suspend fun removeWallet(walletId: String) = rpcClient.removeWallet(walletId)

    override suspend fun getBalance(walletId: String): String = rpcClient.getBalance(walletId)

    override suspend fun handleTonConnectUrl(url: String) = rpcClient.handleTonConnectUrl(url)

    override suspend fun connectionEventFromUrl(url: String): TONWalletConnectionRequest =
        TONWalletConnectionRequest(event = rpcClient.connectionEventFromUrl(url), handler = this)

    override suspend fun handleTonConnectRequest(
        messageId: String,
        method: String,
        paramsJson: String?,
        url: String?,
        responseCallback: (JsonObject) -> Unit,
        walletId: String?,
    ) = rpcClient.handleTonConnectRequest(messageId, method, paramsJson, url, responseCallback, walletId)

    override suspend fun createTransferTonTransaction(
        walletId: String,
        params: TONTransferRequest,
    ): TONTransactionRequest = rpcClient.createTransferTonTransaction(walletId, params)

    override suspend fun handleNewTransaction(
        walletId: String,
        transactionContent: TONTransactionRequest,
    ) = rpcClient.handleNewTransaction(walletId, transactionContent)

    override suspend fun sendTransaction(
        walletId: String,
        transactionContent: TONTransactionRequest,
    ): TONSendTransactionResponse = rpcClient.sendTransaction(walletId, transactionContent)

    override suspend fun approveConnect(
        event: TONConnectionRequestEvent,
        response: TONConnectionApprovalResponse?,
    ) = rpcClient.approveConnect(event, response)

    override suspend fun rejectConnect(
        event: TONConnectionRequestEvent,
        reason: String?,
        errorCode: Int?,
    ) = rpcClient.rejectConnect(event, reason, errorCode)

    override suspend fun approveTransaction(
        event: TONSendTransactionRequestEvent,
        response: TONSendTransactionApprovalResponse?,
    ) = rpcClient.approveTransaction(event, response)

    override suspend fun rejectTransaction(
        event: TONSendTransactionRequestEvent,
        reason: String?,
        errorCode: Int?,
    ) = rpcClient.rejectTransaction(event, reason, errorCode)

    override suspend fun approveSignData(
        event: TONSignDataRequestEvent,
        response: TONSignDataApprovalResponse?,
    ) = rpcClient.approveSignData(event, response)

    override suspend fun rejectSignData(
        event: TONSignDataRequestEvent,
        reason: String?,
        errorCode: Int?,
    ) = rpcClient.rejectSignData(event, reason, errorCode)

    override suspend fun listSessions(): List<TONConnectSession> = rpcClient.listSessions()

    override suspend fun disconnectSession(sessionId: String?) = rpcClient.disconnectSession(sessionId)

    override suspend fun getNfts(walletId: String, limit: Int, offset: Int): TONNFTsResponse =
        rpcClient.getNfts(walletId, limit, offset)

    override suspend fun getNft(nftAddress: String): TONNFT? = rpcClient.getNft(nftAddress)

    override suspend fun createTransferNftTransaction(
        walletId: String,
        params: TONNFTTransferRequest,
    ): TONTransactionRequest = rpcClient.createTransferNftTransaction(walletId, params)

    override suspend fun createTransferNftRawTransaction(
        walletId: String,
        params: TONNFTRawTransferRequest,
    ): TONTransactionRequest = rpcClient.createTransferNftRawTransaction(walletId, params)

    override suspend fun getJettons(walletId: String, limit: Int, offset: Int): TONJettonsResponse =
        rpcClient.getJettons(walletId, limit, offset)

    override suspend fun createTransferJettonTransaction(
        walletId: String,
        params: TONJettonsTransferRequest,
    ): TONTransactionRequest = rpcClient.createTransferJettonTransaction(walletId, params)

    override suspend fun createTransferMultiTonTransaction(
        walletId: String,
        messages: List<TONTransferRequest>,
    ): TONTransactionRequest = rpcClient.createTransferMultiTonTransaction(walletId, messages)

    override suspend fun getTransactionPreview(
        walletId: String,
        transactionContent: TONTransactionRequest,
        options: TONTransactionPreviewOptions?,
    ): TONTransactionEmulatedPreview = rpcClient.getTransactionPreview(walletId, transactionContent, options)

    override suspend fun walletClientSendBoc(walletId: String, boc: String): String =
        rpcClient.walletClientSendBoc(walletId, boc)

    override suspend fun walletClientRunGetMethod(
        walletId: String,
        address: String,
        method: String,
        stack: List<TONRawStackItem>?,
        seqno: Int?,
    ): TONGetMethodResult = rpcClient.walletClientRunGetMethod(walletId, address, method, stack, seqno)

    override suspend fun walletClientGetMasterchainInfo(walletId: String): TONMasterchainInfo =
        rpcClient.walletClientGetMasterchainInfo(walletId)

    override suspend fun getJettonBalance(walletId: String, jettonAddress: String): String =
        rpcClient.getJettonBalance(walletId, jettonAddress)

    override suspend fun getJettonWalletAddress(walletId: String, jettonAddress: String): String =
        rpcClient.getJettonWalletAddress(walletId, jettonAddress)

    override suspend fun createOmnistonSwapProvider(config: TONOmnistonSwapProviderConfig?): String =
        rpcClient.createOmnistonSwapProvider(config)

    override suspend fun createDeDustSwapProvider(config: TONDeDustSwapProviderConfig?): String =
        rpcClient.createDeDustSwapProvider(config)

    override suspend fun registerSwapProvider(providerId: String) = rpcClient.registerSwapProvider(providerId)

    override suspend fun setDefaultSwapProvider(providerId: String) = rpcClient.setDefaultSwapProvider(providerId)

    override suspend fun getRegisteredSwapProviders(): List<String> = rpcClient.getRegisteredSwapProviders()

    override suspend fun hasSwapProvider(providerId: String): Boolean = rpcClient.hasSwapProvider(providerId)

    override suspend fun registerKotlinSwapProvider(providerId: String) {
        callBridgeMethod(
            BridgeMethodConstants.METHOD_REGISTER_KOTLIN_SWAP_PROVIDER,
            buildJsonObject { put("providerId", providerId) },
        )
    }

    override suspend fun getSwapQuote(params: TONSwapQuoteParams<JsonElement>, providerId: String?): TONSwapQuote =
        rpcClient.getSwapQuote(params, providerId)

    override suspend fun buildSwapTransaction(params: TONSwapParams<JsonElement>): TONTransactionRequest =
        rpcClient.buildSwapTransaction(params)

    override suspend fun createTonStakersStakingProvider(chainConfig: Map<String, TONTonStakersChainConfig>?): String =
        rpcClient.createTonStakersStakingProvider(chainConfig)

    override suspend fun registerStakingProvider(providerId: String) = rpcClient.registerStakingProvider(providerId)

    override suspend fun setDefaultStakingProvider(providerId: String) =
        rpcClient.setDefaultStakingProvider(providerId)

    override suspend fun getRegisteredStakingProviders(): List<String> = rpcClient.getRegisteredStakingProviders()

    override suspend fun hasStakingProvider(providerId: String): Boolean = rpcClient.hasStakingProvider(providerId)

    override suspend fun registerKotlinStakingProvider(providerId: String, supportedUnstakeModesJson: String) {
        callBridgeMethod(
            BridgeMethodConstants.METHOD_REGISTER_KOTLIN_STAKING_PROVIDER,
            buildJsonObject {
                put("providerId", providerId)
                put("supportedUnstakeModes", json.parseToJsonElement(supportedUnstakeModesJson))
            },
        )
    }

    override suspend fun getStakingQuote(
        params: TONStakingQuoteParams<JsonElement>,
        providerId: String?,
    ): TONStakingQuote = rpcClient.getStakingQuote(params, providerId)

    override suspend fun buildStakeTransaction(
        params: TONStakeParams<JsonElement>,
        providerId: String?,
    ): TONTransactionRequest = rpcClient.buildStakeTransaction(params, providerId)

    override suspend fun getStakedBalance(
        userAddress: String,
        network: TONNetwork?,
        providerId: String?,
    ): TONStakingBalance = rpcClient.getStakedBalance(userAddress, network, providerId)

    override suspend fun getStakingProviderInfo(
        network: TONNetwork?,
        providerId: String?,
    ): TONStakingProviderInfo = rpcClient.getStakingProviderInfo(network, providerId)

    override suspend fun getSupportedUnstakeModes(providerId: String?): List<TONUnstakeMode> =
        rpcClient.getSupportedUnstakeModes(providerId)

    private fun SignerInfoResponse.toWalletSignerInfo(): WalletSignerInfo {
        val signerId = signerId?.takeIf { it.isNotEmpty() }
            ?: throw WalletKitBridgeException("JS did not return signerId")
        return WalletSignerInfo(
            signerId = signerId,
            publicKey = TONHex(WalletKitUtils.stripHexPrefix(publicKey ?: "")),
        )
    }

    private suspend fun AddWalletResponse.toWalletAccount(
        address: String? = null,
    ): WalletAccount {
        val walletId = walletId?.takeIf { it.isNotEmpty() }
            ?: throw WalletKitBridgeException("Failed to retrieve newly added wallet")
        val resolvedAddress = address ?: rpcClient.getWalletAddress(walletId)
        val rawPublicKey = wallet?.publicKey
        return WalletAccount(
            walletId = walletId,
            address = TONUserFriendlyAddress(resolvedAddress),
            publicKey = rawPublicKey?.takeIf { it.isNotEmpty() }?.let(WalletKitUtils::stripHexPrefix),
            version = wallet?.version?.takeIf { it.isNotEmpty() } ?: "unknown",
        )
    }

    override suspend fun callBridgeMethod(method: String, params: Any?): JsonObject {
        return call(method, params)
    }

    override suspend fun addEventsHandler(eventsHandler: TONBridgeEventsHandler) {
        val outcome = eventRouter.addHandler(eventsHandler, logAcquired = false)

        if (outcome.alreadyRegistered) {
            Logger.w(TAG, "Handler already registered, skipping")
            return
        }

        if (outcome.isFirstHandler) {
            ensureEventListenersSetUp()
        }
    }

    override suspend fun removeEventsHandler(eventsHandler: TONBridgeEventsHandler) {
        val outcome = eventRouter.removeHandler(eventsHandler)
        if (outcome.removed) {
            Logger.d(TAG, "Removed event handler: ${eventsHandler.javaClass.simpleName}. Total handlers: ${eventRouter.getHandlerCount()}")

            if (outcome.isEmpty && messageDispatcher.areEventListenersSetUp()) {
                try {
                    messageDispatcher.removeEventListenersIfNeeded()
                } catch (e: Exception) {
                    Logger.e(TAG, "Failed to remove event listeners from JS bridge", e)
                }
            }
        }
    }

    override suspend fun destroy() {
        if (isDestroyed) {
            Logger.d(TAG, "destroy() called but already destroyed, skipping")
            return
        }
        isDestroyed = true

        withContext(Dispatchers.Main) {
            try {
                if (initManager.isInitialized()) {
                    Logger.d(TAG, "Removing event listeners before destroy...")
                    messageDispatcher.removeEventListenersIfNeeded()
                }
            } catch (e: Exception) {
                Logger.w(TAG, "Failed to remove event listeners during destroy", e)
            }

            kotlinSwapProviderManager.clear()
            kotlinStakingProviderManager.clear()
            kotlinStreamingProviderManager.clear()
            webViewManager.destroy()
        }
    }

    companion object {
        private val instances = mutableMapOf<TONNetwork, WebViewWalletKitEngine>()
        private val instanceMutex = Mutex()

        suspend fun getOrCreate(
            context: Context,
            configuration: TONWalletKitConfiguration,
            eventsHandler: TONBridgeEventsHandler?,
            assetPath: String = WebViewConstants.DEFAULT_ASSET_PATH,
        ): WebViewWalletKitEngine {
            val network = configuration.network

            instances[network]?.let { existingInstance ->
                if (existingInstance.isDestroyed) {
                    Logger.d(TAG, "Existing WebView engine for network $network is destroyed, removing from cache")
                    instances.remove(network)
                } else {
                    Logger.d(TAG, "Reusing existing WebView engine for network: $network")
                    if (eventsHandler != null) {
                        if (!existingInstance.eventRouter.containsHandler(eventsHandler)) {
                            existingInstance.addEventsHandler(eventsHandler)
                        }
                    }
                    return existingInstance
                }
            }

            val instance =
                instanceMutex.withLock {
                    instances[network]?.let {
                        if (!it.isDestroyed) {
                            Logger.d(TAG, "Reusing existing WebView engine for network: $network (after lock)")
                            return@withLock it
                        }
                        instances.remove(network)
                    }

                    Logger.d(TAG, "Creating new WebView engine for network: $network")
                    val storageAdapter = createStorageAdapter(context, configuration.storageType)
                    WebViewWalletKitEngine(context, eventsHandler, storageAdapter, configuration.sessionManager, configuration.apiClients, assetPath).also {
                        instances[network] = it
                    }
                }

            if (eventsHandler != null) {
                if (!instance.eventRouter.containsHandler(eventsHandler)) {
                    instance.addEventsHandler(eventsHandler)
                }
            }

            return instance
        }

        @JvmStatic
        internal suspend fun clearInstances(network: TONNetwork? = null) {
            instanceMutex.withLock {
                if (network != null) {
                    instances[network]?.destroy()
                    instances.remove(network)
                    Logger.d(TAG, "Cleared WebView engine for network: $network")
                } else {
                    instances.values.forEach { it.destroy() }
                    instances.clear()
                    Logger.d(TAG, "Cleared all WebView engine instances")
                }
            }
        }

        private fun createStorageAdapter(
            context: Context,
            storageType: TONWalletKitStorageType,
        ): BridgeStorageAdapter {
            return when (storageType) {
                is TONWalletKitStorageType.Memory -> {
                    Logger.d(TAG, "Using memory storage")
                    MemoryBridgeStorageAdapter()
                }
                is TONWalletKitStorageType.Encrypted -> {
                    Logger.d(TAG, "Using encrypted storage")
                    SecureBridgeStorageAdapter(context.applicationContext)
                }
                is TONWalletKitStorageType.Custom -> {
                    Logger.d(TAG, "Using custom storage")
                    CustomBridgeStorageAdapter(storageType.storage)
                }
            }
        }

        private const val TAG = LogConstants.TAG_WEBVIEW_ENGINE
    }
}
