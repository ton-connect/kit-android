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
import io.ton.walletkit.demo.data.cache.TransactionCache
import io.ton.walletkit.demo.presentation.util.TransactionDiffUtil
import io.ton.walletkit.model.Transaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing transaction history for a wallet.
 * Handles loading, caching, and refreshing transactions.
 */
class TransactionHistoryViewModel(
    private val wallet: ITONWallet,
    private val transactionCache: TransactionCache,
) : ViewModel() {
    private val _state = MutableStateFlow<TransactionState>(TransactionState.Initial)
    val state: StateFlow<TransactionState> = _state.asStateFlow()

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    init {
        Log.d(TAG, "Created for wallet: ${wallet.address?.value}")
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "Cleared for wallet: ${wallet.address}")
    }

    sealed class TransactionState {
        data object Initial : TransactionState()
        data object Loading : TransactionState()
        data object Empty : TransactionState()
        data object Success : TransactionState()
        data class Error(val message: String) : TransactionState()
    }

    /**
     * Load transactions for the wallet.
     * First shows cached transactions for immediate display, then fetches fresh data.
     */
    fun loadTransactions(limit: Int = DEFAULT_LIMIT) {
        viewModelScope.launch {
            try {
                val walletAddress = wallet.address?.value ?: run {
                    _state.value = TransactionState.Error("Wallet address is null")
                    return@launch
                }

                _state.value = TransactionState.Loading

                // TODO: Transaction history is temporarily disabled - Coming Soon
                // Try to get cached transactions first for immediate display
                val cachedTransactions = transactionCache.get(walletAddress)
                if (cachedTransactions != null && cachedTransactions.isNotEmpty()) {
                    Log.d(TAG, "Using cached transactions: ${cachedTransactions.size} items")
                    _transactions.value = cachedTransactions
                    _state.value = TransactionState.Success
                }

                // Fetch fresh transactions from network
                // Temporarily disabled - getRecentTransactions has been removed
                Log.d(TAG, "Transaction fetching temporarily disabled - Coming Soon")
                val freshTransactions = emptyList<Transaction>()

                // Merge with cache
                val mergedTransactions = transactionCache.update(walletAddress, freshTransactions)

                // Calculate diff for logging
                if (cachedTransactions != null && cachedTransactions.isNotEmpty()) {
                    val newItems = TransactionDiffUtil.getNewTransactions(cachedTransactions, mergedTransactions)
                    if (newItems.isNotEmpty()) {
                        Log.d(TAG, "Found ${newItems.size} new transactions")
                    }
                }

                _transactions.value = mergedTransactions

                if (mergedTransactions.isEmpty()) {
                    _state.value = TransactionState.Empty
                } else {
                    _state.value = TransactionState.Success
                }

                Log.d(TAG, "Loaded ${mergedTransactions.size} total transactions")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load transactions", e)
                _state.value = TransactionState.Error(e.message ?: "Failed to load transactions")
            }
        }
    }

    /**
     * Refresh transactions from the network.
     */
    fun refresh() {
        _state.value = TransactionState.Initial
        loadTransactions()
    }

    /**
     * Clear cached transactions for this wallet.
     */
    fun clearCache() {
        viewModelScope.launch {
            val walletAddress = wallet.address?.value ?: run {
                _state.value = TransactionState.Error("Wallet address is null")
                return@launch
            }
            transactionCache.clear(walletAddress)
            _transactions.value = emptyList()
            _state.value = TransactionState.Initial
        }
    }

    companion object {
        private const val TAG = "TransactionHistoryVM"
        private const val DEFAULT_LIMIT = 20
    }
}
