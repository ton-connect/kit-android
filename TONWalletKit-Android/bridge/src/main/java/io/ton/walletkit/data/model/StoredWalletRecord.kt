package io.ton.walletkit.data.model

data class StoredWalletRecord(
    val mnemonic: List<String>,
    val name: String?,
    val network: String?,
    val version: String?,
)
