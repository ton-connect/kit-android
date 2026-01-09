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
import io.ton.walletkit.demo.presentation.model.SessionSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing TON Connect sessions.
 * Handles session listing, disconnection, and refresh.
 *
 * NOTE: Session listing from wallets is not currently exposed in the SDK API.
 * Sessions are tracked internally and only exposed through events.
 * This ViewModel provides stubbed implementations for now.
 */
class SessionsViewModel(
    private val getAllWallets: () -> List<ITONWallet>,
    private val getKit: () -> ITONWalletKit,
    private val unknownDAppLabel: String = "Unknown dApp",
) : ViewModel() {

    private val _state = MutableStateFlow(SessionsState())
    val state: StateFlow<SessionsState> = _state.asStateFlow()

    data class SessionsState(
        val sessions: List<SessionSummary> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null,
    )

    init {
        Log.d(TAG, "SessionsViewModel created")
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "SessionsViewModel cleared")
    }

    /**
     * Load sessions from all wallets.
     *
     * TODO: Sessions listing is not currently exposed in the SDK API.
     * The wallet.sessions() method has been removed.
     * Sessions are now tracked internally and managed through events.
     */
    fun loadSessions() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            // TODO: Implement session listing when API is available
            // For now, return empty list
            Log.d(TAG, "Session listing not currently available in SDK API")

            _state.value = _state.value.copy(
                sessions = emptyList(),
                isLoading = false,
                error = null,
            )
        }
    }

    /**
     * Disconnect a specific session.
     */
    fun disconnectSession(sessionId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(error = null)

            try {
                Log.d(TAG, "Disconnecting session: $sessionId")
                val kit = getKit()

                // Use kit.disconnectSession() directly
                kit.disconnectSession(sessionId)
                Log.d(TAG, "Disconnected session $sessionId")

                // Refresh sessions after disconnection
                loadSessions()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to disconnect session $sessionId", e)
                _state.value = _state.value.copy(
                    error = e.message ?: "Failed to disconnect session",
                )

                // Still refresh to update UI
                loadSessions()
            }
        }
    }

    /**
     * Refresh sessions from all wallets.
     */
    fun refresh() {
        loadSessions()
    }

    /**
     * Clear error message.
     */
    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    companion object {
        private const val TAG = "SessionsViewModel"
    }
}
