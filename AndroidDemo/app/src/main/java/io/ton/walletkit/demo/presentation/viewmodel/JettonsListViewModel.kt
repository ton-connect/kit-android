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
import io.ton.walletkit.model.TONJettonTransferParams
import io.ton.walletkit.model.TONJettonWallet
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for displaying jettons owned by a wallet.
 * Handles jetton loading, pagination, and transfer operations.
 */
class JettonsListViewModel(
    private val wallet: ITONWallet,
) : ViewModel() {
    private val _state = MutableStateFlow<JettonState>(JettonState.Initial)
    val state: StateFlow<JettonState> = _state.asStateFlow()

    private val _jettons = MutableStateFlow<List<TONJettonWallet>>(emptyList())
    val jettons: StateFlow<List<TONJettonWallet>> = _jettons.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _transferError = MutableStateFlow<String?>(null)
    val transferError: StateFlow<String?> = _transferError.asStateFlow()

    private val _canLoadMore = MutableStateFlow(false)
    val canLoadMore: StateFlow<Boolean> = _canLoadMore.asStateFlow()

    private val limit = 100
    private var loadJob: Job? = null

    init {
        Log.d(TAG, "Created for wallet: ${wallet.address}")
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "Cleared for wallet: ${wallet.address}")
        loadJob?.cancel()
    }

    sealed class JettonState {
        data object Initial : JettonState()
        data object Loading : JettonState()
        data object Empty : JettonState()
        data object Success : JettonState()
        data class Error(val message: String) : JettonState()
    }

    /**
     * Load jettons for the wallet.
     */
    fun loadJettons() {
        if (_state.value != JettonState.Initial) {
            Log.d(TAG, "Skipping loadJettons - state is ${_state.value}, not Initial")
            return
        }

        Log.d(TAG, "Starting loadJettons for wallet: ${wallet.address}")
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                _state.value = JettonState.Loading

                val jettons = wallet.getJettons(limit = limit, offset = 0)

                Log.d(TAG, "Loaded ${jettons.size} jettons")
                jettons.forEachIndexed { index, jettonWallet ->
                    Log.d(TAG, "Jetton[$index]: address=${jettonWallet.jettonAddress}, balance=${jettonWallet.balance}, name=${jettonWallet.jetton?.name}")
                }

                if (jettons.isEmpty()) {
                    _state.value = JettonState.Empty
                    _canLoadMore.value = false
                } else {
                    _canLoadMore.value = jettons.size == limit
                    _jettons.value = jettons
                    _state.value = JettonState.Success
                    Log.d(TAG, "canLoadMore: ${_canLoadMore.value} (got ${jettons.size} items, limit=$limit)")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load jettons", e)
                _state.value = JettonState.Error(e.message ?: "Unknown error")
                _canLoadMore.value = false
            }
        }
    }

    /**
     * Load more jettons with pagination.
     */
    fun loadMoreJettons() {
        if (_state.value != JettonState.Success || _isLoadingMore.value || !_canLoadMore.value) {
            Log.d(TAG, "Skipping loadMoreJettons - state=${_state.value}, isLoadingMore=${_isLoadingMore.value}, canLoadMore=${_canLoadMore.value}")
            return
        }

        val currentOffset = _jettons.value.size
        Log.d(TAG, "Loading more jettons with offset=$currentOffset, limit=$limit")

        viewModelScope.launch {
            try {
                _isLoadingMore.value = true

                val jettons = wallet.getJettons(
                    limit = limit,
                    offset = currentOffset,
                )

                Log.d(TAG, "Loaded ${jettons.size} more jettons")

                _canLoadMore.value = jettons.size == limit
                // Filter duplicates by jetton address
                val existingAddresses = _jettons.value.map { it.jettonAddress }.toSet()
                val newJettons = jettons.filterNot { it.jettonAddress in existingAddresses }
                _jettons.value = _jettons.value + newJettons

                Log.d(TAG, "Added ${newJettons.size} new jettons (filtered ${jettons.size - newJettons.size} duplicates), canLoadMore=${_canLoadMore.value}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load more jettons", e)
                _canLoadMore.value = false
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    /**
     * Refresh jettons from the beginning.
     */
    fun refresh() {
        _state.value = JettonState.Initial
        _jettons.value = emptyList()
        _canLoadMore.value = false
        loadJettons()
    }

    /**
     * Transfer jetton to another address.
     *
     * @param jettonAddress The jetton master contract address
     * @param recipient The recipient wallet address
     * @param amount The amount to transfer (in jetton's smallest units)
     * @param comment Optional comment for the transfer
     */
    fun transferJetton(
        jettonAddress: String,
        recipient: String,
        amount: String,
        comment: String = "",
    ) {
        viewModelScope.launch {
            try {
                _transferError.value = null

                val transferParams = TONJettonTransferParams(
                    toAddress = recipient,
                    jettonAddress = jettonAddress,
                    amount = amount,
                    comment = comment.takeIf { it.isNotBlank() },
                )

                Log.i(TAG, "Creating jetton transfer transaction: jetton=$jettonAddress, to=$recipient, amount=$amount")

                // Create the jetton transfer transaction (step 1)
                val transactionBoc = wallet.createTransferJettonTransaction(transferParams)

                Log.i(TAG, "Jetton transfer transaction created, sending...")

                // Send the transaction (step 2)
                wallet.sendTransaction(transactionBoc)

                Log.i(TAG, "Jetton transfer transaction sent successfully")

                // Refresh jettons after delay to allow blockchain indexing
                delay(5000)
                refresh()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to transfer jetton", e)
                _transferError.value = "Transfer failed: ${e.message}"
            }
        }
    }

    /**
     * Clear transfer error.
     */
    fun clearTransferError() {
        _transferError.value = null
    }

    companion object {
        private const val TAG = "JettonsListViewModel"
    }
}
