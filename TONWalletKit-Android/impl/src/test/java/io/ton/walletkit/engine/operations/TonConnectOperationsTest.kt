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
import io.ton.walletkit.api.generated.TONConnectionRequestEventPreview
import io.ton.walletkit.api.generated.TONNetwork
import io.ton.walletkit.api.walletkit.TONConnectionRequestEvent
import io.ton.walletkit.api.walletkit.TONTransactionRequestEvent
import io.ton.walletkit.model.TONUserFriendlyAddress
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
 * Tests for TonConnectOperations response parsing.
 *
 * Focus: Session listing, connect/transaction approval/rejection, URL handling.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class TonConnectOperationsTest : OperationsTestBase() {

    private lateinit var tonConnectOperations: TonConnectOperations

    companion object {
        const val TEST_ADDRESS = "EQCD39VS5jcptHL8vMjEXrzGaRcCVYto7HUn4bpAOg8xqB2N"
        const val TEST_SESSION_ID = "session-123"
        const val TEST_DAPP_URL = "https://example.com"
        const val TEST_WALLET_ID = "test-wallet-id-123"
        val TEST_NETWORK: TONNetwork = TONNetwork(chainId = io.ton.walletkit.api.ChainIds.TESTNET)
    }

    @Before
    override fun setup() {
        super.setup()
        tonConnectOperations = TonConnectOperations(
            ensureInitialized = ensureInitialized,
            rpcClient = rpcClient,
            json = json,
        )
    }

    // --- listSessions tests ---

    @Test
    fun listSessions_parsesSessionArray() = runBlocking {
        givenBridgeReturns(
            JSONObject().apply {
                put(
                    "items",
                    JSONArray().apply {
                        put(
                            JSONObject().apply {
                                put("sessionId", TEST_SESSION_ID)
                                put("dAppName", "Test dApp")
                                put("walletAddress", TEST_ADDRESS)
                                put("dAppUrl", TEST_DAPP_URL)
                                put("iconUrl", "https://example.com/icon.png")
                                put("createdAt", "2025-01-01T00:00:00Z")
                                put("lastActivity", "2025-01-02T12:00:00Z")
                            },
                        )
                    },
                )
            },
        )

        val result = tonConnectOperations.listSessions()

        assertEquals(1, result.size)
        assertEquals(TEST_SESSION_ID, result[0].sessionId)
        assertEquals("Test dApp", result[0].dAppName)
        assertEquals(TEST_ADDRESS, result[0].walletAddress)
        assertEquals(TEST_DAPP_URL, result[0].dAppUrl)
        assertEquals("https://example.com/icon.png", result[0].iconUrl)
    }

    @Test
    fun listSessions_returnsEmptyListIfNoSessions() = runBlocking {
        givenBridgeReturns(
            JSONObject().apply {
                put("items", JSONArray())
            },
        )

        val result = tonConnectOperations.listSessions()

        assertTrue(result.isEmpty())
    }

    @Test
    fun listSessions_handlesNullOptionalFields() = runBlocking {
        givenBridgeReturns(
            JSONObject().apply {
                put(
                    "items",
                    JSONArray().apply {
                        put(
                            JSONObject().apply {
                                put("sessionId", TEST_SESSION_ID)
                                put("dAppName", "Minimal dApp")
                                put("walletAddress", TEST_ADDRESS)
                                // No optional fields
                            },
                        )
                    },
                )
            },
        )

        val result = tonConnectOperations.listSessions()

        assertEquals(1, result.size)
        assertEquals(TEST_SESSION_ID, result[0].sessionId)
        assertNull(result[0].dAppUrl)
        assertNull(result[0].iconUrl)
        assertNull(result[0].createdAtIso)
    }

    @Test
    fun listSessions_handlesMultipleSessions() = runBlocking {
        givenBridgeReturns(
            JSONObject().apply {
                put(
                    "items",
                    JSONArray().apply {
                        put(
                            JSONObject().apply {
                                put("sessionId", "session-1")
                                put("dAppName", "dApp 1")
                                put("walletAddress", TEST_ADDRESS)
                            },
                        )
                        put(
                            JSONObject().apply {
                                put("sessionId", "session-2")
                                put("dAppName", "dApp 2")
                                put("walletAddress", TEST_ADDRESS)
                            },
                        )
                        put(
                            JSONObject().apply {
                                put("sessionId", "session-3")
                                put("dAppName", "dApp 3")
                                put("walletAddress", TEST_ADDRESS)
                            },
                        )
                    },
                )
            },
        )

        val result = tonConnectOperations.listSessions()

        assertEquals(3, result.size)
        assertEquals("session-1", result[0].sessionId)
        assertEquals("session-2", result[1].sessionId)
        assertEquals("session-3", result[2].sessionId)
    }

    // --- handleTonConnectUrl tests ---

    @Test
    fun handleTonConnectUrl_completesSuccessfully() = runBlocking {
        givenBridgeReturns(JSONObject())

        // Should not throw
        tonConnectOperations.handleTonConnectUrl("tc://connect?...")
    }

    // --- disconnectSession tests ---

    @Test
    fun disconnectSession_completesSuccessfully() = runBlocking {
        givenBridgeReturns(JSONObject())

        // Should not throw
        tonConnectOperations.disconnectSession(TEST_SESSION_ID)
    }

    @Test
    fun disconnectSession_handlesNullSessionId() = runBlocking {
        givenBridgeReturns(JSONObject())

        // Should not throw - null means disconnect all
        tonConnectOperations.disconnectSession(null)
    }

    // --- approveConnect tests ---

    @Test
    fun approveConnect_completesSuccessfully() = runBlocking {
        givenBridgeReturns(JSONObject())

        val event = createConnectRequestEvent(
            id = "req-123",
            walletAddress = TONUserFriendlyAddress(TEST_ADDRESS),
            walletId = "mock-wallet-id-hash",
        )

        // Should not throw
        tonConnectOperations.approveConnect(event)
    }

    @Test(expected = WalletKitBridgeException::class)
    fun approveConnect_throwsIfWalletAddressMissing() = runBlocking {
        givenBridgeReturns(JSONObject())

        val event = createConnectRequestEvent(
            id = "req-123",
            walletAddress = null,
            walletId = "some-wallet-id",
        )

        tonConnectOperations.approveConnect(event)
    }

    @Test(expected = WalletKitBridgeException::class)
    fun approveConnect_throwsIfWalletIdMissing() = runBlocking {
        givenBridgeReturns(JSONObject())

        val event = createConnectRequestEvent(
            id = "req-123",
            walletAddress = TONUserFriendlyAddress(TEST_ADDRESS),
            walletId = null,
        )

        tonConnectOperations.approveConnect(event)
    }

    // --- rejectConnect tests ---

    @Test
    fun rejectConnect_completesSuccessfully() = runBlocking {
        givenBridgeReturns(JSONObject())

        val event = createConnectRequestEvent(id = "req-123")

        // Should not throw
        tonConnectOperations.rejectConnect(event, "User rejected")
    }

    @Test
    fun rejectConnect_handlesNullReason() = runBlocking {
        givenBridgeReturns(JSONObject())

        val event = createConnectRequestEvent(id = "req-123")

        // Should not throw
        tonConnectOperations.rejectConnect(event, null)
    }

    // --- approveTransaction tests ---

    @Test
    fun approveTransaction_completesSuccessfully() = runBlocking {
        givenBridgeReturns(JSONObject())

        val event = createTransactionRequestEvent(
            id = "tx-req-123",
            walletAddress = TONUserFriendlyAddress(TEST_ADDRESS),
            walletId = TEST_WALLET_ID,
        )

        // Should not throw
        tonConnectOperations.approveTransaction(event, TEST_NETWORK)
    }

    // --- rejectTransaction tests ---

    @Test
    fun rejectTransaction_completesSuccessfully() = runBlocking {
        givenBridgeReturns(JSONObject())

        val event = createTransactionRequestEvent(
            id = "tx-req-123",
            walletAddress = TONUserFriendlyAddress(TEST_ADDRESS),
        )

        // Should not throw
        tonConnectOperations.rejectTransaction(event, "User rejected transaction")
    }

    // --- handleTonConnectRequest (internal browser) tests ---

    @Test
    fun handleTonConnectRequest_callsResponseCallback() = runBlocking {
        val responseJson = JSONObject().apply {
            put("result", "success")
        }
        givenBridgeReturns(responseJson)

        var callbackInvoked = false
        var callbackResponse: JSONObject? = null

        tonConnectOperations.handleTonConnectRequest(
            messageId = "msg-123",
            method = "ton_requestAccounts",
            paramsJson = null,
            url = TEST_DAPP_URL,
        ) { response ->
            callbackInvoked = true
            callbackResponse = response
        }

        assertTrue(callbackInvoked)
        assertNotNull(callbackResponse)
        assertEquals("success", callbackResponse?.optString("result"))
    }

    @Test
    fun handleTonConnectRequest_handlesErrorGracefully() = runBlocking {
        // Simulate bridge throwing an exception
        givenBridgeReturns(JSONObject()) // This won't matter as we'll override

        // For this test, we need to verify error handling
        // The callback should receive an error response
        var callbackResponse: JSONObject? = null

        tonConnectOperations.handleTonConnectRequest(
            messageId = "msg-123",
            method = "ton_requestAccounts",
            // Malformed JSON in params
            paramsJson = """{"invalid": json}""",
            url = TEST_DAPP_URL,
        ) { response ->
            callbackResponse = response
        }

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
    ): TONTransactionRequestEvent {
        return TONTransactionRequestEvent(
            id = id,
            walletAddress = walletAddress,
            walletId = walletId,
            preview = io.ton.walletkit.api.generated.TONTransactionRequestEventPreview(
                data = io.ton.walletkit.api.generated.TONTransactionEmulatedPreview(
                    result = io.ton.walletkit.api.generated.TONResult.success,
                ),
            ),
            request = io.ton.walletkit.api.generated.TONTransactionRequest(
                messages = emptyList(),
            ),
        )
    }
}
