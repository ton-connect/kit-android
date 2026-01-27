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
import io.ton.walletkit.demo.presentation.model.JettonDetails
import io.ton.walletkit.demo.presentation.model.JettonSummary
import io.ton.walletkit.demo.presentation.model.SignDataRequestUi
import io.ton.walletkit.demo.presentation.model.SignMessageRequestUi
import io.ton.walletkit.demo.presentation.model.TransactionRequestUi
import io.ton.walletkit.demo.presentation.model.WalletSummary

/**
 * Interface defining all wallet-related actions.
 * This consolidates the many callback parameters into a single injected dependency.
 */
interface WalletActions {
    fun onAddWalletClick()
    fun onUrlPromptClick()
    fun onOpenBrowser(url: String, injectTonConnect: Boolean = true)
    fun onRefresh()
    fun onDismissSheet()
    fun onWalletDetails(address: String)
    fun onSendFromWallet(address: String)
    fun onDisconnectSession(sessionId: String)
    fun onToggleWalletSwitcher()
    fun onSwitchWallet(address: String)
    fun onRemoveWallet(address: String)
    fun onRenameWallet(address: String, newName: String)
    fun onImportWallet(
        name: String,
        network: TONNetwork,
        mnemonics: List<String> = emptyList(),
        secretKeyHex: String = "",
        password: String,
        interfaceType: WalletInterfaceType,
    )
    fun onGenerateWallet(name: String, network: TONNetwork, password: String, interfaceType: WalletInterfaceType)
    fun onApproveConnect(request: ConnectRequestUi, wallet: WalletSummary)
    fun onRejectConnect(request: ConnectRequestUi)
    fun onApproveTransaction(request: TransactionRequestUi)
    fun onRejectTransaction(request: TransactionRequestUi)
    fun onApproveSignMessage(request: SignMessageRequestUi)
    fun onRejectSignMessage(request: SignMessageRequestUi)
    fun onApproveSignData(request: SignDataRequestUi)
    fun onRejectSignData(request: SignDataRequestUi)
    fun onConfirmSignerApproval()
    fun onCancelSignerApproval()
    fun onSendTransaction(walletAddress: String, recipient: String, amount: String, comment: String)
    fun onRefreshTransactions(address: String)
    fun onTransactionClick(transactionHash: String, walletAddress: String)
    fun onHandleUrl(url: String)
    fun onDismissUrlPrompt()
    fun onShowJettonDetails(jetton: JettonSummary)
    fun onTransferJetton(jettonAddress: String, recipient: String, amount: String, comment: String)
    fun onShowTransferJetton(jetton: JettonDetails)
    fun onLoadMoreJettons()
    fun onRefreshJettons()
}
