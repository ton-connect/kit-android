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
    /** Register a provider. Must be called before [getQuote] or [buildSwapTransaction]. */
    suspend fun registerProvider(provider: TONSwapProvider<*, *>)

    /** Set the default provider used by [getQuote] when no provider is specified. */
    suspend fun setDefaultProvider(identifier: TONSwapProviderIdentifier<*, *>)

    /** Returns typed identifiers for all registered providers. */
    suspend fun registeredProviders(): List<AnyTONSwapProviderIdentifier>

    /** Returns true if a provider with the given [identifier] is currently registered. */
    suspend fun hasProvider(identifier: TONSwapProviderIdentifier<*, *>): Boolean

    /** Get a quote from the specific [providerId]. */
    suspend fun getQuote(params: TONSwapQuoteParams<JsonElement>, providerId: String): TONSwapQuote

    /** Get a quote from the default registered provider. */
    suspend fun getQuote(params: TONSwapQuoteParams<JsonElement>): TONSwapQuote

    /**
     * Build a swap transaction. The provider is resolved from [TONSwapParams.quote].
     * Prefer calling [TONSwapProvider.buildSwapTransaction] directly for typed provider options.
     */
    suspend fun buildSwapTransaction(params: TONSwapParams<JsonElement>): TONTransactionRequest
}

/**
 * Returns a typed [TONSwapProvider] for [identifier] if it is currently registered, null otherwise.
 * Type parameters are inferred from the identifier, e.g.:
 * ```kotlin
 * val provider: TONOmnistonSwapProvider? = manager.provider(TONOmnistonSwapProviderIdentifier())
 * ```
 */
suspend inline fun <reified TQuoteOptions, reified TSwapOptions> ITONSwapManager.provider(
    identifier: TONSwapProviderIdentifier<TQuoteOptions, TSwapOptions>,
): TONSwapProvider<TQuoteOptions, TSwapOptions>? {
    val handle = TONSwapProvider(identifier, this)
    return if (hasProvider(identifier)) handle else null
}

/** Get a quote via a typed provider. Delegates to [TONSwapProvider.quote]. */
suspend fun <TQuoteOptions, TSwapOptions> ITONSwapManager.getQuote(
    params: TONSwapQuoteParams<TQuoteOptions>,
    provider: TONSwapProvider<TQuoteOptions, TSwapOptions>,
): TONSwapQuote = provider.quote(params)
