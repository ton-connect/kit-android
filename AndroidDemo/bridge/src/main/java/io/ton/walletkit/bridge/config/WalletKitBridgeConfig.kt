package io.ton.walletkit.bridge.config

data class WalletKitBridgeConfig(
    val network: String = "testnet",
    val apiUrl: String? = null,
    val bridgeUrl: String? = null,
    val bridgeName: String? = null,
    val allowMemoryStorage: Boolean? = null,
    val tonClientEndpoint: String? = null,
    val tonApiUrl: String? = null,
    val apiKey: String? = null,
)
