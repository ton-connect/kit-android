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
import io.ton.walletkit.demo.core.TONWalletKitHelper
import io.ton.walletkit.demo.presentation.model.SessionSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * ViewModel for managing TON Connect sessions.
 * Handles session listing, disconnection, and refresh.
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

    fun loadSessions() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            runCatching {
                val walletAddresses = getAllWallets().map { it.address.value }.toSet()
                val manager = TONWalletKitHelper.sessionManager
                if (manager == null) {
                    Log.d(TAG, "Custom session manager not enabled; returning empty session list")
                    emptyList()
                } else {
                    manager.getSessions()
                        .filter { it.walletAddress.value in walletAddresses }
                        .sortedByDescending { parseTimestamp(it.lastActivityAt) ?: 0L }
                        .map { session ->
                            SessionSummary(
                                sessionId = session.sessionId,
                                dAppName = session.dAppName?.takeIf { it.isNotBlank() } ?: unknownDAppLabel,
                                walletAddress = session.walletAddress.value,
                                dAppUrl = session.dAppUrl,
                                manifestUrl = null,
                                iconUrl = session.dAppIconUrl,
                                createdAt = parseTimestamp(session.createdAt),
                                lastActivity = parseTimestamp(session.lastActivityAt),
                            )
                        }
                }
            }.onSuccess { sessions ->
                Log.d(TAG, "Loaded ${sessions.size} sessions")
                _state.value = _state.value.copy(
                    sessions = sessions,
                    isLoading = false,
                    error = null,
                )
            }.onFailure { error ->
                Log.e(TAG, "Failed to load sessions", error)
                _state.value = _state.value.copy(
                    sessions = emptyList(),
                    isLoading = false,
                    error = error.message ?: "Failed to load sessions",
                )
            }
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
                val manager = TONWalletKitHelper.sessionManager

                kit.disconnectSession(sessionId)
                runCatching { manager?.removeSession(sessionId) }
                    .onFailure { cleanupError ->
                        Log.w(TAG, "Session disconnected but local cleanup failed for $sessionId", cleanupError)
                    }
                Log.d(TAG, "Disconnected session $sessionId")

                loadSessions()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to disconnect session $sessionId", e)
                _state.value = _state.value.copy(
                    error = e.message ?: "Failed to disconnect session",
                )

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

    private fun parseTimestamp(value: String?): Long? = runCatching {
        value?.let { Instant.parse(it).toEpochMilli() }
    }.getOrNull()
}
