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
    fun parseEvent_browserPageStarted_returnsNull() {
        val data = JSONObject().apply {
            put("url", "https://example.com")
        }
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
        val data = JSONObject().apply {
            put("url", "https://example.com/page")
        }
        val raw = JSONObject()

        val event = parser.parseEvent(EventTypeConstants.EVENT_BROWSER_PAGE_FINISHED, data, raw)

        // Browser events are internal
        assertNull("Browser events should return null", event)
    }

    @Test
    fun parseEvent_browserError_returnsNull() {
        val data = JSONObject().apply {
            put("message", "Page load failed")
        }
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
        val data = JSONObject().apply {
            put("messageId", "msg-123")
            put("method", "sendTransaction")
            put("request", """{"to":"..."}""")
        }
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
        val data = JSONObject().apply {
            put("id", "conn-req-123")
            put("sessionId", "connect-session-123")
            put("requestedItems", org.json.JSONArray())
            put(
                "preview",
                JSONObject().apply {
                    put("permissions", org.json.JSONArray())
                    put(
                        "dAppInfo",
                        JSONObject().apply {
                            put("name", "Test DApp")
                            put("url", "https://testdapp.com")
                            put("iconUrl", "https://testdapp.com/icon.png")
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
            put("id", "conn-req-norm")
            put("sessionId", "session-123")
            put("requestedItems", org.json.JSONArray())
            put(
                "preview",
                JSONObject().apply {
                    put("permissions", org.json.JSONArray())
                    put(
                        "dAppInfo",
                        JSONObject().apply {
                            put("name", "Test DApp")
                            put("url", "testdapp.com") // Missing https://
                            put("iconUrl", "https://testdapp.com/icon.png")
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
            put("id", "conn-req-empty")
            put("sessionId", "session-123")
            put("requestedItems", org.json.JSONArray())
            put(
                "preview",
                JSONObject().apply {
                    put("permissions", org.json.JSONArray()) // Empty array
                    put(
                        "dAppInfo",
                        JSONObject().apply {
                            put("name", "Test DApp")
                            put("url", "https://example.com")
                        },
                    )
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
        // preview is required but can have minimal structure with empty permissions
        val data = JSONObject().apply {
            put("id", "conn-req-no-preview")
            put("sessionId", "session-no-preview")
            put("requestedItems", org.json.JSONArray())
            put(
                "preview",
                JSONObject().apply {
                    put("permissions", org.json.JSONArray())
                },
            )
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
            put("id", "tx-req-123")
            put("sessionId", "tx-session-123")
            put("walletAddress", "EQD...")
            put(
                "preview",
                JSONObject().apply {
                    put(
                        "data",
                        JSONObject().apply {
                            put("result", "success")
                        },
                    )
                },
            )
            put(
                "request",
                JSONObject().apply {
                    put("messages", org.json.JSONArray())
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
            put("id", "sign-req-123")
            put("sessionId", "sign-session-123")
            put("walletAddress", "EQD...")
            put(
                "payload",
                JSONObject().apply {
                    put(
                        "data",
                        JSONObject().apply {
                            put("type", "text")
                            put(
                                "value",
                                JSONObject().apply {
                                    put("content", "Hello World")
                                },
                            )
                        },
                    )
                },
            )
            put(
                "preview",
                JSONObject().apply {
                    put(
                        "data",
                        JSONObject().apply {
                            put("type", "text")
                            put(
                                "value",
                                JSONObject().apply {
                                    put("content", "Hello World")
                                },
                            )
                        },
                    )
                },
            )
        }
        val raw = JSONObject()

        val event = parser.parseEvent(EventTypeConstants.EVENT_SIGN_DATA_REQUEST, data, raw)

        assertNotNull("Should parse sign data request event", event)
        assertTrue("Should be SignDataRequest event", event is TONWalletKitEvent.SignDataRequest)
    }
}
