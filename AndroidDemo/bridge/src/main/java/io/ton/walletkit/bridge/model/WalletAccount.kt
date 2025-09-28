package io.ton.walletkit.bridge.model

data class WalletAccount(
    val address: String,
    val publicKey: String?,
    val version: String,
    val network: String,
    val index: Int,
)
