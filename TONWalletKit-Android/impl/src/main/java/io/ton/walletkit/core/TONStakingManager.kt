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
package io.ton.walletkit.core

import io.ton.walletkit.AnyTONProviderIdentifier
import io.ton.walletkit.ITONStakingManager
import io.ton.walletkit.ITONStakingProvider
import io.ton.walletkit.TONStakingProvider
import io.ton.walletkit.TONStakingProviderIdentifier
import io.ton.walletkit.api.generated.TONNetwork
import io.ton.walletkit.api.generated.TONStakeParams
import io.ton.walletkit.api.generated.TONStakingBalance
import io.ton.walletkit.api.generated.TONStakingProviderInfo
import io.ton.walletkit.api.generated.TONStakingQuote
import io.ton.walletkit.api.generated.TONStakingQuoteParams
import io.ton.walletkit.api.generated.TONTransactionRequest
import io.ton.walletkit.api.generated.TONUnstakeMode
import io.ton.walletkit.engine.WalletKitEngine
import io.ton.walletkit.model.TONUserFriendlyAddress
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/**
 * Internal implementation of [ITONStakingManager] backed by the JS bridge engine.
 *
 * @suppress Internal. Use [io.ton.walletkit.ITONWalletKit.staking] to obtain an instance.
 */
internal class TONStakingManager(
    private val engine: WalletKitEngine,
) : ITONStakingManager {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun register(provider: ITONStakingProvider<*, *>) {
        if (provider is TONStakingProvider<*, *>) {
            // Built-in JS-backed provider: the JS side already has the instance; just register its name.
            engine.registerStakingProvider(provider.identifier.name)
        } else {
            // Custom Kotlin provider: register locally so reverse-RPC calls from JS's ProxyStakingProvider
            // can reach it, then tell JS to create the proxy and register it with the JS staking manager.
            @Suppress("UNCHECKED_CAST")
            val typedProvider = provider as ITONStakingProvider<JsonElement, JsonElement>
            engine.kotlinStakingProviderManager.register(provider.identifier.name, typedProvider)
            // Pre-fetch supported unstake modes so the JS proxy can satisfy its synchronous
            // `getSupportedUnstakeModes()` contract without a round-trip.
            val modes = typedProvider.getSupportedUnstakeModes()
            val modesJson = json.encodeToString(
                ListSerializer(io.ton.walletkit.api.generated.TONUnstakeMode.serializer()),
                modes,
            )
            engine.registerKotlinStakingProvider(provider.identifier.name, modesJson)
        }
    }

    override suspend fun setDefaultProvider(identifier: TONStakingProviderIdentifier<*, *>) {
        engine.setDefaultStakingProvider(identifier.name)
    }

    override suspend fun registeredProviders(): List<AnyTONProviderIdentifier> =
        engine.getRegisteredStakingProviders().map { AnyTONProviderIdentifier(it) }

    override suspend fun hasProvider(identifier: TONStakingProviderIdentifier<*, *>): Boolean =
        engine.hasStakingProvider(identifier.name)

    override suspend fun <TQuoteOptions, TStakeOptions> getQuote(
        params: TONStakingQuoteParams<TQuoteOptions>,
        identifier: TONStakingProviderIdentifier<TQuoteOptions, TStakeOptions>,
    ): TONStakingQuote {
        val jsonOptions = params.providerOptions?.let { Json.encodeToJsonElement(identifier.quoteOptionsSerializer, it) }
        val jsonParams = TONStakingQuoteParams(
            direction = params.direction,
            amount = params.amount,
            userAddress = params.userAddress,
            network = params.network,
            unstakeMode = params.unstakeMode,
            providerOptions = jsonOptions,
        )
        return engine.getStakingQuote(jsonParams, identifier.name)
    }

    override suspend fun getQuote(params: TONStakingQuoteParams<JsonElement>): TONStakingQuote =
        engine.getStakingQuote(params, null)

    override suspend fun <TQuoteOptions, TStakeOptions> buildStakeTransaction(
        params: TONStakeParams<TStakeOptions>,
        identifier: TONStakingProviderIdentifier<TQuoteOptions, TStakeOptions>,
    ): TONTransactionRequest {
        val jsonOptions = params.providerOptions?.let { Json.encodeToJsonElement(identifier.stakeOptionsSerializer, it) }
        val jsonParams = TONStakeParams(
            quote = params.quote,
            userAddress = params.userAddress,
            providerOptions = jsonOptions,
        )
        val content = engine.buildStakeTransaction(jsonParams, identifier.name)
        return json.decodeFromString(content)
    }

    override suspend fun buildStakeTransaction(params: TONStakeParams<JsonElement>): TONTransactionRequest {
        val content = engine.buildStakeTransaction(params, null)
        return json.decodeFromString(content)
    }

    override suspend fun getStakedBalance(
        userAddress: TONUserFriendlyAddress,
        network: TONNetwork?,
        identifier: TONStakingProviderIdentifier<*, *>?,
    ): TONStakingBalance = engine.getStakedBalance(userAddress.value, network, identifier?.name)

    override suspend fun getStakingProviderInfo(
        network: TONNetwork?,
        identifier: TONStakingProviderIdentifier<*, *>?,
    ): TONStakingProviderInfo = engine.getStakingProviderInfo(network, identifier?.name)

    override suspend fun getSupportedUnstakeModes(
        identifier: TONStakingProviderIdentifier<*, *>?,
    ): List<TONUnstakeMode> = engine.getSupportedUnstakeModes(identifier?.name)
}
