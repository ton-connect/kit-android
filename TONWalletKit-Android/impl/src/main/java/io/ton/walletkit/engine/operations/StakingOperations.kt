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
package io.ton.walletkit.engine.operations

import io.ton.walletkit.api.generated.TONNetwork
import io.ton.walletkit.api.generated.TONStakeParams
import io.ton.walletkit.api.generated.TONStakingBalance
import io.ton.walletkit.api.generated.TONStakingProviderInfo
import io.ton.walletkit.api.generated.TONStakingQuote
import io.ton.walletkit.api.generated.TONStakingQuoteParams
import io.ton.walletkit.api.generated.TONTonStakersChainConfig
import io.ton.walletkit.api.generated.TONTransactionRequest
import io.ton.walletkit.api.generated.TONUnstakeMode
import io.ton.walletkit.engine.infrastructure.BridgeRpcClient
import io.ton.walletkit.engine.infrastructure.callTyped
import io.ton.walletkit.engine.operations.requests.BuildStakeTransactionRequest
import io.ton.walletkit.engine.operations.requests.CreateTonStakersStakingProviderRequest
import io.ton.walletkit.engine.operations.requests.GetStakedBalanceRequest
import io.ton.walletkit.engine.operations.requests.GetStakingProviderInfoRequest
import io.ton.walletkit.engine.operations.requests.GetStakingQuoteRequest
import io.ton.walletkit.engine.operations.requests.GetSupportedUnstakeModesRequest
import io.ton.walletkit.engine.operations.requests.HasStakingProviderRequest
import io.ton.walletkit.engine.operations.requests.RegisterStakingProviderRequest
import io.ton.walletkit.engine.operations.requests.SetDefaultStakingProviderRequest
import io.ton.walletkit.engine.operations.responses.HasProviderResponse
import io.ton.walletkit.engine.operations.responses.ProviderIdResponse
import io.ton.walletkit.engine.operations.responses.ProviderIdsResponse
import io.ton.walletkit.internal.constants.BridgeMethodConstants
import kotlinx.serialization.json.JsonElement

internal suspend fun BridgeRpcClient.createTonStakersStakingProvider(
    chainConfig: Map<String, TONTonStakersChainConfig>?,
): String = callTyped<ProviderIdResponse>(
    BridgeMethodConstants.METHOD_CREATE_TON_STAKERS_STAKING_PROVIDER,
    CreateTonStakersStakingProviderRequest(config = chainConfig),
).providerId

internal suspend fun BridgeRpcClient.registerStakingProvider(providerId: String) {
    send(BridgeMethodConstants.METHOD_REGISTER_STAKING_PROVIDER, RegisterStakingProviderRequest(providerId))
}

internal suspend fun BridgeRpcClient.setDefaultStakingProvider(providerId: String) {
    send(BridgeMethodConstants.METHOD_SET_DEFAULT_STAKING_PROVIDER, SetDefaultStakingProviderRequest(providerId))
}

internal suspend fun BridgeRpcClient.getRegisteredStakingProviders(): List<String> =
    callTyped<ProviderIdsResponse>(BridgeMethodConstants.METHOD_GET_REGISTERED_STAKING_PROVIDERS).providerIds

internal suspend fun BridgeRpcClient.hasStakingProvider(providerId: String): Boolean =
    callTyped<HasProviderResponse>(
        BridgeMethodConstants.METHOD_HAS_STAKING_PROVIDER,
        HasStakingProviderRequest(providerId),
    ).result

internal suspend fun BridgeRpcClient.getStakingQuote(
    params: TONStakingQuoteParams<JsonElement>,
    providerId: String?,
): TONStakingQuote = callTyped(
    BridgeMethodConstants.METHOD_GET_STAKING_QUOTE,
    GetStakingQuoteRequest(
        direction = params.direction,
        amount = params.amount,
        userAddress = params.userAddress?.value,
        network = params.network,
        unstakeMode = params.unstakeMode,
        providerOptions = params.providerOptions,
        providerId = providerId,
    ),
)

internal suspend fun BridgeRpcClient.buildStakeTransaction(
    params: TONStakeParams<JsonElement>,
    providerId: String?,
): TONTransactionRequest = callTyped(
    BridgeMethodConstants.METHOD_BUILD_STAKE_TRANSACTION,
    BuildStakeTransactionRequest(
        quote = params.quote,
        userAddress = params.userAddress.value,
        providerOptions = params.providerOptions,
        providerId = providerId,
    ),
)

internal suspend fun BridgeRpcClient.getStakedBalance(
    userAddress: String,
    network: TONNetwork?,
    providerId: String?,
): TONStakingBalance = callTyped(
    BridgeMethodConstants.METHOD_GET_STAKED_BALANCE,
    GetStakedBalanceRequest(userAddress = userAddress, network = network, providerId = providerId),
)

internal suspend fun BridgeRpcClient.getStakingProviderInfo(
    network: TONNetwork?,
    providerId: String?,
): TONStakingProviderInfo = callTyped(
    BridgeMethodConstants.METHOD_GET_STAKING_PROVIDER_INFO,
    GetStakingProviderInfoRequest(network = network, providerId = providerId),
)

internal suspend fun BridgeRpcClient.getSupportedUnstakeModes(providerId: String?): List<TONUnstakeMode> =
    callTyped(
        BridgeMethodConstants.METHOD_GET_SUPPORTED_UNSTAKE_MODES,
        GetSupportedUnstakeModesRequest(providerId = providerId),
    )
