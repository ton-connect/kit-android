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
 * Manages staking providers and exposes staking operations.
 * Analogous to iOS's `TONStakingManagerProtocol`.
 */
interface ITONStakingManager {

    /** Register a staking provider. Accepts any [ITONStakingProvider] implementation — the built-in [TONStakingProvider] and any user-defined conformers. Mirrors iOS `register<Provider: TONStakingProviderProtocol>(provider:)`. */
    suspend fun register(provider: ITONStakingProvider<*, *>)

    /** Set the default provider used when no identifier is passed to query methods. */
    suspend fun setDefaultProvider(identifier: TONStakingProviderIdentifier<*, *>)

    /** Returns type-erased identifiers for all currently registered staking providers. */
    suspend fun registeredProviders(): List<AnyTONProviderIdentifier>

    /** Returns true if a provider with the given [identifier] is currently registered. */
    suspend fun hasProvider(identifier: TONStakingProviderIdentifier<*, *>): Boolean

    /**
     * Returns a typed [TONStakingProvider] for [identifier] if it is currently registered, null otherwise.
     * Mirrors iOS `provider<Identifier: TONStakingProviderIdentifier>(with: Identifier) -> TONStakingProvider<Identifier>?`.
     */
    suspend fun <TQuoteOptions, TStakeOptions> provider(
        identifier: TONStakingProviderIdentifier<TQuoteOptions, TStakeOptions>,
    ): TONStakingProvider<TQuoteOptions, TStakeOptions>? =
        if (hasProvider(identifier)) TONStakingProvider(identifier, this) else null

    /**
     * Get a stake or unstake quote from the provider with [identifier]. Mirrors iOS
     * `quote<Identifier: TONStakingProviderIdentifier>(params:, identifier:)`.
     *
     * Typed `providerOptions` are serialized via [TONStakingProviderIdentifier.quoteOptionsSerializer].
     */
    suspend fun <TQuoteOptions, TStakeOptions> getQuote(
        params: TONStakingQuoteParams<TQuoteOptions>,
        identifier: TONStakingProviderIdentifier<TQuoteOptions, TStakeOptions>,
    ): TONStakingQuote

    /** Get a quote from the default registered provider. Mirrors iOS `quote(params: TONStakingQuoteParams<AnyCodable>)`. */
    suspend fun getQuote(params: TONStakingQuoteParams<JsonElement>): TONStakingQuote

    /**
     * Build a stake or unstake transaction with the provider [identifier]. Mirrors iOS
     * `stakeTransaction<Identifier>(params:, identifier:)`.
     *
     * Typed `providerOptions` are serialized via [TONStakingProviderIdentifier.stakeOptionsSerializer].
     */
    suspend fun <TQuoteOptions, TStakeOptions> buildStakeTransaction(
        params: TONStakeParams<TStakeOptions>,
        identifier: TONStakingProviderIdentifier<TQuoteOptions, TStakeOptions>,
    ): TONTransactionRequest

    /**
     * Build a stake or unstake transaction using the default registered provider.
     * Mirrors iOS `stakeTransaction(params: TONStakeParams<AnyCodable>)`.
     */
    suspend fun buildStakeTransaction(params: TONStakeParams<JsonElement>): TONTransactionRequest

    /**
     * Get the user's staked balance.
     * @param userAddress User's wallet address
     * @param network TON network (uses current network when null)
     * @param identifier Provider identifier (uses bridge default when null)
     */
    suspend fun getStakedBalance(
        userAddress: TONUserFriendlyAddress,
        network: TONNetwork? = null,
        identifier: TONStakingProviderIdentifier<*, *>? = null,
    ): TONStakingBalance

    /**
     * Get general information about a staking provider (APY, instant-unstake liquidity).
     * @param network TON network (uses current network when null)
     * @param identifier Provider identifier (uses bridge default when null)
     */
    suspend fun getStakingProviderInfo(
        network: TONNetwork? = null,
        identifier: TONStakingProviderIdentifier<*, *>? = null,
    ): TONStakingProviderInfo

    /**
     * Get the unstake modes supported by a staking provider.
     * @param identifier Provider identifier (uses bridge default when null)
     */
    suspend fun getSupportedUnstakeModes(
        identifier: TONStakingProviderIdentifier<*, *>? = null,
    ): List<TONUnstakeMode>
}
