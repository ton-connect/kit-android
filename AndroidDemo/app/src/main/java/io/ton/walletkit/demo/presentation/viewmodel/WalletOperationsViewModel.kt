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

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.ton.walletkit.ITONWallet
import io.ton.walletkit.ITONWalletKit
import io.ton.walletkit.demo.presentation.util.TonFormatter
import io.ton.walletkit.model.TONTransferParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for wallet operations like switching wallets and sending local transactions.
 * Handles operations that require an active wallet context.
 */
class WalletOperationsViewModel(
    private val walletKit: () -> ITONWalletKit,
    private val getWalletByAddress: (String) -> ITONWallet?,
    private val onWalletSwitched: (String) -> Unit = {},
    private val onTransactionInitiated: (String) -> Unit = {},
) : ViewModel() {

    private val _state = MutableStateFlow(WalletOperationsState())
    val state: StateFlow<WalletOperationsState> = _state.asStateFlow()

    data class WalletOperationsState(
        val activeWalletAddress: String? = null,
        val isSendingTransaction: Boolean = false,
        val error: String? = null,
        val successMessage: String? = null,
    )

    /**
     * Switch to a different wallet.
     */
    fun switchWallet(address: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(error = null)

            val wallet = getWalletByAddress(address)
            if (wallet == null) {
                _state.value = _state.value.copy(error = "Wallet not found")
                return@launch
            }

            _state.value = _state.value.copy(activeWalletAddress = address)
            onWalletSwitched(address)

            Log.d(TAG, "Switched to wallet: $address")
        }
    }

    /**
     * Send a local transaction (TON transfer).
     * This creates a transaction request that will trigger the approval flow.
     *
     * This uses the standard JS WalletKit API:
     * 1. wallet.createTransferTonTransaction(params) - creates transaction content
     * 2. kit.handleNewTransaction(wallet, transaction) - triggers approval flow
     *
     * @param walletAddress The sender wallet address
     * @param recipient The recipient address
     * @param amount The amount in TON (will be converted to nanoTON)
     * @param comment Optional comment for the transaction
     */
    fun sendLocalTransaction(
        walletAddress: String,
        recipient: String,
        amount: String,
        comment: String = "",
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSendingTransaction = true, error = null)

            val wallet = getWalletByAddress(walletAddress)
            if (wallet == null) {
                _state.value = _state.value.copy(
                    isSendingTransaction = false,
                    error = "Wallet not found",
                )
                return@launch
            }

            // Convert TON to nanoTON
            val amountInNano = try {
                TonFormatter.tonToNano(amount)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isSendingTransaction = false,
                    error = "Invalid amount: ${e.message}",
                )
                return@launch
            }

            runCatching {
                // Step 1: Create transaction content
                val params = TONTransferParams(
                    toAddress = recipient,
                    amount = amountInNano,
                    comment = comment.takeIf { it.isNotBlank() },
                )
                val transactionContent = wallet.createTransferTonTransaction(params)

                // Step 2: Trigger approval flow
                walletKit().handleNewTransaction(wallet, transactionContent)
            }.onSuccess {
                _state.value = _state.value.copy(
                    isSendingTransaction = false,
                    successMessage = "Transaction initiated",
                )
                onTransactionInitiated(walletAddress)
                Log.d(TAG, "Local transaction initiated from $walletAddress")
            }.onFailure { error ->
                Log.e(TAG, "Failed to send local transaction", error)
                _state.value = _state.value.copy(
                    isSendingTransaction = false,
                    error = error.message ?: "Failed to send transaction",
                )
            }
        }
    }

    /**
     * Clear error or success message.
     */
    fun clearMessage() {
        _state.value = _state.value.copy(error = null, successMessage = null)
    }

    companion object {
        private const val TAG = "WalletOperationsVM"
    }
}
