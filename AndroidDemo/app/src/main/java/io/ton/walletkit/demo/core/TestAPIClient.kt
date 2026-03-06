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
        Log.d(tag, "sendBoc called on network: ${network.chainId}")
        Log.d(tag, "BOC (first 50 chars): ${boc.value.take(50)}...")

        // Simulate network delay
        delay(500)

        // In a real implementation, you would:
        // 1. Make HTTP POST to your TON API endpoint
        // 2. Send the base64-encoded BOC in the request body
        // 3. Return the transaction hash from the response

        // For demo purposes, we'll return a mock transaction hash
        val mockTxHash = "demo_tx_${System.currentTimeMillis()}"
        Log.d(tag, "sendBoc completed, mock hash: $mockTxHash")

        return mockTxHash
    }

    override suspend fun runGetMethod(
        address: TONUserFriendlyAddress,
        method: String,
        stack: List<TONRawStackItem>?,
        seqno: Int?,
    ): TONGetMethodResult {
        Log.d(tag, "runGetMethod called on network: ${network.chainId}")
        Log.d(tag, "Address: ${address.value}")
        Log.d(tag, "Method: $method")
        Log.d(tag, "Stack items: ${stack?.size ?: 0}")
        Log.d(tag, "Seqno: $seqno")

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

        Log.d(tag, "runGetMethod completed, exitCode: ${mockResult.exitCode}")

        return mockResult
    }

    override suspend fun getBalance(
        address: TONUserFriendlyAddress,
        seqno: Int?,
    ): String {
        Log.d(tag, "getBalance called on network: ${network.chainId}")
        Log.d(tag, "Address: ${address.value}")

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

                Log.d(tag, "getBalance completed, balance: $balance")
                balance
            } catch (e: Exception) {
                Log.e(tag, "getBalance failed", e)
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
) : TONAPIClient {

    private val tag = "ToncenterAPIClient"

    private val baseUrl: String = when (network) {
        TONNetwork.MAINNET -> "https://toncenter.com"
        TONNetwork.TESTNET -> "https://testnet.toncenter.com"
        else -> "https://toncenter.com"
    }

    override suspend fun sendBoc(boc: TONBase64): String {
        Log.d(tag, "🚀 [Toncenter] sendBoc on ${network.chainId}")
        // Real implementation would call: POST $baseUrl/api/v3/sendBocReturnHash
        delay(100)
        return "toncenter_tx_${System.currentTimeMillis()}"
    }

    override suspend fun runGetMethod(
        address: TONUserFriendlyAddress,
        method: String,
        stack: List<TONRawStackItem>?,
        seqno: Int?,
    ): TONGetMethodResult {
        Log.d(tag, "📞 [Toncenter] runGetMethod: $method on ${address.value}")
        // Real implementation would call: POST $baseUrl/api/v3/runGetMethod
        delay(100)
        return TONGetMethodResult(gasUsed = 1000, stack = emptyList(), exitCode = 0)
    }

    override suspend fun getBalance(address: TONUserFriendlyAddress, seqno: Int?): String {
        Log.d(tag, "💰 [Toncenter] getBalance on ${network.chainId}")
        return withContext(Dispatchers.IO) {
            val url = URL("$baseUrl/api/v3/addressInformation?address=${address.value}")
            val connection = url.openConnection()
            connection.setRequestProperty("Accept", "application/json")
            val response = connection.getInputStream().bufferedReader().readText()
            JSONObject(response).optString("balance", "0")
        }
    }

    companion object {
        fun mainnet() = ToncenterAPIClient(TONNetwork.MAINNET)
        fun testnet() = ToncenterAPIClient(TONNetwork.TESTNET)
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
