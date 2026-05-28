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
package io.ton.walletkit.engine.infrastructure

import android.content.Context
import io.ton.walletkit.WalletKitBridgeException
import io.ton.walletkit.api.MAINNET
import io.ton.walletkit.api.TESTNET
import io.ton.walletkit.api.generated.TONNetwork
import io.ton.walletkit.config.SignDataType
import io.ton.walletkit.config.TONWalletKitConfiguration
import io.ton.walletkit.internal.constants.BridgeMethodConstants
import io.ton.walletkit.internal.constants.JsonConstants
import io.ton.walletkit.internal.constants.LogConstants
import io.ton.walletkit.internal.constants.NetworkConstants
import io.ton.walletkit.internal.constants.WebViewConstants
import io.ton.walletkit.internal.util.Logger
import io.ton.walletkit.storage.TONWalletKitStorageType
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * Handles WalletKit initialisation and configuration state.
 *
 * Responsibilities:
 * * Track whether the bridge has been initialised.
 * * Build the payload passed to the JavaScript `init` method.
 * * Persist network-related properties for later use by the engine.
 * * Maintain the persistent storage flag used by [StorageManager].
 *
 * @suppress Internal component. Use through [WebViewWalletKitEngine].
 */
internal class InitializationManager(
    context: Context,
    private val rpcClient: BridgeRpcClient,
) {
    private val appContext = context.applicationContext
    private val walletKitInitMutex = Mutex()

    @Volatile private var isWalletKitInitialized: Boolean = false

    @Volatile private var persistentStorageEnabled: Boolean = true

    @Volatile private var currentNetwork: String = NetworkConstants.DEFAULT_NETWORK

    @Volatile private var apiBaseUrl: String = NetworkConstants.DEFAULT_TESTNET_API_URL

    @Volatile private var tonApiKey: String? = null

    private var pendingInitConfig: TONWalletKitConfiguration? = null
    private var currentConfig: TONWalletKitConfiguration? = null

    private var pendingInitKitInstanceId: String? = null

    suspend fun initialize(configuration: TONWalletKitConfiguration, kitInstanceId: String? = null) {
        walletKitInitMutex.withLock {
            if (!isWalletKitInitialized) {
                pendingInitConfig = configuration
                pendingInitKitInstanceId = kitInstanceId
            }
        }
        ensureInitialized(configuration, kitInstanceId)
    }

    fun getConfiguration(): TONWalletKitConfiguration? = currentConfig

    suspend fun ensureInitialized(
        configuration: TONWalletKitConfiguration? = null,
        kitInstanceId: String? = null,
    ) {
        if (isWalletKitInitialized) {
            return
        }

        walletKitInitMutex.withLock {
            if (isWalletKitInitialized) {
                return@withLock
            }

            val effectiveConfig = configuration ?: pendingInitConfig ?: throw WalletKitBridgeException(ERROR_INIT_CONFIG_REQUIRED)
            val effectiveKitInstanceId = kitInstanceId ?: pendingInitKitInstanceId
            pendingInitConfig = null
            pendingInitKitInstanceId = null

            Logger.d(TAG, "Auto-initializing WalletKit with config: network=${resolveNetworkName(effectiveConfig)}")

            try {
                performInitialization(effectiveConfig, effectiveKitInstanceId)
                isWalletKitInitialized = true
                Logger.d(TAG, "WalletKit auto-initialization completed successfully")
            } catch (err: Throwable) {
                Logger.e(TAG, ERROR_WALLETKIT_AUTO_INIT_FAILED, err)
                throw WalletKitBridgeException(ERROR_FAILED_AUTO_INIT_WALLETKIT + err.message)
            }
        }
    }

    fun isInitialized(): Boolean = isWalletKitInitialized

    fun isPersistentStorageEnabled(): Boolean = persistentStorageEnabled

    fun currentNetwork(): String = currentNetwork

    fun apiBaseUrl(): String = apiBaseUrl

    fun updateNetwork(network: String?) {
        if (!network.isNullOrBlank()) {
            currentNetwork = network
        }
    }

    fun updateApiBaseUrl(url: String?) {
        if (!url.isNullOrBlank()) {
            apiBaseUrl = url
        }
    }

    fun tonApiKey(): String? = tonApiKey

    private suspend fun performInitialization(
        configuration: TONWalletKitConfiguration,
        kitInstanceId: String?,
    ) {
        val networkName = resolveNetworkName(configuration)
        currentNetwork = networkName
        persistentStorageEnabled = configuration.storageType != TONWalletKitStorageType.Memory

        val tonClientEndpoint = resolveTonClientEndpoint(configuration)
        apiBaseUrl = resolveTonApiBase(configuration)
        tonApiKey = configuration.apiClientConfiguration?.key?.takeIf { it.isNotBlank() }

        // Use deviceInfo if provided, otherwise auto-detect
        val appVersion = configuration.deviceInfo?.appVersion
            ?: try {
                val packageInfo = appContext.packageManager.getPackageInfo(appContext.packageName, 0)
                packageInfo.versionName ?: NetworkConstants.DEFAULT_APP_VERSION
            } catch (e: Exception) {
                Logger.w(TAG, ERROR_FAILED_GET_APP_VERSION, e)
                NetworkConstants.DEFAULT_APP_VERSION
            }

        val appName = configuration.deviceInfo?.appName
            ?: try {
                val manifestName = configuration.walletManifest.appName
                manifestName.ifBlank {
                    val applicationInfo = appContext.applicationInfo
                    val stringId = applicationInfo.labelRes
                    if (stringId == 0) {
                        applicationInfo.nonLocalizedLabel?.toString() ?: appContext.packageName
                    } else {
                        appContext.getString(stringId)
                    }
                }
            } catch (e: Exception) {
                Logger.w(TAG, ERROR_FAILED_GET_APP_NAME, e)
                appContext.packageName
            }

        val payload = buildJsonObject {
            put(JsonConstants.KEY_NETWORK, currentNetwork)
            tonClientEndpoint?.let { put(JsonConstants.KEY_API_URL, it) }
            put(JsonConstants.KEY_TON_API_URL, apiBaseUrl)

            // Pass all configured networks (matching iOS bridge format: networkConfigurations)
            putJsonArray("networkConfigurations") {
                for (networkConfig in configuration.networkConfigurations) {
                    addJsonObject {
                        putJsonObject("network") {
                            put("chainId", networkConfig.network.chainId)
                        }
                        val apiClientTypeStr = when (networkConfig.apiClientType) {
                            TONWalletKitConfiguration.APIClientType.DEFAULT -> "default"
                            TONWalletKitConfiguration.APIClientType.TONCENTER -> "toncenter"
                            TONWalletKitConfiguration.APIClientType.TONAPI -> "tonapi"
                            TONWalletKitConfiguration.APIClientType.CUSTOM -> "custom"
                        }
                        put("apiClientType", apiClientTypeStr)
                        networkConfig.apiClientConfiguration?.let { apiConfig ->
                            putJsonObject("apiClientConfiguration") {
                                apiConfig.url?.takeIf { it.isNotBlank() }?.let { put("url", it) }
                                apiConfig.key?.takeIf { it.isNotBlank() }?.let { put("key", it) }
                            }
                        }
                    }
                }
            }

            configuration.bridge.bridgeUrl.takeIf { it.isNotBlank() }?.let { put(JsonConstants.KEY_BRIDGE_URL, it) }
            configuration.walletManifest.name.takeIf { it.isNotBlank() }?.let { put(JsonConstants.KEY_BRIDGE_NAME, it) }

            putJsonObject(JsonConstants.KEY_WALLET_MANIFEST) {
                put(JsonConstants.KEY_NAME, configuration.walletManifest.name)
                put(JsonConstants.KEY_APP_NAME, appName)
                put(JsonConstants.KEY_IMAGE_URL, configuration.walletManifest.imageUrl)
                put(JsonConstants.KEY_ABOUT_URL, configuration.walletManifest.aboutUrl)
                configuration.walletManifest.universalLink.takeIf { it.isNotBlank() }?.let {
                    put(JsonConstants.KEY_UNIVERSAL_URL, it)
                }
                putJsonArray(JsonConstants.KEY_PLATFORMS) { add(WebViewConstants.PLATFORM_ANDROID) }
            }

            putJsonObject(JsonConstants.KEY_DEVICE_INFO) {
                put(JsonConstants.KEY_PLATFORM, configuration.deviceInfo?.platform ?: WebViewConstants.PLATFORM_ANDROID)
                put(JsonConstants.KEY_APP_NAME, appName)
                put(JsonConstants.KEY_APP_VERSION, appVersion)
                put(JsonConstants.KEY_MAX_PROTOCOL_VERSION, configuration.deviceInfo?.maxProtocolVersion ?: NetworkConstants.MAX_PROTOCOL_VERSION)
                putJsonArray(JsonConstants.KEY_FEATURES) {
                    // Use deviceInfo.features if provided, otherwise use configuration.features
                    val featuresToUse = configuration.deviceInfo?.features ?: configuration.features

                    // Add "SendTransaction" string for backward compatibility
                    add(JsonConstants.FEATURE_SEND_TRANSACTION)
                    // Add SendTransaction object with maxMessages for extended info
                    addJsonObject {
                        put(JsonConstants.KEY_NAME, JsonConstants.FEATURE_SEND_TRANSACTION)
                        put(JsonConstants.KEY_MAX_MESSAGES, resolveMaxMessages(configuration, featuresToUse))
                    }
                    addJsonObject {
                        put(JsonConstants.KEY_NAME, JsonConstants.FEATURE_SIGN_DATA)
                        putJsonArray(JsonConstants.KEY_TYPES) {
                            resolveSignDataTypes(configuration, featuresToUse).forEach { add(it) }
                        }
                    }
                }
            }

            // Pass disableNetworkSend flag for testing (transactions simulated but not sent)
            configuration.dev?.disableNetworkSend?.let { disableNetworkSend ->
                if (disableNetworkSend) {
                    put(JsonConstants.KEY_DISABLE_NETWORK_SEND, true)
                }
            }
            configuration.eventsConfiguration?.let { eventsConfig ->
                put("disableTransactionEmulation", eventsConfig.disableTransactionEmulation)
            }

            // Namespaced id (when available) makes re-registration idempotent and isolates kits
            // sharing a cached engine. Null → UUID fallback for mock paths.
            configuration.fetchManifest?.let { fetch ->
                val refId = rpcClient.wrappedFunctions.registerTyped(
                    id = kitInstanceId?.let { "$it.fetchManifest" },
                    fn = fetch,
                )
                putJsonObject("fetchManifest") { put("__wrappedFn", refId) }
            }
        }

        Logger.d(
            TAG,
            buildString {
                append("Initializing WalletKit with persistent storage: ")
                append(persistentStorageEnabled)
                append(", app: ")
                append(appName)
                append(" v")
                append(appVersion)
            },
        )
        rpcClient.send(BridgeMethodConstants.METHOD_INIT, payload)

        // Store the configuration for later use (e.g., WebView injection)
        currentConfig = configuration

        Logger.d(TAG, "WalletKit initialized. Event listeners will be set up on-demand.")
    }

    private fun resolveNetworkName(configuration: TONWalletKitConfiguration): String =
        when (configuration.network.chainId) {
            TONNetwork.MAINNET.chainId -> NetworkConstants.NETWORK_MAINNET
            TONNetwork.TESTNET.chainId -> NetworkConstants.NETWORK_TESTNET
            else -> NetworkConstants.NETWORK_MAINNET
        }

    private fun resolveTonClientEndpoint(configuration: TONWalletKitConfiguration): String? =
        configuration.apiClientConfiguration?.url?.takeIf { it.isNotBlank() }

    private fun resolveTonApiBase(configuration: TONWalletKitConfiguration): String {
        val custom = configuration.apiClientConfiguration?.url?.takeIf { it.isNotBlank() }
        return custom ?: when (configuration.network.chainId) {
            TONNetwork.MAINNET.chainId -> NetworkConstants.DEFAULT_MAINNET_API_URL
            TONNetwork.TESTNET.chainId -> NetworkConstants.DEFAULT_TESTNET_API_URL
            else -> NetworkConstants.DEFAULT_MAINNET_API_URL
        }
    }

    private fun resolveMaxMessages(
        configuration: TONWalletKitConfiguration,
        features: List<TONWalletKitConfiguration.Feature>,
    ): Int {
        return features
            .filterIsInstance<TONWalletKitConfiguration.SendTransactionFeature>()
            .firstNotNullOfOrNull { it.maxMessages }
            ?: DEFAULT_MAX_MESSAGES
    }

    private fun resolveSignDataTypes(
        configuration: TONWalletKitConfiguration,
        features: List<TONWalletKitConfiguration.Feature>,
    ): List<String> {
        val types =
            features
                .filterIsInstance<TONWalletKitConfiguration.SignDataFeature>()
                .firstOrNull()
                ?.types
                ?.takeIf { it.isNotEmpty() }
                ?: DEFAULT_SIGN_TYPES
        return types.map {
            when (it) {
                SignDataType.TEXT -> JsonConstants.VALUE_SIGN_DATA_TEXT
                SignDataType.BINARY -> JsonConstants.VALUE_SIGN_DATA_BINARY
                SignDataType.CELL -> JsonConstants.VALUE_SIGN_DATA_CELL
            }
        }
    }

    private companion object {
        private const val TAG = LogConstants.TAG_WEBVIEW_ENGINE
        private const val ERROR_WALLETKIT_AUTO_INIT_FAILED = "WalletKit auto-initialization failed"
        private const val ERROR_FAILED_AUTO_INIT_WALLETKIT = "Failed to auto-initialize WalletKit: "
        private const val ERROR_FAILED_GET_APP_VERSION = "Failed to get app version, using default"
        private const val ERROR_FAILED_GET_APP_NAME = "Failed to get app name, using package name"
        private const val ERROR_INIT_CONFIG_REQUIRED = "TONWalletKit.initialize() must be called before using the SDK."
        private const val DEFAULT_MAX_MESSAGES = 4
        private val DEFAULT_SIGN_TYPES = listOf(SignDataType.TEXT, SignDataType.BINARY, SignDataType.CELL)
    }
}
