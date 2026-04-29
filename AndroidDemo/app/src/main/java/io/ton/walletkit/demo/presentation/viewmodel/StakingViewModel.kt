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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.ton.walletkit.ITONWallet
import io.ton.walletkit.ITONWalletKit
import io.ton.walletkit.api.generated.TONNetwork
import io.ton.walletkit.api.generated.TONStakeParams
import io.ton.walletkit.api.generated.TONStakingBalance
import io.ton.walletkit.api.generated.TONStakingProviderInfo
import io.ton.walletkit.api.generated.TONStakingQuote
import io.ton.walletkit.api.generated.TONStakingQuoteDirection
import io.ton.walletkit.api.generated.TONStakingQuoteParams
import io.ton.walletkit.api.generated.TONUnstakeMode
import io.ton.walletkit.demo.BuildConfig
import io.ton.walletkit.demo.core.DemoApiConfig
import io.ton.walletkit.staking.ITONStakingManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement

/**
 * ViewModel for staking operations (stake, unstake, balance, provider info).
 *
 * Mirrors the iOS `StakingViewModel` for cross-platform consistency.
 *
 * Usage:
 * ```kotlin
 * val vm = StakingViewModel(wallet = activeWallet, kit = ITONWalletKit instance)
 * vm.load()
 * vm.setAmount("1.5")
 * vm.getQuote()
 * vm.executeStake()
 * ```
 */
class StakingViewModel(
    private val wallet: ITONWallet,
    private val kit: ITONWalletKit,
    private val network: TONNetwork,
) : ViewModel() {

    data class State(
        val amount: String = "",
        val direction: TONStakingQuoteDirection = TONStakingQuoteDirection.stake,
        val unstakeMode: TONUnstakeMode = TONUnstakeMode.instant,
        val currentQuote: TONStakingQuote? = null,
        val isLoadingQuote: Boolean = false,
        val isExecuting: Boolean = false,
        val error: String? = null,
        val availableBalanceNano: String? = null,
        val stakedBalance: TONStakingBalance? = null,
        val providerInfo: TONStakingProviderInfo? = null,
        val supportedModes: List<TONUnstakeMode> = listOf(
            TONUnstakeMode.instant,
            TONUnstakeMode.whenAvailable,
            TONUnstakeMode.roundEnd,
        ),
    ) {
        val inputTokenSymbol: String
            get() = if (direction == TONStakingQuoteDirection.stake) "TON" else "tsTON"

        val receiveTokenSymbol: String
            get() = if (direction == TONStakingQuoteDirection.stake) "tsTON" else "TON"

        val canGetQuote: Boolean
            get() = amount.isNotEmpty() && (amount.toDoubleOrNull() ?: 0.0) > 0 && !isLoadingQuote

        val buttonTitle: String
            get() = if (currentQuote != null) {
                if (direction == TONStakingQuoteDirection.stake) "Confirm Stake" else "Confirm Unstake"
            } else {
                if (direction == TONStakingQuoteDirection.stake) "Preview Stake" else "Preview Unstake"
            }

        /** APY formatted as a percentage string (e.g. "5.42%"), or null if not loaded. */
        val formattedAPY: String?
            get() = providerInfo?.apy?.let { String.format("%.2f%%", it) }
    }

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    /** Lazily initialised once [load] is called; reused across operations. */
    private var stakingManager: ITONStakingManager? = null
    private var loadJob: Job? = null

    // ── Public API ──

    fun setAmount(value: String) {
        if (value.isNotEmpty() && value.toDoubleOrNull() == null) return
        _state.update { it.copy(amount = value, currentQuote = null, error = null) }
    }

    fun setDirection(direction: TONStakingQuoteDirection) {
        _state.update { it.copy(direction = direction, amount = "", currentQuote = null, error = null) }
    }

    fun setUnstakeMode(mode: TONUnstakeMode) {
        _state.update { it.copy(unstakeMode = mode, currentQuote = null, error = null) }
    }

    /** Fetch a stake/unstake quote for the current [State.amount] and [State.direction]. */
    fun getQuote() {
        val s = _state.value
        if (!s.canGetQuote) return

        _state.update { it.copy(isLoadingQuote = true, error = null) }

        viewModelScope.launch {
            runCatching {
                val manager = getOrInitStakingManager()
                manager.getQuote(
                    TONStakingQuoteParams<JsonElement>(
                        direction = s.direction,
                        amount = s.amount,
                        userAddress = wallet.address,
                        network = network,
                        unstakeMode = if (s.direction == TONStakingQuoteDirection.unstake) s.unstakeMode else null,
                    ),
                )
            }.onSuccess { quote ->
                _state.update { it.copy(currentQuote = quote, isLoadingQuote = false) }
            }.onFailure { e ->
                Log.e(TAG, "Failed to get quote", e)
                _state.update { it.copy(error = e.message, isLoadingQuote = false) }
            }
        }
    }

    /**
     * Execute the stake/unstake transaction from [State.currentQuote].
     * Requires a previously obtained quote.
     */
    fun executeStake() {
        val quote = _state.value.currentQuote ?: return
        if (_state.value.isExecuting) return

        _state.update { it.copy(isExecuting = true, error = null) }

        viewModelScope.launch {
            runCatching {
                val manager = getOrInitStakingManager()
                val tx = manager.buildStakeTransaction(
                    TONStakeParams<JsonElement>(
                        quote = quote,
                        userAddress = wallet.address,
                    ),
                )
                wallet.send(tx)
            }.onSuccess {
                val previousAvailableBalanceNano = _state.value.availableBalanceNano
                val previousStakedBalance = _state.value.stakedBalance?.stakedBalance
                _state.update { it.copy(isExecuting = false, currentQuote = null, amount = "") }
                refreshBalancesAfterTransaction(previousAvailableBalanceNano, previousStakedBalance)
            }.onFailure { e ->
                Log.e(TAG, "Failed to execute stake", e)
                _state.update { it.copy(isExecuting = false, error = e.message) }
            }
        }
    }

    /**
     * Trigger the primary action button:
     * - If a quote is pending → execute it.
     * - Otherwise → fetch a new quote.
     */
    fun buttonAction() {
        if (_state.value.currentQuote != null) executeStake() else getQuote()
    }

    /** Discard the current quote without executing. */
    fun cancelQuote() {
        _state.update { it.copy(currentQuote = null, error = null) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    /**
     * Initialise the staking manager and load current balance / provider info.
     * Call this when the screen first appears.
     */
    fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            runCatching { getOrInitStakingManager() }
                .onSuccess { loadStakingData() }
                .onFailure { e ->
                    Log.e(TAG, "Failed to initialize staking manager", e)
                    _state.update { it.copy(error = e.message) }
                }
        }
    }

    // ── Private helpers ──

    private suspend fun loadStakingData() {
        refreshBalances()
        refreshProviderInfo()
    }

    private suspend fun refreshBalancesAfterTransaction(
        previousAvailableBalanceNano: String?,
        previousStakedBalance: String?,
    ) {
        repeat(6) { attempt ->
            refreshBalances(previousAvailableBalanceNano, previousStakedBalance)
            if (attempt < 5) {
                delay(1_500)
            }
        }
        refreshProviderInfo()
    }

    private suspend fun refreshBalances(
        previousAvailableBalanceNano: String? = _state.value.availableBalanceNano,
        previousStakedBalance: String? = _state.value.stakedBalance?.stakedBalance,
    ): Boolean {
        val manager = stakingManager ?: return false
        return runCatching {
            val walletBalanceNano = wallet.balance().value
            val balance = manager.getStakedBalance(
                userAddress = wallet.address,
                network = network,
            )
            _state.update {
                it.copy(
                    availableBalanceNano = walletBalanceNano,
                    stakedBalance = balance,
                )
            }
            walletBalanceNano != previousAvailableBalanceNano || balance.stakedBalance != previousStakedBalance
        }.onFailure { e ->
            val message =
                if (DemoApiConfig.toncenterApiKey.isEmpty()) {
                    "Configure walletkitToncenterApiKey in local.properties, then rebuild the demo app."
                } else if (BuildConfig.TONAPI_API_KEY.trim().isNotEmpty() && DemoApiConfig.tonApiKey.isEmpty()) {
                    "walletkitTonApiKey is a placeholder value. Set a real TonAPI key or leave it blank, then rebuild the demo app."
                } else {
                    e.message ?: "Failed to load staking data"
                }
            Log.w(TAG, "Failed to load staking data: ${e.message}")
            _state.update { it.copy(error = message) }
        }.getOrDefault(false)
    }

    private suspend fun refreshProviderInfo() {
        val manager = stakingManager ?: return
        runCatching {
            val info = manager.getStakingProviderInfo(network = network)
            _state.update { it.copy(providerInfo = info) }
        }.onFailure { e ->
            Log.w(TAG, "Failed to load staking provider info: ${e.message}")
        }
    }

    private suspend fun getOrInitStakingManager(): ITONStakingManager {
        stakingManager?.let { return it }

        val manager = kit.staking()
        val provider = kit.tonStakersStakingProvider(DemoApiConfig.tonStakersProviderConfig())
        manager.register(provider)
        manager.setDefaultProvider(provider.identifier)

        runCatching {
            val modes = manager.getSupportedUnstakeModes()
            _state.update { it.copy(supportedModes = modes) }
        }

        stakingManager = manager
        return manager
    }

    companion object {
        private const val TAG = "StakingViewModel"

        fun factory(
            wallet: ITONWallet,
            kit: ITONWalletKit,
            network: TONNetwork,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = StakingViewModel(wallet, kit, network) as T
        }
    }
}
