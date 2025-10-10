package io.ton.walletkit.data.model

/**
 * Bridge configuration that can be persisted.
 *
 * @property network Selected network (mainnet/testnet)
 * @property tonClientEndpoint TON client API endpoint
 * @property tonApiUrl TonAPI endpoint
 * @property apiKey API key (if used) - SENSITIVE
 * @property bridgeUrl TonConnect bridge URL
 * @property bridgeName Bridge identifier
 */
data class StoredBridgeConfig(
    val network: String,
    val tonClientEndpoint: String?,
    val tonApiUrl: String?,
    val apiKey: String?, // SENSITIVE - should be encrypted
    val bridgeUrl: String?,
    val bridgeName: String?,
)
