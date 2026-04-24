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
import io.ton.walletkit.engine.infrastructure.BridgeRpcClient
import io.ton.walletkit.engine.infrastructure.toJSONObject
import io.ton.walletkit.engine.operations.requests.BuildSwapTransactionRequest
import io.ton.walletkit.engine.operations.requests.CreateDeDustSwapProviderRequest
import io.ton.walletkit.engine.operations.requests.CreateOmnistonSwapProviderRequest
import io.ton.walletkit.engine.operations.requests.GetSwapQuoteRequest
import io.ton.walletkit.engine.operations.requests.HasSwapProviderRequest
import io.ton.walletkit.engine.operations.requests.RegisterSwapProviderRequest
import io.ton.walletkit.engine.operations.requests.SetDefaultSwapProviderRequest
import io.ton.walletkit.exceptions.JSValueConversionException
import io.ton.walletkit.internal.constants.BridgeMethodConstants
import io.ton.walletkit.internal.constants.ResponseConstants
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/**
 * Swap bridge operations: provider creation/registration and quote/transaction building.
 *
 * @suppress Internal component used by [WebViewWalletKitEngine].
 */
internal class SwapOperations(
    private val ensureInitialized: suspend () -> Unit,
    private val rpcClient: BridgeRpcClient,
    private val json: Json,
) {

    suspend fun createOmnistonSwapProvider(config: TONOmnistonSwapProviderConfig?): String {
        ensureInitialized()
        val configElement = config?.let { json.encodeToJsonElement(TONOmnistonSwapProviderConfig.serializer(), it) }
        val request = CreateOmnistonSwapProviderRequest(config = configElement)
        val result = rpcClient.call(BridgeMethodConstants.METHOD_CREATE_OMNISTON_SWAP_PROVIDER, json.toJSONObject(request))
        return result.getString(ResponseConstants.KEY_PROVIDER_ID)
    }

    suspend fun createDeDustSwapProvider(config: TONDeDustSwapProviderConfig?): String {
        ensureInitialized()
        val configElement = config?.let { json.encodeToJsonElement(TONDeDustSwapProviderConfig.serializer(), it) }
        val request = CreateDeDustSwapProviderRequest(config = configElement)
        val result = rpcClient.call(BridgeMethodConstants.METHOD_CREATE_DEDUST_SWAP_PROVIDER, json.toJSONObject(request))
        return result.getString(ResponseConstants.KEY_PROVIDER_ID)
    }

    suspend fun registerSwapProvider(providerId: String) {
        ensureInitialized()
        val request = RegisterSwapProviderRequest(providerId = providerId)
        rpcClient.call(BridgeMethodConstants.METHOD_REGISTER_SWAP_PROVIDER, json.toJSONObject(request))
    }

    suspend fun setDefaultSwapProvider(providerId: String) {
        ensureInitialized()
        val request = SetDefaultSwapProviderRequest(providerId = providerId)
        rpcClient.call(BridgeMethodConstants.METHOD_SET_DEFAULT_SWAP_PROVIDER, json.toJSONObject(request))
    }

    suspend fun getRegisteredSwapProviders(): List<String> {
        ensureInitialized()
        val result = rpcClient.call(BridgeMethodConstants.METHOD_GET_REGISTERED_SWAP_PROVIDERS, null)
        val array = result.getJSONArray("providerIds")
        return List(array.length()) { array.getString(it) }
    }

    suspend fun hasSwapProvider(providerId: String): Boolean {
        ensureInitialized()
        val request = HasSwapProviderRequest(providerId = providerId)
        val result = rpcClient.call(BridgeMethodConstants.METHOD_HAS_SWAP_PROVIDER, json.toJSONObject(request))
        return result.getBoolean("result")
    }

    suspend fun getSwapQuote(params: TONSwapQuoteParams<JsonElement>, providerId: String?): TONSwapQuote {
        ensureInitialized()
        val paramsElement = json.encodeToJsonElement(TONSwapQuoteParams.serializer(JsonElement.serializer()), params)
        val request = GetSwapQuoteRequest(params = paramsElement, providerId = providerId)
        val result = rpcClient.call(BridgeMethodConstants.METHOD_GET_SWAP_QUOTE, json.toJSONObject(request))
        return try {
            json.decodeFromString(TONSwapQuote.serializer(), result.toString())
        } catch (e: SerializationException) {
            throw JSValueConversionException.DecodingError(
                message = "Failed to decode TONSwapQuote: ${e.message}",
                cause = e,
            )
        }
    }

    suspend fun buildSwapTransaction(params: TONSwapParams<JsonElement>): String {
        ensureInitialized()
        val paramsElement = json.encodeToJsonElement(TONSwapParams.serializer(JsonElement.serializer()), params)
        val request = BuildSwapTransactionRequest(params = paramsElement)
        val result = rpcClient.call(BridgeMethodConstants.METHOD_BUILD_SWAP_TRANSACTION, json.toJSONObject(request))
        return result.toString()
    }
}
