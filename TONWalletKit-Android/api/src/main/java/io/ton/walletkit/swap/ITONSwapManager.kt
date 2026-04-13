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
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.serializer

/** Manages swap providers and executes swap operations. Obtain via [io.ton.walletkit.ITONWalletKit.swap]. */
interface ITONSwapManager {
    /** Register a provider. Must be called before [getQuote] or [buildSwapTransaction]. */
    suspend fun registerProvider(provider: TONSwapProvider<*>)

    /** Set the default provider used by [getQuote] when no provider is specified. */
    suspend fun setDefaultProvider(provider: TONSwapProvider<*>)

    /** Returns the IDs of all registered providers. */
    suspend fun registeredProviders(): List<String>

    /** Returns true if the provider is currently registered. */
    suspend fun hasProvider(provider: TONSwapProvider<*>): Boolean

    /** Returns [provider] if it is currently registered, null otherwise. */
    suspend fun <TQuoteOptions> provider(provider: TONSwapProvider<TQuoteOptions>): TONSwapProvider<TQuoteOptions>?

    /**
     * Get a quote from a specific typed provider.
     * Prefer the inline [getQuote] extension — it infers [serializer] automatically.
     */
    suspend fun <TQuoteOptions> getQuote(
        params: TONSwapQuoteParams<TQuoteOptions>,
        provider: TONSwapProvider<TQuoteOptions>,
        serializer: KSerializer<TQuoteOptions>,
    ): TONSwapQuote

    /** Get a quote using the default registered provider. */
    suspend fun getQuote(params: TONSwapQuoteParams<JsonElement>): TONSwapQuote

    /**
     * Build a swap transaction with typed provider options.
     * Prefer the inline [buildSwapTransaction] extension — it infers [serializer] automatically.
     */
    suspend fun <TSwapOptions> buildSwapTransaction(
        params: TONSwapParams<TSwapOptions>,
        serializer: KSerializer<TSwapOptions>,
    ): TONTransactionRequest

    /** Build a swap transaction using untyped (JsonElement) provider options. */
    suspend fun buildSwapTransaction(params: TONSwapParams<JsonElement>): TONTransactionRequest
}

/** Get a quote from a specific typed provider, inferring the serializer via reified [TQuoteOptions]. */
suspend inline fun <reified TQuoteOptions> ITONSwapManager.getQuote(
    params: TONSwapQuoteParams<TQuoteOptions>,
    provider: TONSwapProvider<TQuoteOptions>,
): TONSwapQuote = getQuote(params, provider, serializer())

/** Build a swap transaction, inferring the serializer via reified [TSwapOptions]. */
suspend inline fun <reified TSwapOptions> ITONSwapManager.buildSwapTransaction(
    params: TONSwapParams<TSwapOptions>,
): TONTransactionRequest = buildSwapTransaction(params, serializer())
