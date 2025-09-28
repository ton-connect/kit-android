package io.ton.walletkit.demo.state

import io.ton.walletkit.demo.model.SessionSummary
import io.ton.walletkit.demo.model.WalletSummary

data class WalletUiState(
    val initialized: Boolean = false,
    val status: String = "",
    val wallets: List<WalletSummary> = emptyList(),
    val sessions: List<SessionSummary> = emptyList(),
    val sheetState: SheetState = SheetState.None,
    val isUrlPromptVisible: Boolean = false,
    val isLoadingWallets: Boolean = false,
    val isLoadingSessions: Boolean = false,
    val error: String? = null,
    val events: List<String> = emptyList(),
    val lastUpdated: Long? = null,
)
