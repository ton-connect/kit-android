package io.ton.walletkit.demo.model

data class PendingWalletRecord(
    val metadata: WalletMetadata,
    val mnemonic: List<String>?,
)
