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
import kotlinx.serialization.json.JsonElement

/**
 * Reverse-RPC registry for Kotlin-implemented [ITONStakingProvider] instances. JS routes calls
 * from its `ProxyStakingProvider` here via the bridge dispatcher.
 *
 * @suppress Internal engine component.
 */
internal class KotlinStakingProviderManager :
    KotlinProviderRegistry<ITONStakingProvider<JsonElement, JsonElement>>() {

    override val tag: String = "KotlinStakingProviderManager"

    suspend fun getQuote(providerId: String, params: TONStakingQuoteParams<JsonElement>): TONStakingQuote =
        require(providerId).getQuote(params)

    suspend fun buildStakeTransaction(providerId: String, params: TONStakeParams<JsonElement>): TONTransactionRequest =
        require(providerId).buildStakeTransaction(params)

    suspend fun getStakedBalance(providerId: String, userAddress: String, networkChainId: String?): TONStakingBalance =
        require(providerId).getStakedBalance(
            TONUserFriendlyAddress.parse(userAddress),
            networkChainId?.let { TONNetwork(chainId = it) },
        )

    suspend fun info(providerId: String, networkChainId: String?): TONStakingProviderInfo =
        require(providerId).info(networkChainId?.let { TONNetwork(chainId = it) })
}
