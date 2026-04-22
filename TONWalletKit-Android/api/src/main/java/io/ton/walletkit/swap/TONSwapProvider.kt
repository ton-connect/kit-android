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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/**
 * A registered swap provider handle — typed bundle of identifier + manager reference.
 * [TQuoteOptions] carries the provider-specific options for [quote];
 * [TSwapOptions] carries the provider-specific options for [buildSwapTransaction].
 *
 * Typically obtained via [io.ton.walletkit.ITONWalletKit.omnistonSwapProvider] /
 * [io.ton.walletkit.ITONWalletKit.dedustSwapProvider], or via [ITONSwapManager.provider].
 * Register with [ITONSwapManager.registerProvider] before calling [quote] / [buildSwapTransaction].
 */
class TONSwapProvider<TQuoteOptions, TSwapOptions>(
    val identifier: TONSwapProviderIdentifier<TQuoteOptions, TSwapOptions>,
    private val manager: ITONSwapManager,
) {
    /** Get a quote from this provider. Delegates to [ITONSwapManager.getQuote] with this provider's [identifier]. */
    suspend fun quote(params: TONSwapQuoteParams<TQuoteOptions>): TONSwapQuote =
        manager.getQuote(params, identifier)

    /**
     * Build a swap transaction using this provider. Serializes `providerOptions`
     * via [TONSwapProviderIdentifier.swapOptionsSerializer] before delegating to the manager.
     */
    suspend fun buildSwapTransaction(params: TONSwapParams<TSwapOptions>): TONTransactionRequest {
        val jsonOptions = params.providerOptions?.let { Json.encodeToJsonElement(identifier.swapOptionsSerializer, it) }
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

/** Typed handle for the Omniston (STON.fi) swap provider. SwapOptions is [JsonElement] (untyped), matching iOS `AnyCodable`. */
typealias TONOmnistonSwapProvider = TONSwapProvider<TONOmnistonProviderOptions, JsonElement>

/** Typed handle for the DeDust swap provider. */
typealias TONDeDustSwapProvider = TONSwapProvider<TONDeDustProviderOptions, TONDeDustProviderOptions>
