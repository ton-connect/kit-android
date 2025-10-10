package io.ton.walletkit.presentation.model

data class WalletSession(
    val sessionId: String,
    val dAppName: String,
    val walletAddress: String,
    val dAppUrl: String?,
    val manifestUrl: String?,
    val iconUrl: String?,
    val createdAtIso: String?,
    val lastActivityIso: String?,
)
