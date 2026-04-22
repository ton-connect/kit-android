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

import io.ton.walletkit.api.generated.TONStakeParams
import io.ton.walletkit.api.generated.TONStakingQuote
import io.ton.walletkit.api.generated.TONStakingQuoteParams
import io.ton.walletkit.api.generated.TONTransactionRequest
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.serializer

/**
 * A registered staking provider that exposes typed [getQuote] and [buildStakeTransaction] operations.
 *
 * [TQuoteOptions] is the provider-specific options type for [getQuote].
 * [TStakeOptions] is the provider-specific options type for [buildStakeTransaction].
 *
 * Created via [ITONWalletKit.tonStakersStakingProvider] and registered with [ITONStakingManager.register].
 */
class TONStakingProvider<TQuoteOptions, TStakeOptions> @PublishedApi internal constructor(
    val identifier: TONStakingProviderIdentifier<TQuoteOptions, TStakeOptions>,
    private val manager: ITONStakingManager,
    @PublishedApi internal val quoteSerializer: KSerializer<TQuoteOptions>,
    @PublishedApi internal val stakeSerializer: KSerializer<TStakeOptions>,
) {
    val providerId: String get() = identifier.name

    /** Get a quote from this provider. */
    suspend fun getQuote(params: TONStakingQuoteParams<TQuoteOptions>): TONStakingQuote {
        val jsonOptions = params.providerOptions?.let { Json.encodeToJsonElement(quoteSerializer, it) }
        return manager.getQuote(
            TONStakingQuoteParams(
                direction = params.direction,
                amount = params.amount,
                userAddress = params.userAddress,
                network = params.network,
                unstakeMode = params.unstakeMode,
                providerOptions = jsonOptions,
            ),
            identifier,
        )
    }

    /** Build a stake or unstake transaction using this provider. */
    suspend fun buildStakeTransaction(params: TONStakeParams<TStakeOptions>): TONTransactionRequest {
        val jsonOptions = params.providerOptions?.let { Json.encodeToJsonElement(stakeSerializer, it) }
        return manager.buildStakeTransaction(
            TONStakeParams(
                quote = params.quote,
                userAddress = params.userAddress,
                providerOptions = jsonOptions,
            ),
            identifier,
        )
    }
}

/** Creates a typed [TONStakingProvider], capturing serializers via reified type parameters. */
@Suppress("ktlint:standard:function-naming")
inline fun <reified TQuoteOptions, reified TStakeOptions> TONStakingProvider(
    identifier: TONStakingProviderIdentifier<TQuoteOptions, TStakeOptions>,
    manager: ITONStakingManager,
): TONStakingProvider<TQuoteOptions, TStakeOptions> = TONStakingProvider(
    identifier = identifier,
    manager = manager,
    quoteSerializer = serializer(),
    stakeSerializer = serializer(),
)

/** Typed handle for the TonStakers staking provider. Both option types are [JsonElement] (untyped), matching iOS `AnyCodable`. */
typealias TONTonStakersStakingProviderHandle = TONStakingProvider<JsonElement, JsonElement>
