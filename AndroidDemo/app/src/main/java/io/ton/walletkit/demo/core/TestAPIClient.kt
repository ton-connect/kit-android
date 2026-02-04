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
package io.ton.walletkit.demo.core

import android.util.Log
import io.ton.walletkit.api.MAINNET
import io.ton.walletkit.api.TESTNET
import io.ton.walletkit.api.generated.TONGetMethodResult
import io.ton.walletkit.api.generated.TONNetwork
import io.ton.walletkit.api.generated.TONRawStackItem
import io.ton.walletkit.client.TONAPIClient
import io.ton.walletkit.model.TONBase64
import io.ton.walletkit.model.TONUserFriendlyAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

/**
 * Test implementation of TONAPIClient for demonstration purposes.
 *
 * This class demonstrates how a wallet app can provide their own
 * API client implementation to use custom infrastructure instead
 * of the default TONCenter API.
 *
 * In a real implementation, this would:
 * - Connect to your own TON node or API service
 * - Handle authentication/API keys
 * - Implement retry logic and error handling
 */
class TestAPIClient(
    override val network: TONNetwork,
) : TONAPIClient {

    private val tag = "TestAPIClient"

    override suspend fun sendBoc(boc: TONBase64): String {
        Log.d(tag, "🚀 sendBoc called on network: ${network.chainId}")
        Log.d(tag, "📦 BOC (first 50 chars): ${boc.value.take(50)}...")

        // Simulate network delay
        delay(500)

        // In a real implementation, you would:
        // 1. Make HTTP POST to your TON API endpoint
        // 2. Send the base64-encoded BOC in the request body
        // 3. Return the transaction hash from the response

        // For demo purposes, we'll return a mock transaction hash
        val mockTxHash = "demo_tx_${System.currentTimeMillis()}"
        Log.d(tag, "✅ sendBoc completed, mock hash: $mockTxHash")

        return mockTxHash
    }

    override suspend fun runGetMethod(
        address: TONUserFriendlyAddress,
        method: String,
        stack: List<TONRawStackItem>?,
        seqno: Int?,
    ): TONGetMethodResult {
        Log.d(tag, "📞 runGetMethod called on network: ${network.chainId}")
        Log.d(tag, "📍 Address: ${address.value}")
        Log.d(tag, "🔧 Method: $method")
        Log.d(tag, "📚 Stack items: ${stack?.size ?: 0}")
        Log.d(tag, "🔢 Seqno: $seqno")

        // Simulate network delay
        delay(300)

        // In a real implementation, you would:
        // 1. Make HTTP POST to your TON API endpoint (e.g., /runGetMethod)
        // 2. Include address, method name, and optional stack/seqno
        // 3. Parse the response and return TONGetMethodResult

        // For demo purposes, return a mock result
        val mockResult = TONGetMethodResult(
            gasUsed = 1000,
            stack = emptyList(), // Empty stack for demo
            exitCode = 0, // Success exit code
        )

        Log.d(tag, "✅ runGetMethod completed, exitCode: ${mockResult.exitCode}")

        return mockResult
    }

    override suspend fun getBalance(
        address: TONUserFriendlyAddress,
        seqno: Int?,
    ): String {
        Log.d(tag, "💰 getBalance called on network: ${network.chainId}")
        Log.d(tag, "📍 Address: ${address.value}")

        // Make a real HTTP call to toncenter API
        val baseUrl = when (network) {
            TONNetwork.MAINNET -> "https://toncenter.com"
            TONNetwork.TESTNET -> "https://testnet.toncenter.com"
            else -> "https://toncenter.com"
        }

        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$baseUrl/api/v3/addressInformation?address=${address.value}")
                val connection = url.openConnection()
                connection.setRequestProperty("Accept", "application/json")
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val response = connection.getInputStream().bufferedReader().readText()
                val json = JSONObject(response)
                val balance = json.optString("balance", "0")

                Log.d(tag, "✅ getBalance completed, balance: $balance")
                balance
            } catch (e: Exception) {
                Log.e(tag, "❌ getBalance failed", e)
                throw e
            }
        }
    }

    companion object {
        /**
         * Create a TestAPIClient for mainnet.
         */
        fun mainnet(): TestAPIClient = TestAPIClient(TONNetwork.MAINNET)

        /**
         * Create a TestAPIClient for testnet.
         */
        fun testnet(): TestAPIClient = TestAPIClient(TONNetwork.TESTNET)
    }
}

/**
 * Example: ToncenterAPIClient - Uses toncenter.com API
 *
 * This demonstrates how wallet apps can have specialized API clients
 * for different networks. For example:
 * - Toncenter might be preferred for mainnet (stability, caching)
 * - TonAPI might be preferred for testnet (more detailed responses)
 */
class ToncenterAPIClient(
    override val network: TONNetwork,
    private val apiKey: String = API_KEY,
) : TONAPIClient {

    private val tag = "ToncenterAPIClient"

    private val baseUrl: String = when (network) {
        TONNetwork.MAINNET -> "https://toncenter.com"
        TONNetwork.TESTNET -> "https://testnet.toncenter.com"
        else -> "https://toncenter.com"
    }

    companion object {
        private const val API_KEY = "d49325c44c4deaa44c9c2e422d7d33ac6c7ff659cdecc2ae1c9b8389eaf478db"
        private const val MAX_RETRIES = 3
        private const val INITIAL_DELAY_MS = 1000L

        fun mainnet() = ToncenterAPIClient(TONNetwork.MAINNET)
        fun testnet() = ToncenterAPIClient(TONNetwork.TESTNET)
    }

    /**
     * Execute an HTTP request with retry logic for rate limiting (429) errors.
     * Uses exponential backoff: 1s, 2s, 4s delays.
     */
    private suspend fun <T> withRetry(
        operation: String,
        block: suspend () -> T,
    ): T {
        var lastException: Exception? = null
        var delayMs = INITIAL_DELAY_MS

        repeat(MAX_RETRIES) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                lastException = e
                val isRateLimited = e.message?.contains("429") == true
                if (isRateLimited && attempt < MAX_RETRIES - 1) {
                    Log.w(tag, "⏳ [Toncenter] $operation rate limited, retrying in ${delayMs}ms (attempt ${attempt + 1}/$MAX_RETRIES)")
                    delay(delayMs)
                    delayMs *= 2 // Exponential backoff
                } else {
                    throw e
                }
            }
        }
        throw lastException ?: Exception("Unknown error after $MAX_RETRIES retries")
    }

    override suspend fun sendBoc(boc: TONBase64): String {
        Log.d(tag, "🚀 [Toncenter] sendBoc on ${network.chainId}")
        return withRetry("sendBoc") {
            withContext(Dispatchers.IO) {
                val url = URL("$baseUrl/api/v3/message")
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("X-API-Key", apiKey)
                connection.setRequestProperty("Accept", "application/json")
                connection.doOutput = true
                connection.connectTimeout = 30000
                connection.readTimeout = 30000

                val requestBody = JSONObject().apply {
                    put("boc", boc.value)
                }.toString()

                Log.d(tag, "📤 [Toncenter] sendBoc request body length: ${requestBody.length}")
                connection.outputStream.bufferedWriter().use { it.write(requestBody) }

                val responseCode = connection.responseCode
                Log.d(tag, "📥 [Toncenter] sendBoc response code: $responseCode")

                val response = if (responseCode in 200..299) {
                    connection.inputStream.bufferedReader().readText()
                } else {
                    val errorBody = connection.errorStream?.bufferedReader()?.readText() ?: "No error body"
                    Log.e(tag, "❌ [Toncenter] sendBoc HTTP error $responseCode: $errorBody")
                    throw Exception("HTTP error $responseCode: $errorBody")
                }

                Log.d(tag, "📥 [Toncenter] sendBoc response: $response")
                val json = JSONObject(response)
                val hash = json.optString("message_hash_norm", json.optString("message_hash", ""))

                Log.d(tag, "✅ [Toncenter] sendBoc completed, hash: $hash")
                hash
            }
        }
    }

    override suspend fun runGetMethod(
        address: TONUserFriendlyAddress,
        method: String,
        stack: List<TONRawStackItem>?,
        seqno: Int?,
    ): TONGetMethodResult {
        Log.d(tag, "📞 [Toncenter] runGetMethod: $method on ${address.value}")
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$baseUrl/api/v3/runGetMethod")
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty("X-API-Key", apiKey)
                connection.doOutput = true
                connection.connectTimeout = 30000
                connection.readTimeout = 30000

                val requestBody = JSONObject().apply {
                    put("address", address.value)
                    put("method", method)
                    if (stack != null && stack.isNotEmpty()) {
                        val stackArray = org.json.JSONArray()
                        for (item in stack) {
                            val stackItem = JSONObject()
                            when (item) {
                                is TONRawStackItem.Num -> {
                                    stackItem.put("type", "num")
                                    stackItem.put("value", item.value)
                                }
                                is TONRawStackItem.Cell -> {
                                    stackItem.put("type", "cell")
                                    stackItem.put("value", item.value)
                                }
                                is TONRawStackItem.Slice -> {
                                    stackItem.put("type", "slice")
                                    stackItem.put("value", item.value)
                                }
                                is TONRawStackItem.Builder -> {
                                    stackItem.put("type", "builder")
                                    stackItem.put("value", item.value)
                                }
                                is TONRawStackItem.Null -> {
                                    stackItem.put("type", "null")
                                }
                                else -> {
                                    // Tuple and List are complex, skip for now
                                }
                            }
                            stackArray.put(stackItem)
                        }
                        put("stack", stackArray)
                    }
                    if (seqno != null) {
                        put("seqno", seqno)
                    }
                }.toString()

                Log.d(tag, "📤 [Toncenter] Request body: $requestBody")
                connection.outputStream.bufferedWriter().use { it.write(requestBody) }

                val responseCode = connection.responseCode
                val response = if (responseCode in 200..299) {
                    connection.inputStream.bufferedReader().readText()
                } else {
                    val errorBody = connection.errorStream?.bufferedReader()?.readText() ?: "No error body"
                    Log.e(tag, "❌ [Toncenter] HTTP error $responseCode: $errorBody")
                    throw Exception("HTTP error $responseCode: $errorBody")
                }

                Log.d(tag, "📥 [Toncenter] Response: $response")
                val json = JSONObject(response)

                val gasUsed = json.optInt("gas_used", 0)
                val exitCode = json.optInt("exit_code", 0)
                val stackArray = json.optJSONArray("stack") ?: org.json.JSONArray()

                val resultStack = mutableListOf<TONRawStackItem>()
                for (i in 0 until stackArray.length()) {
                    val item = stackArray.getJSONObject(i)
                    val type = item.getString("type")
                    val stackItem: TONRawStackItem? = when (type) {
                        "num" -> TONRawStackItem.Num(item.getString("value"))
                        "cell" -> TONRawStackItem.Cell(item.getString("value"))
                        "slice" -> TONRawStackItem.Slice(item.getString("value"))
                        "builder" -> TONRawStackItem.Builder(item.getString("value"))
                        "null" -> TONRawStackItem.Null
                        else -> {
                            Log.w(tag, "⚠️ [Toncenter] Unknown stack item type: $type")
                            null
                        }
                    }
                    if (stackItem != null) {
                        resultStack.add(stackItem)
                    }
                }

                Log.d(tag, "✅ [Toncenter] runGetMethod completed, exitCode: $exitCode, stack size: ${resultStack.size}")
                TONGetMethodResult(gasUsed = gasUsed, stack = resultStack, exitCode = exitCode)
            } catch (e: Exception) {
                Log.e(tag, "❌ [Toncenter] runGetMethod failed", e)
                throw e
            }
        }
    }

    override suspend fun getBalance(address: TONUserFriendlyAddress, seqno: Int?): String {
        Log.d(tag, "💰 [Toncenter] getBalance on ${network.chainId}")
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$baseUrl/api/v3/addressInformation?address=${address.value}")
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty("X-API-Key", apiKey)
                connection.connectTimeout = 30000
                connection.readTimeout = 30000

                val responseCode = connection.responseCode
                if (responseCode !in 200..299) {
                    val errorBody = connection.errorStream?.bufferedReader()?.readText() ?: "No error body"
                    Log.w(tag, "⚠️ [Toncenter] getBalance HTTP $responseCode: $errorBody")
                    // Return 0 on error rather than throwing - balance is not critical
                    return@withContext "0"
                }

                val response = connection.inputStream.bufferedReader().readText()
                val balance = JSONObject(response).optString("balance", "0")
                Log.d(tag, "✅ [Toncenter] getBalance: $balance")
                balance
            } catch (e: Exception) {
                Log.w(tag, "⚠️ [Toncenter] getBalance failed, returning 0", e)
                "0" // Return 0 on error rather than throwing
            }
        }
    }
}

/**
 * Example: TonAPIClient - Uses tonapi.io API
 *
 * Different API provider with different features.
 * Demonstrates that each network can use a completely different backend.
 */
class TonAPIClient(
    override val network: TONNetwork,
    private val apiKey: String = "",
) : TONAPIClient {

    private val tag = "TonAPIClient"

    private val baseUrl: String = when (network) {
        TONNetwork.MAINNET -> "https://tonapi.io"
        TONNetwork.TESTNET -> "https://testnet.tonapi.io"
        else -> "https://tonapi.io"
    }

    override suspend fun sendBoc(boc: TONBase64): String {
        Log.d(tag, "🚀 [TonAPI] sendBoc on ${network.chainId}")
        // Real implementation would call: POST $baseUrl/v2/blockchain/message
        delay(100)
        return "tonapi_tx_${System.currentTimeMillis()}"
    }

    override suspend fun runGetMethod(
        address: TONUserFriendlyAddress,
        method: String,
        stack: List<TONRawStackItem>?,
        seqno: Int?,
    ): TONGetMethodResult {
        Log.d(tag, "📞 [TonAPI] runGetMethod: $method on ${address.value}")
        // Real implementation would call: POST $baseUrl/v2/blockchain/accounts/{address}/methods/{method}
        delay(100)
        return TONGetMethodResult(gasUsed = 1000, stack = emptyList(), exitCode = 0)
    }

    override suspend fun getBalance(address: TONUserFriendlyAddress, seqno: Int?): String {
        Log.d(tag, "💰 [TonAPI] getBalance on ${network.chainId}")
        return withContext(Dispatchers.IO) {
            val url = URL("$baseUrl/v2/accounts/${address.value}")
            val connection = url.openConnection()
            connection.setRequestProperty("Accept", "application/json")
            if (apiKey.isNotEmpty()) {
                connection.setRequestProperty("Authorization", "Bearer $apiKey")
            }
            val response = connection.getInputStream().bufferedReader().readText()
            JSONObject(response).optString("balance", "0")
        }
    }

    companion object {
        fun mainnet(apiKey: String = "") = TonAPIClient(TONNetwork.MAINNET, apiKey)
        fun testnet(apiKey: String = "") = TonAPIClient(TONNetwork.TESTNET, apiKey)
    }
}
