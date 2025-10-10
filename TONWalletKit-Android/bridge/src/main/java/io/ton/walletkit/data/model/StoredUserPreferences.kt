package io.ton.walletkit.data.model

/**
 * User preferences that should be persisted.
 *
 * @property activeWalletAddress Currently selected wallet address
 * @property lastSelectedNetwork Last selected network (testnet/mainnet)
 */
data class StoredUserPreferences(
    val activeWalletAddress: String?,
    val lastSelectedNetwork: String?,
)
