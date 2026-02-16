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
package io.ton.walletkit.config

import io.ton.walletkit.api.generated.TONNetwork
import io.ton.walletkit.client.TONAPIClient
import io.ton.walletkit.internal.constants.JsonConsts
import io.ton.walletkit.storage.TONWalletKitStorageType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Configuration for TONWalletKit initialization.
 *
 * Mirrors the shared TON Wallet Kit configuration contract for cross-platform consistency.
 * Follows iOS SDK's structure with NetworkConfiguration for per-network settings.
 *
 * @property networkConfigurations Set of network-specific configurations (each with either apiClientConfiguration OR apiClient)
 * @property deviceInfo Device and app information (optional, auto-detected if not provided)
 * @property walletManifest Wallet app manifest for TON Connect
 * @property bridge Bridge configuration
 * @property features Supported wallet features (used if deviceInfo not provided)
 * @property storageType Storage configuration
 * @property sessionManager Custom session manager implementation (optional)
 * @property dev Development options for testing
 */
@Serializable
data class TONWalletKitConfiguration(
    val networkConfigurations: Set<NetworkConfiguration>,
    val walletManifest: Manifest,
    val bridge: Bridge,
    val features: List<Feature>,
    @kotlinx.serialization.Transient
    val storageType: TONWalletKitStorageType = TONWalletKitStorageType.Encrypted,
    @kotlinx.serialization.Transient
    val deviceInfo: DeviceInfo? = null,
    /**
     * Custom session manager implementation.
     * If not provided, a default storage-backed session manager will be used.
     */
    @kotlinx.serialization.Transient
    val sessionManager: io.ton.walletkit.session.TONConnectSessionManager? = null,
    @kotlinx.serialization.Transient
    val dev: DevOptions? = null,
    @kotlinx.serialization.Transient
    val eventsConfiguration: EventsConfiguration? = null,
) {
    /**
     * Returns the primary network (first in the set).
     * Used for backward compatibility and single-network scenarios.
     */
    val network: TONNetwork
        get() = networkConfigurations.first().network

    /**
     * Extracts all custom API clients from network configurations.
     * Only includes networks that have a custom apiClient (not apiClientConfiguration).
     */
    val apiClients: List<TONAPIClient>
        get() = networkConfigurations.mapNotNull { it.apiClient }

    /**
     * Returns the API client configuration for the primary network.
     * Used for backward compatibility.
     */
    val apiClientConfiguration: APIClientConfiguration?
        get() = networkConfigurations.firstOrNull()?.apiClientConfiguration

    /**
     * Wallet manifest for TON Connect.
     *
     * @property name Wallet display name
     * @property appName Application name
     * @property imageUrl Wallet icon URL
     * @property tondns TON DNS name (optional)
     * @property aboutUrl About/website URL
     * @property universalLink Universal link URL for deep linking
     * @property deepLink Deep link URL scheme (optional)
     * @property bridgeUrl TON Connect bridge URL
     * @property jsBridgeKey JS bridge key for TonConnect injection (the window property name).
     *   Must match the key registered in wallets-v2.json (e.g. "tonkeeper").
     *   Defaults to [appName] if not specified.
     */
    @Serializable
    data class Manifest(
        val name: String,
        val appName: String,
        val imageUrl: String,
        val tondns: String? = null,
        val aboutUrl: String,
        val universalLink: String,
        val deepLink: String? = null,
        val bridgeUrl: String,
        val jsBridgeKey: String? = null,
    )

    /**
     * Bridge connection configuration.
     *
     * @property bridgeUrl Bridge server URL
     * @property heartbeatInterval Heartbeat interval in milliseconds
     * @property reconnectInterval Reconnection interval in milliseconds
     * @property maxReconnectAttempts Maximum reconnection attempts
     */
    @Serializable
    data class Bridge(
        val bridgeUrl: String,
        val heartbeatInterval: Long? = null,
        val reconnectInterval: Long? = null,
        val maxReconnectAttempts: Int? = null,
    )

    /**
     * API client configuration.
     *
     * @property url API endpoint URL (optional, uses default if not provided)
     * @property key API key for authentication
     */
    @Serializable
    data class APIClientConfiguration(
        val url: String? = null,
        val key: String,
    )

    /**
     * Network-specific configuration.
     *
     * Each network configuration can have either:
     * - apiClientConfiguration: Use built-in API client with the provided config (key + optional URL)
     * - apiClient: Use a custom TONAPIClient implementation
     *
     * These are mutually exclusive. Use the appropriate constructor based on your needs.
     *
     * @property network The blockchain network this configuration applies to
     * @property apiClientConfiguration Built-in API client configuration (optional, mutually exclusive with apiClient)
     * @property apiClient Custom API client implementation (optional, mutually exclusive with apiClientConfiguration)
     */
    @Serializable
    data class NetworkConfiguration(
        val network: TONNetwork,
        @SerialName("apiClient")
        val apiClientConfiguration: APIClientConfiguration? = null,
        @kotlinx.serialization.Transient
        val apiClient: TONAPIClient? = null,
    ) {
        /**
         * Create a network configuration with a built-in API client configuration.
         * Use this when you want to use the SDK's built-in API client with your API key.
         */
        constructor(network: TONNetwork, apiClientConfiguration: APIClientConfiguration) : this(
            network = network,
            apiClientConfiguration = apiClientConfiguration,
            apiClient = null,
        )

        /**
         * Create a network configuration with a custom API client.
         * Use this when you want to provide your own API client implementation.
         */
        constructor(network: TONNetwork, apiClient: TONAPIClient) : this(
            network = network,
            apiClientConfiguration = null,
            apiClient = apiClient,
        )

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is NetworkConfiguration) return false
            return network == other.network
        }

        override fun hashCode(): Int = network.hashCode()
    }

    data class EventsConfiguration(
        val disableEvents: Boolean = false,
        val disableTransactionEmulation: Boolean = false,
    )

    /**
     * Development options for testing.
     *
     * @property disableNetworkSend When true, transactions will be simulated but not sent to the network
     */
    data class DevOptions(
        val disableNetworkSend: Boolean = false,
    )

    /**
     * Base interface for wallet features.
     * Implement this to define supported wallet capabilities.
     */
    interface Feature

    /**
     * Send transaction feature configuration.
     *
     * @property maxMessages Maximum messages per transaction
     * @property extraCurrencySupported Support for extra currencies beyond TON
     */
    @Serializable
    data class SendTransactionFeature(
        val maxMessages: Int? = null,
        val extraCurrencySupported: Boolean? = null,
    ) : Feature

    /**
     * Sign data feature configuration.
     *
     * @property types Supported sign data types (text, binary, cell)
     */
    @Serializable
    data class SignDataFeature(
        val types: List<SignDataType>,
    ) : Feature
}

/**
 * Types of data that can be signed.
 *
 * Mirrors the shared TON Wallet Kit specification.
 */
@Serializable
enum class SignDataType {
    /** Plain text data */
    @SerialName(JsonConsts.VALUE_SIGN_DATA_TEXT)
    TEXT,

    /** Binary data */
    @SerialName(JsonConsts.VALUE_SIGN_DATA_BINARY)
    BINARY,

    /** TON Cell data */
    @SerialName(JsonConsts.VALUE_SIGN_DATA_CELL)
    CELL,
}
