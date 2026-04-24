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

import io.ton.walletkit.TONProviderType
import io.ton.walletkit.api.generated.TONSwapParams
import io.ton.walletkit.api.generated.TONSwapQuote
import io.ton.walletkit.api.generated.TONSwapQuoteParams
import io.ton.walletkit.api.generated.TONTransactionRequest

/**
 * Contract every swap provider must satisfy. Mirrors iOS's `TONSwapProviderProtocol`.
 *
 * The built-in [TONSwapProvider] implements this interface. Custom providers implement it directly.
 *
 * [TQuoteOptions] is the provider-specific options type for [quote];
 * [TSwapOptions] is the provider-specific options type for [buildSwapTransaction].
 */
interface ITONSwapProvider<TQuoteOptions, TSwapOptions> {
    /** Always [TONProviderType.Swap]. Used to discriminate at runtime across domains. */
    val type: TONProviderType get() = TONProviderType.Swap

    /** Typed provider identifier. */
    val identifier: TONSwapProviderIdentifier<TQuoteOptions, TSwapOptions>

    /** Get a quote from this provider. */
    suspend fun quote(params: TONSwapQuoteParams<TQuoteOptions>): TONSwapQuote

    /** Build a swap transaction using this provider. */
    suspend fun buildSwapTransaction(params: TONSwapParams<TSwapOptions>): TONTransactionRequest
}
