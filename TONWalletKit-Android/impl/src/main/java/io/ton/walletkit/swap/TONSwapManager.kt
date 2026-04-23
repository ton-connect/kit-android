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
import io.ton.walletkit.api.generated.TONTransactionRequest
import io.ton.walletkit.engine.WalletKitEngine
import io.ton.walletkit.exceptions.JSValueConversionException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

internal class TONSwapManager(
    private val engine: WalletKitEngine,
) : ITONSwapManager {

    override suspend fun registerProvider(provider: ITONSwapProvider<*, *>) {
        if (provider is BuiltInSwapProvider<*, *>) {
            // Built-in JS-backed provider: the JS side already has the instance; just register its name.
            engine.registerSwapProvider(provider.identifier.name)
        } else {
            // Custom Kotlin provider: register locally so reverse-RPC calls from JS's ProxySwapProvider
            // can reach it, then tell JS to create the proxy and register it with the JS swap manager.
            @Suppress("UNCHECKED_CAST")
            engine.kotlinSwapProviderManager.register(
                provider.identifier.name,
                provider as ITONSwapProvider<JsonElement, JsonElement>,
            )
            engine.registerKotlinSwapProvider(provider.identifier.name)
        }
    }

    override suspend fun setDefaultProvider(identifier: TONSwapProviderIdentifier<*, *>) {
        engine.setDefaultSwapProvider(identifier.name)
    }

    override suspend fun registeredProviders(): List<AnyTONSwapProviderIdentifier> =
        engine.getRegisteredSwapProviders().map { AnyTONSwapProviderIdentifier(it) }

    override suspend fun hasProvider(identifier: TONSwapProviderIdentifier<*, *>): Boolean =
        engine.hasSwapProvider(identifier.name)

    override suspend fun <TQuoteOptions, TSwapOptions> provider(
        identifier: TONSwapProviderIdentifier<TQuoteOptions, TSwapOptions>,
    ): ITONSwapProvider<TQuoteOptions, TSwapOptions>? {
        // Custom Kotlin provider: return the user's actual registered instance.
        engine.kotlinSwapProviderManager.getProvider(identifier.name)?.let { custom ->
            @Suppress("UNCHECKED_CAST")
            return custom as ITONSwapProvider<TQuoteOptions, TSwapOptions>
        }
        // Built-in JS-backed provider: wrap in a fresh handle that talks to the engine.
        return BuiltInSwapProvider(identifier, engine)
    }

    override suspend fun <TQuoteOptions, TSwapOptions> getQuote(
        params: TONSwapQuoteParams<TQuoteOptions>,
        identifier: TONSwapProviderIdentifier<TQuoteOptions, TSwapOptions>,
    ): TONSwapQuote {
        val jsonOptions = params.providerOptions?.let {
            Json.encodeToJsonElement(SwapSerializers.quoteSerializer(identifier), it)
        }
        val jsonParams = TONSwapQuoteParams(
            amount = params.amount,
            from = params.from,
            to = params.to,
            network = params.network,
            slippageBps = params.slippageBps,
            maxOutgoingMessages = params.maxOutgoingMessages,
            providerOptions = jsonOptions,
            isReverseSwap = params.isReverseSwap,
        )
        return engine.getSwapQuote(jsonParams, identifier.name)
    }

    override suspend fun getQuote(params: TONSwapQuoteParams<JsonElement>): TONSwapQuote =
        engine.getSwapQuote(params, null)

    override suspend fun buildSwapTransaction(params: TONSwapParams<JsonElement>): TONTransactionRequest =
        decodeTransactionRequest(engine.buildSwapTransaction(params))

    private fun decodeTransactionRequest(json: String): TONTransactionRequest = try {
        Json.decodeFromString(TONTransactionRequest.serializer(), json)
    } catch (e: SerializationException) {
        throw JSValueConversionException.DecodingError(
            message = "Failed to decode TONTransactionRequest: ${e.message}",
            cause = e,
        )
    }
}
