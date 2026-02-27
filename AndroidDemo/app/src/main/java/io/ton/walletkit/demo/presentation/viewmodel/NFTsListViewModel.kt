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
import io.ton.walletkit.api.generated.TONNetwork
import io.ton.walletkit.demo.data.api.TonApiClient
import io.ton.walletkit.demo.presentation.model.NFTSummary
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for displaying NFTs owned by a wallet.
 * Uses native TON API client for fetching NFTs.
 */
class NFTsListViewModel(
    private val walletAddress: String,
    network: TONNetwork = TONNetwork("-239"), // Mainnet by default
) : ViewModel() {
    private val apiClient = TonApiClient(network)

    private val _state = MutableStateFlow<NFTState>(NFTState.Initial)
    val state: StateFlow<NFTState> = _state.asStateFlow()

    private val _nfts = MutableStateFlow<List<NFTSummary>>(emptyList())
    val nfts: StateFlow<List<NFTSummary>> = _nfts.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _canLoadMore = MutableStateFlow(false)
    val canLoadMore: StateFlow<Boolean> = _canLoadMore.asStateFlow()

    private val limit = 50
    private var currentOffset = 0
    private var loadJob: Job? = null

    init {
        Log.d(TAG, "Created for wallet: $walletAddress")
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "Cleared for wallet: $walletAddress")
        loadJob?.cancel()
    }

    sealed class NFTState {
        data object Initial : NFTState()
        data object Loading : NFTState()
        data object Empty : NFTState()
        data object Success : NFTState()
        data class Error(val message: String) : NFTState()
    }

    fun loadNFTs() {
        if (_state.value != NFTState.Initial) {
            Log.d(TAG, "Skipping loadNFTs - state is ${_state.value}, not Initial")
            return
        }

        Log.d(TAG, "Starting loadNFTs for wallet: $walletAddress")
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                _state.value = NFTState.Loading

                val response = apiClient.getNfts(walletAddress, limit = limit, offset = 0)
                val nfts = response.nftItems

                Log.d(TAG, "Loaded ${nfts.size} NFTs via native API")
                nfts.forEachIndexed { index, nft ->
                    val imageUrl = nft.previews?.firstOrNull()?.url ?: nft.metadata?.image
                    Log.d(TAG, "NFT[$index]: address=${nft.address}, name=${nft.metadata?.name}, image=$imageUrl")
                }

                if (nfts.isEmpty()) {
                    _state.value = NFTState.Empty
                    _canLoadMore.value = false
                } else {
                    _canLoadMore.value = nfts.size == limit
                    currentOffset = nfts.size
                    _nfts.value = nfts.map { NFTSummary.from(it) }
                    _state.value = NFTState.Success
                    Log.d(TAG, "canLoadMore: ${_canLoadMore.value} (got ${nfts.size} items, limit=$limit)")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load NFTs", e)
                _state.value = NFTState.Error(e.message ?: "Unknown error")
                _canLoadMore.value = false
            }
        }
    }

    fun loadMoreNFTs() {
        if (_state.value != NFTState.Success || _isLoadingMore.value || !_canLoadMore.value) {
            Log.d(TAG, "Skipping loadMoreNFTs - state=${_state.value}, isLoadingMore=${_isLoadingMore.value}, canLoadMore=${_canLoadMore.value}")
            return
        }

        val offset = _nfts.value.size
        Log.d(TAG, "Loading more NFTs with offset=$offset, limit=$limit")
        viewModelScope.launch {
            try {
                _isLoadingMore.value = true

                val response = apiClient.getNfts(walletAddress, limit = limit, offset = offset)
                val nfts = response.nftItems

                Log.d(TAG, "Loaded ${nfts.size} more NFTs")

                _canLoadMore.value = nfts.size == limit
                val existingAddresses = _nfts.value.map { it.address }.toSet()
                val newNftSummaries = nfts.map { NFTSummary.from(it) }
                    .filterNot { it.address in existingAddresses }
                _nfts.value = _nfts.value + newNftSummaries
                currentOffset = offset + nfts.size

                Log.d(TAG, "Added ${newNftSummaries.size} new NFTs, canLoadMore=${_canLoadMore.value}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load more NFTs", e)
                _canLoadMore.value = false
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    fun refresh() {
        _state.value = NFTState.Initial
        _nfts.value = emptyList()
        _canLoadMore.value = false
        loadNFTs()
    }

    companion object {
        private const val TAG = "NFTsListViewModel"
    }
}
