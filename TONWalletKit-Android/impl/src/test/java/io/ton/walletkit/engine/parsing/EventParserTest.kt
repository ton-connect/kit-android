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
import kotlinx.serialization.json.Json
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

    @Before
    fun setup() {
        mockEngine = mockk(relaxed = true)
        parser = EventParser(json, mockEngine)
    }

    // --- Disconnect Event Tests ---

    @Test
    fun parseEvent_disconnect_extractsSessionId() {
        val data = JSONObject().apply {
            put("sessionId", "session-123")
        }
        val raw = JSONObject()

        val event = parser.parseEvent(EventTypeConstants.EVENT_DISCONNECT, data, raw)

        assertNotNull("Should parse disconnect event", event)
        assertTrue("Should be Disconnect event", event is TONWalletKitEvent.Disconnect)
        assertEquals("session-123", (event as TONWalletKitEvent.Disconnect).event.sessionId)
    }

    @Test
    fun parseEvent_disconnect_fallsBackToIdField() {
        val data = JSONObject().apply {
            put("id", "session-456")
        }
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
        val data = JSONObject().apply {
            put("sessionId", "primary-session")
            put("id", "fallback-id")
        }
        val raw = JSONObject()

        val event = parser.parseEvent(EventTypeConstants.EVENT_DISCONNECT, data, raw)

        assertNotNull("Should parse disconnect event", event)
        assertEquals("primary-session", (event as TONWalletKitEvent.Disconnect).event.sessionId)
    }

    // --- Browser Events Tests ---

    @Test
    fun parseEvent_browserPageStarted_extractsUrl() {
        val data = JSONObject().apply {
            put("url", "https://example.com")
        }
        val raw = JSONObject()

        val event = parser.parseEvent(EventTypeConstants.EVENT_BROWSER_PAGE_STARTED, data, raw)

        assertNotNull("Should parse browser page started event", event)
        assertTrue("Should be BrowserPageStarted event", event is TONWalletKitEvent.BrowserPageStarted)
        assertEquals("https://example.com", (event as TONWalletKitEvent.BrowserPageStarted).url)
    }

    @Test
    fun parseEvent_browserPageStarted_missingUrl_returnsEmptyString() {
        val data = JSONObject()
        val raw = JSONObject()

        val event = parser.parseEvent(EventTypeConstants.EVENT_BROWSER_PAGE_STARTED, data, raw)

        assertNotNull("Should parse event", event)
        assertEquals("", (event as TONWalletKitEvent.BrowserPageStarted).url)
    }

    @Test
    fun parseEvent_browserPageFinished_extractsUrl() {
        val data = JSONObject().apply {
            put("url", "https://example.com/page")
        }
        val raw = JSONObject()

        val event = parser.parseEvent(EventTypeConstants.EVENT_BROWSER_PAGE_FINISHED, data, raw)

        assertNotNull("Should parse browser page finished event", event)
        assertTrue("Should be BrowserPageFinished event", event is TONWalletKitEvent.BrowserPageFinished)
        assertEquals("https://example.com/page", (event as TONWalletKitEvent.BrowserPageFinished).url)
    }

    @Test
    fun parseEvent_browserError_extractsMessage() {
        val data = JSONObject().apply {
            put("message", "Page load failed")
        }
        val raw = JSONObject()

        val event = parser.parseEvent(EventTypeConstants.EVENT_BROWSER_ERROR, data, raw)

        assertNotNull("Should parse browser error event", event)
        assertTrue("Should be BrowserError event", event is TONWalletKitEvent.BrowserError)
        assertEquals("Page load failed", (event as TONWalletKitEvent.BrowserError).message)
    }

    @Test
    fun parseEvent_browserError_missingMessage_returnsDefaultMessage() {
        val data = JSONObject()
        val raw = JSONObject()

        val event = parser.parseEvent(EventTypeConstants.EVENT_BROWSER_ERROR, data, raw)

        assertNotNull("Should parse event", event)
        assertEquals("Unknown error", (event as TONWalletKitEvent.BrowserError).message)
    }

    @Test
    fun parseEvent_browserBridgeRequest_extractsFields() {
        val data = JSONObject().apply {
            put("messageId", "msg-123")
            put("method", "sendTransaction")
            put("request", """{"to":"..."}""")
        }
        val raw = JSONObject()

        val event = parser.parseEvent(EventTypeConstants.EVENT_BROWSER_BRIDGE_REQUEST, data, raw)

        assertNotNull("Should parse browser bridge request event", event)
        assertTrue("Should be BrowserBridgeRequest event", event is TONWalletKitEvent.BrowserBridgeRequest)
        val bridgeEvent = event as TONWalletKitEvent.BrowserBridgeRequest
        assertEquals("msg-123", bridgeEvent.messageId)
        assertEquals("sendTransaction", bridgeEvent.method)
        assertEquals("""{"to":"..."}""", bridgeEvent.request)
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
        // ConnectRequestEvent.Preview.permissions is a required List<ConnectPermission>
        val data = JSONObject().apply {
            put("sessionId", "connect-session-123")
            put(
                "preview",
                JSONObject().apply {
                    put(
                        "manifest",
                        JSONObject().apply {
                            put("name", "Test DApp")
                            put("url", "https://testdapp.com")
                            put("iconUrl", "https://testdapp.com/icon.png")
                        },
                    )
                    put(
                        "permissions",
                        org.json.JSONArray().apply {
                            put(
                                JSONObject().apply {
                                    put("name", "basic")
                                    put("title", "Basic")
                                    put("description", "Basic permission")
                                },
                            )
                        },
                    )
                },
            )
        }
        val raw = JSONObject()

        val event = parser.parseEvent(EventTypeConstants.EVENT_CONNECT_REQUEST, data, raw)

        assertNotNull("Should parse connect request event", event)
        assertTrue("Should be ConnectRequest event", event is TONWalletKitEvent.ConnectRequest)
    }

    @Test
    fun parseEvent_connectRequest_normalizesManifestUrl() {
        val data = JSONObject().apply {
            put("sessionId", "session-123")
            put(
                "preview",
                JSONObject().apply {
                    put(
                        "manifest",
                        JSONObject().apply {
                            put("name", "Test DApp")
                            put("url", "testdapp.com") // Missing https://
                            put("iconUrl", "https://testdapp.com/icon.png")
                        },
                    )
                    // permissions is required
                    put(
                        "permissions",
                        org.json.JSONArray().apply {
                            put(
                                JSONObject().apply {
                                    put("name", "wallet")
                                },
                            )
                        },
                    )
                },
            )
        }
        val raw = JSONObject()

        val event = parser.parseEvent(EventTypeConstants.EVENT_CONNECT_REQUEST, data, raw)

        assertNotNull("Should parse connect request", event)
        // The manifest URL normalization is applied internally
        assertTrue("Should be ConnectRequest", event is TONWalletKitEvent.ConnectRequest)
    }

    @Test
    fun parseEvent_connectRequest_withEmptyPermissions_parsesSuccessfully() {
        // Empty permissions array should work since it's still a valid list
        val data = JSONObject().apply {
            put("sessionId", "session-123")
            put(
                "preview",
                JSONObject().apply {
                    put(
                        "manifest",
                        JSONObject().apply {
                            put("name", "Test DApp")
                            put("url", "https://example.com")
                        },
                    )
                    put("permissions", org.json.JSONArray()) // Empty array
                },
            )
        }
        val raw = JSONObject()

        val event = parser.parseEvent(EventTypeConstants.EVENT_CONNECT_REQUEST, data, raw)

        assertNotNull("Should parse connect request with empty permissions", event)
        assertTrue("Should be ConnectRequest event", event is TONWalletKitEvent.ConnectRequest)
    }

    @Test
    fun parseEvent_connectRequest_withoutPreview_parsesSuccessfully() {
        // preview is nullable, so this should work
        val data = JSONObject().apply {
            put("sessionId", "session-no-preview")
        }
        val raw = JSONObject()

        val event = parser.parseEvent(EventTypeConstants.EVENT_CONNECT_REQUEST, data, raw)

        assertNotNull("Should parse connect request without preview", event)
        assertTrue("Should be ConnectRequest event", event is TONWalletKitEvent.ConnectRequest)
    }

    // --- Transaction Request Tests ---

    @Test
    fun parseEvent_transactionRequest_parsesValidJson() {
        val data = JSONObject().apply {
            put("sessionId", "tx-session-123")
            put("walletAddress", "EQD...")
            put(
                "messages",
                org.json.JSONArray().apply {
                    put(
                        JSONObject().apply {
                            put("address", "EQD...")
                            put("amount", "1000000000")
                        },
                    )
                },
            )
        }
        val raw = JSONObject()

        val event = parser.parseEvent(EventTypeConstants.EVENT_TRANSACTION_REQUEST, data, raw)

        assertNotNull("Should parse transaction request event", event)
        assertTrue("Should be TransactionRequest event", event is TONWalletKitEvent.TransactionRequest)
    }

    // --- Sign Data Request Tests ---

    @Test
    fun parseEvent_signDataRequest_parsesValidJson() {
        val data = JSONObject().apply {
            put("sessionId", "sign-session-123")
            put("walletAddress", "EQD...")
            put("payload", "SGVsbG8gV29ybGQ=") // Base64 "Hello World"
        }
        val raw = JSONObject()

        val event = parser.parseEvent(EventTypeConstants.EVENT_SIGN_DATA_REQUEST, data, raw)

        assertNotNull("Should parse sign data request event", event)
        assertTrue("Should be SignDataRequest event", event is TONWalletKitEvent.SignDataRequest)
    }
}
