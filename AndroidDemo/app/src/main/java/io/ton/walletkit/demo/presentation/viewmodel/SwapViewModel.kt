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
import io.ton.walletkit.api.MAINNET
import io.ton.walletkit.api.generated.TONNetwork
import io.ton.walletkit.api.generated.TONSwapParams
import io.ton.walletkit.api.generated.TONSwapQuote
import io.ton.walletkit.api.generated.TONSwapQuoteParams
import io.ton.walletkit.api.generated.TONSwapToken
import io.ton.walletkit.swap.ITONSwapManager
import io.ton.walletkit.swap.TONDeDustSwapProvider
import io.ton.walletkit.swap.TONOmnistonSwapProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement

class SwapViewModel(
    private val wallet: ITONWallet,
    private val kit: ITONWalletKit,
) : ViewModel() {

    enum class SwapProvider(val displayName: String) {
        OMNISTON("Omniston"),
        DEDUST("DeDust"),
    }

    data class UiState(
        val fromToken: TONSwapToken = TONSwapToken(address = "ton", decimals = 9.0, name = "TON", symbol = "TON"),
        val toToken: TONSwapToken = TONSwapToken(
            address = "EQCxE6mUtQJKFnGfaROTKOt1lZbDiiX1kCixRv7Nw2Id_sDs",
            decimals = 6.0,
            name = "USDT",
            symbol = "USDT",
        ),
        val fromAmount: String = "",
        val toAmount: String = "",
        val isReverseSwap: Boolean = false,
        val selectedProvider: SwapProvider = SwapProvider.OMNISTON,
        val currentQuote: TONSwapQuote? = null,
        val isLoadingQuote: Boolean = false,
        val isSwapping: Boolean = false,
        val error: String? = null,
        val slippageBps: Int = 100,
        val registeredProviders: List<String> = emptyList(),
    ) {
        val fromSymbol: String get() = fromToken.symbol ?: "???"
        val toSymbol: String get() = toToken.symbol ?: "???"
        val canGetQuote: Boolean get() = fromAmount.isNotEmpty() && (fromAmount.toDoubleOrNull() ?: 0.0) > 0 && !isLoadingQuote
        val canSwap: Boolean get() = currentQuote != null && !isSwapping && !isLoadingQuote
        val buttonTitle: String get() = if (currentQuote != null) "Swap $fromSymbol for $toSymbol" else "Get Quote"
        val priceImpactLevel: PriceImpactLevel get() {
            val impact = currentQuote?.priceImpact ?: return PriceImpactLevel.LOW
            return when {
                impact > 500 -> PriceImpactLevel.HIGH
                impact > 200 -> PriceImpactLevel.MEDIUM
                else -> PriceImpactLevel.LOW
            }
        }
    }

    enum class PriceImpactLevel { LOW, MEDIUM, HIGH }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private data class SwapResources(
        val manager: ITONSwapManager,
        val omnistonProvider: TONOmnistonSwapProvider,
        val deDustProvider: TONDeDustSwapProvider,
    )

    private var swapResources: SwapResources? = null

    fun setFromAmount(value: String) {
        if (value.isNotEmpty() && value.toDoubleOrNull() == null) return
        _state.update { it.copy(fromAmount = value, isReverseSwap = false, currentQuote = null, error = null) }
    }

    fun setToAmount(value: String) {
        if (value.isNotEmpty() && value.toDoubleOrNull() == null) return
        _state.update { it.copy(toAmount = value, isReverseSwap = true, currentQuote = null, error = null) }
    }

    fun setSlippageBps(bps: Int) {
        _state.update { it.copy(slippageBps = bps) }
    }

    fun setProvider(provider: SwapProvider) {
        _state.update { it.copy(selectedProvider = provider, currentQuote = null, error = null) }
        swapResources?.let { resources ->
            viewModelScope.launch {
                val newDefault = if (provider == SwapProvider.OMNISTON) resources.omnistonProvider else resources.deDustProvider
                resources.manager.setDefaultProvider(newDefault.identifier)
            }
        }
    }

    fun swapTokens() {
        _state.update { current ->
            current.copy(
                fromToken = current.toToken,
                toToken = current.fromToken,
                fromAmount = "",
                toAmount = "",
                currentQuote = null,
                error = null,
            )
        }
    }

    fun buttonAction() {
        if (_state.value.currentQuote != null) {
            executeSwap()
        } else {
            getQuote()
        }
    }

    fun getQuote() {
        val current = _state.value
        val amount = if (current.isReverseSwap) current.toAmount else current.fromAmount
        if (amount.isEmpty() || (amount.toDoubleOrNull() ?: 0.0) <= 0) return

        _state.update { it.copy(isLoadingQuote = true, error = null) }

        viewModelScope.launch {
            runCatching {
                val manager = getSwapResources().manager
                manager.getQuote(
                    TONSwapQuoteParams<JsonElement>(
                        amount = amount,
                        from = current.fromToken,
                        to = current.toToken,
                        network = TONNetwork.MAINNET,
                        slippageBps = current.slippageBps.toDouble(),
                        maxOutgoingMessages = 4.0,
                        isReverseSwap = current.isReverseSwap,
                    ),
                )
            }.onSuccess { quote ->
                _state.update { current ->
                    val updatedFromAmount = if (current.isReverseSwap) quote.fromAmount else current.fromAmount
                    val updatedToAmount = if (current.isReverseSwap) current.toAmount else quote.toAmount
                    current.copy(
                        currentQuote = quote,
                        fromAmount = updatedFromAmount,
                        toAmount = updatedToAmount,
                        isLoadingQuote = false,
                    )
                }
            }.onFailure { error ->
                Log.e(TAG, "getQuote failed", error)
                _state.update { it.copy(isLoadingQuote = false, error = error.message ?: "Failed to get quote") }
            }
        }
    }

    fun executeSwap() {
        val current = _state.value
        val quote = current.currentQuote ?: return
        if (current.isSwapping) return

        _state.update { it.copy(isSwapping = true, error = null) }

        viewModelScope.launch {
            runCatching {
                val manager = getSwapResources().manager
                val transactionRequest = manager.buildSwapTransaction(
                    TONSwapParams<JsonElement>(
                        quote = quote,
                        userAddress = wallet.address,
                    ),
                )
                wallet.send(transactionRequest)
            }.onSuccess {
                _state.update {
                    it.copy(
                        isSwapping = false,
                        currentQuote = null,
                        fromAmount = "",
                        toAmount = "",
                    )
                }
            }.onFailure { error ->
                Log.e(TAG, "executeSwap failed", error)
                _state.update { it.copy(isSwapping = false, error = error.message ?: "Swap failed") }
            }
        }
    }

    private suspend fun getSwapResources(): SwapResources {
        swapResources?.let { return it }

        val manager = kit.swap()

        val omniston = kit.omnistonSwapProvider()
        manager.registerProvider(omniston)

        val deDust = kit.dedustSwapProvider()
        manager.registerProvider(deDust)

        val defaultProvider = if (_state.value.selectedProvider == SwapProvider.OMNISTON) omniston else deDust
        manager.setDefaultProvider(defaultProvider.identifier)

        // Verify both providers are registered and update UI with their IDs
        Log.d(TAG, "hasOmniston=${manager.hasProvider(omniston.identifier)}, hasDeDust=${manager.hasProvider(deDust.identifier)}")
        val providerIds = manager.registeredProviders().map { it.name }
        _state.update { it.copy(registeredProviders = providerIds) }

        return SwapResources(manager, omniston, deDust).also { swapResources = it }
    }

    companion object {
        private const val TAG = "SwapViewModel"
    }
}
