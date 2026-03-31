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
import kotlinx.serialization.json.JsonElement

/**
 * Manages swap providers and executes swap operations.
 *
 * Obtain via [io.ton.walletkit.ITONWalletKit.swap].
 */
interface ITONSwapManager {
    /**
     * Register a swap provider so it can be used in [getQuote] and [buildSwapTransaction].
     * Providers created via [io.ton.walletkit.ITONWalletKit.omnistonSwapProvider] or
     * [io.ton.walletkit.ITONWalletKit.deDustSwapProvider] must be registered before use.
     */
    suspend fun registerProvider(provider: TONSwapProvider)

    /**
     * Get a swap quote from a specific provider.
     *
     * @param params Quote parameters including from/to tokens and amount.
     * @param providerId ID of the provider to use.
     * @return Swap quote with pricing information.
     */
    suspend fun getQuote(params: TONSwapQuoteParams<JsonElement>, providerId: String): TONSwapQuote

    /**
     * Get a swap quote using the default registered provider.
     *
     * @param params Quote parameters including from/to tokens and amount.
     * @return Swap quote with pricing information.
     */
    suspend fun getQuote(params: TONSwapQuoteParams<JsonElement>): TONSwapQuote

    /**
     * Build a transaction for executing a swap.
     *
     * @param params Swap parameters including the quote obtained from [getQuote].
     * @return Transaction content as JSON string (pass to [io.ton.walletkit.ITONWallet.sendTransaction]).
     */
    suspend fun buildSwapTransaction(params: TONSwapParams<JsonElement>): String
}
