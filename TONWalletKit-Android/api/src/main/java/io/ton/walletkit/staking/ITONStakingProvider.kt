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
package io.ton.walletkit.staking

import io.ton.walletkit.TONProviderType
import io.ton.walletkit.api.generated.TONNetwork
import io.ton.walletkit.api.generated.TONStakeParams
import io.ton.walletkit.api.generated.TONStakingBalance
import io.ton.walletkit.api.generated.TONStakingProviderInfo
import io.ton.walletkit.api.generated.TONStakingQuote
import io.ton.walletkit.api.generated.TONStakingQuoteParams
import io.ton.walletkit.api.generated.TONTransactionRequest
import io.ton.walletkit.api.generated.TONUnstakeMode
import io.ton.walletkit.model.TONUserFriendlyAddress

/**
 * Contract every staking provider must satisfy. Mirrors iOS's `TONStakingProviderProtocol`.
 *
 * The SDK's built-in TonStakers provider implements this interface. Custom providers implement it directly.
 *
 * [TQuoteOptions] is the provider-specific options type for [getQuote];
 * [TStakeOptions] is the provider-specific options type for [buildStakeTransaction].
 */
interface ITONStakingProvider<TQuoteOptions, TStakeOptions> {
    /** Always [TONProviderType.Staking]. Used to discriminate at runtime across domains. */
    val type: TONProviderType get() = TONProviderType.Staking

    /** Typed provider identifier. */
    val identifier: TONStakingProviderIdentifier<TQuoteOptions, TStakeOptions>

    /** Get a stake or unstake quote from this provider. */
    suspend fun getQuote(params: TONStakingQuoteParams<TQuoteOptions>): TONStakingQuote

    /** Build a stake or unstake transaction using this provider. */
    suspend fun buildStakeTransaction(params: TONStakeParams<TStakeOptions>): TONTransactionRequest

    /** Get the user's staked balance for this provider. */
    suspend fun getStakedBalance(
        userAddress: TONUserFriendlyAddress,
        network: TONNetwork? = null,
    ): TONStakingBalance

    /** Get general info (APY, instant-unstake liquidity) for this provider. */
    suspend fun getStakingProviderInfo(network: TONNetwork? = null): TONStakingProviderInfo

    /** Unstake modes supported by this provider. */
    suspend fun getSupportedUnstakeModes(): List<TONUnstakeMode>
}
