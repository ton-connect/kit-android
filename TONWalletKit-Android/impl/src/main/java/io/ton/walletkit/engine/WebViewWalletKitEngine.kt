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
import android.view.ViewGroup
import io.ton.walletkit.WalletKitBridgeException
import io.ton.walletkit.api.generated.TONConnectionApprovalResponse
import io.ton.walletkit.api.generated.TONConnectionRequestEvent
import io.ton.walletkit.api.generated.TONJettonsResponse
import io.ton.walletkit.api.generated.TONJettonsTransferRequest
import io.ton.walletkit.api.generated.TONNFT
import io.ton.walletkit.api.generated.TONNFTRawTransferRequest
import io.ton.walletkit.api.generated.TONNFTTransferRequest
import io.ton.walletkit.api.generated.TONNFTsResponse
import io.ton.walletkit.api.generated.TONNetwork
import io.ton.walletkit.api.generated.TONSendTransactionApprovalResponse
import io.ton.walletkit.api.generated.TONSendTransactionRequestEvent
import io.ton.walletkit.api.generated.TONSignDataApprovalResponse
import io.ton.walletkit.api.generated.TONSignDataRequestEvent
import io.ton.walletkit.api.generated.TONTransactionEmulatedPreview
import io.ton.walletkit.api.generated.TONTransferRequest
import io.ton.walletkit.client.TONAPIClient
import io.ton.walletkit.config.TONWalletKitConfiguration
import io.ton.walletkit.core.WalletKitEngineKind
import io.ton.walletkit.engine.infrastructure.BridgeRpcClient
import io.ton.walletkit.engine.infrastructure.InitializationManager
import io.ton.walletkit.engine.infrastructure.MessageDispatcher
import io.ton.walletkit.engine.infrastructure.StorageManager
import io.ton.walletkit.engine.infrastructure.WebViewManager
import io.ton.walletkit.engine.model.WalletAccount
import io.ton.walletkit.engine.operations.AssetOperations
import io.ton.walletkit.engine.operations.CryptoOperations
import io.ton.walletkit.engine.operations.TonConnectOperations
import io.ton.walletkit.engine.operations.TransactionOperations
import io.ton.walletkit.engine.operations.WalletOperations
import io.ton.walletkit.engine.parsing.EventParser
import io.ton.walletkit.engine.state.EventRouter
import io.ton.walletkit.engine.state.SignerManager
import io.ton.walletkit.internal.constants.LogConstants
import io.ton.walletkit.internal.constants.NetworkConstants
import io.ton.walletkit.internal.constants.WebViewConstants
import io.ton.walletkit.internal.util.Logger
import io.ton.walletkit.listener.TONBridgeEventsHandler
import io.ton.walletkit.model.KeyPair
import io.ton.walletkit.model.WalletAdapterInfo
import io.ton.walletkit.model.WalletSigner
import io.ton.walletkit.model.WalletSignerInfo
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
import org.json.JSONObject

/**
 * Refactored WebView-backed WalletKit engine that orchestrates specialised components
 * for WebView lifecycle, RPC communication, event handling, and parsing.
 *
 * The public behaviour and logging semantics remain identical to the legacy monolithic
 * implementation to guarantee backward compatibility.
 *
 * @suppress Internal implementation class. Use [WalletKitEngineFactory.create] instead.
 */
internal class WebViewWalletKitEngine private constructor(
    context: Context,
    eventsHandler: TONBridgeEventsHandler?,
    private val storageAdapter: BridgeStorageAdapter,
    private val sessionManager: TONConnectSessionManager?,
    private val apiClients: List<TONAPIClient>,
    private val assetPath: String = WebViewConstants.DEFAULT_ASSET_PATH,
) : WalletKitEngine {
    override val kind: WalletKitEngineKind = WalletKitEngineKind.WEBVIEW

    private val appContext = context.applicationContext

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Volatile private var persistentStorageEnabled: Boolean = true

    @Volatile private var isDestroyed: Boolean = false

    @Volatile private var currentNetwork: String = NetworkConstants.DEFAULT_NETWORK

    @Volatile private var apiBaseUrl: String = NetworkConstants.DEFAULT_TESTNET_API_URL

    @Volatile private var tonApiKey: String? = null

    private val signerManager = SignerManager()
    private val eventRouter = EventRouter()
    private val storageManager = StorageManager(storageAdapter) { persistentStorageEnabled }

    private val webViewManager: WebViewManager
    private val rpcClient: BridgeRpcClient
    private val initManager: InitializationManager
    private val eventParser: EventParser
    private val messageDispatcher: MessageDispatcher
    private val walletOperations: WalletOperations
    private val cryptoOperations: CryptoOperations
    private val transactionOperations: TransactionOperations
    private val tonConnectOperations: TonConnectOperations
    private val assetOperations: AssetOperations

    init {
        webViewManager =
            WebViewManager(
                context = appContext,
                assetPath = assetPath,
                storageManager = storageManager,
                signerManager = signerManager,
                sessionManager = sessionManager,
                apiClients = apiClients,
                json = json,
                onMessage = ::handleBridgeMessage,
                onBridgeError = ::handleBridgeError,
            )
        rpcClient = BridgeRpcClient(webViewManager)
        initManager = InitializationManager(appContext, rpcClient)
        eventParser = EventParser(json, this)
        messageDispatcher =
            MessageDispatcher(
                rpcClient = rpcClient,
                eventParser = eventParser,
                eventRouter = eventRouter,
                initManager = initManager,
                webViewManager = webViewManager,
                onInitialized = ::refreshDerivedState,
                onNetworkChanged = ::handleNetworkChanged,
                onApiBaseUrlChanged = ::handleApiBaseUrlChanged,
            )

        val ensureInitialized: suspend () -> Unit = { ensureWalletKitInitialized() }

        walletOperations =
            WalletOperations(
                ensureInitialized = ensureInitialized,
                rpcClient = rpcClient,
                signerManager = signerManager,
                currentNetworkProvider = { currentNetwork },
                json = json,
            )
        cryptoOperations =
            CryptoOperations(
                ensureInitialized = ensureInitialized,
                rpcClient = rpcClient,
                json = json,
            )
        transactionOperations =
            TransactionOperations(
                ensureInitialized = ensureInitialized,
                rpcClient = rpcClient,
                json = json,
            )
        tonConnectOperations =
            TonConnectOperations(
                ensureInitialized = ensureInitialized,
                rpcClient = rpcClient,
                json = json,
            )
        assetOperations =
            AssetOperations(
                ensureInitialized = ensureInitialized,
                rpcClient = rpcClient,
                json = json,
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
        currentNetwork = initManager.currentNetwork()
        apiBaseUrl = initManager.apiBaseUrl()
        tonApiKey = initManager.tonApiKey()
    }

    private fun handleNetworkChanged(network: String?) {
        if (!network.isNullOrBlank()) {
            currentNetwork = network
        }
    }

    private fun handleApiBaseUrlChanged(url: String?) {
        if (!url.isNullOrBlank()) {
            apiBaseUrl = url
        }
    }

    private fun handleBridgeMessage(payload: JSONObject) {
        messageDispatcher.dispatchMessage(payload)
    }

    private fun handleBridgeError(exception: WalletKitBridgeException, malformedJson: String? = null) {
        messageDispatcher.dispatchError(exception, malformedJson)
    }

    private suspend fun ensureEventListenersSetUp() {
        messageDispatcher.ensureEventListenersSetUp()
    }

    private suspend fun call(method: String, params: JSONObject? = null): JSONObject {
        if (isDestroyed) {
            throw WalletKitBridgeException("Cannot call method '$method' - SDK has been destroyed")
        }
        return rpcClient.call(method, params)
    }

    /**
     * Attach the underlying WebView to a parent view so it can be inspected/debugged if needed.
     * @suppress Internal debugging method. Not part of public API.
     */
    internal fun attachTo(parent: ViewGroup) {
        webViewManager.attachTo(parent)
    }

    /**
     * Get the underlying WebView instance.
     * @suppress Internal debugging method. Not part of public API.
     */
    internal fun asView() = webViewManager.asView()

    override suspend fun init(configuration: TONWalletKitConfiguration) {
        initManager.initialize(configuration)
        refreshDerivedState()
    }

    override fun getConfiguration(): TONWalletKitConfiguration? =
        initManager.getConfiguration()

    override suspend fun mnemonicToKeyPair(words: List<String>, mnemonicType: String): KeyPair =
        cryptoOperations.mnemonicToKeyPair(words, mnemonicType)

    override suspend fun sign(data: ByteArray, secretKey: ByteArray): ByteArray =
        cryptoOperations.sign(data, secretKey)

    override suspend fun createTonMnemonic(wordCount: Int): List<String> =
        cryptoOperations.createTonMnemonic(wordCount)

    override suspend fun createSignerFromMnemonic(
        mnemonic: List<String>,
        mnemonicType: String,
    ): WalletSignerInfo = walletOperations.createSignerFromMnemonic(mnemonic, mnemonicType)

    override suspend fun createSignerFromSecretKey(secretKey: ByteArray): WalletSignerInfo =
        walletOperations.createSignerFromSecretKey(secretKey)

    override suspend fun createSignerFromCustom(signer: WalletSigner): WalletSignerInfo =
        walletOperations.createSignerFromCustom(signer)

    override fun isCustomSigner(signerId: String): Boolean =
        signerManager.hasCustomSigner(signerId)

    override suspend fun createV5R1Adapter(
        signerId: String,
        network: TONNetwork?,
        workchain: Int,
        walletId: Long,
        publicKey: String?,
        isCustom: Boolean,
    ): WalletAdapterInfo = walletOperations.createV5R1Adapter(signerId, network, workchain, walletId, publicKey, isCustom)

    override suspend fun createV4R2Adapter(
        signerId: String,
        network: TONNetwork?,
        workchain: Int,
        walletId: Long,
        publicKey: String?,
        isCustom: Boolean,
    ): WalletAdapterInfo = walletOperations.createV4R2Adapter(signerId, network, workchain, walletId, publicKey, isCustom)

    override suspend fun addWallet(adapterId: String): WalletAccount =
        walletOperations.addWallet(adapterId)

    override suspend fun getWallets(): List<WalletAccount> = walletOperations.getWallets()

    override suspend fun getWallet(walletId: String): WalletAccount? = walletOperations.getWallet(walletId)

    override suspend fun removeWallet(walletId: String) = walletOperations.removeWallet(walletId)

    override suspend fun getBalance(walletId: String): String = walletOperations.getBalance(walletId)

    override suspend fun handleTonConnectUrl(url: String) = tonConnectOperations.handleTonConnectUrl(url)

    override suspend fun handleTonConnectRequest(
        messageId: String,
        method: String,
        paramsJson: String?,
        url: String?,
        responseCallback: (JSONObject) -> Unit,
    ) = tonConnectOperations.handleTonConnectRequest(messageId, method, paramsJson, url, responseCallback)

    override suspend fun createTransferTonTransaction(
        walletId: String,
        params: TONTransferRequest,
    ): String = transactionOperations.createTransferTonTransaction(walletId, params)

    override suspend fun handleNewTransaction(
        walletId: String,
        transactionContent: String,
    ) = transactionOperations.handleNewTransaction(walletId, transactionContent)

    override suspend fun sendTransaction(
        walletId: String,
        transactionContent: String,
    ): String = transactionOperations.sendTransaction(walletId, transactionContent)

    override suspend fun approveConnect(
        event: TONConnectionRequestEvent,
        response: TONConnectionApprovalResponse?,
    ) {
        tonConnectOperations.approveConnect(event, response)
    }

    override suspend fun rejectConnect(
        event: TONConnectionRequestEvent,
        reason: String?,
        errorCode: Int?,
    ) = tonConnectOperations.rejectConnect(event, reason, errorCode)

    override suspend fun approveTransaction(
        event: TONSendTransactionRequestEvent,
        response: TONSendTransactionApprovalResponse?,
    ) = tonConnectOperations.approveTransaction(event, response)

    override suspend fun rejectTransaction(
        event: TONSendTransactionRequestEvent,
        reason: String?,
        errorCode: Int?,
    ) = tonConnectOperations.rejectTransaction(event, reason, errorCode)

    override suspend fun approveSignData(
        event: TONSignDataRequestEvent,
        response: TONSignDataApprovalResponse?,
    ) = tonConnectOperations.approveSignData(event, response)

    override suspend fun rejectSignData(
        event: TONSignDataRequestEvent,
        reason: String?,
        errorCode: Int?,
    ) = tonConnectOperations.rejectSignData(event, reason, errorCode)

    override suspend fun listSessions(): List<TONConnectSession> = tonConnectOperations.listSessions()

    override suspend fun disconnectSession(sessionId: String?) =
        tonConnectOperations.disconnectSession(sessionId)

    override suspend fun getNfts(
        walletId: String,
        limit: Int,
        offset: Int,
    ): TONNFTsResponse = assetOperations.getNfts(walletId, limit, offset)

    override suspend fun getNft(
        nftAddress: String,
    ): TONNFT? = assetOperations.getNft(nftAddress)

    override suspend fun createTransferNftTransaction(
        walletId: String,
        params: TONNFTTransferRequest,
    ): String = assetOperations.createTransferNftTransaction(walletId, params)

    override suspend fun createTransferNftRawTransaction(
        walletId: String,
        params: TONNFTRawTransferRequest,
    ): String = assetOperations.createTransferNftRawTransaction(walletId, params)

    override suspend fun getJettons(
        walletId: String,
        limit: Int,
        offset: Int,
    ): TONJettonsResponse = assetOperations.getJettons(walletId, limit, offset)

    override suspend fun createTransferJettonTransaction(
        walletId: String,
        params: TONJettonsTransferRequest,
    ): String = assetOperations.createTransferJettonTransaction(walletId, params)

    override suspend fun createTransferMultiTonTransaction(
        walletId: String,
        messages: List<TONTransferRequest>,
    ): String = transactionOperations.createTransferMultiTonTransaction(walletId, messages)

    override suspend fun getTransactionPreview(
        walletId: String,
        transactionContent: String,
    ): TONTransactionEmulatedPreview =
        transactionOperations.getTransactionPreview(walletId, transactionContent)

    override suspend fun getJettonBalance(walletId: String, jettonAddress: String): String =
        assetOperations.getJettonBalance(walletId, jettonAddress)

    override suspend fun getJettonWalletAddress(walletId: String, jettonAddress: String): String =
        assetOperations.getJettonWalletAddress(walletId, jettonAddress)

    override suspend fun callBridgeMethod(method: String, params: JSONObject?): JSONObject {
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

            webViewManager.destroy()
        }
    }

    private fun failBridgeFutures(exception: WalletKitBridgeException) {
        if (!webViewManager.bridgeLoaded.isCompleted) {
            webViewManager.bridgeLoaded.completeExceptionally(exception)
        }
        if (!webViewManager.jsBridgeReady.isCompleted) {
            webViewManager.jsBridgeReady.completeExceptionally(exception)
        }
        rpcClient.failAll(exception)
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
                Logger.d(TAG, "Reusing existing WebView engine for network: $network")
                if (eventsHandler != null) {
                    if (!existingInstance.eventRouter.containsHandler(eventsHandler)) {
                        existingInstance.addEventsHandler(eventsHandler)
                    }
                }
                return existingInstance
            }

            val instance =
                instanceMutex.withLock {
                    instances[network]?.let {
                        Logger.d(TAG, "Reusing existing WebView engine for network: $network (after lock)")
                        return@withLock it
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

        /**
         * Create engine for testing with custom asset path.
         *
         * This allows tests to load mock JavaScript files instead of the production bridge.
         * Only use this in test code!
         *
         * @param context Android context
         * @param assetPath Path to the HTML file to load (e.g., "mock-bridge/normal-flow.html")
         * @param eventsHandler Optional events handler to track SDK events
         * @param storageType Storage type to use (defaults to Memory for tests)
         * @return New WebViewWalletKitEngine instance configured for testing
         */
        @JvmStatic
        internal fun createForTesting(
            context: Context,
            assetPath: String,
            eventsHandler: TONBridgeEventsHandler? = null,
            storageType: TONWalletKitStorageType = TONWalletKitStorageType.Memory,
            sessionManager: TONConnectSessionManager? = null,
            apiClients: List<TONAPIClient> = emptyList(),
        ): WebViewWalletKitEngine {
            Logger.d(TAG, "Creating test WebView engine with asset path: $assetPath")
            val storageAdapter = createStorageAdapter(context, storageType)
            return WebViewWalletKitEngine(context, eventsHandler, storageAdapter, sessionManager, apiClients, assetPath)
        }

        /**
         * Create storage adapter based on storage type.
         */
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
