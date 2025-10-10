package io.ton.walletkit.presentation.config

/**
 * Configuration for WalletKit bridge initialization.
 *
 * @property network Network to use ("mainnet" or "testnet")
 * @property apiUrl Optional custom API URL for TON client
 * @property bridgeUrl Optional custom bridge URL for TonConnect
 * @property bridgeName Optional custom bridge name
 * @property tonClientEndpoint Optional custom TON client endpoint
 * @property tonApiUrl Optional custom TON API URL
 * @property apiKey Optional API key for TON API
 * @property enablePersistentStorage Enable persistent storage for wallets and sessions (default: true).
 *                                   When false, all data is stored in memory and cleared on app restart.
 *                                   Use cases for disabling:
 *                                   - Testing/development (quick reset)
 *                                   - Privacy-focused session-only mode
 *                                   - Kiosk/demo applications
 *                                   - Compliance requirements for ephemeral storage
 */
data class WalletKitBridgeConfig(
    val network: String = "testnet",
    val apiUrl: String? = null,
    val bridgeUrl: String? = null,
    val bridgeName: String? = null,
    val tonClientEndpoint: String? = null,
    val tonApiUrl: String? = null,
    val apiKey: String? = null,
    val enablePersistentStorage: Boolean = true,
)
