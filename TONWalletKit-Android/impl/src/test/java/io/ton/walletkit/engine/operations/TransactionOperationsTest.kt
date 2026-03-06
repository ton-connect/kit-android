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

import io.ton.walletkit.api.generated.TONTransferRequest
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for TransactionOperations response parsing.
 *
 * Focus: Transaction creation, preview extraction, send result parsing.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class TransactionOperationsTest : OperationsTestBase() {

    private lateinit var transactionOperations: TransactionOperations

    companion object {
        const val TEST_ADDRESS = "EQCD39VS5jcptHL8vMjEXrzGaRcCVYto7HUn4bpAOg8xqB2N"
        const val TEST_TO_ADDRESS = "Ef8zMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzM0vF"
    }

    @Before
    override fun setup() {
        super.setup()
        transactionOperations = TransactionOperations(
            ensureInitialized = ensureInitialized,
            rpcClient = rpcClient,
            json = json,
        )
    }

    // --- createTransferTonTransaction tests ---

    @Test
    fun createTransferTonTransaction_returnsTransactionJson() = runBlocking {
        givenBridgeReturns(
            JSONObject().apply {
                put(
                    "messages",
                    org.json.JSONArray().apply {
                        put(
                            JSONObject().apply {
                                put("address", TEST_TO_ADDRESS)
                                put("amount", "1000000000")
                            },
                        )
                    },
                )
                put("fromAddress", TEST_ADDRESS)
            },
        )

        val params = TONTransferRequest(
            recipientAddress = io.ton.walletkit.model.TONUserFriendlyAddress(TEST_TO_ADDRESS),
            transferAmount = "1000000000",
            comment = "Test",
        )
        val result = transactionOperations.createTransferTonTransaction(TEST_ADDRESS, params)

        assertNotNull(result)
        assertTrue(result.contains("messages"))
        assertTrue(result.contains("fromAddress"))
    }

    @Test
    fun createTransferTonTransaction_passesCorrectParams() = runBlocking {
        givenBridgeReturns(
            JSONObject().apply {
                put("messages", org.json.JSONArray())
                put("fromAddress", TEST_ADDRESS)
            },
        )

        val params = TONTransferRequest(
            recipientAddress = io.ton.walletkit.model.TONUserFriendlyAddress(TEST_TO_ADDRESS),
            transferAmount = "1000000000",
        )
        transactionOperations.createTransferTonTransaction(TEST_ADDRESS, params)

        // Verify the method was called with correct params
        assertEquals("createTransferTonTransaction", capturedMethod)
        assertNotNull(capturedParams)
    }

    // --- createTransferMultiTonTransaction tests ---

    @Test
    fun createTransferMultiTonTransaction_returnsTransactionJson() = runBlocking {
        givenBridgeReturns(
            JSONObject().apply {
                put(
                    "messages",
                    org.json.JSONArray().apply {
                        put(
                            JSONObject().apply {
                                put("address", TEST_TO_ADDRESS)
                                put("amount", "1000000000")
                            },
                        )
                        put(
                            JSONObject().apply {
                                put("address", TEST_ADDRESS)
                                put("amount", "2000000000")
                            },
                        )
                    },
                )
                put("fromAddress", TEST_ADDRESS)
            },
        )

        val messages = listOf(
            TONTransferRequest(recipientAddress = io.ton.walletkit.model.TONUserFriendlyAddress(TEST_TO_ADDRESS), transferAmount = "1000000000"),
            TONTransferRequest(recipientAddress = io.ton.walletkit.model.TONUserFriendlyAddress(TEST_ADDRESS), transferAmount = "2000000000"),
        )
        val result = transactionOperations.createTransferMultiTonTransaction(TEST_ADDRESS, messages)

        assertTrue(result.contains("messages"))
        assertTrue(result.contains("fromAddress"))
    }

    // --- sendTransaction tests ---

    @Test
    fun sendTransaction_extractsSignedBoc() = runBlocking {
        givenBridgeReturns(
            jsonOf(
                "signedBoc" to "te6ccgEBAgEA...",
            ),
        )

        val result = transactionOperations.sendTransaction(TEST_ADDRESS, """{"boc":"..."}""")

        assertEquals("te6ccgEBAgEA...", result)
    }

    @Test
    fun sendTransaction_throwsIfSignedBocMissing() {
        runBlocking {
            givenBridgeReturns(JSONObject()) // No signedBoc

            assertThrows(org.json.JSONException::class.java) {
                runBlocking { transactionOperations.sendTransaction(TEST_ADDRESS, """{"boc":"..."}""") }
            }
        }
    }

    // --- getTransactionPreview tests ---

    @Test
    fun getTransactionPreview_parsesSuccessPreview() = runBlocking {
        givenBridgeReturns(
            JSONObject().apply {
                put("result", "success")
            },
        )

        val result = transactionOperations.getTransactionPreview(TEST_ADDRESS, """{"boc":"..."}""")

        // Result should be a Success type
        assertNotNull(result)
        assertNotNull(result.result)
    }

    // --- handleNewTransaction tests ---

    @Test
    fun handleNewTransaction_completesWithoutError() = runBlocking {
        givenBridgeReturns(JSONObject()) // Success, no return value needed

        // Should not throw
        transactionOperations.handleNewTransaction(TEST_ADDRESS, """{"boc":"..."}""")
    }
}
