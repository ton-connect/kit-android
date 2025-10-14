package io.ton.walletkit.data.model

/**
 * Persistent storage model for wallet data.
 *
 * This model is used internally by the storage layer to persist wallet information
 * between app sessions. It contains sensitive data and should only be stored using
 * encrypted storage mechanisms.
 *
 * **Security Note:** The mnemonic phrase is the master key to the wallet and must
 * be handled with extreme care. It should never be logged or transmitted.
 *
 * @property mnemonic The wallet's recovery phrase (12/24 words) - **highly sensitive**
 * @property name Optional user-defined name for the wallet (e.g., "Main Wallet")
 * @property network Optional network identifier (e.g., "mainnet", "testnet")
 * @property version Optional wallet contract version (e.g., "v4R2")
 */
data class StoredWalletRecord(
    val mnemonic: List<String>,
    val name: String?,
    val network: String?,
    val version: String?,
)
