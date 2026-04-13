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
import io.ton.walletkit.demo.BuildConfig
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
            val storage = getSharedPreferences("wallet_storage", MODE_PRIVATE)
            val walletDataJson = storage.getString("wallets", "[]") ?: "[]"

            if (walletDataJson == "[]") {
                Log.d(TAG, "No stored wallets to load")
                return
            }

            val walletDataList = kotlinx.serialization.json.Json.decodeFromString<List<WalletDataRecord>>(walletDataJson)

            Log.d(TAG, "Loading ${walletDataList.size} stored wallets into SDK")

            for (walletRecord in walletDataList) {
                try {
                    val mnemonicWords = walletRecord.mnemonic.split(" ").filter { it.isNotBlank() }

                    val network = when (walletRecord.network.lowercase()) {
                        ChainIds.MAINNET -> TONNetwork.MAINNET
                        ChainIds.TESTNET -> TONNetwork.TESTNET
                        else -> TONNetwork.MAINNET
                    }

                    val signer = kit.createSignerFromMnemonic(mnemonicWords)
                    val adapter = when (walletRecord.version) {
                        WalletVersions.V4R2 -> kit.createV4R2Adapter(signer, network)
                        WalletVersions.V5R1 -> kit.createV5R1Adapter(signer, network)
                        else -> {
                            Log.w(TAG, "Unsupported wallet version: ${walletRecord.version}, skipping")
                            continue
                        }
                    }
                    kit.addWallet(adapter)
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

object TONWalletKitHelper {
    private var mainnetInstance: ITONWalletKit? = null
    private val mutex = kotlinx.coroutines.sync.Mutex()

    @Volatile
    var disableNetworkSend: Boolean = false

    @Volatile
    var useCustomSessionManager: Boolean = true

    @Volatile
    var useCustomApiClient: Boolean = true

    var sessionManager: TestSessionManager? = null
        private set

    private fun checkInstrumentationDisableNetworkSend(): Boolean = try {
        val registryClass = Class.forName("androidx.test.platform.app.InstrumentationRegistry")
        val getArgumentsMethod = registryClass.getMethod("getArguments")
        val arguments = getArgumentsMethod.invoke(null) as? android.os.Bundle
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
                TestSessionManager().also { sessionManager = it }
            } else {
                null
            }

            val networkConfigurations = if (useCustomApiClient) {
                setOf(
                    TONWalletKitConfiguration.NetworkConfiguration(
                        network = TONNetwork.MAINNET,
                        apiClient = ToncenterAPIClient.mainnet(),
                    ),
                    TONWalletKitConfiguration.NetworkConfiguration(
                        network = TONNetwork.TESTNET,
                        apiClient = TonAPIClient.testnet(),
                    ),
                )
            } else {
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
            val tonCenterApiKey = BuildConfig.TONCENTER_API_KEY.takeIf { it.isNotBlank() }
            if (tonCenterApiKey != null) {
                try {
                    val toncenterStreaming = kit.createStreamingProvider(
                        io.ton.walletkit.api.generated.TONTonCenterStreamingProviderConfig(
                            network = TONNetwork.MAINNET,
                            apiKey = tonCenterApiKey,
                        ),
                    )
                    kit.streaming().register(toncenterStreaming)
                } catch (e: Exception) {
                    Log.e("WalletKitDemoApp", "Streaming init ERROR - ${e.message}", e)
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
    private const val DEFAULT_MANIFEST_IMAGE_URL = "https://wallet.ton.org/icon.png"
    private const val DEFAULT_MANIFEST_ABOUT_URL = "https://wallet.ton.org"
    private const val DEFAULT_MANIFEST_UNIVERSAL_LINK = "https://wallet.ton.org/tc"
    private const val DEFAULT_BRIDGE_URL = "https://bridge.tonapi.io/bridge"
    private const val TAG = "TONWalletKitHelper"
}
