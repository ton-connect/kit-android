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
import io.ton.walletkit.api.generated.TONSwapParams
import io.ton.walletkit.api.generated.TONSwapQuoteParams
import io.ton.walletkit.api.generated.TONSwapToken
import io.ton.walletkit.model.TONUserFriendlyAddress
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for [io.ton.walletkit.engine.operations.SwapOperations] — the bridge contract for
 * swap provider registration, metadata, quoting, and transaction building. Mirrors iOS's
 * `TONSwapManagerTests` at the bridge protocol layer.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class SwapOperationsTest : OperationsTestBase() {

    companion object {
        const val PROVIDER_ID = "omniston"
        const val USER_ADDRESS = "EQCD39VS5jcptHL8vMjEXrzGaRcCVYto7HUn4bpAOg8xqB2N"
        val MAINNET = TONNetwork(chainId = "-239")
    }

    private fun token(symbol: String): TONSwapToken = TONSwapToken(
        address = "EQ${symbol}123456789012345678901234567890123456789012",
        decimals = 9.0,
        name = symbol,
        symbol = symbol,
        chainId = MAINNET.chainId,
    )

    private fun tokenJson(symbol: String): JsonObject = buildJsonObject {
        put("address", "EQ${symbol}123456789012345678901234567890123456789012")
        put("decimals", 9)
        put("name", symbol)
        put("symbol", symbol)
        put("chainId", MAINNET.chainId)
    }

    private fun swapQuoteJson(): JsonObject = buildJsonObject {
        put("fromToken", tokenJson("TON"))
        put("toToken", tokenJson("USDT"))
        put("rawFromAmount", "1000000000")
        put("rawToAmount", "990000000")
        put("fromAmount", "1.0")
        put("toAmount", "0.99")
        put("rawMinReceived", "980000000")
        put("minReceived", "0.98")
        put("network", buildJsonObject { put("chainId", MAINNET.chainId) })
        put("providerId", PROVIDER_ID)
    }

    private fun swapQuoteModel(): io.ton.walletkit.api.generated.TONSwapQuote =
        io.ton.walletkit.api.generated.TONSwapQuote(
            fromToken = token("TON"),
            toToken = token("USDT"),
            rawFromAmount = "1000000000",
            rawToAmount = "990000000",
            fromAmount = "1.0",
            toAmount = "0.99",
            rawMinReceived = "980000000",
            minReceived = "0.98",
            network = MAINNET,
            providerId = PROVIDER_ID,
        )

    // --- register / remove / setDefault ---

    @Test
    fun registerSwapProvider_sendsProviderId() = runBlocking {
        rpcClient.registerSwapProvider(PROVIDER_ID)

        assertEquals("registerSwapProvider", capturedMethod)
        val encoded = encodeCapturedParams() as JsonObject
        assertEquals(PROVIDER_ID, (encoded["providerId"] as JsonPrimitive).content)
    }

    @Test
    fun removeSwapProvider_sendsProviderId() = runBlocking {
        rpcClient.removeSwapProvider(PROVIDER_ID)

        assertEquals("removeSwapProvider", capturedMethod)
        val encoded = encodeCapturedParams() as JsonObject
        assertEquals(PROVIDER_ID, (encoded["providerId"] as JsonPrimitive).content)
    }

    @Test
    fun setDefaultSwapProvider_sendsProviderId() = runBlocking {
        rpcClient.setDefaultSwapProvider(PROVIDER_ID)

        assertEquals("setDefaultSwapProvider", capturedMethod)
        val encoded = encodeCapturedParams() as JsonObject
        assertEquals(PROVIDER_ID, (encoded["providerId"] as JsonPrimitive).content)
    }

    // --- getRegisteredSwapProviders ---

    @Test
    fun getRegisteredSwapProviders_returnsProviderIds() = runBlocking {
        givenBridgeReturns(
            buildJsonObject {
                put(
                    "providerIds",
                    buildJsonArray {
                        add("omniston")
                        add("dedust")
                    },
                )
            },
        )

        val ids = rpcClient.getRegisteredSwapProviders()

        assertEquals(listOf("omniston", "dedust"), ids)
        assertEquals("getRegisteredSwapProviders", capturedMethod)
    }

    @Test
    fun getRegisteredSwapProviders_emptyProviders() = runBlocking {
        givenBridgeReturns(buildJsonObject { put("providerIds", JsonArray(emptyList())) })

        assertTrue(rpcClient.getRegisteredSwapProviders().isEmpty())
    }

    // --- hasSwapProvider ---

    @Test
    fun hasSwapProvider_returnsTrueWhenResultTrue() = runBlocking {
        givenBridgeReturns(buildJsonObject { put("result", true) })

        assertTrue(rpcClient.hasSwapProvider(PROVIDER_ID))
        assertEquals("hasSwapProvider", capturedMethod)
    }

    @Test
    fun hasSwapProvider_returnsFalseWhenResultFalse() = runBlocking {
        givenBridgeReturns(buildJsonObject { put("result", false) })

        assertEquals(false, rpcClient.hasSwapProvider(PROVIDER_ID))
    }

    // --- getSwapProviderMetadata ---

    @Test
    fun getSwapProviderMetadata_decodesResponse() = runBlocking {
        givenBridgeReturns(
            buildJsonObject {
                put("name", "Omniston")
                put("logo", "https://example.com/logo.png")
                put("url", "https://omniston.com")
            },
        )

        val metadata = rpcClient.getSwapProviderMetadata(PROVIDER_ID)

        assertEquals("Omniston", metadata.name)
        assertEquals("https://example.com/logo.png", metadata.logo)
        assertEquals("https://omniston.com", metadata.url)
        assertEquals("getSwapProviderMetadata", capturedMethod)
    }

    // --- getSwapProviderSupportedNetworks ---

    @Test
    fun getSwapProviderSupportedNetworks_decodesNetworks() = runBlocking {
        givenBridgeReturns(
            buildJsonObject {
                put(
                    "networks",
                    buildJsonArray {
                        add(buildJsonObject { put("chainId", "-239") })
                        add(buildJsonObject { put("chainId", "-3") })
                    },
                )
            },
        )

        val networks = rpcClient.getSwapProviderSupportedNetworks(PROVIDER_ID)

        assertEquals(2, networks.size)
        assertEquals("-239", networks[0].chainId)
        assertEquals("-3", networks[1].chainId)
        assertEquals("getSwapProviderSupportedNetworks", capturedMethod)
    }

    // --- getSwapQuote ---

    @Test
    fun getSwapQuote_passesParamsAndProviderId() = runBlocking {
        givenBridgeReturns(swapQuoteJson())

        val params = TONSwapQuoteParams<JsonElement>(
            amount = "1000000000",
            from = token("TON"),
            to = token("USDT"),
            network = MAINNET,
        )
        val quote = rpcClient.getSwapQuote(params, PROVIDER_ID)

        assertNotNull(quote)
        assertEquals(PROVIDER_ID, quote.providerId)
        assertEquals("getSwapQuote", capturedMethod)
        val encoded = encodeCapturedParams() as JsonObject
        assertEquals(PROVIDER_ID, (encoded["providerId"] as JsonPrimitive).content)
        assertTrue(encoded["params"] is JsonObject)
    }

    @Test
    fun getSwapQuote_omitsProviderIdWhenNull() = runBlocking {
        givenBridgeReturns(swapQuoteJson())

        val params = TONSwapQuoteParams<JsonElement>(
            amount = "1000",
            from = token("TON"),
            to = token("USDT"),
            network = MAINNET,
        )
        rpcClient.getSwapQuote(params, providerId = null)

        val encoded = encodeCapturedParams() as JsonObject
        val providerIdEntry = encoded["providerId"]
        assertTrue(providerIdEntry == null || providerIdEntry == JsonNull)
    }

    // --- buildSwapTransaction ---

    @Test
    fun buildSwapTransaction_sendsParams() = runBlocking {
        givenBridgeReturns(buildJsonObject { put("messages", JsonArray(emptyList())) })

        val params = TONSwapParams<JsonElement>(
            quote = swapQuoteModel(),
            userAddress = TONUserFriendlyAddress(USER_ADDRESS),
        )
        rpcClient.buildSwapTransaction(params)

        assertEquals("buildSwapTransaction", capturedMethod)
        val encoded = encodeCapturedParams() as JsonObject
        assertTrue(encoded["params"] is JsonObject)
    }

    @Test
    fun buildSwapTransaction_includesDestinationWhenSet() = runBlocking {
        givenBridgeReturns(buildJsonObject { put("messages", JsonArray(emptyList())) })

        val params = TONSwapParams<JsonElement>(
            quote = swapQuoteModel(),
            userAddress = TONUserFriendlyAddress(USER_ADDRESS),
            destinationAddress = TONUserFriendlyAddress(USER_ADDRESS),
            slippageBps = 100.0,
        )
        rpcClient.buildSwapTransaction(params)

        val encoded = encodeCapturedParams() as JsonObject
        val innerParams = encoded["params"] as JsonObject
        assertNotNull(innerParams["destinationAddress"])
        assertNull(innerParams["destinationAddress"]?.takeIf { it == JsonNull })
    }
}
