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

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonElement

/**
 * Identifies a swap provider and carries its option types as generic parameters.
 * Analogous to iOS's `TONSwapProviderIdentifier` protocol, whose `associatedtype QuoteOptions: Codable`
 * is mirrored here by an explicit [quoteOptionsSerializer] (same for [swapOptionsSerializer]).
 *
 * [TQuoteOptions] is the provider-specific type for [ITONSwapManager.getQuote] options.
 * [TSwapOptions] is the provider-specific type for swap-transaction building options.
 */
interface TONSwapProviderIdentifier<TQuoteOptions, TSwapOptions> {
    val name: String
    val quoteOptionsSerializer: KSerializer<TQuoteOptions>
    val swapOptionsSerializer: KSerializer<TSwapOptions>
}

/**
 * Type-erased swap provider identifier returned by [ITONSwapManager.registeredProviders].
 * Analogous to iOS's `AnyTONProviderIdentifier`.
 */
data class AnyTONSwapProviderIdentifier(override val name: String) :
    TONSwapProviderIdentifier<JsonElement, JsonElement> {
    override val quoteOptionsSerializer: KSerializer<JsonElement> = JsonElement.serializer()
    override val swapOptionsSerializer: KSerializer<JsonElement> = JsonElement.serializer()
}
