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
package io.ton.walletkit.engine.state

import io.ton.walletkit.api.generated.TONSwapParams
import io.ton.walletkit.api.generated.TONSwapQuote
import io.ton.walletkit.api.generated.TONSwapQuoteParams
import io.ton.walletkit.api.generated.TONTransactionRequest
import io.ton.walletkit.swap.ITONSwapProvider
import kotlinx.serialization.json.JsonElement

/**
 * Reverse-RPC registry for Kotlin-implemented [ITONSwapProvider] instances. JS routes calls from
 * its `ProxySwapProvider` here via the bridge dispatcher.
 *
 * @suppress Internal engine component.
 */
internal class KotlinSwapProviderManager :
    KotlinProviderRegistry<ITONSwapProvider<JsonElement, JsonElement>>() {

    override val tag: String = "KotlinSwapProviderManager"

    suspend fun quote(providerId: String, params: TONSwapQuoteParams<JsonElement>): TONSwapQuote =
        require(providerId).quote(params)

    suspend fun buildSwapTransaction(providerId: String, params: TONSwapParams<JsonElement>): TONTransactionRequest =
        require(providerId).buildSwapTransaction(params)
}
