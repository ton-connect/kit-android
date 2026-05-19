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

import io.ton.walletkit.api.generated.TONDeDustSwapProviderConfig
import io.ton.walletkit.api.generated.TONOmnistonSwapProviderConfig
import io.ton.walletkit.api.generated.TONSwapParams
import io.ton.walletkit.api.generated.TONSwapQuote
import io.ton.walletkit.api.generated.TONSwapQuoteParams
import io.ton.walletkit.api.generated.TONTransactionRequest
import io.ton.walletkit.engine.infrastructure.BridgeRpcClient
import io.ton.walletkit.engine.infrastructure.callTyped
import io.ton.walletkit.engine.operations.requests.BuildSwapTransactionRequest
import io.ton.walletkit.engine.operations.requests.CreateDeDustSwapProviderRequest
import io.ton.walletkit.engine.operations.requests.CreateOmnistonSwapProviderRequest
import io.ton.walletkit.engine.operations.requests.GetSwapQuoteRequest
import io.ton.walletkit.engine.operations.requests.HasSwapProviderRequest
import io.ton.walletkit.engine.operations.requests.RegisterSwapProviderRequest
import io.ton.walletkit.engine.operations.requests.SetDefaultSwapProviderRequest
import io.ton.walletkit.engine.operations.responses.HasProviderResponse
import io.ton.walletkit.engine.operations.responses.ProviderIdResponse
import io.ton.walletkit.engine.operations.responses.ProviderIdsResponse
import io.ton.walletkit.internal.constants.BridgeMethodConstants
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

internal suspend fun BridgeRpcClient.createOmnistonSwapProvider(config: TONOmnistonSwapProviderConfig?): String =
    callTyped<ProviderIdResponse>(
        BridgeMethodConstants.METHOD_CREATE_OMNISTON_SWAP_PROVIDER,
        CreateOmnistonSwapProviderRequest(
            config = config?.let { Json.encodeToJsonElement(TONOmnistonSwapProviderConfig.serializer(), it) },
        ),
    ).providerId

internal suspend fun BridgeRpcClient.createDeDustSwapProvider(config: TONDeDustSwapProviderConfig?): String =
    callTyped<ProviderIdResponse>(
        BridgeMethodConstants.METHOD_CREATE_DEDUST_SWAP_PROVIDER,
        CreateDeDustSwapProviderRequest(
            config = config?.let { Json.encodeToJsonElement(TONDeDustSwapProviderConfig.serializer(), it) },
        ),
    ).providerId

internal suspend fun BridgeRpcClient.registerSwapProvider(providerId: String) {
    send(BridgeMethodConstants.METHOD_REGISTER_SWAP_PROVIDER, RegisterSwapProviderRequest(providerId))
}

internal suspend fun BridgeRpcClient.setDefaultSwapProvider(providerId: String) {
    send(BridgeMethodConstants.METHOD_SET_DEFAULT_SWAP_PROVIDER, SetDefaultSwapProviderRequest(providerId))
}

internal suspend fun BridgeRpcClient.getRegisteredSwapProviders(): List<String> =
    callTyped<ProviderIdsResponse>(BridgeMethodConstants.METHOD_GET_REGISTERED_SWAP_PROVIDERS).providerIds

internal suspend fun BridgeRpcClient.hasSwapProvider(providerId: String): Boolean =
    callTyped<HasProviderResponse>(
        BridgeMethodConstants.METHOD_HAS_SWAP_PROVIDER,
        HasSwapProviderRequest(providerId),
    ).result

internal suspend fun BridgeRpcClient.getSwapQuote(
    params: TONSwapQuoteParams<JsonElement>,
    providerId: String?,
): TONSwapQuote = callTyped(
    BridgeMethodConstants.METHOD_GET_SWAP_QUOTE,
    GetSwapQuoteRequest(
        params = json.encodeToJsonElement(TONSwapQuoteParams.serializer(JsonElement.serializer()), params),
        providerId = providerId,
    ),
)

internal suspend fun BridgeRpcClient.buildSwapTransaction(
    params: TONSwapParams<JsonElement>,
): TONTransactionRequest = callTyped(
    BridgeMethodConstants.METHOD_BUILD_SWAP_TRANSACTION,
    BuildSwapTransactionRequest(
        params = json.encodeToJsonElement(TONSwapParams.serializer(JsonElement.serializer()), params),
    ),
)
