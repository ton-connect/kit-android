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
 * Stub implementation of TONAPIClient demonstrating how a wallet app can inject
 * a custom API client. All methods return fake data — no real network calls are made.
 *
 * In a real implementation this would connect to your own TON node or API service,
 * handle authentication/API keys, and implement retry logic and error handling.
 */
class TestAPIClient(
    override val network: TONNetwork,
) : TONAPIClient {

    private val tag = "TestAPIClient"

    override suspend fun sendBoc(boc: TONBase64): String {
        Log.d(tag, "sendBoc called on network: ${network.chainId}")
        Log.d(tag, "BOC (first 50 chars): ${boc.value.take(50)}...")
        delay(500)
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
        Log.d(tag, "runGetMethod called: $method on ${address.value}")
        delay(300)
        return TONGetMethodResult(gasUsed = 1000, stack = emptyList(), exitCode = 0)
    }

    override suspend fun getBalance(address: TONUserFriendlyAddress, seqno: Int?): String {
        Log.d(tag, "getBalance called on network: ${network.chainId}")
        delay(300)
        return "1000000000"
    }

    companion object {
        fun mainnet(): TestAPIClient = TestAPIClient(TONNetwork.MAINNET)
        fun testnet(): TestAPIClient = TestAPIClient(TONNetwork.TESTNET)
    }
}

/**
 * Example: ToncenterAPIClient — demonstrates how to implement a custom client
 * backed by toncenter.com. All methods are stubs; a real implementation would
 * make HTTP calls to the endpoints shown in the comments.
 */
class ToncenterAPIClient(
    override val network: TONNetwork,
) : TONAPIClient {

    private val tag = "ToncenterAPIClient"

    override suspend fun sendBoc(boc: TONBase64): String {
        Log.d(tag, "🚀 [Toncenter] sendBoc on ${network.chainId}")
        // Real: POST https://toncenter.com/api/v3/message  body={"boc":"<boc>"}
        delay(500)
        return "toncenter_tx_${System.currentTimeMillis()}"
    }

    override suspend fun runGetMethod(
        address: TONUserFriendlyAddress,
        method: String,
        stack: List<TONRawStackItem>?,
        seqno: Int?,
    ): TONGetMethodResult {
        Log.d(tag, "📞 [Toncenter] runGetMethod: $method on ${address.value}")
        // Real: POST https://toncenter.com/api/v3/runGetMethod
        delay(300)
        return TONGetMethodResult(gasUsed = 1000, stack = emptyList(), exitCode = 0)
    }

    override suspend fun getBalance(address: TONUserFriendlyAddress, seqno: Int?): String {
        Log.d(tag, "💰 [Toncenter] getBalance on ${network.chainId}")
        // Real: GET https://toncenter.com/api/v3/addressInformation?address=<address>
        delay(300)
        return "1000000000"
    }

    companion object {
        fun mainnet() = ToncenterAPIClient(TONNetwork.MAINNET)
        fun testnet() = ToncenterAPIClient(TONNetwork.TESTNET)
    }
}

/**
 * Example: TonAPIClient — demonstrates how to implement a custom client
 * backed by tonapi.io. All methods are stubs; a real implementation would
 * make HTTP calls to the endpoints shown in the comments.
 */
class TonAPIClient(
    override val network: TONNetwork,
    private val apiKey: String = "",
) : TONAPIClient {

    private val tag = "TonAPIClient"

    override suspend fun sendBoc(boc: TONBase64): String {
        Log.d(tag, "🚀 [TonAPI] sendBoc on ${network.chainId}")
        // Real: POST https://tonapi.io/v2/blockchain/message
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
        // Real: POST https://tonapi.io/v2/blockchain/accounts/{address}/methods/{method}
        delay(100)
        return TONGetMethodResult(gasUsed = 1000, stack = emptyList(), exitCode = 0)
    }

    override suspend fun getBalance(address: TONUserFriendlyAddress, seqno: Int?): String {
        Log.d(tag, "💰 [TonAPI] getBalance on ${network.chainId}")
        // Real: GET https://tonapi.io/v2/accounts/{address}  (parse "balance" field)
        delay(100)
        return "1000000000"
    }

    companion object {
        fun mainnet(apiKey: String = "") = TonAPIClient(TONNetwork.MAINNET, apiKey)
        fun testnet(apiKey: String = "") = TonAPIClient(TONNetwork.TESTNET, apiKey)
    }
}
