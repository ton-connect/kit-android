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

import io.ton.walletkit.ITONStakingProvider
import io.ton.walletkit.api.generated.TONNetwork
import io.ton.walletkit.api.generated.TONStakeParams
import io.ton.walletkit.api.generated.TONStakingBalance
import io.ton.walletkit.api.generated.TONStakingProviderInfo
import io.ton.walletkit.api.generated.TONStakingQuote
import io.ton.walletkit.api.generated.TONStakingQuoteParams
import io.ton.walletkit.api.generated.TONTransactionRequest
import io.ton.walletkit.internal.util.Logger
import io.ton.walletkit.model.TONUserFriendlyAddress
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages Kotlin-implemented [ITONStakingProvider] instances.
 *
 * When a developer registers a custom Kotlin staking provider, JS creates a `ProxyStakingProvider`
 * that calls back here via the reverse-RPC mechanism (same pattern as Kotlin wallet adapters /
 * Kotlin streaming providers). Each call targets the Kotlin provider by `providerId` and returns
 * a JSON response to the JS side.
 *
 * Custom providers are expected to implement `ITONStakingProvider<JsonElement, JsonElement>` since
 * the JS side transports provider options as JSON. Users can still decode the JsonElement to their
 * own types inside their provider implementation if desired.
 *
 * @suppress Internal engine component.
 */
internal class KotlinStakingProviderManager(
    private val json: Json,
) {
    private val providers = ConcurrentHashMap<String, ITONStakingProvider<JsonElement, JsonElement>>()

    fun register(providerId: String, provider: ITONStakingProvider<JsonElement, JsonElement>) {
        providers[providerId] = provider
    }

    fun unregister(providerId: String) {
        providers.remove(providerId)
    }

    fun getProvider(providerId: String): ITONStakingProvider<JsonElement, JsonElement>? = providers[providerId]

    fun clear() {
        providers.clear()
    }

    /**
     * Invoked from the reverse-RPC dispatcher when JS requests a quote for a custom Kotlin provider.
     * Returns the serialized [TONStakingQuote] as a JSON string.
     */
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

    private fun require(providerId: String): ITONStakingProvider<JsonElement, JsonElement> =
        providers[providerId] ?: run {
            Logger.w(TAG, "No Kotlin staking provider registered for id=$providerId")
            throw IllegalStateException("No Kotlin staking provider registered for id=$providerId")
        }

    private companion object {
        private const val TAG = "KotlinStakingProviderManager"
    }
}
