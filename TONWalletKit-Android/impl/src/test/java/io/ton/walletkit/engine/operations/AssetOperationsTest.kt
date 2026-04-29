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
import io.ton.walletkit.api.generated.TONNFTTransferRequest
import io.ton.walletkit.testfixtures.TestAddressBody
import io.ton.walletkit.testfixtures.TestBalanceBody
import io.ton.walletkit.testfixtures.TestJettonEntry
import io.ton.walletkit.testfixtures.TestJettonInfo
import io.ton.walletkit.testfixtures.TestJettonWalletAddressBody
import io.ton.walletkit.testfixtures.TestJettonsResponse
import io.ton.walletkit.testfixtures.TestNftEntry
import io.ton.walletkit.testfixtures.TestNftsResponse
import io.ton.walletkit.testfixtures.TestSingleNftResponse
import io.ton.walletkit.testfixtures.jsonObjectOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
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

    private lateinit var assetOperations: AssetOperations

    companion object {
        const val TEST_ADDRESS = "EQCD39VS5jcptHL8vMjEXrzGaRcCVYto7HUn4bpAOg8xqB2N"
        const val TEST_NFT_ADDRESS = "EQNft123456789012345678901234567890123456789012"
        const val TEST_JETTON_ADDRESS = "EQJetton12345678901234567890123456789012345678"
    }

    @Before
    override fun setup() {
        super.setup()
        assetOperations = AssetOperations(
            ensureInitialized = ensureInitialized,
            rpcClient = rpcClient,
            json = json,
        )
    }

    // --- getNfts tests ---

    @Test
    fun getNfts_parsesNftItemsArray() = runBlocking {
        givenBridgeReturns(
            jsonObjectOf(
                TestNftsResponse(
                    nfts = listOf(
                        TestNftEntry(
                            address = TEST_NFT_ADDRESS,
                            owner = TestAddressBody(address = TEST_ADDRESS),
                        ),
                    ),
                ),
            ),
        )

        val result = assetOperations.getNfts(TEST_ADDRESS, limit = 10, offset = 0)

        assertEquals(1, result.nfts.size)
        assertEquals(TEST_NFT_ADDRESS, result.nfts[0].address.value)
    }

    @Test
    fun getNfts_returnsEmptyItemsIfNone() = runBlocking {
        givenBridgeReturns(jsonObjectOf(TestNftsResponse(nfts = emptyList())))

        val result = assetOperations.getNfts(TEST_ADDRESS, limit = 10, offset = 0)

        assertTrue(result.nfts.isEmpty())
    }

    // --- getNft tests ---

    @Test
    fun getNft_parsesSingleNft() = runBlocking {
        givenBridgeReturns(
            jsonObjectOf(
                TestSingleNftResponse(
                    address = TEST_NFT_ADDRESS,
                    ownerAddress = TEST_ADDRESS,
                ),
            ),
        )

        val result = assetOperations.getNft(TEST_NFT_ADDRESS)

        assertNotNull(result)
        assertEquals(TEST_NFT_ADDRESS, result!!.address.value)
    }

    @Test
    fun getNft_returnsNullIfNoAddress() = runBlocking {
        givenBridgeReturns(JSONObject()) // No address field

        val result = assetOperations.getNft(TEST_NFT_ADDRESS)

        assertNull(result)
    }

    // --- getJettons tests ---

    @Test
    fun getJettons_parsesJettonWallets() = runBlocking {
        // TONJettonWallets uses "jettons" for items, and TONJettonWallet requires "jettonWalletAddress"
        givenBridgeReturns(
            jsonObjectOf(
                TestJettonsResponse(
                    addressBook = JsonObject(emptyMap()),
                    jettons = listOf(
                        // address is the jetton master address; walletAddress is this wallet's jetton wallet.
                        TestJettonEntry(
                            address = TEST_JETTON_ADDRESS,
                            walletAddress = TEST_JETTON_ADDRESS,
                            balance = "1000000000",
                            info = TestJettonInfo(name = "Test Jetton", symbol = "TST"),
                            isVerified = false,
                            prices = JsonArray(emptyList()),
                        ),
                    ),
                ),
            ),
        )

        val result = assetOperations.getJettons(TEST_ADDRESS, limit = 10, offset = 0)

        assertEquals(1, result.jettons.size)
        assertEquals(TEST_JETTON_ADDRESS, result.jettons[0].address.value)
    }

    @Test
    fun getJettons_returnsEmptyIfNone() = runBlocking {
        givenBridgeReturns(
            jsonObjectOf(
                TestJettonsResponse(
                    addressBook = JsonObject(emptyMap()),
                    jettons = emptyList(),
                ),
            ),
        )

        val result = assetOperations.getJettons(TEST_ADDRESS, limit = 10, offset = 0)

        assertTrue(result.jettons.isEmpty())
    }

    // --- getJettonBalance tests ---

    @Test
    fun getJettonBalance_extractsBalanceString() = runBlocking {
        givenBridgeReturns(jsonObjectOf(TestBalanceBody(balance = "5000000000")))

        val result = assetOperations.getJettonBalance(TEST_ADDRESS, TEST_JETTON_ADDRESS)

        assertEquals("5000000000", result)
    }

    @Test
    fun getJettonBalance_returnsZeroIfMissing() = runBlocking {
        givenBridgeReturns(JSONObject())

        val result = assetOperations.getJettonBalance(TEST_ADDRESS, TEST_JETTON_ADDRESS)

        assertEquals("0", result)
    }

    // --- getJettonWalletAddress tests ---

    @Test
    fun getJettonWalletAddress_extractsAddress() = runBlocking {
        givenBridgeReturns(
            jsonObjectOf(TestJettonWalletAddressBody(jettonWalletAddress = TEST_JETTON_ADDRESS)),
        )

        val result = assetOperations.getJettonWalletAddress(TEST_ADDRESS, TEST_JETTON_ADDRESS)

        assertEquals(TEST_JETTON_ADDRESS, result)
    }

    @Test
    fun getJettonWalletAddress_returnsEmptyIfMissing() = runBlocking {
        givenBridgeReturns(JSONObject())

        val result = assetOperations.getJettonWalletAddress(TEST_ADDRESS, TEST_JETTON_ADDRESS)

        assertEquals("", result)
    }

    // --- createTransferNftTransaction tests ---

    @Test
    fun createTransferNftTransaction_returnsTransactionString() = runBlocking {
        val transactionContent = """{"messages":[{"address":"EQ...","amount":"50000000"}]}"""
        // The bridge returns the transaction-content JSON verbatim, not a typed DTO,
        // so we round-trip through JSONObject(jsonString) — same shape as production.
        givenBridgeReturns(JSONObject(transactionContent))

        val params = TONNFTTransferRequest(
            nftAddress = io.ton.walletkit.model.TONUserFriendlyAddress(TEST_NFT_ADDRESS),
            recipientAddress = io.ton.walletkit.model.TONUserFriendlyAddress(TEST_ADDRESS),
            comment = "Test transfer",
        )
        val result = assetOperations.createTransferNftTransaction(TEST_ADDRESS, params)

        assertTrue(result.contains("messages"))
    }

    // --- createTransferNftRawTransaction tests ---

    @Test
    fun createTransferNftRawTransaction_returnsTransactionString() = runBlocking {
        val transactionContent = """{"boc":"te6..."}"""
        givenBridgeReturns(JSONObject(transactionContent))

        val params = TONNFTRawTransferRequest(
            nftAddress = io.ton.walletkit.model.TONUserFriendlyAddress(TEST_NFT_ADDRESS),
            transferAmount = "50000000",
            message = io.ton.walletkit.api.generated.TONNFTRawTransferRequestMessage(
                queryId = "0",
                newOwner = io.ton.walletkit.model.TONUserFriendlyAddress(TEST_ADDRESS),
                forwardAmount = "1",
            ),
        )
        val result = assetOperations.createTransferNftRawTransaction(TEST_ADDRESS, params)

        assertTrue(result.contains("boc"))
    }

    // --- createTransferJettonTransaction tests ---

    @Test
    fun createTransferJettonTransaction_returnsTransactionString() = runBlocking {
        val transactionContent = """{"messages":[{"address":"EQ..."}]}"""
        givenBridgeReturns(JSONObject(transactionContent))

        val params = TONJettonsTransferRequest(
            recipientAddress = io.ton.walletkit.model.TONUserFriendlyAddress(TEST_ADDRESS),
            jettonAddress = io.ton.walletkit.model.TONUserFriendlyAddress(TEST_JETTON_ADDRESS),
            transferAmount = "1000000000",
            comment = "Jetton transfer",
        )
        val result = assetOperations.createTransferJettonTransaction(TEST_ADDRESS, params)

        assertTrue(result.contains("messages"))
    }
}
