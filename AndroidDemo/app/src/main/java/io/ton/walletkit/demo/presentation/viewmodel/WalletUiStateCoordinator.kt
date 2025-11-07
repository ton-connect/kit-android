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
package io.ton.walletkit.demo.presentation.viewmodel

import io.ton.walletkit.demo.presentation.model.JettonDetails
import io.ton.walletkit.demo.presentation.model.JettonSummary
import io.ton.walletkit.demo.presentation.model.WalletSummary
import io.ton.walletkit.demo.presentation.state.SheetState
import io.ton.walletkit.demo.presentation.state.WalletUiState
import io.ton.walletkit.demo.presentation.viewmodel.SessionsViewModel.SessionsState
import io.ton.walletkit.demo.presentation.viewmodel.TonConnectViewModel.TonConnectState
import io.ton.walletkit.demo.presentation.viewmodel.WalletOperationsViewModel.WalletOperationsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * Centralises mutations of [WalletUiState] so the main ViewModel becomes a thin orchestrator.
 * Responsible for sheet visibility, prompt toggles, and wiring UI sub-sections together.
 */
class WalletUiStateCoordinator(
    private val state: MutableStateFlow<WalletUiState>,
) {

    fun setSheet(sheet: SheetState, savePrevious: Boolean = false) {
        state.update { current ->
            val previous = if (savePrevious) current.sheetState else null
            current.copy(sheetState = sheet, previousSheet = previous)
        }
    }

    fun dismissSheet() {
        setSheet(SheetState.None)
    }

    fun openAddWalletSheet() {
        setSheet(SheetState.AddWallet)
    }

    fun showWalletDetails(summary: WalletSummary) {
        setSheet(SheetState.WalletDetails(summary))
    }

    fun openSendTransactionSheet(summary: WalletSummary) {
        setSheet(SheetState.SendTransaction(summary))
    }

    fun showTransactionDetail(detail: SheetState.TransactionDetail) {
        setSheet(detail)
    }

    fun showJettonDetails(details: JettonDetails) {
        setSheet(SheetState.JettonDetails(details))
    }

    fun showTransferJetton(details: JettonDetails) {
        setSheet(SheetState.TransferJetton(details))
    }

    fun openBrowser(url: String) {
        setSheet(SheetState.Browser(url))
    }

    fun showUrlPrompt() {
        state.update { it.copy(isUrlPromptVisible = true) }
    }

    fun hideUrlPrompt() {
        state.update { it.copy(isUrlPromptVisible = false) }
    }

    fun toggleWalletSwitcher() {
        state.update { it.copy(isWalletSwitcherExpanded = !it.isWalletSwitcherExpanded) }
    }

    fun collapseWalletSwitcher() {
        state.update { it.copy(isWalletSwitcherExpanded = false) }
    }

    fun setActiveWallet(address: String?) {
        state.update {
            it.copy(activeWalletAddress = address, isWalletSwitcherExpanded = false, error = null)
        }
    }

    fun updateJettons(
        jettons: List<JettonSummary>,
        isLoading: Boolean,
        error: String?,
        canLoadMore: Boolean,
    ) {
        state.update {
            it.copy(
                jettons = jettons,
                isLoadingJettons = isLoading,
                jettonsError = error,
                canLoadMoreJettons = canLoadMore,
            )
        }
    }

    fun onSessionsStateChanged(sessionsState: SessionsState) {
        state.update { current ->
            val errorMessage = sessionsState.error ?: current.error
            current.copy(
                sessions = sessionsState.sessions,
                isLoadingSessions = sessionsState.isLoading,
                error = errorMessage,
            )
        }
    }

    fun onWalletOperationsStateChanged(operationsState: WalletOperationsState) {
        state.update { current ->
            val errorMessage = operationsState.error ?: current.error
            current.copy(
                isSendingTransaction = operationsState.isSendingTransaction,
                error = errorMessage,
            )
        }
    }

    fun onTonConnectStateChanged(tonConnectState: TonConnectState) {
        state.update { current ->
            val status = tonConnectState.successMessage ?: current.status
            val errorMessage = tonConnectState.error ?: current.error
            current.copy(
                status = status,
                error = errorMessage,
            )
        }
    }
}
