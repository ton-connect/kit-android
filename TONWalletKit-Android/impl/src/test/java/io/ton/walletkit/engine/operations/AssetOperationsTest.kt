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

import io.ton.walletkit.api.generated.TONJettonsTransferRequest
import io.ton.walletkit.api.generated.TONNFTRawTransferRequest
import io.ton.walletkit.api.generated.TONNFTRawTransferRequestMessage
import io.ton.walletkit.api.generated.TONNFTTransferRequest
import io.ton.walletkit.model.TONUserFriendlyAddress
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
 * Tests for AssetOperations (NFT and Jetton) response parsing.
 *
 * Focus: NFT/Jetton list parsing, balance extraction, transfer transaction building.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class AssetOperationsTest : OperationsTestBase() {

    companion object {
        const val TEST_ADDRESS = "EQCD39VS5jcptHL8vMjEXrzGaRcCVYto7HUn4bpAOg8xqB2N"
        const val TEST_NFT_ADDRESS = "EQNft123456789012345678901234567890123456789012"
        const val TEST_JETTON_ADDRESS = "EQJetton12345678901234567890123456789012345678"
    }

    // --- getNfts tests ---

    @Test
    fun getNfts_parsesNftItemsArray() = runBlocking {
        givenBridgeReturns(
            buildJsonObject {
                put(
                    "nfts",
                    buildJsonArray {
                        add(
                            buildJsonObject {
                                put("address", TEST_NFT_ADDRESS)
                                put(
                                    "owner",
                                    buildJsonObject {
                                        put("address", TEST_ADDRESS)
                                    },
                                )
                            },
                        )
                    },
                )
            },
        )

        val result = rpcClient.getNfts(TEST_ADDRESS, limit = 10, offset = 0)

        assertEquals(1, result.nfts.size)
        assertEquals(TEST_NFT_ADDRESS, result.nfts[0].address.value)
    }

    @Test
    fun getNfts_returnsEmptyItemsIfNone() = runBlocking {
        givenBridgeReturns(
            buildJsonObject {
                put("nfts", JsonArray(emptyList()))
            },
        )

        val result = rpcClient.getNfts(TEST_ADDRESS, limit = 10, offset = 0)

        assertTrue(result.nfts.isEmpty())
    }

    // --- getNft tests ---

    @Test
    fun getNft_parsesSingleNft() = runBlocking {
        givenBridgeReturns(
            buildJsonObject {
                put("address", TEST_NFT_ADDRESS)
                put("ownerAddress", TEST_ADDRESS)
            },
        )

        val result = rpcClient.getNft(TEST_NFT_ADDRESS)

        assertNotNull(result)
        assertEquals(TEST_NFT_ADDRESS, result!!.address.value)
    }

    @Test
    fun getNft_returnsNullIfNoAddress() = runBlocking {
        givenBridgeReturns(JsonObject(emptyMap())) // No address field

        val result = rpcClient.getNft(TEST_NFT_ADDRESS)

        assertNull(result)
    }

    // --- getJettons tests ---

    @Test
    fun getJettons_parsesJettonWallets() = runBlocking {
        // TONJettonWallets uses "jettons" for items, and TONJettonWallet requires "jettonWalletAddress"
        givenBridgeReturns(
            buildJsonObject {
                put("addressBook", JsonObject(emptyMap()))
                put(
                    "jettons",
                    buildJsonArray {
                        add(
                            buildJsonObject {
                                put("address", TEST_JETTON_ADDRESS) // Jetton master address
                                put("walletAddress", TEST_JETTON_ADDRESS) // Wallet address
                                put("balance", "1000000000")
                                put(
                                    "info",
                                    buildJsonObject {
                                        put("name", "Test Jetton")
                                        put("symbol", "TST")
                                    },
                                )
                                put("isVerified", false)
                                put("prices", JsonArray(emptyList()))
                            },
                        )
                    },
                )
            },
        )

        val result = rpcClient.getJettons(TEST_ADDRESS, limit = 10, offset = 0)

        assertEquals(1, result.jettons.size)
        assertEquals(TEST_JETTON_ADDRESS, result.jettons[0].address.value)
    }

    @Test
    fun getJettons_returnsEmptyIfNone() = runBlocking {
        givenBridgeReturns(
            buildJsonObject {
                put("addressBook", JsonObject(emptyMap()))
                put("jettons", JsonArray(emptyList()))
            },
        )

        val result = rpcClient.getJettons(TEST_ADDRESS, limit = 10, offset = 0)

        assertTrue(result.jettons.isEmpty())
    }

    // --- getJettonBalance tests ---

    @Test
    fun getJettonBalance_extractsBalanceString() = runBlocking {
        // JS bridge returns the balance as a raw string
        givenBridgeReturnsRaw("5000000000")

        val result = rpcClient.getJettonBalance(TEST_ADDRESS, TEST_JETTON_ADDRESS)

        assertEquals("5000000000", result)
    }

    // --- getJettonWalletAddress tests ---

    @Test
    fun getJettonWalletAddress_extractsAddress() = runBlocking {
        // JS bridge returns the address as a raw string
        givenBridgeReturnsRaw(TEST_JETTON_ADDRESS)

        val result = rpcClient.getJettonWalletAddress(TEST_ADDRESS, TEST_JETTON_ADDRESS)

        assertEquals(TEST_JETTON_ADDRESS, result)
    }

    // --- createTransferNftTransaction tests ---

    @Test
    fun createTransferNftTransaction_returnsTransactionString() = runBlocking {
        val transactionContent = """{"messages":[{"address":"EQ...","amount":"50000000"}]}"""
        givenBridgeReturns(transactionContent)

        val params = TONNFTTransferRequest(
            nftAddress = TONUserFriendlyAddress(TEST_NFT_ADDRESS),
            recipientAddress = TONUserFriendlyAddress(TEST_ADDRESS),
            comment = "Test transfer",
        )
        val result = rpcClient.createTransferNftTransaction(TEST_ADDRESS, params)

        assertEquals(1, result.messages.size)
    }

    // --- createTransferNftRawTransaction tests ---

    @Test
    fun createTransferNftRawTransaction_returnsTransactionString() = runBlocking {
        val transactionContent = """{"messages":[{"address":"EQ...","amount":"1"}]}"""
        givenBridgeReturns(transactionContent)

        val params = TONNFTRawTransferRequest(
            nftAddress = TONUserFriendlyAddress(TEST_NFT_ADDRESS),
            transferAmount = "50000000",
            message = TONNFTRawTransferRequestMessage(
                queryId = "0",
                newOwner = TONUserFriendlyAddress(TEST_ADDRESS),
                forwardAmount = "1",
            ),
        )
        val result = rpcClient.createTransferNftRawTransaction(TEST_ADDRESS, params)

        assertEquals(1, result.messages.size)
    }

    // --- createTransferJettonTransaction tests ---

    @Test
    fun createTransferJettonTransaction_returnsTransactionString() = runBlocking {
        val transactionContent = """{"messages":[{"address":"EQ...","amount":"100"}]}"""
        givenBridgeReturns(transactionContent)

        val params = TONJettonsTransferRequest(
            recipientAddress = TONUserFriendlyAddress(TEST_ADDRESS),
            jettonAddress = TONUserFriendlyAddress(TEST_JETTON_ADDRESS),
            transferAmount = "1000000000",
            comment = "Jetton transfer",
        )
        val result = rpcClient.createTransferJettonTransaction(TEST_ADDRESS, params)

        assertEquals(1, result.messages.size)
    }
}
