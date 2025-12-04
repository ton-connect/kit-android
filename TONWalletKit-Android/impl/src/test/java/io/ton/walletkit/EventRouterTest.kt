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
package io.ton.walletkit

import io.ton.walletkit.engine.state.EventRouter
import io.ton.walletkit.event.TONWalletKitEvent
import io.ton.walletkit.listener.TONBridgeEventsHandler
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

/**
 * Tests for EventRouter - Event Handling & Event Dispatcher
 *
 * Covers edge cases 11-22 from ANDROID_SDK_EDGE_CASE_TEST_SCENARIOS.md:
 * - Duplicate events with same ID
 * - Event flooding (many rapid events)
 * - Events before/after handlers registered
 * - Handler exceptions
 * - Multiple handlers with failures
 * - Missing required fields
 * - Wrong payload types
 * - Unknown event types
 * - Events after destroy
 * - Duplicate handler registration
 * - Remove unregistered handler
 */
class EventRouterTest {

    private lateinit var eventRouter: EventRouter
    private val receivedEvents = mutableListOf<TONWalletKitEvent>()

    private val testHandler = object : TONBridgeEventsHandler {
        override fun handle(event: TONWalletKitEvent) {
            receivedEvents.add(event)
        }
    }

    @Before
    fun setup() {
        eventRouter = EventRouter()
        receivedEvents.clear()
    }

    // ===== Scenario 11: Duplicate event with same event ID =====

    @Test
    fun `dispatchEvent - duplicate events with same ID both delivered`() = runTest {
        val handler = object : TONBridgeEventsHandler {
            val events = mutableListOf<TONWalletKitEvent>()
            override fun handle(event: TONWalletKitEvent) {
                events.add(event)
            }
        }

        eventRouter.addHandler(handler)

        // Create disconnect event (simplest event type)
        val event1 = TONWalletKitEvent.Disconnect(
            io.ton.walletkit.event.DisconnectEvent("session-1"),
        )
        val event2 = TONWalletKitEvent.Disconnect(
            io.ton.walletkit.event.DisconnectEvent("session-1"),
        )

        // Dispatch same event ID twice
        eventRouter.dispatchEvent("event-1", "disconnect", event1)
        eventRouter.dispatchEvent("event-1", "disconnect", event2)

        // Both should be delivered (no deduplication)
        assertEquals(2, handler.events.size)
    }

    // ===== Scenario 12: Events arrive in rapid succession (flooding) =====

    @Test
    fun `dispatchEvent - flooding with 100+ events handles all`() = runTest {
        val counter = AtomicInteger(0)
        val handler = object : TONBridgeEventsHandler {
            override fun handle(event: TONWalletKitEvent) {
                counter.incrementAndGet()
            }
        }

        eventRouter.addHandler(handler)

        // Flood with 200 events
        repeat(200) { i ->
            val event = TONWalletKitEvent.Disconnect(
                io.ton.walletkit.event.DisconnectEvent("session-$i"),
            )
            eventRouter.dispatchEvent("event-$i", "disconnect", event)
        }

        // All 200 events should be processed
        assertEquals(200, counter.get())
    }

    // ===== Scenario 13: Event arrives before handler registered =====

    @Test
    fun `dispatchEvent - event dispatched before any handler added`() = runTest {
        // No handler registered yet
        val event = TONWalletKitEvent.Disconnect(
            io.ton.walletkit.event.DisconnectEvent("session-1"),
        )

        // Should not crash, just no-op
        eventRouter.dispatchEvent("event-1", "disconnect", event)

        // Now add handler
        eventRouter.addHandler(testHandler)

        // Handler should not receive the old event (no replay)
        assertEquals(0, receivedEvents.size)
    }

    // ===== Scenario 14: Event arrives after handler removed =====

    @Test
    fun `dispatchEvent - after handler removed does not deliver`() = runTest {
        eventRouter.addHandler(testHandler)

        // Remove handler immediately
        eventRouter.removeHandler(testHandler)

        // Dispatch event
        val event = TONWalletKitEvent.Disconnect(
            io.ton.walletkit.event.DisconnectEvent("session-1"),
        )
        eventRouter.dispatchEvent("event-1", "disconnect", event)

        // Handler should not receive event
        assertEquals(0, receivedEvents.size)
    }

    // ===== Scenario 15: Handler throws exception during event processing =====

    @Test
    fun `dispatchEvent - handler exception is caught and logged`() = runTest {
        val throwingHandler = object : TONBridgeEventsHandler {
            override fun handle(event: TONWalletKitEvent) {
                throw RuntimeException("Handler crashed!")
            }
        }

        eventRouter.addHandler(throwingHandler)

        val event = TONWalletKitEvent.Disconnect(
            io.ton.walletkit.event.DisconnectEvent("session-1"),
        )

        // Should not propagate exception
        eventRouter.dispatchEvent("event-1", "disconnect", event)

        // No exception thrown - it's caught and logged
    }

    // ===== Scenario 16: Multiple handlers with one throwing exception =====

    @Test
    fun `dispatchEvent - one handler fails but others still receive event`() = runTest {
        val handler1Events = mutableListOf<TONWalletKitEvent>()
        val handler2Events = mutableListOf<TONWalletKitEvent>()
        val handler3Events = mutableListOf<TONWalletKitEvent>()

        val handler1 = object : TONBridgeEventsHandler {
            override fun handle(event: TONWalletKitEvent) {
                handler1Events.add(event)
            }
        }

        val handler2Throwing = object : TONBridgeEventsHandler {
            override fun handle(event: TONWalletKitEvent) {
                throw RuntimeException("Handler 2 failed!")
            }
        }

        val handler3 = object : TONBridgeEventsHandler {
            override fun handle(event: TONWalletKitEvent) {
                handler3Events.add(event)
            }
        }

        eventRouter.addHandler(handler1)
        eventRouter.addHandler(handler2Throwing)
        eventRouter.addHandler(handler3)

        val event = TONWalletKitEvent.Disconnect(
            io.ton.walletkit.event.DisconnectEvent("session-1"),
        )

        eventRouter.dispatchEvent("event-1", "disconnect", event)

        // Handler 1 and 3 should receive event despite handler 2 throwing
        assertEquals(1, handler1Events.size)
        assertEquals(1, handler3Events.size)
    }

    // ===== Scenario 17-19: Malformed events handled by EventParser, not EventRouter =====
    // EventRouter receives already-parsed TONWalletKitEvent objects
    // So these scenarios are tested in DataValidationTest/EventParserTest

    // ===== Scenario 20: Event arrives after SDK destroyed =====

    @Test
    fun `dispatchEvent - after all handlers removed behaves gracefully`() = runTest {
        eventRouter.addHandler(testHandler)
        eventRouter.removeHandler(testHandler)

        // Simulate post-destroy event dispatch
        val event = TONWalletKitEvent.Disconnect(
            io.ton.walletkit.event.DisconnectEvent("session-1"),
        )

        // Should not crash, just no-op
        eventRouter.dispatchEvent("event-1", "disconnect", event)

        assertEquals(0, receivedEvents.size)
    }

    // ===== Scenario 21: Same event handler added multiple times =====

    @Test
    fun `addHandler - same handler instance added twice is idempotent`() = runTest {
        val result1 = eventRouter.addHandler(testHandler)
        assertFalse(result1.alreadyRegistered)
        assertTrue(result1.isFirstHandler)

        val result2 = eventRouter.addHandler(testHandler)
        assertTrue(result2.alreadyRegistered)
        assertFalse(result2.isFirstHandler)

        // Handler count should be 1, not 2
        assertEquals(1, eventRouter.getHandlerCount())

        // Dispatch event - should only be called once
        val event = TONWalletKitEvent.Disconnect(
            io.ton.walletkit.event.DisconnectEvent("session-1"),
        )
        eventRouter.dispatchEvent("event-1", "disconnect", event)

        assertEquals(1, receivedEvents.size)
    }

    // ===== Scenario 22: Remove handler that was never added =====

    @Test
    fun `removeHandler - unregistered handler returns false`() = runTest {
        val neverAddedHandler = object : TONBridgeEventsHandler {
            override fun handle(event: TONWalletKitEvent) {}
        }

        val result = eventRouter.removeHandler(neverAddedHandler)

        assertFalse(result.removed)
        assertTrue(result.isEmpty)
    }

    // ===== Additional: Multiple handlers work correctly =====

    @Test
    fun `addHandler - multiple different handlers all receive events`() = runTest {
        val events1 = mutableListOf<TONWalletKitEvent>()
        val events2 = mutableListOf<TONWalletKitEvent>()

        val handler1 = object : TONBridgeEventsHandler {
            override fun handle(event: TONWalletKitEvent) {
                events1.add(event)
            }
        }

        val handler2 = object : TONBridgeEventsHandler {
            override fun handle(event: TONWalletKitEvent) {
                events2.add(event)
            }
        }

        eventRouter.addHandler(handler1)
        eventRouter.addHandler(handler2)

        val event = TONWalletKitEvent.Disconnect(
            io.ton.walletkit.event.DisconnectEvent("session-1"),
        )

        eventRouter.dispatchEvent("event-1", "disconnect", event)

        assertEquals(1, events1.size)
        assertEquals(1, events2.size)
    }

    // ===== Edge case: containsHandler check =====

    @Test
    fun `containsHandler - returns true for registered handler`() = runTest {
        assertFalse(eventRouter.containsHandler(testHandler))

        eventRouter.addHandler(testHandler)
        assertTrue(eventRouter.containsHandler(testHandler))

        eventRouter.removeHandler(testHandler)
        assertFalse(eventRouter.containsHandler(testHandler))
    }

    // ===== Edge case: Handler count tracking =====

    @Test
    fun `getHandlerCount - tracks handlers correctly`() = runTest {
        assertEquals(0, eventRouter.getHandlerCount())

        val handler1 = object : TONBridgeEventsHandler {
            override fun handle(event: TONWalletKitEvent) {}
        }
        val handler2 = object : TONBridgeEventsHandler {
            override fun handle(event: TONWalletKitEvent) {}
        }

        eventRouter.addHandler(handler1)
        assertEquals(1, eventRouter.getHandlerCount())

        eventRouter.addHandler(handler2)
        assertEquals(2, eventRouter.getHandlerCount())

        eventRouter.removeHandler(handler1)
        assertEquals(1, eventRouter.getHandlerCount())

        eventRouter.removeHandler(handler2)
        assertEquals(0, eventRouter.getHandlerCount())
    }
}
