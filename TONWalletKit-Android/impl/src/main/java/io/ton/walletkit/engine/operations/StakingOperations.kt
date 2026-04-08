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
import io.ton.walletkit.api.generated.TONUnstakeMode
import io.ton.walletkit.engine.infrastructure.BridgeRpcClient
import io.ton.walletkit.engine.infrastructure.toJSONObject
import io.ton.walletkit.engine.operations.requests.BuildStakeTransactionRequest
import io.ton.walletkit.engine.operations.requests.CreateTonStakersStakingProviderRequest
import io.ton.walletkit.engine.operations.requests.GetStakedBalanceRequest
import io.ton.walletkit.engine.operations.requests.GetStakingProviderInfoRequest
import io.ton.walletkit.engine.operations.requests.GetStakingQuoteRequest
import io.ton.walletkit.engine.operations.requests.GetSupportedUnstakeModesRequest
import io.ton.walletkit.engine.operations.requests.RegisterStakingProviderRequest
import io.ton.walletkit.engine.operations.requests.SetDefaultStakingProviderRequest
import io.ton.walletkit.exceptions.JSValueConversionException
import io.ton.walletkit.internal.constants.BridgeMethodConstants
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/**
 * Contains staking-related bridge calls for creating providers, fetching quotes,
 * building stake/unstake transactions, and querying balances.
 *
 * @property ensureInitialized Suspended callback to guarantee bridge initialisation.
 * @property rpcClient Bridge RPC transport.
 * @property json Serializer for encoding and decoding bridge payloads.
 *
 * @suppress Internal component used by [WebViewWalletKitEngine].
 */
internal class StakingOperations(
    private val ensureInitialized: suspend () -> Unit,
    private val rpcClient: BridgeRpcClient,
    private val json: Json,
) {

    /**
     * Creates a TonStakers staking provider in the JS bridge and returns its registry ID.
     *
     * @param chainConfig Chain-ID keyed config map, e.g. { "-239": { tonApiToken = "..." } }
     * @return JS registry reference ID for the created provider
     */
    suspend fun createTonStakersStakingProvider(chainConfig: Map<String, TONTonStakersChainConfig>?): String {
        ensureInitialized()

        val request = CreateTonStakersStakingProviderRequest(config = chainConfig)
        val result = rpcClient.call(BridgeMethodConstants.METHOD_CREATE_TON_STAKERS_STAKING_PROVIDER, json.toJSONObject(request))
        return result.getString("providerId")
    }

    suspend fun registerStakingProvider(providerId: String) {
        ensureInitialized()

        val request = RegisterStakingProviderRequest(providerId = providerId)
        rpcClient.call(BridgeMethodConstants.METHOD_REGISTER_STAKING_PROVIDER, json.toJSONObject(request))
    }

    suspend fun setDefaultStakingProvider(providerId: String) {
        ensureInitialized()

        val request = SetDefaultStakingProviderRequest(providerId = providerId)
        rpcClient.call(BridgeMethodConstants.METHOD_SET_DEFAULT_STAKING_PROVIDER, json.toJSONObject(request))
    }

    suspend fun getStakingQuote(params: TONStakingQuoteParams<JsonElement>, providerId: String?): TONStakingQuote {
        ensureInitialized()

        val request = GetStakingQuoteRequest(
            direction = params.direction,
            amount = params.amount,
            userAddress = params.userAddress?.value,
            network = params.network,
            unstakeMode = params.unstakeMode,
            providerOptions = params.providerOptions,
            providerId = providerId,
        )
        val result = rpcClient.call(BridgeMethodConstants.METHOD_GET_STAKING_QUOTE, json.toJSONObject(request))
        return try {
            json.decodeFromString(result.toString())
        } catch (e: SerializationException) {
            throw JSValueConversionException.DecodingError(
                message = "Failed to decode TONStakingQuote: ${e.message}",
                cause = e,
            )
        }
    }

    suspend fun buildStakeTransaction(params: TONStakeParams<JsonElement>, providerId: String?): String {
        ensureInitialized()

        val request = BuildStakeTransactionRequest(
            quote = params.quote,
            userAddress = params.userAddress.value,
            providerOptions = params.providerOptions,
            providerId = providerId,
        )
        val result = rpcClient.call(BridgeMethodConstants.METHOD_BUILD_STAKE_TRANSACTION, json.toJSONObject(request))
        return result.toString()
    }

    suspend fun getStakedBalance(userAddress: String, network: TONNetwork?, providerId: String?): TONStakingBalance {
        ensureInitialized()

        val request = GetStakedBalanceRequest(
            userAddress = userAddress,
            network = network,
            providerId = providerId,
        )
        val result = rpcClient.call(BridgeMethodConstants.METHOD_GET_STAKED_BALANCE, json.toJSONObject(request))
        return try {
            json.decodeFromString(result.toString())
        } catch (e: SerializationException) {
            throw JSValueConversionException.DecodingError(
                message = "Failed to decode TONStakingBalance: ${e.message}",
                cause = e,
            )
        }
    }

    suspend fun getStakingProviderInfo(network: TONNetwork?, providerId: String?): TONStakingProviderInfo {
        ensureInitialized()

        val request = GetStakingProviderInfoRequest(network = network, providerId = providerId)
        val result = rpcClient.call(BridgeMethodConstants.METHOD_GET_STAKING_PROVIDER_INFO, json.toJSONObject(request))
        return try {
            json.decodeFromString(result.toString())
        } catch (e: SerializationException) {
            throw JSValueConversionException.DecodingError(
                message = "Failed to decode TONStakingProviderInfo: ${e.message}",
                cause = e,
            )
        }
    }

    suspend fun getSupportedUnstakeModes(providerId: String?): List<TONUnstakeMode> {
        ensureInitialized()

        val request = GetSupportedUnstakeModesRequest(providerId = providerId)
        val result = rpcClient.call(BridgeMethodConstants.METHOD_GET_SUPPORTED_UNSTAKE_MODES, json.toJSONObject(request))
        return try {
            json.decodeFromString<List<TONUnstakeMode>>(result.toString())
        } catch (e: SerializationException) {
            throw JSValueConversionException.DecodingError(
                message = "Failed to decode unstake modes: ${e.message}",
                cause = e,
            )
        }
    }
}
