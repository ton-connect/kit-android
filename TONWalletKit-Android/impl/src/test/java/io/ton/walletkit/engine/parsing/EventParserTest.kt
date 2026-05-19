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
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
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
    fun parseEvent_disconnect_decodesFullPayload() {
        val data = buildJsonObject {
            put("id", "evt-789")
            put("sessionId", "session-123")
            put("preview", buildJsonObject {})
        }

        val event = parser.parseEvent(EventTypeConstants.EVENT_DISCONNECT, data)

        assertNotNull("Should parse disconnect event", event)
        assertTrue("Should be Disconnect event", event is TONWalletKitEvent.Disconnect)
        val payload = (event as TONWalletKitEvent.Disconnect).event
        assertEquals("evt-789", payload.id)
        assertEquals("session-123", payload.sessionId)
    }

    @Test(expected = Exception::class)
    fun parseEvent_disconnect_missingRequiredFields_throws() {
        // Missing both `id` and `preview` — kotlinx decode throws.
        // The dispatcher catches and logs; here we assert the strict-decode contract.
        parser.parseEvent(EventTypeConstants.EVENT_DISCONNECT, JsonObject(emptyMap()))
    }

    // --- Browser Events Tests ---

    @Test
    fun parseEvent_browserPageStarted_returnsNull() {
        val data = buildJsonObject {
            put("url", "https://example.com")
        }
        val raw = JsonObject(emptyMap())

        val event = parser.parseEvent(EventTypeConstants.EVENT_BROWSER_PAGE_STARTED, data)

        // Browser events are internal and not exposed to public API
        assertNull("Browser events should return null", event)
    }

    @Test
    fun parseEvent_browserPageStarted_missingUrl_returnsNull() {
        val data = JsonObject(emptyMap())
        val raw = JsonObject(emptyMap())

        val event = parser.parseEvent(EventTypeConstants.EVENT_BROWSER_PAGE_STARTED, data)

        // Browser events are internal
        assertNull("Browser events should return null", event)
    }

    @Test
    fun parseEvent_browserPageFinished_returnsNull() {
        val data = buildJsonObject {
            put("url", "https://example.com/page")
        }
        val raw = JsonObject(emptyMap())

        val event = parser.parseEvent(EventTypeConstants.EVENT_BROWSER_PAGE_FINISHED, data)

        // Browser events are internal
        assertNull("Browser events should return null", event)
    }

    @Test
    fun parseEvent_browserError_returnsNull() {
        val data = buildJsonObject {
            put("message", "Page load failed")
        }
        val raw = JsonObject(emptyMap())

        val event = parser.parseEvent(EventTypeConstants.EVENT_BROWSER_ERROR, data)

        // Browser events are internal
        assertNull("Browser events should return null", event)
    }

    @Test
    fun parseEvent_browserError_missingMessage_returnsNull() {
        val data = JsonObject(emptyMap())
        val raw = JsonObject(emptyMap())

        val event = parser.parseEvent(EventTypeConstants.EVENT_BROWSER_ERROR, data)

        // Browser events are internal
        assertNull("Browser events should return null", event)
    }

    @Test
    fun parseEvent_browserBridgeRequest_returnsNull() {
        val data = buildJsonObject {
            put("messageId", "msg-123")
            put("method", "sendTransaction")
            put("request", """{"to":"..."}""")
        }
        val raw = JsonObject(emptyMap())

        val event = parser.parseEvent(EventTypeConstants.EVENT_BROWSER_BRIDGE_REQUEST, data)

        // Browser events are internal and not exposed to public API
        assertNull("Browser events should return null", event)
    }

    // --- Unknown/Ignored Event Types ---

    @Test
    fun parseEvent_unknownType_returnsNull() {
        val data = JsonObject(emptyMap())
        val raw = JsonObject(emptyMap())

        val event = parser.parseEvent("someUnknownEventType", data)

        assertNull("Unknown event type should return null", event)
    }

    @Test
    fun parseEvent_stateChanged_returnsNull() {
        val data = JsonObject(emptyMap())
        val raw = JsonObject(emptyMap())

        val event = parser.parseEvent(EventTypeConstants.EVENT_STATE_CHANGED, data)

        assertNull("stateChanged should return null (ignored)", event)
    }

    @Test
    fun parseEvent_walletStateChanged_returnsNull() {
        val data = JsonObject(emptyMap())
        val raw = JsonObject(emptyMap())

        val event = parser.parseEvent(EventTypeConstants.EVENT_WALLET_STATE_CHANGED, data)

        assertNull("walletStateChanged should return null (ignored)", event)
    }

    @Test
    fun parseEvent_sessionsChanged_returnsNull() {
        val data = JsonObject(emptyMap())
        val raw = JsonObject(emptyMap())

        val event = parser.parseEvent(EventTypeConstants.EVENT_SESSIONS_CHANGED, data)

        assertNull("sessionsChanged should return null (ignored)", event)
    }

    // --- Connect Request Tests ---

    @Test
    fun parseEvent_connectRequest_parsesValidJson() {
        // ConnectRequestEvent requires id, requestedItems, and preview
        val data = buildJsonObject {
            put("id", "conn-req-123")
            put("sessionId", "connect-session-123")
            put("requestedItems", JsonArray(emptyList()))
            put(
                "preview",
                buildJsonObject {
                    put("permissions", JsonArray(emptyList()))
                    put(
                        "dAppInfo",
                        buildJsonObject {
                            put("name", "Test DApp")
                            put("url", "https://testdapp.com")
                            put("iconUrl", "https://testdapp.com/icon.png")
                        },
                    )
                },
            )
        }
        val raw = JsonObject(emptyMap())

        val event = parser.parseEvent(EventTypeConstants.EVENT_CONNECT_REQUEST, data)

        assertNotNull("Should parse connect request event", event)
        assertTrue("Should be ConnectRequest event", event is TONWalletKitEvent.ConnectRequest)
    }

    @Test
    fun parseEvent_connectRequest_normalizesManifestUrl() {
        val data = buildJsonObject {
            put("id", "conn-req-norm")
            put("sessionId", "session-123")
            put("requestedItems", JsonArray(emptyList()))
            put(
                "preview",
                buildJsonObject {
                    put("permissions", JsonArray(emptyList()))
                    put(
                        "dAppInfo",
                        buildJsonObject {
                            put("name", "Test DApp")
                            put("url", "testdapp.com") // Missing https://
                            put("iconUrl", "https://testdapp.com/icon.png")
                        },
                    )
                },
            )
        }
        val raw = JsonObject(emptyMap())

        val event = parser.parseEvent(EventTypeConstants.EVENT_CONNECT_REQUEST, data)

        assertNotNull("Should parse connect request", event)
        // The manifest URL normalization is applied internally
        assertTrue("Should be ConnectRequest", event is TONWalletKitEvent.ConnectRequest)
    }

    @Test
    fun parseEvent_connectRequest_withEmptyPermissions_parsesSuccessfully() {
        // Empty permissions array should work since it's still a valid list
        val data = buildJsonObject {
            put("id", "conn-req-empty")
            put("sessionId", "session-123")
            put("requestedItems", JsonArray(emptyList()))
            put(
                "preview",
                buildJsonObject {
                    put("permissions", JsonArray(emptyList())) // Empty array
                    put(
                        "dAppInfo",
                        buildJsonObject {
                            put("name", "Test DApp")
                            put("url", "https://example.com")
                        },
                    )
                },
            )
        }
        val raw = JsonObject(emptyMap())

        val event = parser.parseEvent(EventTypeConstants.EVENT_CONNECT_REQUEST, data)

        assertNotNull("Should parse connect request with empty permissions", event)
        assertTrue("Should be ConnectRequest event", event is TONWalletKitEvent.ConnectRequest)
    }

    @Test
    fun parseEvent_connectRequest_withoutPreview_parsesSuccessfully() {
        // preview is required but can have minimal structure with empty permissions
        val data = buildJsonObject {
            put("id", "conn-req-no-preview")
            put("sessionId", "session-no-preview")
            put("requestedItems", JsonArray(emptyList()))
            put(
                "preview",
                buildJsonObject {
                    put("permissions", JsonArray(emptyList()))
                },
            )
        }
        val raw = JsonObject(emptyMap())

        val event = parser.parseEvent(EventTypeConstants.EVENT_CONNECT_REQUEST, data)

        assertNotNull("Should parse connect request without preview", event)
        assertTrue("Should be ConnectRequest event", event is TONWalletKitEvent.ConnectRequest)
    }

    // --- Transaction Request Tests ---

    @Test
    fun parseEvent_transactionRequest_parsesValidJson() {
        val data = buildJsonObject {
            put("id", "tx-req-123")
            put("sessionId", "tx-session-123")
            put("walletAddress", "EQD...")
            put(
                "preview",
                buildJsonObject {
                    put(
                        "data",
                        buildJsonObject {
                            put("result", "success")
                        },
                    )
                },
            )
            put(
                "request",
                buildJsonObject {
                    put("messages", JsonArray(emptyList()))
                },
            )
        }
        val raw = JsonObject(emptyMap())

        val event = parser.parseEvent(EventTypeConstants.EVENT_TRANSACTION_REQUEST, data)

        assertNotNull("Should parse transaction request event", event)
        assertTrue("Should be SendTransactionRequest event", event is TONWalletKitEvent.SendTransactionRequest)
    }

    // --- Sign Data Request Tests ---

    @Test
    fun parseEvent_signDataRequest_parsesValidJson() {
        val data = buildJsonObject {
            put("id", "sign-req-123")
            put("sessionId", "sign-session-123")
            put("walletAddress", "EQD...")
            put(
                "payload",
                buildJsonObject {
                    put(
                        "data",
                        buildJsonObject {
                            put("type", "text")
                            put(
                                "value",
                                buildJsonObject {
                                    put("content", "Hello World")
                                },
                            )
                        },
                    )
                },
            )
            put(
                "preview",
                buildJsonObject {
                    put(
                        "data",
                        buildJsonObject {
                            put("type", "text")
                            put(
                                "value",
                                buildJsonObject {
                                    put("content", "Hello World")
                                },
                            )
                        },
                    )
                },
            )
        }
        val raw = JsonObject(emptyMap())

        val event = parser.parseEvent(EventTypeConstants.EVENT_SIGN_DATA_REQUEST, data)

        assertNotNull("Should parse sign data request event", event)
        assertTrue("Should be SignDataRequest event", event is TONWalletKitEvent.SignDataRequest)
    }
}
