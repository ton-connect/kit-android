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
import android.os.Bundle
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
import io.ton.walletkit.api.TETRA
import io.ton.walletkit.api.WalletVersions
import io.ton.walletkit.api.generated.TONNetwork
import io.ton.walletkit.api.generated.TONSignatureDomain
import io.ton.walletkit.api.generated.TONTonCenterStreamingProviderConfig
import io.ton.walletkit.api.isTetra
import io.ton.walletkit.config.SignDataType
import io.ton.walletkit.config.TONWalletKitConfiguration
import io.ton.walletkit.demo.BuildConfig
import io.ton.walletkit.demo.data.storage.DemoAppStorage
import io.ton.walletkit.demo.data.storage.SecureDemoAppStorage
import io.ton.walletkit.demo.domain.model.WalletInterfaceType
import io.ton.walletkit.event.TONWalletKitEvent
import io.ton.walletkit.listener.TONBridgeEventsHandler
import io.ton.walletkit.storage.TONWalletKitStorageType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okio.Path.Companion.toOkioPath

@HiltAndroidApp
class WalletKitDemoApp :
    Application(),
    SingletonImageLoader.Factory {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun newImageLoader(context: PlatformContext): ImageLoader = ImageLoader.Builder(context)
        .crossfade(true)
        .memoryCache {
            MemoryCache.Builder()
                .maxSizePercent(context, 0.25)
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(context.cacheDir.resolve("image_cache").toOkioPath())
                .maxSizeBytes(512L * 1024 * 1024)
                .build()
        }
        .components {
            add(OkHttpNetworkFetcherFactory())
        }
        .logger(DebugLogger())
        .build()

    /**
     * Demo app storage for wallets, metadata, and user preferences.
     */
    val storage: DemoAppStorage by lazy {
        SecureDemoAppStorage(this)
    }

    private val _sdkEvents = MutableSharedFlow<TONWalletKitEvent>(extraBufferCapacity = 10)
    val sdkEvents = _sdkEvents.asSharedFlow()

    private val _sdkInitialized = MutableSharedFlow<Boolean>(replay = 1)
    val sdkInitialized = _sdkInitialized.asSharedFlow()

    override fun onCreate() {
        super.onCreate()

        // Initialize ITONWalletKit SDK and add event handler
        applicationScope.launch {
            try {
                val kit = TONWalletKitHelper.mainnet(this@WalletKitDemoApp)
                loadAndAddStoredWallets(kit)

                kit.addEventsHandler(object : TONBridgeEventsHandler {
                    override fun handle(event: TONWalletKitEvent) {
                        _sdkEvents.tryEmit(event)
                    }
                })

                _sdkInitialized.emit(true)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize SDK", e)
            }
        }
    }

    private suspend fun loadAndAddStoredWallets(kit: ITONWalletKit) {
        try {
            val storedWallets = storage.loadAllWallets()
            if (storedWallets.isEmpty()) {
                Log.d(TAG, "No stored wallets to load")
                return
            }

            val existingAddresses = kit.getWallets().mapNotNull { it.address?.value }.toMutableSet()
            Log.d(TAG, "Loading ${storedWallets.size} stored wallets into SDK")

            for ((address, walletRecord) in storedWallets) {
                if (!existingAddresses.add(address)) {
                    continue
                }

                try {
                    if (walletRecord.interfaceType != WalletInterfaceType.MNEMONIC.value) {
                        Log.d(TAG, "Skipping auto-restore for $address: interfaceType=${walletRecord.interfaceType}")
                        continue
                    }

                    if (walletRecord.mnemonic.isEmpty()) {
                        Log.w(TAG, "Skipping auto-restore for $address: mnemonic is empty")
                        continue
                    }

                    val network = when (walletRecord.network) {
                        ChainIds.MAINNET -> TONNetwork.MAINNET
                        ChainIds.TESTNET -> TONNetwork.TESTNET
                        ChainIds.TETRA -> TONNetwork.TETRA
                        else -> TONNetwork.MAINNET
                    }

                    val domain = if (network.isTetra) TONSignatureDomain.L2(globalId = 662387) else null
                    val signer = kit.createSignerFromMnemonic(walletRecord.mnemonic)
                    val adapter = when (walletRecord.version) {
                        WalletVersions.V4R2 -> kit.createV4R2Adapter(signer, network, domain = domain)
                        WalletVersions.V5R1 -> kit.createV5R1Adapter(signer, network, domain = domain)
                        else -> {
                            Log.w(TAG, "Unsupported wallet version: ${walletRecord.version}, skipping")
                            continue
                        }
                    }
                    kit.addWallet(adapter)
                    Log.d(TAG, "Added wallet to SDK: $address")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to add wallet $address to SDK", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load stored wallets", e)
        }
    }

    private companion object {
        private const val TAG = "WalletKitDemoApp"
    }
}

object TONWalletKitHelper {
    private var mainnetInstance: ITONWalletKit? = null
    private val mutex = Mutex()

    @Volatile
    var disableNetworkSend: Boolean = false

    @Volatile
    var useCustomSessionManager: Boolean = true

    @Volatile
    var useCustomApiClient: Boolean = false

    var sessionManager: TestSessionManager? = null
        private set

    private fun checkInstrumentationDisableNetworkSend(): Boolean = try {
        val registryClass = Class.forName("androidx.test.platform.app.InstrumentationRegistry")
        val getArgumentsMethod = registryClass.getMethod("getArguments")
        val arguments = getArgumentsMethod.invoke(null) as? Bundle
        val value = arguments?.getString("disableNetworkSend")
        val result = value?.equals("true", ignoreCase = true) == true
        if (result) {
            Log.d("TONWalletKitHelper", "Detected disableNetworkSend=true from instrumentation arguments")
        }
        result
    } catch (e: Exception) {
        false
    }

    suspend fun mainnet(application: Application): ITONWalletKit {
        mainnetInstance?.let { return it }

        return mutex.withLock {
            mainnetInstance?.let {
                return@withLock it
            }

            val shouldDisableNetwork = disableNetworkSend || checkInstrumentationDisableNetworkSend()

            val devOptions = if (shouldDisableNetwork) {
                TONWalletKitConfiguration.DevOptions(disableNetworkSend = true)
            } else {
                null
            }

            val customSessionManager = if (useCustomSessionManager) {
                TestSessionManager(application).also { sessionManager = it }
            } else {
                null
            }

            // Create network configurations for both mainnet and testnet
            // This demonstrates the iOS-like pattern where each network config has either:
            // - apiClientConfiguration: Use SDK's built-in API client with your API key
            // - apiClient: Use your own custom API client implementation
            val toncenterApiKey = DemoApiConfig.toncenterApiKey
            val tonApiKey = DemoApiConfig.tonApiKey
            val networkConfigurations = if (useCustomApiClient) {
                setOf(
                    TONWalletKitConfiguration.NetworkConfiguration(
                        network = TONNetwork.MAINNET,
                        apiClient = ToncenterAPIClient.mainnet(apiKey = toncenterApiKey),
                    ),
                    TONWalletKitConfiguration.NetworkConfiguration(
                        network = TONNetwork.TESTNET,
                        apiClient = TonAPIClient.testnet(apiKey = tonApiKey),
                    ),
                    TONWalletKitConfiguration.NetworkConfiguration(
                        network = TONNetwork.TETRA,
                        apiClient = TonAPIClient.tetra(),
                    ),
                )
            } else {
                // Configure via -PwalletkitToncenterApiKey / WALLETKIT_TONCENTER_API_KEY.
                setOf(
                    TONWalletKitConfiguration.NetworkConfiguration(
                        network = TONNetwork.MAINNET,
                        apiClientConfiguration = TONWalletKitConfiguration.APIClientConfiguration(
                            key = toncenterApiKey,
                        ),
                    ),
                    TONWalletKitConfiguration.NetworkConfiguration(
                        network = TONNetwork.TESTNET,
                        apiClientConfiguration = TONWalletKitConfiguration.APIClientConfiguration(
                            key = toncenterApiKey,
                        ),
                    ),
                    TONWalletKitConfiguration.NetworkConfiguration(
                        network = TONNetwork.TETRA,
                        apiClientConfiguration = TONWalletKitConfiguration.APIClientConfiguration(
                            key = "",
                        ),
                        apiClientType = TONWalletKitConfiguration.APIClientType.TONAPI,
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
                    jsBridgeKey = DEFAULT_JS_BRIDGE_KEY,
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
            val tonCenterApiKey = BuildConfig.TONCENTER_API_KEY.takeIf { it.isNotBlank() }
            if (tonCenterApiKey != null) {
                listOf(TONNetwork.MAINNET, TONNetwork.TESTNET, TONNetwork.TETRA).forEach { network ->
                    try {
                        val streamingProvider = kit.createStreamingProvider(
                            TONTonCenterStreamingProviderConfig(
                                network = network,
                                apiKey = tonCenterApiKey,
                            ),
                        )
                        kit.streaming().register(streamingProvider)
                    } catch (e: Exception) {
                        Log.e("WalletKitDemoApp", "Streaming init ERROR network=${network.chainId} - ${e.message}", e)
                    }
                }
            } else {
                Log.w("WalletKitDemoApp", "TONCENTER_API_KEY is not set; TonCenter streaming provider disabled")
            }

            mainnetInstance = kit
            kit
        }
    }

    suspend fun clearMainnet() {
        mutex.withLock {
            mainnetInstance?.destroy()
            mainnetInstance = null
        }
    }

    private const val DEFAULT_MANIFEST_NAME = "Wallet"
    private const val DEFAULT_APP_NAME = "Wallet"

    // "tonkeeper" matches the jsBridgeKey in the official TonConnect wallet list, so dApps
    // using @tonconnect/ui-react will discover our injected bridge via window["tonkeeper"].tonconnect.
    private const val DEFAULT_JS_BRIDGE_KEY = "tonkeeper"
    private const val DEFAULT_MANIFEST_IMAGE_URL = "https://wallet.ton.org/icon.png"
    private const val DEFAULT_MANIFEST_ABOUT_URL = "https://wallet.ton.org"
    private const val DEFAULT_MANIFEST_UNIVERSAL_LINK = "https://wallet.ton.org/tc"
    private const val DEFAULT_BRIDGE_URL = "https://bridge.tonapi.io/bridge"
    private const val TAG = "TONWalletKitHelper"
}
