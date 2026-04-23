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
import io.ton.walletkit.engine.infrastructure.decodeTransactionRequest
import kotlinx.serialization.json.Json

/**
 * Built-in, JS-backed implementation of [ITONSwapProvider]. Talks to the JS engine directly;
 * does not hold a reference to [ITONSwapManager] — the engine is enough.
 *
 * Users never construct this: it's created by the SDK (e.g. via [io.ton.walletkit.ITONWalletKit.omnistonSwapProvider])
 * and distinguishes the "built-in" case from user-defined conformers of [ITONSwapProvider] inside
 * [TONSwapManager.registerProvider].
 *
 * @suppress Internal implementation.
 */
internal class BuiltInSwapProvider<TQuoteOptions, TSwapOptions>(
    override val identifier: TONSwapProviderIdentifier<TQuoteOptions, TSwapOptions>,
    private val engine: WalletKitEngine,
) : ITONSwapProvider<TQuoteOptions, TSwapOptions> {

    override suspend fun quote(params: TONSwapQuoteParams<TQuoteOptions>): TONSwapQuote {
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

    override suspend fun buildSwapTransaction(params: TONSwapParams<TSwapOptions>): TONTransactionRequest {
        val jsonOptions = params.providerOptions?.let {
            Json.encodeToJsonElement(SwapSerializers.swapOptionsSerializer(identifier), it)
        }
        val jsonParams = TONSwapParams(
            quote = params.quote,
            userAddress = params.userAddress,
            destinationAddress = params.destinationAddress,
            slippageBps = params.slippageBps,
            deadline = params.deadline,
            providerOptions = jsonOptions,
        )
        return decodeTransactionRequest(engine.buildSwapTransaction(jsonParams))
    }
}
