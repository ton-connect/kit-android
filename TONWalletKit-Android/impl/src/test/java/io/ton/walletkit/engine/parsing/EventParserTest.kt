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
package io.ton.walletkit.engine.parsing

import io.mockk.mockk
import io.ton.walletkit.engine.WalletKitEngine
import io.ton.walletkit.event.TONWalletKitEvent
import io.ton.walletkit.internal.constants.EventTypeConstants
import io.ton.walletkit.testfixtures.TestBrowserBridgeRequestBody
import io.ton.walletkit.testfixtures.TestConnectRequestData
import io.ton.walletkit.testfixtures.TestConnectRequestPreview
import io.ton.walletkit.testfixtures.TestDAppInfo
import io.ton.walletkit.testfixtures.TestIdBody
import io.ton.walletkit.testfixtures.TestMessageBody
import io.ton.walletkit.testfixtures.TestPreviewDataWrap
import io.ton.walletkit.testfixtures.TestPreviewResultData
import io.ton.walletkit.testfixtures.TestSessionIdAndIdBody
import io.ton.walletkit.testfixtures.TestSessionIdBody
import io.ton.walletkit.testfixtures.TestSignDataInner
import io.ton.walletkit.testfixtures.TestSignDataRequestData
import io.ton.walletkit.testfixtures.TestSignDataValue
import io.ton.walletkit.testfixtures.TestSignDataWrap
import io.ton.walletkit.testfixtures.TestTransactionMessagesBody
import io.ton.walletkit.testfixtures.TestTransactionRequestData
import io.ton.walletkit.testfixtures.TestUrlBody
import io.ton.walletkit.testfixtures.jsonObjectOf
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for EventParser - JSON to typed event conversion.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class EventParserTest {

    private lateinit var parser: EventParser
    private lateinit var mockEngine: WalletKitEngine
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    private val emptyJsonArray = JsonArray(emptyList())

    @Before
    fun setup() {
        mockEngine = mockk(relaxed = true)
        parser = EventParser(json, mockEngine)
    }

    // --- Disconnect Event Tests ---

    @Test
    fun parseEvent_disconnect_extractsSessionId() {
        val data = jsonObjectOf(TestSessionIdBody(sessionId = "session-123"))
        val raw = JSONObject()

        val event = parser.parseEvent(EventTypeConstants.EVENT_DISCONNECT, data, raw)

        assertNotNull("Should parse disconnect event", event)
        assertTrue("Should be Disconnect event", event is TONWalletKitEvent.Disconnect)
        assertEquals("session-123", (event as TONWalletKitEvent.Disconnect).event.sessionId)
    }

    @Test
    fun parseEvent_disconnect_fallsBackToIdField() {
        val data = jsonObjectOf(TestIdBody(id = "session-456"))
        val raw = JSONObject()

        val event = parser.parseEvent(EventTypeConstants.EVENT_DISCONNECT, data, raw)

        assertNotNull("Should parse disconnect event", event)
        assertTrue("Should be Disconnect event", event is TONWalletKitEvent.Disconnect)
        assertEquals("session-456", (event as TONWalletKitEvent.Disconnect).event.sessionId)
    }

    @Test
    fun parseEvent_disconnect_noSessionId_returnsNull() {
        val data = JSONObject() // No sessionId or id
        val raw = JSONObject()

        val event = parser.parseEvent(EventTypeConstants.EVENT_DISCONNECT, data, raw)

        assertNull("Should return null when no session ID", event)
    }

    @Test
    fun parseEvent_disconnect_prefersSessionIdOverId() {
        val data = jsonObjectOf(
            TestSessionIdAndIdBody(sessionId = "primary-session", id = "fallback-id"),
        )
        val raw = JSONObject()

        val event = parser.parseEvent(EventTypeConstants.EVENT_DISCONNECT, data, raw)

        assertNotNull("Should parse disconnect event", event)
        assertEquals("primary-session", (event as TONWalletKitEvent.Disconnect).event.sessionId)
    }

    // --- Browser Events Tests ---

    @Test
    fun parseEvent_browserPageStarted_returnsNull() {
        val data = jsonObjectOf(TestUrlBody(url = "https://example.com"))
        val raw = JSONObject()

        val event = parser.parseEvent(EventTypeConstants.EVENT_BROWSER_PAGE_STARTED, data, raw)

        // Browser events are internal and not exposed to public API
        assertNull("Browser events should return null", event)
    }

    @Test
    fun parseEvent_browserPageStarted_missingUrl_returnsNull() {
        val data = JSONObject()
        val raw = JSONObject()

        val event = parser.parseEvent(EventTypeConstants.EVENT_BROWSER_PAGE_STARTED, data, raw)

        // Browser events are internal
        assertNull("Browser events should return null", event)
    }

    @Test
    fun parseEvent_browserPageFinished_returnsNull() {
        val data = jsonObjectOf(TestUrlBody(url = "https://example.com/page"))
        val raw = JSONObject()

        val event = parser.parseEvent(EventTypeConstants.EVENT_BROWSER_PAGE_FINISHED, data, raw)

        // Browser events are internal
        assertNull("Browser events should return null", event)
    }

    @Test
    fun parseEvent_browserError_returnsNull() {
        val data = jsonObjectOf(TestMessageBody(message = "Page load failed"))
        val raw = JSONObject()

        val event = parser.parseEvent(EventTypeConstants.EVENT_BROWSER_ERROR, data, raw)

        // Browser events are internal
        assertNull("Browser events should return null", event)
    }

    @Test
    fun parseEvent_browserError_missingMessage_returnsNull() {
        val data = JSONObject()
        val raw = JSONObject()

        val event = parser.parseEvent(EventTypeConstants.EVENT_BROWSER_ERROR, data, raw)

        // Browser events are internal
        assertNull("Browser events should return null", event)
    }

    @Test
    fun parseEvent_browserBridgeRequest_returnsNull() {
        val data = jsonObjectOf(
            TestBrowserBridgeRequestBody(
                messageId = "msg-123",
                method = "sendTransaction",
                request = """{"to":"..."}""",
            ),
        )
        val raw = JSONObject()

        val event = parser.parseEvent(EventTypeConstants.EVENT_BROWSER_BRIDGE_REQUEST, data, raw)

        // Browser events are internal and not exposed to public API
        assertNull("Browser events should return null", event)
    }

    // --- Unknown/Ignored Event Types ---

    @Test
    fun parseEvent_unknownType_returnsNull() {
        val data = JSONObject()
        val raw = JSONObject()

        val event = parser.parseEvent("someUnknownEventType", data, raw)

        assertNull("Unknown event type should return null", event)
    }

    @Test
    fun parseEvent_stateChanged_returnsNull() {
        val data = JSONObject()
        val raw = JSONObject()

        val event = parser.parseEvent(EventTypeConstants.EVENT_STATE_CHANGED, data, raw)

        assertNull("stateChanged should return null (ignored)", event)
    }

    @Test
    fun parseEvent_walletStateChanged_returnsNull() {
        val data = JSONObject()
        val raw = JSONObject()

        val event = parser.parseEvent(EventTypeConstants.EVENT_WALLET_STATE_CHANGED, data, raw)

        assertNull("walletStateChanged should return null (ignored)", event)
    }

    @Test
    fun parseEvent_sessionsChanged_returnsNull() {
        val data = JSONObject()
        val raw = JSONObject()

        val event = parser.parseEvent(EventTypeConstants.EVENT_SESSIONS_CHANGED, data, raw)

        assertNull("sessionsChanged should return null (ignored)", event)
    }

    // --- Connect Request Tests ---

    @Test
    fun parseEvent_connectRequest_parsesValidJson() {
        // ConnectRequestEvent requires id, requestedItems, and preview
        val data = jsonObjectOf(
            TestConnectRequestData(
                id = "conn-req-123",
                sessionId = "connect-session-123",
                requestedItems = emptyJsonArray,
                preview = TestConnectRequestPreview(
                    permissions = emptyJsonArray,
                    dAppInfo = TestDAppInfo(
                        name = "Test DApp",
                        url = "https://testdapp.com",
                        iconUrl = "https://testdapp.com/icon.png",
                    ),
                ),
            ),
        )
        val raw = JSONObject()

        val event = parser.parseEvent(EventTypeConstants.EVENT_CONNECT_REQUEST, data, raw)

        assertNotNull("Should parse connect request event", event)
        assertTrue("Should be ConnectRequest event", event is TONWalletKitEvent.ConnectRequest)
    }

    @Test
    fun parseEvent_connectRequest_normalizesManifestUrl() {
        val data = jsonObjectOf(
            TestConnectRequestData(
                id = "conn-req-norm",
                sessionId = "session-123",
                requestedItems = emptyJsonArray,
                preview = TestConnectRequestPreview(
                    permissions = emptyJsonArray,
                    // url is intentionally missing the https:// scheme to exercise the parser's
                    // sanitisation path.
                    dAppInfo = TestDAppInfo(
                        name = "Test DApp",
                        url = "testdapp.com",
                        iconUrl = "https://testdapp.com/icon.png",
                    ),
                ),
            ),
        )
        val raw = JSONObject()

        val event = parser.parseEvent(EventTypeConstants.EVENT_CONNECT_REQUEST, data, raw)

        assertNotNull("Should parse connect request", event)
        // The manifest URL normalization is applied internally
        assertTrue("Should be ConnectRequest", event is TONWalletKitEvent.ConnectRequest)
    }

    @Test
    fun parseEvent_connectRequest_withEmptyPermissions_parsesSuccessfully() {
        // Empty permissions array should work since it's still a valid list
        val data = jsonObjectOf(
            TestConnectRequestData(
                id = "conn-req-empty",
                sessionId = "session-123",
                requestedItems = emptyJsonArray,
                preview = TestConnectRequestPreview(
                    permissions = emptyJsonArray,
                    dAppInfo = TestDAppInfo(
                        name = "Test DApp",
                        url = "https://example.com",
                    ),
                ),
            ),
        )
        val raw = JSONObject()

        val event = parser.parseEvent(EventTypeConstants.EVENT_CONNECT_REQUEST, data, raw)

        assertNotNull("Should parse connect request with empty permissions", event)
        assertTrue("Should be ConnectRequest event", event is TONWalletKitEvent.ConnectRequest)
    }

    @Test
    fun parseEvent_connectRequest_withoutPreview_parsesSuccessfully() {
        // preview is required but can have minimal structure with empty permissions
        val data = jsonObjectOf(
            TestConnectRequestData(
                id = "conn-req-no-preview",
                sessionId = "session-no-preview",
                requestedItems = emptyJsonArray,
                preview = TestConnectRequestPreview(permissions = emptyJsonArray),
            ),
        )
        val raw = JSONObject()

        val event = parser.parseEvent(EventTypeConstants.EVENT_CONNECT_REQUEST, data, raw)

        assertNotNull("Should parse connect request without preview", event)
        assertTrue("Should be ConnectRequest event", event is TONWalletKitEvent.ConnectRequest)
    }

    // --- Transaction Request Tests ---

    @Test
    fun parseEvent_transactionRequest_parsesValidJson() {
        val data = jsonObjectOf(
            TestTransactionRequestData(
                id = "tx-req-123",
                sessionId = "tx-session-123",
                walletAddress = "EQD...",
                preview = TestPreviewDataWrap(data = TestPreviewResultData(result = "success")),
                request = TestTransactionMessagesBody(messages = emptyJsonArray),
            ),
        )
        val raw = JSONObject()

        val event = parser.parseEvent(EventTypeConstants.EVENT_TRANSACTION_REQUEST, data, raw)

        assertNotNull("Should parse transaction request event", event)
        assertTrue("Should be SendTransactionRequest event", event is TONWalletKitEvent.SendTransactionRequest)
    }

    // --- Sign Data Request Tests ---

    @Test
    fun parseEvent_signDataRequest_parsesValidJson() {
        val signDataInner = TestSignDataInner(
            type = "text",
            value = TestSignDataValue(content = "Hello World"),
        )
        val data = jsonObjectOf(
            TestSignDataRequestData(
                id = "sign-req-123",
                sessionId = "sign-session-123",
                walletAddress = "EQD...",
                payload = TestSignDataWrap(data = signDataInner),
                preview = TestSignDataWrap(data = signDataInner),
            ),
        )
        val raw = JSONObject()

        val event = parser.parseEvent(EventTypeConstants.EVENT_SIGN_DATA_REQUEST, data, raw)

        assertNotNull("Should parse sign data request event", event)
        assertTrue("Should be SignDataRequest event", event is TONWalletKitEvent.SignDataRequest)
    }
}
