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
import kotlinx.coroutines.delay

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
