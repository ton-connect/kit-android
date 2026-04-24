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

import io.ton.walletkit.api.generated.TONSwapParams
import io.ton.walletkit.api.generated.TONSwapQuote
import io.ton.walletkit.api.generated.TONSwapQuoteParams
import io.ton.walletkit.api.generated.TONTransactionRequest
import kotlinx.serialization.json.JsonElement

/** Manages swap providers and executes swap operations. Obtain via [io.ton.walletkit.ITONWalletKit.swap]. */
interface ITONSwapManager {
    /**
     * Register a provider. Accepts any [ITONSwapProvider] implementation — the SDK's built-in
     * providers returned from `ITONWalletKit.omnistonSwapProvider` / `dedustSwapProvider`, and any
     * user-defined conformer. Mirrors iOS `register<Provider: TONSwapProviderProtocol>(provider:)`.
     */
    suspend fun registerProvider(provider: ITONSwapProvider<*, *>)

    /** Set the default provider used by [getQuote] when no identifier is specified. */
    suspend fun setDefaultProvider(identifier: TONSwapProviderIdentifier<*, *>)

    /** Returns typed identifiers for all registered providers. */
    suspend fun registeredProviders(): List<AnyTONSwapProviderIdentifier>

    /** Returns true if a provider with the given [identifier] is currently registered. */
    suspend fun hasProvider(identifier: TONSwapProviderIdentifier<*, *>): Boolean

    /**
     * Returns a typed [ITONSwapProvider] for [identifier] if it is currently registered, null otherwise.
     * Mirrors iOS `provider<Identifier: TONSwapProviderIdentifier>(with: Identifier) -> TONSwapProvider<Identifier>?`.
     */
    suspend fun <TQuoteOptions, TSwapOptions> provider(
        identifier: TONSwapProviderIdentifier<TQuoteOptions, TSwapOptions>,
    ): ITONSwapProvider<TQuoteOptions, TSwapOptions>?

    /**
     * Get a quote from the provider with [identifier]. Mirrors iOS
     * `quote<Identifier: TONSwapProviderIdentifier>(params:, identifier:)`.
     *
     * Typed `providerOptions` are serialized internally by the SDK before reaching the JS bridge.
     */
    suspend fun <TQuoteOptions, TSwapOptions> getQuote(
        params: TONSwapQuoteParams<TQuoteOptions>,
        identifier: TONSwapProviderIdentifier<TQuoteOptions, TSwapOptions>,
    ): TONSwapQuote

    /** Get a quote from the default registered provider. Mirrors iOS `quote(params: TONSwapQuoteParams<AnyCodable>)`. */
    suspend fun getQuote(params: TONSwapQuoteParams<JsonElement>): TONSwapQuote

    /**
     * Build a swap transaction. The provider is resolved from [TONSwapParams.quote.providerId].
     * For typed `providerOptions`, call [ITONSwapProvider.buildSwapTransaction] — it handles
     * serialization internally before delegating here.
     */
    suspend fun buildSwapTransaction(params: TONSwapParams<JsonElement>): TONTransactionRequest
}
