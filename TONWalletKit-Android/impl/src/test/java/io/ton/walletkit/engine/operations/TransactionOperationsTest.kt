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
 * Tests for TransactionOperations response parsing.
 *
 * Focus: Transaction creation, preview extraction, send result parsing.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class TransactionOperationsTest : OperationsTestBase() {

    companion object {
        const val TEST_ADDRESS = "EQCD39VS5jcptHL8vMjEXrzGaRcCVYto7HUn4bpAOg8xqB2N"
        const val TEST_TO_ADDRESS = "Ef8zMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzM0vF"
    }

    private fun emptyTx(): io.ton.walletkit.api.generated.TONTransactionRequest =
        io.ton.walletkit.api.generated.TONTransactionRequest(messages = emptyList())

    // --- createTransferTonTransaction tests ---

    @Test
    fun createTransferTonTransaction_returnsTransactionJson() = runBlocking {
        givenBridgeReturns(
            buildJsonObject {
                put(
                    "messages",
                    buildJsonArray {
                        add(
                            buildJsonObject {
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
            recipientAddress = TONUserFriendlyAddress(TEST_TO_ADDRESS),
            transferAmount = "1000000000",
            comment = "Test",
        )
        val result = rpcClient.createTransferTonTransaction(TEST_ADDRESS, params)

        assertNotNull(result)
        assertEquals(TEST_ADDRESS, result.fromAddress)
        assertTrue(result.messages.isNotEmpty())
    }

    @Test
    fun createTransferTonTransaction_passesCorrectParams() = runBlocking {
        givenBridgeReturns(
            buildJsonObject {
                put("messages", JsonArray(emptyList()))
                put("fromAddress", TEST_ADDRESS)
            },
        )

        val params = TONTransferRequest(
            recipientAddress = TONUserFriendlyAddress(TEST_TO_ADDRESS),
            transferAmount = "1000000000",
        )
        rpcClient.createTransferTonTransaction(TEST_ADDRESS, params)

        // Verify the method was called with correct params
        assertEquals("createTransferTonTransaction", capturedMethod)
        assertNotNull(capturedParams)
    }

    // --- createTransferMultiTonTransaction tests ---

    @Test
    fun createTransferMultiTonTransaction_returnsTransactionJson() = runBlocking {
        givenBridgeReturns(
            buildJsonObject {
                put(
                    "messages",
                    buildJsonArray {
                        add(
                            buildJsonObject {
                                put("address", TEST_TO_ADDRESS)
                                put("amount", "1000000000")
                            },
                        )
                        add(
                            buildJsonObject {
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
            TONTransferRequest(recipientAddress = TONUserFriendlyAddress(TEST_TO_ADDRESS), transferAmount = "1000000000"),
            TONTransferRequest(recipientAddress = TONUserFriendlyAddress(TEST_ADDRESS), transferAmount = "2000000000"),
        )
        val result = rpcClient.createTransferMultiTonTransaction(TEST_ADDRESS, messages)

        assertEquals(TEST_ADDRESS, result.fromAddress)
        assertEquals(2, result.messages.size)
    }

    // --- sendTransaction tests ---

    @Test
    fun sendTransaction_returnsFullResponse() = runBlocking {
        givenBridgeReturns(
            jsonOf(
                "boc" to "te6ccgEBAgEA...",
                "normalizedBoc" to "te6ccgEBAgEA...normalized",
                "normalizedHash" to "0xabc123",
            ),
        )

        val result = rpcClient.sendTransaction(TEST_ADDRESS, emptyTx())

        assertEquals("te6ccgEBAgEA...", result.boc.value)
        assertEquals("te6ccgEBAgEA...normalized", result.normalizedBoc.value)
        assertEquals("0xabc123", result.normalizedHash.value)
        val encoded = encodeCapturedParams() as JsonObject
        assertTrue(encoded.get("transactionContent") is JsonObject)
    }

    // --- getTransactionPreview tests ---

    @Test
    fun getTransactionPreview_parsesSuccessPreview() = runBlocking {
        givenBridgeReturns(
            buildJsonObject {
                put("result", "success")
            },
        )

        val result = rpcClient.getTransactionPreview(TEST_ADDRESS, emptyTx())

        // Result should be a Success type
        assertNotNull(result)
        assertNotNull(result.result)
        val encoded = encodeCapturedParams() as JsonObject
        assertTrue(encoded.get("transactionContent") is JsonObject)
    }

    // --- handleNewTransaction tests ---

    @Test
    fun handleNewTransaction_completesWithoutError() = runBlocking {
        givenBridgeReturns(JsonObject(emptyMap())) // Success, no return value needed

        // Should not throw
        rpcClient.handleNewTransaction(TEST_ADDRESS, emptyTx())
        val encoded = encodeCapturedParams() as JsonObject
        assertTrue(encoded.get("transactionContent") is JsonObject)
    }
}
