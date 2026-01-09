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
import io.ton.walletkit.api.generated.TONNFT
import io.ton.walletkit.api.generated.TONNFTsRequest
import io.ton.walletkit.api.generated.TONPagination
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for displaying NFTs owned by a wallet.
 */
class NFTsListViewModel(
    private val wallet: ITONWallet,
) : ViewModel() {
    private val _state = MutableStateFlow<NFTState>(NFTState.Initial)
    val state: StateFlow<NFTState> = _state.asStateFlow()

    private val _nfts = MutableStateFlow<List<TONNFT>>(emptyList())
    val nfts: StateFlow<List<TONNFT>> = _nfts.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _canLoadMore = MutableStateFlow(false)
    val canLoadMore: StateFlow<Boolean> = _canLoadMore.asStateFlow()

    private val limit = 10
    private var loadJob: Job? = null

    init {
        Log.d("NFTsListViewModel", "Created for wallet: ${wallet.address.value}")
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("NFTsListViewModel", "Cleared for wallet: ${wallet.address.value}")
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
            Log.d("NFTsListViewModel", "Skipping loadNFTs - state is ${_state.value}, not Initial")
            return
        }

        Log.d("NFTsListViewModel", "Starting loadNFTs for wallet: ${wallet.address.value}")
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                _state.value = NFTState.Loading

                val request = TONNFTsRequest(pagination = TONPagination(limit = limit, offset = 0))
                val nftsResponse = wallet.nfts(request)
                val nfts = nftsResponse.nfts

                Log.d("NFTsListViewModel", "Loaded ${nfts.size} NFTs")
                nfts.forEachIndexed { index, nft ->
                    Log.d("NFTsListViewModel", "NFT[$index]: address=${nft.address.value}, name=${nft.info?.name}, image=${nft.info?.image?.url}")
                }

                if (nfts.isEmpty()) {
                    _state.value = NFTState.Empty
                    _canLoadMore.value = false
                } else {
                    _canLoadMore.value = nfts.size == limit
                    _nfts.value = nfts
                    _state.value = NFTState.Success
                    Log.d("NFTsListViewModel", "canLoadMore: ${_canLoadMore.value} (got ${nfts.size} items, limit=$limit)")
                }
            } catch (e: Exception) {
                Log.e("NFTsListViewModel", "Failed to load NFTs", e)
                _state.value = NFTState.Error(e.message ?: "Unknown error")
                _canLoadMore.value = false
            }
        }
    }

    fun loadMoreNFTs() {
        if (_state.value != NFTState.Success || _isLoadingMore.value || !_canLoadMore.value) {
            Log.d("NFTsListViewModel", "Skipping loadMoreNFTs - state=${_state.value}, isLoadingMore=${_isLoadingMore.value}, canLoadMore=${_canLoadMore.value}")
            return
        }

        val currentOffset = _nfts.value.size
        Log.d("NFTsListViewModel", "Loading more NFTs with offset=$currentOffset, limit=$limit")
        viewModelScope.launch {
            try {
                _isLoadingMore.value = true

                val request = TONNFTsRequest(pagination = TONPagination(limit = limit, offset = currentOffset))
                val nftsResponse = wallet.nfts(request)
                val nfts = nftsResponse.nfts

                Log.d("NFTsListViewModel", "Loaded ${nfts.size} more NFTs")

                _canLoadMore.value = nfts.size == limit
                // Only add NFTs that don't already exist (filter duplicates by address)
                val existingAddresses = _nfts.value.map { it.address.value }.toSet()
                val newNfts = nfts.filterNot { it.address.value in existingAddresses }
                _nfts.value = _nfts.value + newNfts

                Log.d("NFTsListViewModel", "Added ${newNfts.size} new NFTs (filtered ${nfts.size - newNfts.size} duplicates), canLoadMore=${_canLoadMore.value}")
            } catch (e: Exception) {
                Log.e("NFTsListViewModel", "Failed to load more NFTs", e)
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
}
