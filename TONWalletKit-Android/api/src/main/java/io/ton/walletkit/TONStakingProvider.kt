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
package io.ton.walletkit

import io.ton.walletkit.api.generated.TONNetwork
import io.ton.walletkit.api.generated.TONStakeParams
import io.ton.walletkit.api.generated.TONStakingBalance
import io.ton.walletkit.api.generated.TONStakingProviderInfo
import io.ton.walletkit.api.generated.TONStakingQuote
import io.ton.walletkit.api.generated.TONStakingQuoteParams
import io.ton.walletkit.api.generated.TONTransactionRequest
import io.ton.walletkit.api.generated.TONUnstakeMode
import io.ton.walletkit.model.TONUserFriendlyAddress
import kotlinx.serialization.json.JsonElement

/**
 * A registered staking provider handle — typed bundle of identifier + manager reference.
 * [TQuoteOptions] carries the provider-specific options for [getQuote];
 * [TStakeOptions] carries the provider-specific options for [buildStakeTransaction].
 *
 * Typically obtained via [ITONWalletKit.tonStakersStakingProvider] or via
 * [ITONStakingManager.provider]. Register with [ITONStakingManager.register] before use.
 */
class TONStakingProvider<TQuoteOptions, TStakeOptions>(
    override val identifier: TONStakingProviderIdentifier<TQuoteOptions, TStakeOptions>,
    private val manager: ITONStakingManager,
) : ITONStakingProvider<TQuoteOptions, TStakeOptions> {
    /** Get a quote from this provider. Delegates to [ITONStakingManager.getQuote] with this provider's [identifier]. */
    override suspend fun getQuote(params: TONStakingQuoteParams<TQuoteOptions>): TONStakingQuote =
        manager.getQuote(params, identifier)

    /** Build a stake or unstake transaction using this provider. Delegates to [ITONStakingManager.buildStakeTransaction] with this provider's [identifier]. */
    override suspend fun buildStakeTransaction(params: TONStakeParams<TStakeOptions>): TONTransactionRequest =
        manager.buildStakeTransaction(params, identifier)

    /** Get the user's staked balance for this provider. Mirrors iOS `TONStakingProvider.stakedBalance(userAddress:network:)`. */
    override suspend fun getStakedBalance(
        userAddress: TONUserFriendlyAddress,
        network: TONNetwork?,
    ): TONStakingBalance = manager.getStakedBalance(userAddress, network, identifier)

    /** Get this provider's general info (APY, instant-unstake liquidity). Mirrors iOS `TONStakingProvider.stakingProviderInfo(network:)`. */
    override suspend fun getStakingProviderInfo(
        network: TONNetwork?,
    ): TONStakingProviderInfo = manager.getStakingProviderInfo(network, identifier)

    /** Get the unstake modes supported by this provider. Mirrors iOS `TONStakingProvider.supportedUnstakeModes()`. */
    override suspend fun getSupportedUnstakeModes(): List<TONUnstakeMode> =
        manager.getSupportedUnstakeModes(identifier)
}

/** Typed handle for the TonStakers staking provider. Both option types are [JsonElement] (untyped), matching iOS `AnyCodable`. */
typealias TONTonStakersStakingProvider = TONStakingProvider<JsonElement, JsonElement>
