package io.ton.walletkit.storage.model

data class StoredWalletRecord(
    val mnemonic: List<String>,
    val name: String?,
    val network: String?,
    val version: String?,
)
