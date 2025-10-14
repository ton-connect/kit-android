package io.ton.walletkit.data.model

/**
 * User preferences that should be persisted.
 *
 * @property activeWalletAddress Currently selected wallet address
 * @property lastSelectedNetwork Last selected network (testnet/mainnet)
 * @suppress Internal storage model. Not part of public API.
 */
internal data class StoredUserPreferences(
    val activeWalletAddress: String?,
    val lastSelectedNetwork: String?,
)
