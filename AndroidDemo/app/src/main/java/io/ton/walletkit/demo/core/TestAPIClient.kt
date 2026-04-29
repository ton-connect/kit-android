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

import android.util.Base64
import android.util.Log
import io.ton.walletkit.api.MAINNET
import io.ton.walletkit.api.TESTNET
import io.ton.walletkit.api.TETRA
import io.ton.walletkit.api.generated.TONGetMethodResult
import io.ton.walletkit.api.generated.TONMasterchainInfo
import io.ton.walletkit.api.generated.TONNetwork
import io.ton.walletkit.api.generated.TONRawStackItem
import io.ton.walletkit.client.TONAPIClient
import io.ton.walletkit.demo.BuildConfig
import io.ton.walletkit.model.TONBase64
import io.ton.walletkit.model.TONHex
import io.ton.walletkit.model.TONUserFriendlyAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
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
            gasUsed = 1000.0,
            stack = emptyList(), // Empty stack for demo
            exitCode = 0.0, // Success exit code
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
        delay(300)
        return mockBalance(network).also { balance ->
            Log.d(tag, "getBalance completed, balance: $balance")
        }
    }

    override suspend fun getMasterchainInfo(): TONMasterchainInfo {
        Log.d(tag, "getMasterchainInfo called on network: ${network.chainId}")
        val baseUrl = if (network == TONNetwork.MAINNET) "https://toncenter.com" else "https://testnet.toncenter.com"
        return withContext(Dispatchers.IO) {
            val conn = URL("$baseUrl/api/v3/masterchainInfo").openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            try {
                val last = JSONObject(conn.inputStream.bufferedReader().readText()).getJSONObject("last")
                TONMasterchainInfo(
                    seqno = last.getInt("seqno"),
                    shard = last.getString("shard"),
                    workchain = last.getInt("workchain"),
                    fileHash = TONHex("0x${base64ToHex(last.getString("file_hash"))}"),
                    rootHash = TONHex("0x${base64ToHex(last.getString("root_hash"))}"),
                )
            } finally {
                conn.disconnect()
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
    private val apiKey: String = "",
) : TONAPIClient {

    private val tag = "ToncenterAPIClient"

    override suspend fun sendBoc(boc: TONBase64): String {
        Log.d(tag, "🚀 [Toncenter] sendBoc on ${network.chainId}")
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
        delay(100)
        return TONGetMethodResult(gasUsed = 1000.0, stack = emptyList(), exitCode = 0.0)
    }

    override suspend fun getBalance(address: TONUserFriendlyAddress, seqno: Int?): String {
        Log.d(tag, "💰 [Toncenter] getBalance on ${network.chainId}")
        val baseUrl = if (network == TONNetwork.MAINNET) "https://toncenter.com" else "https://testnet.toncenter.com"
        return withContext(Dispatchers.IO) {
            val conn = URL("$baseUrl/api/v3/addressInformation?address=${address.value}").openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.setRequestProperty("Accept", "application/json")
            if (apiKey.isNotEmpty()) conn.setRequestProperty("x-api-key", apiKey)
            try {
                JSONObject(conn.inputStream.bufferedReader().readText()).optString("balance", "0")
            } finally {
                conn.disconnect()
            }
        }
    }

    override suspend fun getMasterchainInfo(): TONMasterchainInfo {
        Log.d(tag, "🔗 [Toncenter] getMasterchainInfo on ${network.chainId}")
        val baseUrl = if (network == TONNetwork.MAINNET) "https://toncenter.com" else "https://testnet.toncenter.com"
        return withContext(Dispatchers.IO) {
            val conn = URL("$baseUrl/api/v3/masterchainInfo").openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.setRequestProperty("Accept", "application/json")
            if (apiKey.isNotEmpty()) conn.setRequestProperty("x-api-key", apiKey)
            try {
                val last = JSONObject(conn.inputStream.bufferedReader().readText()).getJSONObject("last")
                TONMasterchainInfo(
                    seqno = last.getInt("seqno"),
                    shard = last.getString("shard"),
                    workchain = last.getInt("workchain"),
                    fileHash = TONHex("0x${base64ToHex(last.getString("file_hash"))}"),
                    rootHash = TONHex("0x${base64ToHex(last.getString("root_hash"))}"),
                )
            } finally {
                conn.disconnect()
            }
        }
    }

    companion object {
        fun mainnet(apiKey: String = "") = ToncenterAPIClient(TONNetwork.MAINNET, apiKey)
        fun testnet(apiKey: String = "") = ToncenterAPIClient(TONNetwork.TESTNET, apiKey)
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

    override suspend fun sendBoc(boc: TONBase64): String {
        Log.d(tag, "🚀 [TonAPI] sendBoc on ${network.chainId}")
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
        delay(100)
        return TONGetMethodResult(gasUsed = 1000.0, stack = emptyList(), exitCode = 0.0)
    }

    override suspend fun getBalance(address: TONUserFriendlyAddress, seqno: Int?): String {
        Log.d(tag, "💰 [TonAPI] getBalance on ${network.chainId}, apiKeyConfigured=${apiKey.isNotEmpty()}")
        delay(100)
        return mockBalance(network)
    }

    override suspend fun getMasterchainInfo(): TONMasterchainInfo {
        Log.d(tag, "🔗 [TonAPI] getMasterchainInfo on ${network.chainId}, apiKeyConfigured=${apiKey.isNotEmpty()}")
        val baseUrl = when (network) {
            TONNetwork.MAINNET -> "https://tonapi.io"
            TONNetwork.TETRA -> "https://tetra.tonapi.io"
            else -> "https://testnet.tonapi.io"
        }
        return withContext(Dispatchers.IO) {
            val conn = URL("$baseUrl/v2/blockchain/masterchain-head").openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            if (apiKey.isNotEmpty()) conn.setRequestProperty("Authorization", "Bearer $apiKey")
            try {
                val root = JSONObject(conn.inputStream.bufferedReader().readText())
                TONMasterchainInfo(
                    seqno = root.getInt("seqno"),
                    shard = root.getString("shard"),
                    workchain = root.getInt("workchain_id"),
                    fileHash = TONHex("0x${root.getString("file_hash")}"),
                    rootHash = TONHex("0x${root.getString("root_hash")}"),
                )
            } finally {
                conn.disconnect()
            }
        }
    }

    companion object {
        fun mainnet(apiKey: String = BuildConfig.MAINNET_API_KEY) = TonAPIClient(TONNetwork.MAINNET, apiKey)
        fun testnet(apiKey: String = BuildConfig.TESTNET_API_KEY) = TonAPIClient(TONNetwork.TESTNET, apiKey)
        fun tetra(apiKey: String = BuildConfig.TETRA_API_KEY) = TonAPIClient(TONNetwork.TETRA, apiKey)
    }
}

private fun mockBalance(network: TONNetwork): String = when (network) {
    TONNetwork.MAINNET -> "1000000000"
    TONNetwork.TESTNET -> "250000000"
    TONNetwork.TETRA -> "50000000"
    else -> "1000000000"
}

private fun base64ToHex(base64: String): String {
    val bytes = Base64.decode(base64, Base64.DEFAULT)
    return bytes.joinToString("") { "%02x".format(it) }
}
