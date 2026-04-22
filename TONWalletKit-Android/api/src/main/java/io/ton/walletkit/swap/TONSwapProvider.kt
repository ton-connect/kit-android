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
package io.ton.walletkit.swap

import io.ton.walletkit.api.generated.TONDeDustProviderOptions
import io.ton.walletkit.api.generated.TONOmnistonProviderOptions
import io.ton.walletkit.api.generated.TONSwapParams
import io.ton.walletkit.api.generated.TONSwapQuote
import io.ton.walletkit.api.generated.TONSwapQuoteParams
import io.ton.walletkit.api.generated.TONTransactionRequest
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.serializer

/**
 * A registered swap provider that can be used to obtain quotes and build swap transactions.
 * [TQuoteOptions] carries the provider-specific options for [quote];
 * [TSwapOptions] carries the provider-specific options for [buildSwapTransaction].
 *
 * Created via [io.ton.walletkit.ITONWalletKit.omnistonSwapProvider] or
 * [io.ton.walletkit.ITONWalletKit.dedustSwapProvider].
 * Register with [ITONSwapManager.registerProvider] before calling [quote] or [buildSwapTransaction].
 */
class TONSwapProvider<TQuoteOptions, TSwapOptions> @PublishedApi internal constructor(
    val identifier: TONSwapProviderIdentifier<TQuoteOptions, TSwapOptions>,
    private val manager: ITONSwapManager,
    @PublishedApi internal val quoteSerializer: KSerializer<TQuoteOptions>,
    @PublishedApi internal val swapSerializer: KSerializer<TSwapOptions>,
) {
    val providerId: String get() = identifier.name

    /** Get a quote from this provider. */
    suspend fun quote(params: TONSwapQuoteParams<TQuoteOptions>): TONSwapQuote {
        val jsonOptions = params.providerOptions?.let { Json.encodeToJsonElement(quoteSerializer, it) }
        return manager.getQuote(
            TONSwapQuoteParams(
                amount = params.amount,
                from = params.from,
                to = params.to,
                network = params.network,
                slippageBps = params.slippageBps,
                maxOutgoingMessages = params.maxOutgoingMessages,
                providerOptions = jsonOptions,
                isReverseSwap = params.isReverseSwap,
            ),
            providerId,
        )
    }

    /** Build a swap transaction using this provider. */
    suspend fun buildSwapTransaction(params: TONSwapParams<TSwapOptions>): TONTransactionRequest {
        val jsonOptions = params.providerOptions?.let { Json.encodeToJsonElement(swapSerializer, it) }
        return manager.buildSwapTransaction(
            TONSwapParams(
                quote = params.quote,
                userAddress = params.userAddress,
                destinationAddress = params.destinationAddress,
                slippageBps = params.slippageBps,
                deadline = params.deadline,
                providerOptions = jsonOptions,
            ),
        )
    }
}

/** Creates a typed [TONSwapProvider], capturing serializers via reified type parameters. */
@Suppress("ktlint:standard:function-naming")
inline fun <reified TQuoteOptions, reified TSwapOptions> TONSwapProvider(
    identifier: TONSwapProviderIdentifier<TQuoteOptions, TSwapOptions>,
    manager: ITONSwapManager,
): TONSwapProvider<TQuoteOptions, TSwapOptions> = TONSwapProvider(
    identifier = identifier,
    manager = manager,
    quoteSerializer = serializer(),
    swapSerializer = serializer(),
)

/** Typed handle for the Omniston (STON.fi) swap provider. SwapOptions is [JsonElement] (untyped), matching iOS `AnyCodable`. */
typealias TONOmnistonSwapProvider = TONSwapProvider<TONOmnistonProviderOptions, JsonElement>

/** Typed handle for the DeDust swap provider. */
typealias TONDeDustSwapProvider = TONSwapProvider<TONDeDustProviderOptions, TONDeDustProviderOptions>
