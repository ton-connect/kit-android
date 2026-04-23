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

import io.ton.walletkit.api.generated.TONNetwork
import io.ton.walletkit.api.generated.TONStakeParams
import io.ton.walletkit.api.generated.TONStakingBalance
import io.ton.walletkit.api.generated.TONStakingProviderInfo
import io.ton.walletkit.api.generated.TONStakingQuote
import io.ton.walletkit.api.generated.TONStakingQuoteParams
import io.ton.walletkit.api.generated.TONTransactionRequest
import io.ton.walletkit.model.TONUserFriendlyAddress
import io.ton.walletkit.staking.ITONStakingProvider
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/**
 * Reverse-RPC registry for Kotlin-implemented [ITONStakingProvider] instances. When a developer
 * registers a custom Kotlin staking provider, JS creates a `ProxyStakingProvider` that routes
 * calls here (mirroring the Kotlin swap / wallet-adapter / streaming proxy pattern).
 *
 * Custom providers must implement `ITONStakingProvider<JsonElement, JsonElement>` because the
 * JS side transports provider options as JSON. Users can decode the [JsonElement] to concrete
 * types inside their implementation.
 *
 * @suppress Internal engine component.
 */
internal class KotlinStakingProviderManager(
    private val json: Json,
) : KotlinProviderRegistry<ITONStakingProvider<JsonElement, JsonElement>>() {

    override val tag: String = "KotlinStakingProviderManager"

    suspend fun getQuote(providerId: String, paramsJson: String): String {
        val provider = require(providerId)
        val params = json.decodeFromString(
            TONStakingQuoteParams.serializer(JsonElement.serializer()),
            paramsJson,
        )
        val quote = provider.getQuote(params)
        return json.encodeToString(TONStakingQuote.serializer(), quote)
    }

    suspend fun buildStakeTransaction(providerId: String, paramsJson: String): String {
        val provider = require(providerId)
        val params = json.decodeFromString(
            TONStakeParams.serializer(JsonElement.serializer()),
            paramsJson,
        )
        val request = provider.buildStakeTransaction(params)
        return json.encodeToString(TONTransactionRequest.serializer(), request)
    }

    suspend fun getStakedBalance(providerId: String, userAddress: String, networkChainId: String?): String {
        val provider = require(providerId)
        val parsed = TONUserFriendlyAddress.parse(userAddress)
        val network = networkChainId?.let { TONNetwork(chainId = it) }
        val balance = provider.getStakedBalance(parsed, network)
        return json.encodeToString(TONStakingBalance.serializer(), balance)
    }

    suspend fun getStakingProviderInfo(providerId: String, networkChainId: String?): String {
        val provider = require(providerId)
        val network = networkChainId?.let { TONNetwork(chainId = it) }
        val info = provider.getStakingProviderInfo(network)
        return json.encodeToString(TONStakingProviderInfo.serializer(), info)
    }
}
