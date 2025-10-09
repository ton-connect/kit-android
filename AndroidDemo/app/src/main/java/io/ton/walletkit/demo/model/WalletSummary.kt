package io.ton.walletkit.demo.model

import io.ton.walletkit.bridge.model.Transaction

data class WalletSummary(
    val address: String,
    val name: String,
    val network: TonNetwork,
    val version: String,
    val publicKey: String?,
    val balanceNano: String?,
    val balance: String?,
    val transactions: List<Transaction>?,
    val lastUpdated: Long?,
    val connectedSessions: List<SessionSummary> = emptyList(), // Sessions connected to this wallet
)
