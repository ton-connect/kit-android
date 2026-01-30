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
package io.ton.walletkit.demo.core

import android.app.Application
import android.util.Log
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import coil3.util.DebugLogger
import dagger.hilt.android.HiltAndroidApp
import io.ton.walletkit.ITONWalletKit
import io.ton.walletkit.api.ChainIds
import io.ton.walletkit.api.MAINNET
import io.ton.walletkit.api.TESTNET
import io.ton.walletkit.api.WalletVersions
import io.ton.walletkit.api.generated.TONNetwork
import io.ton.walletkit.config.SignDataType
import io.ton.walletkit.config.TONWalletKitConfiguration
import io.ton.walletkit.demo.data.storage.DemoAppStorage
import io.ton.walletkit.demo.data.storage.SecureDemoAppStorage
import io.ton.walletkit.event.TONWalletKitEvent
import io.ton.walletkit.listener.TONBridgeEventsHandler
import io.ton.walletkit.storage.TONWalletKitStorageType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import okio.Path.Companion.toOkioPath

@HiltAndroidApp
class WalletKitDemoApp :
    Application(),
    SingletonImageLoader.Factory {

    // Application-level coroutine scope
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /**
     * Create Coil ImageLoader with optimized settings for LazyGrid
     */
    override fun newImageLoader(context: PlatformContext): ImageLoader = ImageLoader.Builder(context)
        .crossfade(true)
        .memoryCache {
            MemoryCache.Builder()
                .maxSizePercent(context, 0.25) // Use 25% of app memory for image cache
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(context.cacheDir.resolve("image_cache").toOkioPath())
                .maxSizeBytes(512L * 1024 * 1024) // 512 MB
                .build()
        }
        // Add components with retry capability
        .components {
            add(OkHttpNetworkFetcherFactory())
        }
        .logger(DebugLogger()) // Enable logging for debugging
        .build()

    /**
     * Demo app storage for wallet metadata and user preferences.
     * Note: Wallet data (mnemonics) are managed by SDK's internal persistent storage.
     */
    val storage: DemoAppStorage by lazy {
        SecureDemoAppStorage(this)
    }

    /**
     * Shared flow for SDK events to be consumed by ViewModel.
     */
    private val _sdkEvents = MutableSharedFlow<TONWalletKitEvent>(extraBufferCapacity = 10)
    val sdkEvents = _sdkEvents.asSharedFlow()

    /**
     * State flow for SDK initialization status.
     */
    private val _sdkInitialized = MutableSharedFlow<Boolean>(replay = 1)
    val sdkInitialized = _sdkInitialized.asSharedFlow()

    override fun onCreate() {
        super.onCreate()

        Log.w(TAG, "🔴🔴🔴 WalletKitDemoApp.onCreate() CALLED!")
        Log.w(TAG, "🔴 Process ID: ${android.os.Process.myPid()}")
        Log.w(TAG, "🔴 Application instance: ${System.identityHashCode(this)}")

        // Initialize ITONWalletKit SDK and add event handler
        applicationScope.launch {
            try {
                Log.w(TAG, "🔴 Launching SDK initialization coroutine...")
                val kit = TONWalletKitHelper.mainnet(this@WalletKitDemoApp)
                Log.w(TAG, "🔴 Got kit instance: ${System.identityHashCode(kit)}")

                // CRITICAL: Load and add wallets BEFORE setting up event listeners
                // This ensures that when events are replayed (which happens when the first
                // event handler is added), the wallets are already available in the SDK
                // for event approval/rejection operations.
                loadAndAddStoredWallets(kit)

                Log.w(TAG, "🔴🔴🔴 About to add event handler...")
                // Add event handler (this triggers setEventsListeners() and event replay)
                kit.addEventsHandler(object : TONBridgeEventsHandler {
                    override fun handle(event: TONWalletKitEvent) {
                        Log.d(TAG, "🟢 Event handler callback invoked for event: ${event.javaClass.simpleName}")
                        _sdkEvents.tryEmit(event)
                    }
                })
                Log.w(TAG, "✅ Event handler added successfully")

                _sdkInitialized.emit(true)
                Log.w(TAG, "✅ SDK initialization complete, sdkInitialized emitted")
            } catch (e: Exception) {
                Log.e(TAG, "❌❌❌ Failed to initialize SDK", e)
            }
        }
    }

    /**
     * Load wallets from encrypted storage and add them to the SDK.
     * This must be done BEFORE adding event handlers to ensure wallets are available
     * when replayed events are processed.
     */
    private suspend fun loadAndAddStoredWallets(kit: ITONWalletKit) {
        try {
            // Get all stored wallet records
            val storage = getSharedPreferences("wallet_storage", MODE_PRIVATE)
            val walletDataJson = storage.getString("wallets", "[]") ?: "[]"

            // Skip if no wallets stored (empty list is fine)
            if (walletDataJson == "[]") {
                Log.d(TAG, "No stored wallets to load")
                return
            }

            val walletDataList = kotlinx.serialization.json.Json.decodeFromString<List<WalletDataRecord>>(walletDataJson)

            Log.d(TAG, "Loading ${walletDataList.size} stored wallets into SDK")

            // Add each wallet to the SDK
            for (walletRecord in walletDataList) {
                try {
                    // Convert mnemonic string to list of words
                    val mnemonicWords = walletRecord.mnemonic.split(" ").filter { it.isNotBlank() }

                    // Convert network string to TONNetwork enum
                    val network = when (walletRecord.network.lowercase()) {
                        ChainIds.MAINNET -> TONNetwork.MAINNET
                        ChainIds.TESTNET -> TONNetwork.TESTNET
                        else -> TONNetwork.MAINNET
                    }

                    // Use 3-step wallet creation pattern
                    val signer = kit.createSignerFromMnemonic(mnemonicWords)
                    val adapter = when (walletRecord.version) {
                        WalletVersions.V4R2 -> kit.createV4R2Adapter(signer, network)
                        WalletVersions.V5R1 -> kit.createV5R1Adapter(signer, network)
                        else -> {
                            Log.w(TAG, "Unsupported wallet version: ${walletRecord.version}, skipping")
                            continue
                        }
                    }
                    kit.addWallet(adapter.adapterId)
                    Log.d(TAG, "Added wallet to SDK: ${walletRecord.address}")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to add wallet ${walletRecord.address} to SDK", e)
                }
            }

            Log.d(TAG, "Finished loading wallets into SDK")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load stored wallets", e)
        }
    }

    @kotlinx.serialization.Serializable
    private data class WalletDataRecord(
        val mnemonic: String,
        val address: String,
        val network: String,
        val version: String,
    )

    private companion object {
        private const val TAG = "WalletKitDemoApp"
    }
}

/**
 * Helper to get cached ITONWalletKit instance.
 */
object TONWalletKitHelper {
    private var mainnetInstance: ITONWalletKit? = null
    private val mutex = kotlinx.coroutines.sync.Mutex()

    /**
     * Flag to disable network send for testing.
     * When true, transactions will be simulated but not actually sent to the network.
     * Set this BEFORE initializing the SDK.
     */
    @Volatile
    var disableNetworkSend: Boolean = false

    /**
     * Flag to use custom session manager for testing.
     * When true, uses TestSessionManager instead of SDK's built-in session storage.
     * Set this BEFORE initializing the SDK.
     */
    @Volatile
    var useCustomSessionManager: Boolean = true // Enable by default for testing

    /**
     * Flag to use custom API client for testing.
     * When true, uses TestAPIClient instead of SDK's built-in API client.
     * Set this BEFORE initializing the SDK.
     */
    @Volatile
    var useCustomApiClient: Boolean = true // Enable by default for testing

    /**
     * The custom session manager instance (if enabled).
     * Exposed so tests can inspect session state.
     */
    var sessionManager: TestSessionManager? = null
        private set

    /**
     * Check if we're running under instrumentation tests and if disableNetworkSend is requested.
     * Uses reflection to avoid compile-time dependency on test libraries.
     */
    private fun checkInstrumentationDisableNetworkSend(): Boolean = try {
        // Use reflection to access InstrumentationRegistry without compile-time dependency
        val registryClass = Class.forName("androidx.test.platform.app.InstrumentationRegistry")
        val getArgumentsMethod = registryClass.getMethod("getArguments")
        val arguments = getArgumentsMethod.invoke(null) as? android.os.Bundle
        val value = arguments?.getString("disableNetworkSend")
        val result = value?.equals("true", ignoreCase = true) == true
        if (result) {
            Log.w("TONWalletKitHelper", "🧪 Detected disableNetworkSend=true from instrumentation arguments")
        }
        result
    } catch (e: Exception) {
        // Not running under instrumentation or class not found, ignore
        false
    }

    suspend fun mainnet(application: Application): ITONWalletKit {
        // Fast path: already initialized
        mainnetInstance?.let { return it }

        // Slow path: need to initialize (with mutex to prevent double-init)
        return mutex.withLock {
            // Double-check after acquiring lock
            mainnetInstance?.let {
                Log.d("TONWalletKitHelper", "Returning cached mainnet instance (after lock): ${System.identityHashCode(it)}")
                return@withLock it
            }

            Log.w("TONWalletKitHelper", "🔶🔶🔶 Creating NEW ITONWalletKit instance...")

            // Check both the flag and instrumentation arguments
            val shouldDisableNetwork = disableNetworkSend || checkInstrumentationDisableNetworkSend()

            val devOptions = if (shouldDisableNetwork) {
                Log.w("TONWalletKitHelper", "⚠️ Network send is DISABLED - transactions will be simulated only")
                TONWalletKitConfiguration.DevOptions(disableNetworkSend = true)
            } else {
                null
            }

            // Create custom session manager if enabled
            val customSessionManager = if (useCustomSessionManager) {
                Log.w("TONWalletKitHelper", "🔧 Using CUSTOM session manager (TestSessionManager)")
                TestSessionManager().also { sessionManager = it }
            } else {
                Log.w("TONWalletKitHelper", "📦 Using SDK's built-in session storage")
                null
            }

            // Create network configurations for both mainnet and testnet
            // This demonstrates the iOS-like pattern where each network config has either:
            // - apiClientConfiguration: Use SDK's built-in API client with your API key
            // - apiClient: Use your own custom API client implementation
            val networkConfigurations = if (useCustomApiClient) {
                Log.w("TONWalletKitHelper", "🌐 Using CUSTOM API clients (TestAPIClient)")
                // Use custom API client - wallet app provides their own API infrastructure
                setOf(
                    TONWalletKitConfiguration.NetworkConfiguration(
                        network = TONNetwork.MAINNET,
                        apiClient = TestAPIClient.mainnet(),
                    ),
                    TONWalletKitConfiguration.NetworkConfiguration(
                        network = TONNetwork.TESTNET,
                        apiClient = TestAPIClient.testnet(),
                    ),
                )
            } else {
                Log.w("TONWalletKitHelper", "📡 Using SDK's built-in API client")
                // Use SDK's built-in API client with default configuration
                setOf(
                    TONWalletKitConfiguration.NetworkConfiguration(
                        network = TONNetwork.MAINNET,
                        apiClientConfiguration = TONWalletKitConfiguration.APIClientConfiguration(
                            key = "", // Empty key uses default behavior
                        ),
                    ),
                    TONWalletKitConfiguration.NetworkConfiguration(
                        network = TONNetwork.TESTNET,
                        apiClientConfiguration = TONWalletKitConfiguration.APIClientConfiguration(
                            key = "", // Empty key uses default behavior
                        ),
                    ),
                )
            }

            val config = TONWalletKitConfiguration(
                networkConfigurations = networkConfigurations,
                walletManifest = TONWalletKitConfiguration.Manifest(
                    name = DEFAULT_MANIFEST_NAME,
                    appName = DEFAULT_APP_NAME,
                    imageUrl = DEFAULT_MANIFEST_IMAGE_URL,
                    aboutUrl = DEFAULT_MANIFEST_ABOUT_URL,
                    universalLink = DEFAULT_MANIFEST_UNIVERSAL_LINK,
                    bridgeUrl = DEFAULT_BRIDGE_URL,
                ),
                bridge = TONWalletKitConfiguration.Bridge(
                    bridgeUrl = DEFAULT_BRIDGE_URL,
                ),
                features = listOf(
                    // V5R1 wallets support up to 255 messages per transaction
                    TONWalletKitConfiguration.SendTransactionFeature(maxMessages = 255),
                    TONWalletKitConfiguration.SignDataFeature(
                        types = listOf(SignDataType.TEXT, SignDataType.BINARY, SignDataType.CELL),
                    ),
                ),
                storageType = TONWalletKitStorageType.Encrypted,
                sessionManager = customSessionManager,
                dev = devOptions,
            )

            val kit = ITONWalletKit.initialize(application, config)
            mainnetInstance = kit
            Log.w("TONWalletKitHelper", "✅ Created and cached mainnet instance: ${System.identityHashCode(kit)}")
            kit
        }
    }

    /**
     * Clear the cached instance (for testing or logout scenarios).
     */
    suspend fun clearMainnet() {
        mutex.withLock {
            mainnetInstance?.destroy()
            mainnetInstance = null
        }
    }

    private const val DEFAULT_MANIFEST_NAME = "Wallet"
    private const val DEFAULT_APP_NAME = "Wallet"
    private const val DEFAULT_MANIFEST_IMAGE_URL = "https://wallet.ton.org/icon.png"
    private const val DEFAULT_MANIFEST_ABOUT_URL = "https://wallet.ton.org"
    private const val DEFAULT_MANIFEST_UNIVERSAL_LINK = "https://wallet.ton.org/tc"
    private const val DEFAULT_BRIDGE_URL = "https://bridge.tonapi.io/bridge"

    /**
     * Example: Multi-network config with different API clients per network.
     */
    @Suppress("unused")
    fun createMultiNetworkConfiguration(): TONWalletKitConfiguration = TONWalletKitConfiguration(
        networkConfigurations = setOf(
            // Network 1: Mainnet with Toncenter API
            TONWalletKitConfiguration.NetworkConfiguration(
                network = TONNetwork.MAINNET,
                apiClient = ToncenterAPIClient.mainnet(),
            ),
            // Network 2: Testnet with TonAPI
            TONWalletKitConfiguration.NetworkConfiguration(
                network = TONNetwork.TESTNET,
                apiClient = TonAPIClient.testnet(),
            ),
        ),
        walletManifest = TONWalletKitConfiguration.Manifest(
            name = DEFAULT_MANIFEST_NAME,
            appName = DEFAULT_APP_NAME,
            imageUrl = DEFAULT_MANIFEST_IMAGE_URL,
            aboutUrl = DEFAULT_MANIFEST_ABOUT_URL,
            universalLink = DEFAULT_MANIFEST_UNIVERSAL_LINK,
            bridgeUrl = DEFAULT_BRIDGE_URL,
        ),
        bridge = TONWalletKitConfiguration.Bridge(
            bridgeUrl = DEFAULT_BRIDGE_URL,
        ),
        features = listOf(
            TONWalletKitConfiguration.SendTransactionFeature(maxMessages = 255),
            TONWalletKitConfiguration.SignDataFeature(
                types = listOf(SignDataType.TEXT, SignDataType.BINARY, SignDataType.CELL),
            ),
        ),
    )

    /**
     * Example: Mixed config - built-in client for mainnet, custom for testnet.
     */
    @Suppress("unused")
    fun createMixedApiClientConfiguration(): TONWalletKitConfiguration = TONWalletKitConfiguration(
        networkConfigurations = setOf(
            // Mainnet: Use SDK's built-in API client with API key
            TONWalletKitConfiguration.NetworkConfiguration(
                network = TONNetwork.MAINNET,
                apiClientConfiguration = TONWalletKitConfiguration.APIClientConfiguration(
                    key = "your-toncenter-api-key",
                    url = "https://toncenter.com/api/v3", // Optional custom URL
                ),
            ),
            // Testnet: Use custom TonAPIClient
            TONWalletKitConfiguration.NetworkConfiguration(
                network = TONNetwork.TESTNET,
                apiClient = TonAPIClient.testnet(apiKey = "your-tonapi-key"),
            ),
        ),
        walletManifest = TONWalletKitConfiguration.Manifest(
            name = DEFAULT_MANIFEST_NAME,
            appName = DEFAULT_APP_NAME,
            imageUrl = DEFAULT_MANIFEST_IMAGE_URL,
            aboutUrl = DEFAULT_MANIFEST_ABOUT_URL,
            universalLink = DEFAULT_MANIFEST_UNIVERSAL_LINK,
            bridgeUrl = DEFAULT_BRIDGE_URL,
        ),
        bridge = TONWalletKitConfiguration.Bridge(
            bridgeUrl = DEFAULT_BRIDGE_URL,
        ),
        features = listOf(
            TONWalletKitConfiguration.SendTransactionFeature(maxMessages = 255),
        ),
    )
}
