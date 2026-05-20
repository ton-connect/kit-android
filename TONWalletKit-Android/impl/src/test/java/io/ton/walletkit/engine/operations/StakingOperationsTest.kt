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
import io.ton.walletkit.api.generated.TONStakingQuote
import io.ton.walletkit.api.generated.TONStakingQuoteDirection
import io.ton.walletkit.api.generated.TONStakingQuoteParams
import io.ton.walletkit.api.generated.TONUnstakeMode
import io.ton.walletkit.model.TONUserFriendlyAddress
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for [io.ton.walletkit.engine.operations.StakingOperations] — bridge contract for
 * staking provider registration, metadata, quoting, and transaction building.
 *
 * Mirrors iOS's `TONStakingManagerTests` at the bridge protocol layer.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class StakingOperationsTest : OperationsTestBase() {

    companion object {
        const val PROVIDER_ID = "tonstakers"
        const val USER_ADDRESS = "EQCD39VS5jcptHL8vMjEXrzGaRcCVYto7HUn4bpAOg8xqB2N"
        val MAINNET = TONNetwork(chainId = "-239")
    }

    private fun stakingQuoteJson(): JsonObject = buildJsonObject {
        put("direction", "stake")
        put("rawAmountIn", "1000000000")
        put("rawAmountOut", "950000000")
        put("amountIn", "1.0")
        put("amountOut", "0.95")
        put("network", buildJsonObject { put("chainId", MAINNET.chainId) })
        put("providerId", PROVIDER_ID)
    }

    private fun stakingQuoteModel(): TONStakingQuote = TONStakingQuote(
        direction = TONStakingQuoteDirection.stake,
        rawAmountIn = "1000000000",
        rawAmountOut = "950000000",
        amountIn = "1.0",
        amountOut = "0.95",
        network = MAINNET,
        providerId = PROVIDER_ID,
    )

    // --- createTonStakersStakingProvider ---

    @Test
    fun createTonStakersStakingProvider_returnsProviderId() = runBlocking {
        givenBridgeReturns(buildJsonObject { put("providerId", PROVIDER_ID) })

        val id = rpcClient.createTonStakersStakingProvider(chainConfig = null)

        assertEquals(PROVIDER_ID, id)
        assertEquals("createTonStakersStakingProvider", capturedMethod)
    }

    // --- register / remove / setDefault ---

    @Test
    fun registerStakingProvider_sendsProviderId() = runBlocking {
        rpcClient.registerStakingProvider(PROVIDER_ID)

        assertEquals("registerStakingProvider", capturedMethod)
        val encoded = encodeCapturedParams() as JsonObject
        assertEquals(PROVIDER_ID, (encoded["providerId"] as JsonPrimitive).content)
    }

    @Test
    fun removeStakingProvider_sendsProviderId() = runBlocking {
        rpcClient.removeStakingProvider(PROVIDER_ID)

        assertEquals("removeStakingProvider", capturedMethod)
        val encoded = encodeCapturedParams() as JsonObject
        assertEquals(PROVIDER_ID, (encoded["providerId"] as JsonPrimitive).content)
    }

    @Test
    fun setDefaultStakingProvider_sendsProviderId() = runBlocking {
        rpcClient.setDefaultStakingProvider(PROVIDER_ID)

        assertEquals("setDefaultStakingProvider", capturedMethod)
        val encoded = encodeCapturedParams() as JsonObject
        assertEquals(PROVIDER_ID, (encoded["providerId"] as JsonPrimitive).content)
    }

    // --- getRegisteredStakingProviders ---

    @Test
    fun getRegisteredStakingProviders_returnsProviderIds() = runBlocking {
        givenBridgeReturns(
            buildJsonObject {
                put("providerIds", buildJsonArray { add("tonstakers") })
            },
        )

        val ids = rpcClient.getRegisteredStakingProviders()

        assertEquals(listOf("tonstakers"), ids)
        assertEquals("getRegisteredStakingProviders", capturedMethod)
    }

    @Test
    fun getRegisteredStakingProviders_emptyProviders() = runBlocking {
        givenBridgeReturns(buildJsonObject { put("providerIds", JsonArray(emptyList())) })

        assertTrue(rpcClient.getRegisteredStakingProviders().isEmpty())
    }

    // --- hasStakingProvider ---

    @Test
    fun hasStakingProvider_returnsTrueWhenResultTrue() = runBlocking {
        givenBridgeReturns(buildJsonObject { put("result", true) })

        assertTrue(rpcClient.hasStakingProvider(PROVIDER_ID))
        assertEquals("hasStakingProvider", capturedMethod)
    }

    @Test
    fun hasStakingProvider_returnsFalseWhenResultFalse() = runBlocking {
        givenBridgeReturns(buildJsonObject { put("result", false) })

        assertEquals(false, rpcClient.hasStakingProvider(PROVIDER_ID))
    }

    // --- getStakingProviderMetadata ---

    @Test
    fun getStakingProviderMetadata_decodesResponse() = runBlocking {
        givenBridgeReturns(
            buildJsonObject {
                put("name", "Tonstakers")
                put(
                    "supportedUnstakeModes",
                    buildJsonArray { add("INSTANT"); add("WHEN_AVAILABLE") },
                )
                put("supportsReversedQuote", true)
                put(
                    "stakeToken",
                    buildJsonObject {
                        put("ticker", "TON")
                        put("decimals", 9)
                        put("address", "ton")
                    },
                )
            },
        )

        val metadata = rpcClient.getStakingProviderMetadata(network = MAINNET, providerId = PROVIDER_ID)

        assertEquals("Tonstakers", metadata.name)
        assertEquals(2, metadata.supportedUnstakeModes.size)
        assertEquals(TONUnstakeMode.instant, metadata.supportedUnstakeModes[0])
        assertEquals("TON", metadata.stakeToken.ticker)
        assertEquals("getStakingProviderMetadata", capturedMethod)
    }

    @Test
    fun getStakingProviderMetadata_passesProviderIdAndNetwork() = runBlocking {
        givenBridgeReturns(
            buildJsonObject {
                put("name", "Tonstakers")
                put("supportedUnstakeModes", JsonArray(emptyList()))
                put("supportsReversedQuote", false)
                put(
                    "stakeToken",
                    buildJsonObject {
                        put("ticker", "TON"); put("decimals", 9); put("address", "ton")
                    },
                )
            },
        )

        rpcClient.getStakingProviderMetadata(network = MAINNET, providerId = PROVIDER_ID)

        val encoded = encodeCapturedParams() as JsonObject
        assertEquals(PROVIDER_ID, (encoded["providerId"] as JsonPrimitive).content)
        assertNotNull(encoded["network"])
    }

    // --- getStakingProviderSupportedNetworks ---

    @Test
    fun getStakingProviderSupportedNetworks_decodesNetworks() = runBlocking {
        givenBridgeReturns(
            buildJsonObject {
                put(
                    "networks",
                    buildJsonArray {
                        add(buildJsonObject { put("chainId", "-239") })
                    },
                )
            },
        )

        val networks = rpcClient.getStakingProviderSupportedNetworks(PROVIDER_ID)

        assertEquals(1, networks.size)
        assertEquals("-239", networks[0].chainId)
        assertEquals("getStakingProviderSupportedNetworks", capturedMethod)
    }

    // --- getStakingQuote ---

    @Test
    fun getStakingQuote_passesParamsAndProviderId() = runBlocking {
        givenBridgeReturns(stakingQuoteJson())

        val params = TONStakingQuoteParams<JsonElement>(
            direction = TONStakingQuoteDirection.stake,
            amount = "1000000000",
            userAddress = TONUserFriendlyAddress(USER_ADDRESS),
            network = MAINNET,
        )
        val quote = rpcClient.getStakingQuote(params, PROVIDER_ID)

        assertEquals(TONStakingQuoteDirection.stake, quote.direction)
        assertEquals(PROVIDER_ID, quote.providerId)
        assertEquals("getStakingQuote", capturedMethod)
        val encoded = encodeCapturedParams() as JsonObject
        assertEquals(PROVIDER_ID, (encoded["providerId"] as JsonPrimitive).content)
        assertEquals("stake", (encoded["direction"] as JsonPrimitive).content)
        assertEquals("1000000000", (encoded["amount"] as JsonPrimitive).content)
    }

    @Test
    fun getStakingQuote_unstakeWithMode() = runBlocking {
        givenBridgeReturns(
            buildJsonObject {
                put("direction", "unstake")
                put("rawAmountIn", "950000000")
                put("rawAmountOut", "1000000000")
                put("amountIn", "0.95")
                put("amountOut", "1.0")
                put("network", buildJsonObject { put("chainId", MAINNET.chainId) })
                put("providerId", PROVIDER_ID)
                put("unstakeMode", "INSTANT")
            },
        )

        val params = TONStakingQuoteParams<JsonElement>(
            direction = TONStakingQuoteDirection.unstake,
            amount = "950000000",
            userAddress = TONUserFriendlyAddress(USER_ADDRESS),
            network = MAINNET,
            unstakeMode = TONUnstakeMode.instant,
        )
        val quote = rpcClient.getStakingQuote(params, PROVIDER_ID)

        assertEquals(TONUnstakeMode.instant, quote.unstakeMode)
        val encoded = encodeCapturedParams() as JsonObject
        assertEquals("INSTANT", (encoded["unstakeMode"] as JsonPrimitive).content)
    }

    // --- buildStakeTransaction ---

    @Test
    fun buildStakeTransaction_sendsQuoteAndUserAddress() = runBlocking {
        givenBridgeReturns(buildJsonObject { put("messages", JsonArray(emptyList())) })

        val params = TONStakeParams<JsonElement>(
            quote = stakingQuoteModel(),
            userAddress = TONUserFriendlyAddress(USER_ADDRESS),
        )
        rpcClient.buildStakeTransaction(params, PROVIDER_ID)

        assertEquals("buildStakeTransaction", capturedMethod)
        val encoded = encodeCapturedParams() as JsonObject
        assertEquals(PROVIDER_ID, (encoded["providerId"] as JsonPrimitive).content)
        assertEquals(USER_ADDRESS, (encoded["userAddress"] as JsonPrimitive).content)
        assertTrue(encoded["quote"] is JsonObject)
    }

    // --- getStakedBalance ---

    @Test
    fun getStakedBalance_decodesBalance() = runBlocking {
        givenBridgeReturns(
            buildJsonObject {
                put("rawStakedBalance", "5000000000")
                put("stakedBalance", "5.0")
                put("rawInstantUnstakeAvailable", "4000000000")
                put("instantUnstakeAvailable", "4.0")
                put("providerId", PROVIDER_ID)
            },
        )

        val balance = rpcClient.getStakedBalance(
            userAddress = USER_ADDRESS,
            network = MAINNET,
            providerId = PROVIDER_ID,
        )

        assertEquals("5.0", balance.stakedBalance)
        assertEquals("4.0", balance.instantUnstakeAvailable)
        assertEquals(PROVIDER_ID, balance.providerId)
        assertEquals("getStakedBalance", capturedMethod)
    }

    // --- getStakingProviderInfo ---

    @Test
    fun getStakingProviderInfo_decodesApyAndRate() = runBlocking {
        givenBridgeReturns(
            buildJsonObject {
                put("apy", 500.0)
                put("rawInstantUnstakeAvailable", "1000000000")
                put("instantUnstakeAvailable", "1.0")
                put("exchangeRate", "0.95")
            },
        )

        val info = rpcClient.getStakingProviderInfo(network = MAINNET, providerId = PROVIDER_ID)

        assertEquals(500.0, info.apy, 0.001)
        assertEquals("1.0", info.instantUnstakeAvailable)
        assertEquals("0.95", info.exchangeRate)
        assertEquals("getStakingProviderInfo", capturedMethod)
    }
}
