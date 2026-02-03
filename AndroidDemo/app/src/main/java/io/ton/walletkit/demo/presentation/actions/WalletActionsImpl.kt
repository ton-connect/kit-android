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
package io.ton.walletkit.demo.presentation.actions

import io.ton.walletkit.api.generated.TONNetwork
import io.ton.walletkit.demo.domain.model.WalletInterfaceType
import io.ton.walletkit.demo.presentation.model.ConnectRequestUi
import io.ton.walletkit.demo.presentation.model.IntentRequestUi
import io.ton.walletkit.demo.presentation.model.JettonDetails
import io.ton.walletkit.demo.presentation.model.JettonSummary
import io.ton.walletkit.demo.presentation.model.SignDataRequestUi
import io.ton.walletkit.demo.presentation.model.TransactionRequestUi
import io.ton.walletkit.demo.presentation.model.WalletSummary
import io.ton.walletkit.demo.presentation.viewmodel.WalletKitViewModel
import javax.inject.Inject

/**
 * Implementation of WalletActions that delegates to WalletKitViewModel.
 * This adapter allows the ViewModel to be injected as a single dependency instead of
 * passing 30+ individual callback parameters.
 */
class WalletActionsImpl @Inject constructor(
    private val viewModel: WalletKitViewModel,
) : WalletActions {

    override fun onAddWalletClick() = viewModel.openAddWalletSheet()

    override fun onUrlPromptClick() = viewModel.showUrlPrompt()

    override fun onOpenBrowser(url: String, injectTonConnect: Boolean) = viewModel.openBrowser(url, injectTonConnect)

    override fun onRefresh() = viewModel.refreshAll()

    override fun onDismissSheet() = viewModel.dismissSheet()

    override fun onWalletDetails(address: String) = viewModel.showWalletDetails(address)

    override fun onSendFromWallet(address: String) = viewModel.openSendTransactionSheet(address)

    override fun onDisconnectSession(sessionId: String) = viewModel.disconnectSession(sessionId)

    override fun onToggleWalletSwitcher() = viewModel.toggleWalletSwitcher()

    override fun onSwitchWallet(address: String) = viewModel.switchWallet(address)

    override fun onRemoveWallet(address: String) = viewModel.removeWallet(address)

    override fun onRenameWallet(address: String, newName: String) = viewModel.renameWallet(address, newName)

    override fun onImportWallet(
        name: String,
        network: TONNetwork,
        mnemonics: List<String>,
        secretKeyHex: String,
        password: String,
        interfaceType: WalletInterfaceType,
    ) = viewModel.importWallet(name, network, mnemonics, secretKeyHex, password, interfaceType)

    override fun onGenerateWallet(
        name: String,
        network: TONNetwork,
        password: String,
        interfaceType: WalletInterfaceType,
    ) = viewModel.generateWallet(name, network, password, interfaceType)

    override fun onApproveConnect(request: ConnectRequestUi, wallet: WalletSummary) = viewModel.approveConnect(request, wallet)

    override fun onRejectConnect(request: ConnectRequestUi) = viewModel.rejectConnect(request)

    override fun onApproveTransaction(request: TransactionRequestUi) = viewModel.approveTransaction(request)

    override fun onRejectTransaction(request: TransactionRequestUi) = viewModel.rejectTransaction(request)

    override fun onApproveSignData(request: SignDataRequestUi) = viewModel.approveSignData(request)

    override fun onRejectSignData(request: SignDataRequestUi) = viewModel.rejectSignData(request)

    override fun onConfirmSignerApproval() = viewModel.confirmSignerApproval()

    override fun onCancelSignerApproval() = viewModel.cancelSignerApproval()

    override fun onSendTransaction(
        walletAddress: String,
        recipient: String,
        amount: String,
        comment: String,
    ) = viewModel.sendLocalTransaction(walletAddress, recipient, amount, comment)

    override fun onRefreshTransactions(address: String) = viewModel.refreshTransactions(address)

    override fun onTransactionClick(transactionHash: String, walletAddress: String) = viewModel.showTransactionDetail(transactionHash, walletAddress)

    override fun onHandleUrl(url: String) = viewModel.handleTonConnectUrl(url)

    override fun onDismissUrlPrompt() = viewModel.hideUrlPrompt()

    override fun onShowJettonDetails(jetton: JettonSummary) = viewModel.showJettonDetails(jetton)

    override fun onTransferJetton(
        jettonAddress: String,
        recipient: String,
        amount: String,
        comment: String,
    ) = viewModel.transferJetton(jettonAddress, recipient, amount, comment)

    override fun onShowTransferJetton(jetton: JettonDetails) = viewModel.showTransferJetton(jetton)

    override fun onLoadMoreJettons() = viewModel.loadMoreJettons()

    override fun onRefreshJettons() = viewModel.refreshJettons()

    // Intent actions (from deep links without session)
    override fun onApproveTransactionIntent(request: IntentRequestUi.Transaction) = viewModel.approveTransactionIntent(request)

    override fun onRejectTransactionIntent(request: IntentRequestUi.Transaction) = viewModel.rejectTransactionIntent(request)

    override fun onApproveSignDataIntent(request: IntentRequestUi.SignData) = viewModel.approveSignDataIntent(request)

    override fun onRejectSignDataIntent(request: IntentRequestUi.SignData) = viewModel.rejectSignDataIntent(request)

    override fun onApproveActionIntent(request: IntentRequestUi.Action) = viewModel.approveActionIntent(request)

    override fun onRejectActionIntent(request: IntentRequestUi.Action) = viewModel.rejectActionIntent(request)
}
