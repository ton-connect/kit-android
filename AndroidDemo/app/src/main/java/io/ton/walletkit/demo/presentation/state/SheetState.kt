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

import io.ton.walletkit.demo.presentation.model.ConnectRequestUi
import io.ton.walletkit.demo.presentation.model.JettonDetails
import io.ton.walletkit.demo.presentation.model.SignDataRequestUi
import io.ton.walletkit.demo.presentation.model.TransactionDetailUi
import io.ton.walletkit.demo.presentation.model.TransactionRequestUi
import io.ton.walletkit.demo.presentation.model.WalletSummary

sealed interface SheetState {
    data object None : SheetState
    data object AddWallet : SheetState
    data class Connect(val request: ConnectRequestUi) : SheetState
    data class Transaction(val request: TransactionRequestUi) : SheetState
    data class SignData(val request: SignDataRequestUi) : SheetState
    data class WalletDetails(val wallet: WalletSummary) : SheetState
    data class SendTransaction(val wallet: WalletSummary) : SheetState
    data class TransactionDetail(val transaction: TransactionDetailUi) : SheetState
    data class Browser(val url: String) : SheetState
    data class JettonDetails(val jetton: io.ton.walletkit.demo.presentation.model.JettonDetails) : SheetState
    data class TransferJetton(val jetton: io.ton.walletkit.demo.presentation.model.JettonDetails) : SheetState
}
