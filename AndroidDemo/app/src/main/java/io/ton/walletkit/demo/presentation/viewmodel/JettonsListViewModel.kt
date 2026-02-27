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
import io.ton.walletkit.api.generated.TONJetton
import io.ton.walletkit.api.generated.TONJettonsTransferRequest
import io.ton.walletkit.api.generated.TONNetwork
import io.ton.walletkit.demo.data.api.JettonBalance
import io.ton.walletkit.demo.data.api.TonApiClient
import io.ton.walletkit.model.TONUserFriendlyAddress
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for displaying jettons owned by a wallet.
 * Uses native TON API client for fetching jettons.
 */
class JettonsListViewModel(
    private val wallet: ITONWallet,
    network: TONNetwork = TONNetwork("-239"), // Mainnet by default
) : ViewModel() {
    private val apiClient = TonApiClient(network)

    private val _state = MutableStateFlow<JettonState>(JettonState.Initial)
    val state: StateFlow<JettonState> = _state.asStateFlow()

    private val _jettons = MutableStateFlow<List<JettonBalance>>(emptyList())
    val jettons: StateFlow<List<JettonBalance>> = _jettons.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _transferError = MutableStateFlow<String?>(null)
    val transferError: StateFlow<String?> = _transferError.asStateFlow()

    private val _canLoadMore = MutableStateFlow(false)
    val canLoadMore: StateFlow<Boolean> = _canLoadMore.asStateFlow()

    private var loadJob: Job? = null

    init {
        Log.d(TAG, "Created for wallet: ${wallet.address.value}")
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "Cleared for wallet: ${wallet.address.value}")
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
     * Load jettons for the wallet using native API.
     */
    fun loadJettons() {
        if (_state.value != JettonState.Initial) {
            Log.d(TAG, "Skipping loadJettons - state is ${_state.value}, not Initial")
            return
        }

        Log.d(TAG, "Starting loadJettons for wallet: ${wallet.address.value}")
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                _state.value = JettonState.Loading

                val response = apiClient.getJettons(wallet.address.value)
                val jettons = response.balances

                Log.d(TAG, "Loaded ${jettons.size} jettons via native API")
                jettons.forEachIndexed { index, jetton ->
                    Log.d(TAG, "Jetton[$index]: address=${jetton.walletAddress.address}, balance=${jetton.balance}, name=${jetton.jetton.name}")
                }

                if (jettons.isEmpty()) {
                    _state.value = JettonState.Empty
                    _canLoadMore.value = false
                } else {
                    _canLoadMore.value = false // TON API returns all jettons at once
                    _jettons.value = jettons
                    _state.value = JettonState.Success
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
        // TON API returns all jettons at once, no pagination needed
        Log.d(TAG, "loadMoreJettons called but API returns all at once")
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

                val transferRequest = TONJettonsTransferRequest(
                    jettonAddress = TONUserFriendlyAddress(jettonAddress),
                    recipientAddress = TONUserFriendlyAddress(recipient),
                    transferAmount = amount,
                    comment = comment.takeIf { it.isNotBlank() },
                )

                Log.i(TAG, "Creating jetton transfer transaction: jetton=$jettonAddress, to=$recipient, amount=$amount")

                val transactionRequest = wallet.transferJettonTransaction(transferRequest)
                Log.i(TAG, "Jetton transfer transaction created, sending...")

                wallet.send(transactionRequest)
                Log.i(TAG, "Jetton transfer transaction sent successfully")

                delay(5000)
                refresh()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to transfer jetton", e)
                _transferError.value = "Transfer failed: ${e.message}"
            }
        }
    }

    fun clearTransferError() {
        _transferError.value = null
    }

    companion object {
        private const val TAG = "JettonsListViewModel"
    }
}
