/*
 * Copyright (c) 2025 TonTech
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.ton.walletkit.demo.presentation.state

import io.ton.walletkit.demo.presentation.model.JettonSummary
import io.ton.walletkit.demo.presentation.model.SessionSummary
import io.ton.walletkit.demo.presentation.model.SignDataRequestUi
import io.ton.walletkit.demo.presentation.model.WalletSummary

data class WalletUiState(
    val initialized: Boolean = false,
    val status: String = "",
    val wallets: List<WalletSummary> = emptyList(),
    val activeWalletAddress: String? = null,
    val sessions: List<SessionSummary> = emptyList(),
    val sheetState: SheetState = SheetState.None,
    val previousSheet: SheetState? = null, // Used to restore sheet after modal interactions
    val isUrlPromptVisible: Boolean = false,
    val isWalletSwitcherExpanded: Boolean = false,
    val isLoadingWallets: Boolean = false,
    val isLoadingSessions: Boolean = false,
    val isLoadingTransactions: Boolean = false,
    val isSendingTransaction: Boolean = false,
    val isGeneratingMnemonic: Boolean = false,
    val error: String? = null,
    val events: List<String> = emptyList(),
    val lastUpdated: Long? = null,
    val clipboardContent: String? = null,
    val pendingSignerConfirmation: SignDataRequestUi? = null, // Request awaiting signer confirmation
    val jettons: List<JettonSummary> = emptyList(),
    val isLoadingJettons: Boolean = false,
    val jettonsError: String? = null,
    val canLoadMoreJettons: Boolean = false,
)
