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
package io.ton.walletkit.staking

import io.ton.walletkit.api.generated.TONNetwork
import io.ton.walletkit.api.generated.TONStakeParams
import io.ton.walletkit.api.generated.TONStakingBalance
import io.ton.walletkit.api.generated.TONStakingProviderInfo
import io.ton.walletkit.api.generated.TONStakingProviderMetadata
import io.ton.walletkit.api.generated.TONStakingQuote
import io.ton.walletkit.api.generated.TONStakingQuoteParams
import io.ton.walletkit.api.generated.TONTransactionRequest
import io.ton.walletkit.engine.WalletKitEngine
import io.ton.walletkit.model.TONUserFriendlyAddress
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

    override suspend fun register(provider: ITONStakingProvider<*, *>) {
        if (provider is BuiltInStakingProvider<*, *>) {
            // Built-in JS-backed provider: the JS side already has the instance; just register its name.
            engine.registerStakingProvider(provider.identifier.name)
        } else {
            // Custom Kotlin provider: pre-fetch metadata + supportedNetworks (JS-side ProxyStakingProvider
            // caches them at construction — per iOS TON-841 contract), register locally so reverse-RPC
            // calls for quote/build reach the Kotlin instance, then have JS create the proxy.
            @Suppress("UNCHECKED_CAST")
            val typedProvider = provider as ITONStakingProvider<JsonElement, JsonElement>
            engine.kotlinStakingProviderManager.register(provider.identifier.name, typedProvider)
            val metadata = typedProvider.metadata()
            val supportedNetworks = typedProvider.supportedNetworks()
            engine.registerKotlinStakingProvider(provider.identifier.name, metadata, supportedNetworks)
        }
    }

    override suspend fun remove(provider: ITONStakingProvider<*, *>) {
        val name = provider.identifier.name
        engine.removeStakingProvider(name)
        // Local Kotlin registry is keyed by the same name; safe to drop unconditionally.
        engine.kotlinStakingProviderManager.unregister(name)
    }

    override suspend fun setDefaultProvider(identifier: TONStakingProviderIdentifier<*, *>) {
        engine.setDefaultStakingProvider(identifier.name)
    }

    override suspend fun providers(): List<ITONStakingProvider<JsonElement, JsonElement>> =
        engine.getRegisteredStakingProviders().map { name ->
            // Custom Kotlin provider: return the user's instance.
            engine.kotlinStakingProviderManager.getProvider(name)
                ?: BuiltInStakingProvider(
                    identifier = AnyTONStakingProviderIdentifier(name),
                    engine = engine,
                )
        }

    override suspend fun hasProvider(identifier: TONStakingProviderIdentifier<*, *>): Boolean =
        engine.hasStakingProvider(identifier.name)

    override suspend fun <TQuoteOptions, TStakeOptions> provider(
        identifier: TONStakingProviderIdentifier<TQuoteOptions, TStakeOptions>,
    ): ITONStakingProvider<TQuoteOptions, TStakeOptions>? {
        // Custom Kotlin provider: return the user's actual registered instance.
        engine.kotlinStakingProviderManager.getProvider(identifier.name)?.let { custom ->
            @Suppress("UNCHECKED_CAST")
            return custom as ITONStakingProvider<TQuoteOptions, TStakeOptions>
        }
        // Built-in JS-backed provider: wrap in a fresh handle that talks to the engine.
        return BuiltInStakingProvider(identifier, engine)
    }

    override suspend fun metadata(
        network: TONNetwork?,
        identifier: TONStakingProviderIdentifier<*, *>?,
    ): TONStakingProviderMetadata = engine.getStakingProviderMetadata(network, identifier?.name)

    override suspend fun <TQuoteOptions, TStakeOptions> getQuote(
        params: TONStakingQuoteParams<TQuoteOptions>,
        identifier: TONStakingProviderIdentifier<TQuoteOptions, TStakeOptions>,
    ): TONStakingQuote {
        val jsonOptions = params.providerOptions?.let {
            Json.encodeToJsonElement(StakingSerializers.quoteSerializer(identifier), it)
        }
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
        val jsonOptions = params.providerOptions?.let {
            Json.encodeToJsonElement(StakingSerializers.stakeSerializer(identifier), it)
        }
        val jsonParams = TONStakeParams(
            quote = params.quote,
            userAddress = params.userAddress,
            providerOptions = jsonOptions,
        )
        return engine.buildStakeTransaction(jsonParams, identifier.name)
    }

    override suspend fun buildStakeTransaction(params: TONStakeParams<JsonElement>): TONTransactionRequest =
        engine.buildStakeTransaction(params, null)

    override suspend fun getStakedBalance(
        userAddress: TONUserFriendlyAddress,
        network: TONNetwork?,
        identifier: TONStakingProviderIdentifier<*, *>?,
    ): TONStakingBalance = engine.getStakedBalance(userAddress.value, network, identifier?.name)

    override suspend fun info(
        network: TONNetwork?,
        identifier: TONStakingProviderIdentifier<*, *>?,
    ): TONStakingProviderInfo = engine.getStakingProviderInfo(network, identifier?.name)
}
