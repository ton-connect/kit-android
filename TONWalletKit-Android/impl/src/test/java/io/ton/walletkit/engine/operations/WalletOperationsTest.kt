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

import io.ton.walletkit.api.WalletVersions
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for the wallet bridge extensions on [io.ton.walletkit.engine.infrastructure.BridgeRpcClient].
 *
 * These cover the raw bridge ↔ JSON contract: request encoding, response decoding into DTOs,
 * and the array-vs-envelope shapes the JS side actually returns. WalletAccount mapping
 * (hex prefix stripping, default version, etc.) lives in WebViewWalletKitEngine and is
 * exercised by the engine-level tests.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class WalletOperationsTest : OperationsTestBase() {

    companion object {
        const val TEST_ADDRESS_1 = "EQCD39VS5jcptHL8vMjEXrzGaRcCVYto7HUn4bpAOg8xqB2N"
    }

    // --- createSignerFromMnemonic ---

    @Test
    fun createSignerFromMnemonic_decodesEnvelope() = runBlocking {
        givenBridgeReturns(jsonOf("signerId" to "signer-123", "publicKey" to "0xabcdef"))

        val response = rpcClient.createSignerFromMnemonic(listOf("word1", "word2"), "ton")

        assertEquals("signer-123", response.signerId)
        assertEquals("0xabcdef", response.publicKey)
    }

    // --- getWallets ---

    @Test
    fun getWallets_decodesRawArray() = runBlocking {
        givenBridgeReturnsRaw(
            buildJsonArray {
                add(
                    buildJsonObject {
                        put("walletId", "-239:wallet-1")
                        put(
                            "wallet",
                            buildJsonObject {
                                put("publicKey", "0xpub1")
                                put("version", WalletVersions.V5R1)
                            },
                        )
                    },
                )
            },
        )

        val response = rpcClient.getWallets()

        assertEquals(1, response.size)
        assertEquals("-239:wallet-1", response[0].walletId)
        assertEquals("0xpub1", response[0].wallet?.publicKey)
        assertEquals(WalletVersions.V5R1, response[0].wallet?.version)
    }

    @Test
    fun getWallets_returnsEmptyListIfEmptyArray() = runBlocking {
        givenBridgeReturnsRaw(JsonArray(emptyList()))

        assertTrue(rpcClient.getWallets().isEmpty())
    }

    // --- getWallet ---

    @Test
    fun getWallet_decodesEnvelope() = runBlocking {
        givenBridgeReturns(
            jsonOf(
                "walletId" to "-239:$TEST_ADDRESS_1",
                "wallet" to buildJsonObject {
                    put("publicKey", "0xsinglekey")
                    put("version", WalletVersions.V5R1)
                },
            ),
        )

        val response = rpcClient.getWallet(TEST_ADDRESS_1)

        assertNotNull(response)
        assertEquals("-239:$TEST_ADDRESS_1", response!!.walletId)
        assertEquals("0xsinglekey", response.wallet?.publicKey)
    }

    @Test
    fun getWallet_returnsNullIfBridgeReturnsNull() = runBlocking {
        givenBridgeReturnsRawNull()

        assertNull(rpcClient.getWallet("nonexistent"))
    }

    // --- getWalletAddress / getBalance — both return raw strings ---

    @Test
    fun getWalletAddress_returnsRawString() = runBlocking {
        givenBridgeReturnsRaw(TEST_ADDRESS_1)

        assertEquals(TEST_ADDRESS_1, rpcClient.getWalletAddress("walletId"))
    }

    @Test
    fun getBalance_returnsRawString() = runBlocking {
        givenBridgeReturnsRaw("1000000000")

        assertEquals("1000000000", rpcClient.getBalance("walletId"))
    }

    // --- removeWallet (side-effect only) ---

    @Test
    fun removeWallet_completesSuccessfully() = runBlocking {
        givenBridgeReturns(JsonObject(emptyMap()))

        rpcClient.removeWallet("walletId")
    }

    // --- addWallet ---

    @Test
    fun addWallet_decodesEnvelope() = runBlocking {
        givenBridgeReturns(
            jsonOf(
                "walletId" to "-239:$TEST_ADDRESS_1",
                "wallet" to buildJsonObject {
                    put("publicKey", "0xnewkey")
                    put("version", WalletVersions.V5R1)
                },
            ),
        )

        val response = rpcClient.addWallet("adapter-123")

        assertEquals("-239:$TEST_ADDRESS_1", response.walletId)
        assertEquals("0xnewkey", response.wallet?.publicKey)
        assertEquals(WalletVersions.V5R1, response.wallet?.version)
    }
}
