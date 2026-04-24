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
import io.ton.walletkit.api.generated.TONStakingQuote
import io.ton.walletkit.api.generated.TONStakingQuoteParams
import io.ton.walletkit.api.generated.TONTransactionRequest
import io.ton.walletkit.api.generated.TONUnstakeMode
import io.ton.walletkit.engine.WalletKitEngine
import io.ton.walletkit.engine.infrastructure.decodeTransactionRequest
import io.ton.walletkit.model.TONUserFriendlyAddress
import kotlinx.serialization.json.Json

/**
 * Built-in, JS-backed implementation of [ITONStakingProvider]. Talks to the JS engine directly;
 * does not hold a reference to [ITONStakingManager] — the engine is enough.
 *
 * Users never construct this: it's created by the SDK (e.g. via [io.ton.walletkit.ITONWalletKit.tonStakersStakingProvider])
 * and distinguishes the "built-in" case from user-defined conformers of [ITONStakingProvider] inside
 * [TONStakingManager.register].
 *
 * @suppress Internal implementation.
 */
internal class BuiltInStakingProvider<TQuoteOptions, TStakeOptions>(
    override val identifier: TONStakingProviderIdentifier<TQuoteOptions, TStakeOptions>,
    private val engine: WalletKitEngine,
) : ITONStakingProvider<TQuoteOptions, TStakeOptions> {

    override suspend fun getQuote(params: TONStakingQuoteParams<TQuoteOptions>): TONStakingQuote {
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

    override suspend fun buildStakeTransaction(params: TONStakeParams<TStakeOptions>): TONTransactionRequest {
        val jsonOptions = params.providerOptions?.let {
            Json.encodeToJsonElement(StakingSerializers.stakeSerializer(identifier), it)
        }
        val jsonParams = TONStakeParams(
            quote = params.quote,
            userAddress = params.userAddress,
            providerOptions = jsonOptions,
        )
        return decodeTransactionRequest(engine.buildStakeTransaction(jsonParams, identifier.name))
    }

    override suspend fun getStakedBalance(
        userAddress: TONUserFriendlyAddress,
        network: TONNetwork?,
    ): TONStakingBalance = engine.getStakedBalance(userAddress.value, network, identifier.name)

    override suspend fun getStakingProviderInfo(network: TONNetwork?): TONStakingProviderInfo =
        engine.getStakingProviderInfo(network, identifier.name)

    override suspend fun getSupportedUnstakeModes(): List<TONUnstakeMode> =
        engine.getSupportedUnstakeModes(identifier.name)
}
