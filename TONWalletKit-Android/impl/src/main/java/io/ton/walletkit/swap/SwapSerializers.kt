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
import io.ton.walletkit.swap.dedust.TONDeDustSwapProviderIdentifier
import io.ton.walletkit.swap.omniston.TONOmnistonSwapProviderIdentifier
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonElement

/**
 * Internal resolver that maps a [TONSwapProviderIdentifier] concrete type to its
 * kotlinx.serialization serializers. Keeps `KSerializer` out of the public API surface —
 * only the impl module needs to serialize typed `providerOptions` to `JsonElement` for
 * transport to the JS bridge.
 *
 * For unknown (custom) identifier types, falls back to [JsonElement.serializer] so the call
 * does not crash, but custom provider authors should typically implement [io.ton.walletkit.swap.ITONSwapProvider]
 * directly and use [io.ton.walletkit.swap.ITONSwapProvider.quote] rather than the typed manager path.
 *
 * @suppress Internal implementation detail.
 */
internal object SwapSerializers {
    @Suppress("UNCHECKED_CAST")
    fun <T, S> quoteSerializer(identifier: TONSwapProviderIdentifier<T, S>): KSerializer<T> =
        when (identifier) {
            is TONOmnistonSwapProviderIdentifier -> TONOmnistonProviderOptions.serializer()
            is TONDeDustSwapProviderIdentifier -> TONDeDustProviderOptions.serializer()
            else -> JsonElement.serializer()
        } as KSerializer<T>

    @Suppress("UNCHECKED_CAST")
    fun <T, S> swapOptionsSerializer(identifier: TONSwapProviderIdentifier<T, S>): KSerializer<S> =
        when (identifier) {
            is TONOmnistonSwapProviderIdentifier -> JsonElement.serializer()
            is TONDeDustSwapProviderIdentifier -> TONDeDustProviderOptions.serializer()
            else -> JsonElement.serializer()
        } as KSerializer<S>
}
