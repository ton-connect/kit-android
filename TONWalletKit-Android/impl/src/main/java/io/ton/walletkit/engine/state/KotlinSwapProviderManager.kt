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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/**
 * Reverse-RPC registry for Kotlin-implemented [ITONSwapProvider] instances. When a developer
 * registers a custom Kotlin swap provider, JS creates a `ProxySwapProvider` that routes calls
 * here (mirroring the Kotlin wallet-adapter / staking / streaming proxy pattern).
 *
 * Custom providers must implement `ITONSwapProvider<JsonElement, JsonElement>` because the JS
 * side transports provider options as JSON. Users can decode the [JsonElement] to concrete
 * types inside their implementation.
 *
 * @suppress Internal engine component.
 */
internal class KotlinSwapProviderManager(
    private val json: Json,
) : KotlinProviderRegistry<ITONSwapProvider<JsonElement, JsonElement>>() {

    override val tag: String = "KotlinSwapProviderManager"

    suspend fun quote(providerId: String, paramsJson: String): String {
        val provider = require(providerId)
        val params = json.decodeFromString(
            TONSwapQuoteParams.serializer(JsonElement.serializer()),
            paramsJson,
        )
        val quote = provider.quote(params)
        return json.encodeToString(TONSwapQuote.serializer(), quote)
    }

    suspend fun buildSwapTransaction(providerId: String, paramsJson: String): String {
        val provider = require(providerId)
        val params = json.decodeFromString(
            TONSwapParams.serializer(JsonElement.serializer()),
            paramsJson,
        )
        val request = provider.buildSwapTransaction(params)
        return json.encodeToString(TONTransactionRequest.serializer(), request)
    }
}
