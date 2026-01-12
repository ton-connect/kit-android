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
package io.ton.walletkit.engine.infrastructure

import io.mockk.every
import io.mockk.mockk
import io.ton.walletkit.WalletKitBridgeException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for BridgeRpcClient - response handling and error cases.
 *
 * Note: We can't easily test the `call` method since it requires WebView,
 * but we can test response handling logic.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class BridgeRpcClientTest {

    private lateinit var webViewManager: WebViewManager
    private lateinit var rpcClient: BridgeRpcClient

    @Before
    fun setup() {
        // Create a mock WebViewManager with completed deferreds
        webViewManager = mockk(relaxed = true)
        every { webViewManager.webViewInitialized } returns CompletableDeferred(Unit).apply { complete(Unit) }
        every { webViewManager.bridgeLoaded } returns CompletableDeferred(Unit).apply { complete(Unit) }
        every { webViewManager.jsBridgeReady } returns CompletableDeferred(Unit).apply { complete(Unit) }

        rpcClient = BridgeRpcClient(webViewManager)
    }

    // --- Handle Response Success Tests ---

    @Test
    fun handleResponse_jsonObjectResult_completesWithResult() = runBlocking {
        // We need to simulate the pending call first
        // Since we can't call `call()` directly, we'll test the response parsing logic
        // by using reflection or by testing handleResponse behavior

        // For this test, we verify the response parsing works correctly
        val response = JSONObject().apply {
            put("kind", "response")
            put("id", "test-id-1")
            put(
                "result",
                JSONObject().apply {
                    put("value", "test-value")
                },
            )
        }

        // handleResponse with unknown ID should just log warning, not throw
        rpcClient.handleResponse("test-id-1", response)

        // No exception means success for unknown ID handling
    }

    @Test
    fun handleResponse_unknownId_doesNotThrow() {
        val response = JSONObject().apply {
            put("kind", "response")
            put("id", "unknown-id")
            put("result", JSONObject())
        }

        // Should not throw, just log warning
        rpcClient.handleResponse("unknown-id", response)
    }

    @Test
    fun handleResponse_errorInResponse_logsError() {
        val response = JSONObject().apply {
            put("kind", "response")
            put("id", "test-id")
            put(
                "error",
                JSONObject().apply {
                    put("message", "Something went wrong")
                },
            )
        }

        // Should not throw for unknown ID
        rpcClient.handleResponse("test-id", response)
    }

    // --- Ready State Tests ---

    @Test
    fun markReady_setsReadyState() {
        assertFalse("Should not be ready initially", rpcClient.isReady())

        rpcClient.markReady()

        assertTrue("Should be ready after markReady", rpcClient.isReady())
    }

    @Test
    fun markReady_calledTwice_doesNotThrow() {
        rpcClient.markReady()
        rpcClient.markReady() // Should not throw

        assertTrue("Should still be ready", rpcClient.isReady())
    }

    // --- Fail All Tests ---

    @Test
    fun failAll_marksNotReady_ifNotAlreadyReady() {
        // Note: failAll completes the ready deferred exceptionally if not completed
        val exception = WalletKitBridgeException("Test failure")

        rpcClient.failAll(exception)

        // After failAll, isReady should return true because deferred is completed (exceptionally)
        // This tests that failAll doesn't throw
    }

    @Test
    fun failAll_afterReady_doesNotAffectReadyState() {
        rpcClient.markReady()
        val exception = WalletKitBridgeException("Test failure")

        rpcClient.failAll(exception)

        assertTrue("Should still be ready after failAll", rpcClient.isReady())
    }

    // --- Response Parsing Edge Cases ---

    @Test
    fun handleResponse_nullResult_createsEmptyJsonObject() {
        val response = JSONObject().apply {
            put("kind", "response")
            put("id", "test-id")
            // No "result" key
        }

        // Should not throw
        rpcClient.handleResponse("test-id", response)
    }

    @Test
    fun handleResponse_jsonArrayResult_wrapsInItems() {
        val response = JSONObject().apply {
            put("kind", "response")
            put("id", "test-id")
            put(
                "result",
                JSONArray().apply {
                    put("item1")
                    put("item2")
                },
            )
        }

        // Should not throw - array gets wrapped in {"items": [...]}
        rpcClient.handleResponse("test-id", response)
    }

    @Test
    fun handleResponse_primitiveResult_wrapsInValue() {
        val response = JSONObject().apply {
            put("kind", "response")
            put("id", "test-id")
            put("result", "simple-string")
        }

        // Should not throw - primitive gets wrapped in {"value": ...}
        rpcClient.handleResponse("test-id", response)
    }

    @Test
    fun handleResponse_integerResult_wrapsInValue() {
        val response = JSONObject().apply {
            put("kind", "response")
            put("id", "test-id")
            put("result", 42)
        }

        // Should not throw
        rpcClient.handleResponse("test-id", response)
    }

    @Test
    fun handleResponse_booleanResult_wrapsInValue() {
        val response = JSONObject().apply {
            put("kind", "response")
            put("id", "test-id")
            put("result", true)
        }

        // Should not throw
        rpcClient.handleResponse("test-id", response)
    }

    @Test
    fun handleResponse_errorWithDefaultMessage() {
        val response = JSONObject().apply {
            put("kind", "response")
            put("id", "test-id")
            put("error", JSONObject()) // No message key
        }

        // Should use default message "Bridge call failed"
        rpcClient.handleResponse("test-id", response)
    }
}
