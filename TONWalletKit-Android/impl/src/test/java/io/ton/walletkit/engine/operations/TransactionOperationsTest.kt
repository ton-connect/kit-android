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

import io.ton.walletkit.model.TONTransferParams
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
    fun createTransferTonTransaction_parsesTransactionWithPreview() = runBlocking {
        givenBridgeReturns(
            JSONObject().apply {
                put("transaction", """{"boc":"te6ccgEBAQEA..."}""")
                put(
                    "preview",
                    JSONObject().apply {
                        // Use "type" as discriminator (kotlinx.serialization default) not "kind"
                        put("type", "success")
                        put(
                            "emulationResult",
                            JSONObject().apply {
                                put("moneyFlow", JSONObject())
                                put("emulationResult", JSONObject())
                            },
                        )
                    },
                )
            },
        )

        val params = TONTransferParams(
            toAddress = TEST_TO_ADDRESS,
            amount = "1000000000",
            comment = "Test",
        )
        val result = transactionOperations.createTransferTonTransaction(TEST_ADDRESS, params)

        assertNotNull(result.transaction)
        assertTrue(result.transaction.contains("boc"))
        assertNotNull(result.preview)
    }

    @Test
    fun createTransferTonTransaction_parsesTransactionWithoutPreview() = runBlocking {
        givenBridgeReturns(
            JSONObject().apply {
                put("transaction", """{"boc":"te6ccgEBAQEA..."}""")
                // No preview
            },
        )

        val params = TONTransferParams(
            toAddress = TEST_TO_ADDRESS,
            amount = "1000000000",
        )
        val result = transactionOperations.createTransferTonTransaction(TEST_ADDRESS, params)

        assertNotNull(result.transaction)
        assertNull(result.preview)
    }

    @Test
    fun createTransferTonTransaction_handlesLegacyFormat() = runBlocking {
        // Legacy format: just the transaction content without wrapper
        givenBridgeReturns(
            JSONObject().apply {
                put("boc", "te6ccgEBAQEA...")
            },
        )

        val params = TONTransferParams(
            toAddress = TEST_TO_ADDRESS,
            amount = "1000000000",
        )
        val result = transactionOperations.createTransferTonTransaction(TEST_ADDRESS, params)

        // Falls back to returning the whole object as transaction
        assertTrue(result.transaction.contains("boc"))
        assertNull(result.preview)
    }

    // --- createTransferMultiTonTransaction tests ---

    @Test
    fun createTransferMultiTonTransaction_parsesMultipleMessages() = runBlocking {
        givenBridgeReturns(
            JSONObject().apply {
                put("transaction", """{"messages":[{"to":"EQ1..."},{"to":"EQ2..."}]}""")
                put(
                    "preview",
                    JSONObject().apply {
                        put("type", "success")
                        put(
                            "emulationResult",
                            JSONObject().apply {
                                put("moneyFlow", JSONObject())
                                put("emulationResult", JSONObject())
                            },
                        )
                    },
                )
            },
        )

        val messages = listOf(
            TONTransferParams(toAddress = TEST_TO_ADDRESS, amount = "1000000000"),
            TONTransferParams(toAddress = TEST_ADDRESS, amount = "2000000000"),
        )
        val result = transactionOperations.createTransferMultiTonTransaction(TEST_ADDRESS, messages)

        assertTrue(result.transaction.contains("messages"))
        assertNotNull(result.preview)
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
                put("type", "success")
                put(
                    "emulationResult",
                    JSONObject().apply {
                        put("moneyFlow", JSONObject())
                        put("emulationResult", JSONObject())
                    },
                )
            },
        )

        val result = transactionOperations.getTransactionPreview(TEST_ADDRESS, """{"boc":"..."}""")

        // Result should be a Success type
        assertTrue(result is io.ton.walletkit.model.TONTransactionPreview.Success)
    }

    // --- handleNewTransaction tests ---

    @Test
    fun handleNewTransaction_completesWithoutError() = runBlocking {
        givenBridgeReturns(JSONObject()) // Success, no return value needed

        // Should not throw
        transactionOperations.handleNewTransaction(TEST_ADDRESS, """{"boc":"..."}""")
    }
}
