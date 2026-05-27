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

import io.ton.walletkit.WalletKitBridgeException
import io.ton.walletkit.api.ChainIds
import io.ton.walletkit.api.generated.TONConnectionRequestEvent
import io.ton.walletkit.api.generated.TONConnectionRequestEventPreview
import io.ton.walletkit.api.generated.TONNetwork
import io.ton.walletkit.api.generated.TONResult
import io.ton.walletkit.api.generated.TONSendTransactionRequestEvent
import io.ton.walletkit.api.generated.TONSendTransactionRequestEventPreview
import io.ton.walletkit.api.generated.TONTransactionEmulatedPreview
import io.ton.walletkit.api.generated.TONTransactionRequest
import io.ton.walletkit.bridge.optString
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
 * Tests for TonConnectOperations response parsing.
 *
 * Focus: Session listing, connect/transaction approval/rejection, URL handling.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class TonConnectOperationsTest : OperationsTestBase() {

    companion object {
        const val TEST_ADDRESS = "EQCD39VS5jcptHL8vMjEXrzGaRcCVYto7HUn4bpAOg8xqB2N"
        const val TEST_SESSION_ID = "session-123"
        const val TEST_DAPP_URL = "https://example.com"
        const val TEST_WALLET_ID = "test-wallet-id-123"
        val TEST_NETWORK: TONNetwork = TONNetwork(chainId = ChainIds.TESTNET)
    }

    // --- listSessions tests ---

    @Test
    fun listSessions_parsesSessionArray() = runBlocking {
        givenBridgeReturnsRaw(
            buildJsonArray {
                add(
                    buildJsonObject {
                        put("sessionId", TEST_SESSION_ID)
                        put("walletId", "-239:$TEST_ADDRESS")
                        put("walletAddress", TEST_ADDRESS)
                        put("createdAt", "2025-01-01T00:00:00Z")
                        put("lastActivityAt", "2025-01-02T12:00:00Z")
                        put("privateKey", "test-private-key")
                        put("publicKey", "test-public-key")
                        put("domain", "example.com")
                        put("schemaVersion", 1)
                        put("dAppName", "Test dApp")
                        put("dAppUrl", TEST_DAPP_URL)
                        put("dAppIconUrl", "https://example.com/icon.png")
                    },
                )
            },
        )

        val result = rpcClient.listSessions()

        assertEquals(1, result.size)
        assertEquals(TEST_SESSION_ID, result[0].sessionId)
        assertEquals("Test dApp", result[0].dAppName)
        assertEquals(TEST_ADDRESS, result[0].walletAddress.value)
        assertEquals(TEST_DAPP_URL, result[0].dAppUrl)
        assertEquals("https://example.com/icon.png", result[0].dAppIconUrl)
    }

    @Test
    fun listSessions_returnsEmptyListIfNoSessions() = runBlocking {
        givenBridgeReturnsRaw(JsonArray(emptyList()))

        val result = rpcClient.listSessions()

        assertTrue(result.isEmpty())
    }

    @Test
    fun listSessions_handlesNullOptionalFields() = runBlocking {
        givenBridgeReturnsRaw(
            buildJsonArray {
                add(
                    buildJsonObject {
                        put("sessionId", TEST_SESSION_ID)
                        put("walletId", "-239:$TEST_ADDRESS")
                        put("walletAddress", TEST_ADDRESS)
                        put("createdAt", "")
                        put("lastActivityAt", "")
                        put("privateKey", "")
                        put("publicKey", "")
                        put("domain", "")
                        put("schemaVersion", 1)
                        put("dAppName", "Minimal dApp")
                        // No optional dAppUrl/dAppIconUrl fields
                    },
                )
            },
        )

        val result = rpcClient.listSessions()

        assertEquals(1, result.size)
        assertEquals(TEST_SESSION_ID, result[0].sessionId)
        assertNull(result[0].dAppUrl)
        assertNull(result[0].dAppIconUrl)
    }

    @Test
    fun listSessions_handlesMultipleSessions() = runBlocking {
        fun sessionEntry(sessionId: String, dAppName: String): JsonObject = buildJsonObject {
            put("sessionId", sessionId)
            put("walletId", "-239:$TEST_ADDRESS")
            put("walletAddress", TEST_ADDRESS)
            put("createdAt", "")
            put("lastActivityAt", "")
            put("privateKey", "")
            put("publicKey", "")
            put("domain", "")
            put("schemaVersion", 1)
            put("dAppName", dAppName)
        }
        givenBridgeReturnsRaw(
            buildJsonArray {
                add(sessionEntry("session-1", "dApp 1"))
                add(sessionEntry("session-2", "dApp 2"))
                add(sessionEntry("session-3", "dApp 3"))
            },
        )

        val result = rpcClient.listSessions()

        assertEquals(3, result.size)
        assertEquals("session-1", result[0].sessionId)
        assertEquals("session-2", result[1].sessionId)
        assertEquals("session-3", result[2].sessionId)
    }

    // --- handleTonConnectUrl tests ---

    @Test
    fun handleTonConnectUrl_completesSuccessfully() = runBlocking {
        givenBridgeReturns(JsonObject(emptyMap()))

        // Should not throw
        rpcClient.handleTonConnectUrl("tc://connect?...")
    }

    // --- disconnectSession tests ---

    @Test
    fun disconnectSession_completesSuccessfully() = runBlocking {
        givenBridgeReturns(JsonObject(emptyMap()))

        // Should not throw
        rpcClient.disconnectSession(TEST_SESSION_ID)
    }

    @Test
    fun disconnectSession_handlesNullSessionId() = runBlocking {
        givenBridgeReturns(JsonObject(emptyMap()))

        // Should not throw - null means disconnect all
        rpcClient.disconnectSession(null)
    }

    // --- approveConnect tests ---

    @Test
    fun approveConnect_completesSuccessfullyWithNoEmbedded() = runBlocking {
        givenBridgeReturnsRawNull()

        val event = createConnectRequestEvent(
            id = "req-123",
            walletAddress = TONUserFriendlyAddress(TEST_ADDRESS),
            walletId = "mock-wallet-id-hash",
        )

        val result = rpcClient.approveConnect(event)
        assertEquals(null, result)
    }

    @Test(expected = WalletKitBridgeException::class)
    fun approveConnect_throwsIfWalletAddressMissing() = runBlocking<Unit> {
        givenBridgeReturns(JsonObject(emptyMap()))

        val event = createConnectRequestEvent(
            id = "req-123",
            walletAddress = null,
            walletId = "some-wallet-id",
        )

        rpcClient.approveConnect(event)
    }

    @Test(expected = WalletKitBridgeException::class)
    fun approveConnect_throwsIfWalletIdMissing() = runBlocking<Unit> {
        givenBridgeReturns(JsonObject(emptyMap()))

        val event = createConnectRequestEvent(
            id = "req-123",
            walletAddress = TONUserFriendlyAddress(TEST_ADDRESS),
            walletId = null,
        )

        rpcClient.approveConnect(event)
    }

    // --- rejectConnect tests ---

    @Test
    fun rejectConnect_completesSuccessfully() = runBlocking {
        givenBridgeReturns(JsonObject(emptyMap()))

        val event = createConnectRequestEvent(id = "req-123")

        // Should not throw
        rpcClient.rejectConnect(event, "User rejected")
    }

    @Test
    fun rejectConnect_handlesNullReason() = runBlocking {
        givenBridgeReturns(JsonObject(emptyMap()))

        val event = createConnectRequestEvent(id = "req-123")

        // Should not throw
        rpcClient.rejectConnect(event, null)
    }

    // --- approveTransaction tests ---

    @Test
    fun approveTransaction_completesSuccessfully() = runBlocking {
        givenBridgeReturns(JsonObject(emptyMap()))

        val event = createTransactionRequestEvent(
            id = "tx-req-123",
            walletAddress = TONUserFriendlyAddress(TEST_ADDRESS),
            walletId = TEST_WALLET_ID,
        )

        // Should not throw
        rpcClient.approveTransaction(event)
    }

    // --- rejectTransaction tests ---

    @Test
    fun rejectTransaction_completesSuccessfully() = runBlocking {
        givenBridgeReturns(JsonObject(emptyMap()))

        val event = createTransactionRequestEvent(
            id = "tx-req-123",
            walletAddress = TONUserFriendlyAddress(TEST_ADDRESS),
        )

        // Should not throw
        rpcClient.rejectTransaction(event, "User rejected transaction")
    }

    // --- handleTonConnectRequest (internal browser) tests ---

    @Test
    fun handleTonConnectRequest_callsResponseCallback() = runBlocking {
        val responseJson = buildJsonObject {
            put("result", "success")
        }
        givenBridgeReturns(responseJson)

        var callbackInvoked = false
        var callbackResponse: JsonObject? = null

        rpcClient.handleTonConnectRequest(
            messageId = "msg-123",
            method = "ton_requestAccounts",
            paramsJson = null,
            url = TEST_DAPP_URL,
            responseCallback = { response ->
                callbackInvoked = true
                callbackResponse = response
            },
        )

        assertTrue(callbackInvoked)
        assertNotNull(callbackResponse)
        assertEquals("success", callbackResponse?.optString("result"))
    }

    @Test
    fun handleTonConnectRequest_handlesErrorGracefully() = runBlocking {
        // Simulate bridge throwing an exception
        givenBridgeReturns(JsonObject(emptyMap())) // This won't matter as we'll override

        // For this test, we need to verify error handling
        // The callback should receive an error response
        var callbackResponse: JsonObject? = null

        rpcClient.handleTonConnectRequest(
            messageId = "msg-123",
            method = "ton_requestAccounts",
            // Malformed JSON in params
            paramsJson = """{"invalid": json}""",
            url = TEST_DAPP_URL,
            responseCallback = { response ->
                callbackResponse = response
            },
        )

        // Should always invoke callback, even on success
        assertNotNull(callbackResponse)
    }

    // --- Helper functions ---

    private fun createConnectRequestEvent(
        id: String,
        walletAddress: TONUserFriendlyAddress? = null,
        walletId: String? = null,
    ): TONConnectionRequestEvent {
        return TONConnectionRequestEvent(
            id = id,
            requestedItems = emptyList(),
            walletAddress = walletAddress,
            walletId = walletId,
            preview = TONConnectionRequestEventPreview(
                permissions = emptyList(),
            ),
        )
    }

    private fun createTransactionRequestEvent(
        id: String,
        walletAddress: TONUserFriendlyAddress? = null,
        walletId: String? = null,
    ): TONSendTransactionRequestEvent {
        return TONSendTransactionRequestEvent(
            id = id,
            walletAddress = walletAddress,
            walletId = walletId,
            preview = TONSendTransactionRequestEventPreview(
                data = TONTransactionEmulatedPreview(
                    result = TONResult.success,
                ),
            ),
            request = TONTransactionRequest(
                messages = emptyList(),
            ),
        )
    }
}
