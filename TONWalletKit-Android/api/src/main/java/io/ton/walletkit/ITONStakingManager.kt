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
 */
interface ITONStakingManager {

    /**
     * Register a staking provider created via [ITONWalletKit.tonStakersStakingProvider].
     */
    suspend fun register(provider: ITONTonStakersStakingProvider)

    /**
     * Set the default provider used when no identifier is passed to query methods.
     * Must be called explicitly before using [getQuote] or other methods without an identifier.
     */
    suspend fun setDefaultProvider(identifier: TONStakingProviderIdentifier<*, *>)

    /**
     * Returns type-erased identifiers for all currently registered staking providers.
     */
    suspend fun registeredProviders(): List<AnyTONProviderIdentifier>

    /**
     * Returns true if a provider with the given [identifier] is currently registered.
     */
    suspend fun hasProvider(identifier: TONStakingProviderIdentifier<*, *>): Boolean

    /**
     * Get a stake or unstake quote.
     *
     * @param params Quote parameters including direction, amount, and optional fields
     * @param identifier Provider identifier (uses bridge default when null)
     * @return Staking quote with input/output amounts and metadata
     */
    suspend fun getQuote(
        params: TONStakingQuoteParams<JsonElement>,
        identifier: TONStakingProviderIdentifier<*, *>? = null,
    ): TONStakingQuote

    /**
     * Build a stake or unstake transaction from a previously obtained quote.
     *
     * The returned [TONTransactionRequest] can be passed to [ITONWallet.send].
     *
     * @param params Stake parameters including the quote and user address
     * @param identifier Provider identifier (uses bridge default when null)
     * @return Transaction request ready for [ITONWallet.send] or [ITONWallet.preview]
     */
    suspend fun buildStakeTransaction(
        params: TONStakeParams<JsonElement>,
        identifier: TONStakingProviderIdentifier<*, *>? = null,
    ): TONTransactionRequest

    /**
     * Get the user's staked balance.
     *
     * @param userAddress User's wallet address
     * @param network TON network (uses current network when null)
     * @param identifier Provider identifier (uses bridge default when null)
     * @return Staking balance including staked amount and instant-unstake availability
     */
    suspend fun getStakedBalance(
        userAddress: TONUserFriendlyAddress,
        network: TONNetwork? = null,
        identifier: TONStakingProviderIdentifier<*, *>? = null,
    ): TONStakingBalance

    /**
     * Get general information about a staking provider (APY, instant-unstake liquidity).
     *
     * @param network TON network (uses current network when null)
     * @param identifier Provider identifier (uses bridge default when null)
     * @return Provider info including APY as a percentage value
     */
    suspend fun getStakingProviderInfo(
        network: TONNetwork? = null,
        identifier: TONStakingProviderIdentifier<*, *>? = null,
    ): TONStakingProviderInfo

    /**
     * Get the unstake modes supported by a staking provider.
     *
     * @param identifier Provider identifier (uses bridge default when null)
     * @return List of supported unstake modes
     */
    suspend fun getSupportedUnstakeModes(
        identifier: TONStakingProviderIdentifier<*, *>? = null,
    ): List<TONUnstakeMode>
}

/**
 * Represents a registered TonStakers staking provider.
 *
 * Created via [ITONWalletKit.tonStakersStakingProvider] and registered with [ITONStakingManager.register].
 */
interface ITONTonStakersStakingProvider {
    /** Typed staking provider identifier recognised by [ITONStakingManager]. */
    val identifier: TONTonStakersStakingProviderIdentifier
}

/**
 * Returns a typed [TONStakingProvider] for [identifier] if it is currently registered, null otherwise.
 * Type parameters are inferred from the identifier:
 * ```kotlin
 * val provider = manager.provider(TONTonStakersStakingProviderIdentifier())
 * ```
 */
suspend inline fun <reified TQuoteOptions, reified TStakeOptions> ITONStakingManager.provider(
    identifier: TONStakingProviderIdentifier<TQuoteOptions, TStakeOptions>,
): TONStakingProvider<TQuoteOptions, TStakeOptions>? {
    val handle = TONStakingProvider(identifier, this)
    return if (hasProvider(identifier)) handle else null
}

/** Get a quote via a typed provider. Delegates to [TONStakingProvider.getQuote]. */
suspend fun <TQuoteOptions, TStakeOptions> ITONStakingManager.getQuote(
    params: TONStakingQuoteParams<TQuoteOptions>,
    provider: TONStakingProvider<TQuoteOptions, TStakeOptions>,
): TONStakingQuote = provider.getQuote(params)
