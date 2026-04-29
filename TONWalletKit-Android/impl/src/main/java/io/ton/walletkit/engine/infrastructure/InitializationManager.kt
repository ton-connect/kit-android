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
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement
import org.json.JSONObject

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
    private val json = Json {
        encodeDefaults = false
        explicitNulls = false
    }
    private val walletKitInitMutex = Mutex()

    @Volatile private var isWalletKitInitialized: Boolean = false

    @Volatile private var persistentStorageEnabled: Boolean = true

    @Volatile private var currentNetwork: String = NetworkConstants.DEFAULT_NETWORK

    @Volatile private var apiBaseUrl: String = NetworkConstants.DEFAULT_TESTNET_API_URL

    @Volatile private var tonApiKey: String? = null

    private var pendingInitConfig: TONWalletKitConfiguration? = null
    private var currentConfig: TONWalletKitConfiguration? = null

    suspend fun initialize(configuration: TONWalletKitConfiguration) {
        walletKitInitMutex.withLock {
            if (!isWalletKitInitialized) {
                pendingInitConfig = configuration
            }
        }
        ensureInitialized(configuration)
    }

    fun getConfiguration(): TONWalletKitConfiguration? = currentConfig

    suspend fun ensureInitialized(configuration: TONWalletKitConfiguration? = null) {
        if (isWalletKitInitialized) {
            return
        }

        walletKitInitMutex.withLock {
            if (isWalletKitInitialized) {
                return@withLock
            }

            val effectiveConfig = configuration ?: pendingInitConfig ?: throw WalletKitBridgeException(ERROR_INIT_CONFIG_REQUIRED)
            pendingInitConfig = null

            Logger.d(TAG, "Auto-initializing WalletKit with config: network=${resolveNetworkName(effectiveConfig)}")

            try {
                performInitialization(effectiveConfig)
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

    private suspend fun performInitialization(configuration: TONWalletKitConfiguration) {
        currentNetwork = resolveNetworkName(configuration)
        persistentStorageEnabled = configuration.storageType != TONWalletKitStorageType.Memory
        apiBaseUrl = resolveTonApiBase(configuration)
        tonApiKey = configuration.apiClientConfiguration?.key?.takeIf { it.isNotBlank() }

        val appVersion = resolveAppVersion(configuration)
        val appName = resolveAppName(configuration)
        val featuresToUse = configuration.deviceInfo?.features ?: configuration.features

        val payload = InitPayload(
            network = currentNetwork,
            apiUrl = resolveTonClientEndpoint(configuration),
            tonApiUrl = apiBaseUrl,
            networkConfigurations = configuration.networkConfigurations.map { nc ->
                NetworkConfigPayload(
                    network = NetworkPayload(chainId = nc.network.chainId),
                    apiClientType = when (nc.apiClientType) {
                        TONWalletKitConfiguration.APIClientType.DEFAULT -> "default"
                        TONWalletKitConfiguration.APIClientType.TONCENTER -> "toncenter"
                        TONWalletKitConfiguration.APIClientType.TONAPI -> "tonapi"
                        TONWalletKitConfiguration.APIClientType.CUSTOM -> "custom"
                    },
                    apiClientConfiguration = nc.apiClientConfiguration?.let { ac ->
                        val url = ac.url?.takeIf { it.isNotBlank() }
                        val key = ac.key?.takeIf { it.isNotBlank() }
                        if (url != null || key != null) ApiClientConfigPayload(url, key) else null
                    },
                )
            },
            bridgeUrl = configuration.bridge.bridgeUrl.takeIf { it.isNotBlank() },
            bridgeName = configuration.walletManifest.name.takeIf { it.isNotBlank() },
            walletManifest = WalletManifestPayload(
                name = configuration.walletManifest.name,
                appName = appName,
                imageUrl = configuration.walletManifest.imageUrl,
                aboutUrl = configuration.walletManifest.aboutUrl,
                universalUrl = configuration.walletManifest.universalLink.takeIf { it.isNotBlank() },
                platforms = listOf(WebViewConstants.PLATFORM_ANDROID),
            ),
            deviceInfo = DeviceInfoPayload(
                platform = configuration.deviceInfo?.platform ?: WebViewConstants.PLATFORM_ANDROID,
                appName = appName,
                appVersion = appVersion,
                maxProtocolVersion = configuration.deviceInfo?.maxProtocolVersion ?: NetworkConstants.MAX_PROTOCOL_VERSION,
                features = buildList {
                    add(JsonPrimitive(JsonConstants.FEATURE_SEND_TRANSACTION))
                    add(
                        json.encodeToJsonElement(
                            SendTransactionFeatureDto.serializer(),
                            SendTransactionFeatureDto(
                                maxMessages = resolveMaxMessages(configuration, featuresToUse),
                            ),
                        ),
                    )
                    add(
                        json.encodeToJsonElement(
                            SignDataFeatureDto.serializer(),
                            SignDataFeatureDto(
                                types = resolveSignDataTypes(configuration, featuresToUse),
                            ),
                        ),
                    )
                },
            ),
            disableNetworkSend = if (configuration.dev?.disableNetworkSend == true) true else null,
            disableTransactionEmulation = configuration.eventsConfiguration?.disableTransactionEmulation,
        )

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
        rpcClient.call(BridgeMethodConstants.METHOD_INIT, JSONObject(json.encodeToString(payload)))

        currentConfig = configuration
        Logger.d(TAG, "WalletKit initialized. Event listeners will be set up on-demand.")
    }

    private fun resolveAppVersion(configuration: TONWalletKitConfiguration): String =
        configuration.deviceInfo?.appVersion
            ?: try {
                appContext.packageManager.getPackageInfo(appContext.packageName, 0)
                    .versionName ?: NetworkConstants.DEFAULT_APP_VERSION
            } catch (e: Exception) {
                Logger.w(TAG, ERROR_FAILED_GET_APP_VERSION, e)
                NetworkConstants.DEFAULT_APP_VERSION
            }

    private fun resolveAppName(configuration: TONWalletKitConfiguration): String =
        configuration.deviceInfo?.appName
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

    @Serializable
    private data class InitPayload(
        val network: String,
        @SerialName("apiUrl") val apiUrl: String? = null,
        @SerialName("tonApiUrl") val tonApiUrl: String,
        val networkConfigurations: List<NetworkConfigPayload>,
        @SerialName("bridgeUrl") val bridgeUrl: String? = null,
        @SerialName("bridgeName") val bridgeName: String? = null,
        @SerialName("walletManifest") val walletManifest: WalletManifestPayload,
        @SerialName("deviceInfo") val deviceInfo: DeviceInfoPayload,
        @SerialName("disableNetworkSend") val disableNetworkSend: Boolean? = null,
        @SerialName("disableTransactionEmulation") val disableTransactionEmulation: Boolean? = null,
    )

    @Serializable
    private data class NetworkConfigPayload(
        val network: NetworkPayload,
        val apiClientType: String,
        val apiClientConfiguration: ApiClientConfigPayload? = null,
    )

    @Serializable
    private data class NetworkPayload(val chainId: String)

    @Serializable
    private data class ApiClientConfigPayload(
        val url: String? = null,
        val key: String? = null,
    )

    @Serializable
    private data class WalletManifestPayload(
        val name: String,
        @SerialName("appName") val appName: String,
        @SerialName("imageUrl") val imageUrl: String,
        @SerialName("aboutUrl") val aboutUrl: String,
        @SerialName("universalUrl") val universalUrl: String? = null,
        val platforms: List<String>,
    )

    @Serializable
    private data class DeviceInfoPayload(
        val platform: String,
        @SerialName("appName") val appName: String,
        @SerialName("appVersion") val appVersion: String,
        @SerialName("maxProtocolVersion") val maxProtocolVersion: Int,
        val features: List<JsonElement>,
    )

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
