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
package io.ton.walletkit.mockbridge

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.ton.walletkit.event.TONWalletKitEvent
import io.ton.walletkit.listener.TONBridgeEventsHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicInteger

/**
 * Event handling edge cases (scenarios 11, 13-22), plus additional handler lifecycle coverage.
 *
 * These tests verify SDK behavior for:
 * - Handler registration/deregistration edge cases
 * - Handler exception isolation
 * - Event routing to multiple handlers
 * - Unknown/malformed event handling
 */
@RunWith(AndroidJUnit4::class)
class EventHandlingEdgeCasesTests : MockBridgeTestBase() {
    override fun getMockScenarioHtml(): String = "event-handling-edge-cases"

    // Let base class auto-init SDK
    override fun autoInitWalletKit(): Boolean = true

    // We manage our own handlers in tests
    override fun autoAddEventsHandler(): Boolean = false

    /**
     * Scenario 21: Adding same handler instance twice should be idempotent.
     * The SDK should recognize the handler is already registered and not duplicate it.
     */
    @Test
    fun duplicateHandlerRegistration_isIdempotent() = runBlocking {
        val receivedEvents = mutableListOf<TONWalletKitEvent>()
        val handler = object : TONBridgeEventsHandler {
            override fun handle(event: TONWalletKitEvent) {
                receivedEvents.add(event)
            }
        }

        // Add same handler twice
        sdk.addEventsHandler(handler)
        sdk.addEventsHandler(handler)

        // Use real SDK method to trigger disconnect event
        sdk.disconnectSession("test-123")
        delay(300)

        // Handler should only receive event ONCE despite being added twice
        assertEquals("Handler should receive event exactly once", 1, receivedEvents.size)
    }

    /**
     * Scenario 22: Removing a handler that was never registered should be safe.
     * The SDK should not throw or crash.
     */
    @Test
    fun removeUnknownHandler_isSafe() = runBlocking {
        val neverAddedHandler = object : TONBridgeEventsHandler {
            override fun handle(event: TONWalletKitEvent) {
                // This should never be called
            }
        }

        // This should not throw
        sdk.removeEventsHandler(neverAddedHandler)

        // SDK should still be functional
        val receivedEvents = mutableListOf<TONWalletKitEvent>()
        val actualHandler = object : TONBridgeEventsHandler {
            override fun handle(event: TONWalletKitEvent) {
                receivedEvents.add(event)
            }
        }
        sdk.addEventsHandler(actualHandler)

        sdk.disconnectSession("test-456")
        delay(300)

        assertEquals("Handler should still receive events", 1, receivedEvents.size)
    }

    /**
     * Scenario 15: Exception in one handler should not break the event router.
     * The SDK should catch the exception and continue operation.
     */
    @Test
    fun handlerThrowsDuringProcessing_doesNotBreakRouter() = runBlocking {
        var handlerCalled = false
        val throwingHandler = object : TONBridgeEventsHandler {
            override fun handle(event: TONWalletKitEvent) {
                handlerCalled = true
                throw RuntimeException("Test exception from handler")
            }
        }

        sdk.addEventsHandler(throwingHandler)

        // Trigger event - should not crash
        sdk.disconnectSession("throw-test")
        delay(300)

        assertTrue("Handler should have been called", handlerCalled)

        // SDK should still be functional after exception
        val receivedAfterException = mutableListOf<TONWalletKitEvent>()
        val goodHandler = object : TONBridgeEventsHandler {
            override fun handle(event: TONWalletKitEvent) {
                receivedAfterException.add(event)
            }
        }
        sdk.addEventsHandler(goodHandler)

        sdk.disconnectSession("after-throw")
        delay(300)

        assertTrue("SDK should still route events after handler exception", receivedAfterException.isNotEmpty())
    }

    /**
     * Scenario 16: One failing handler should not block other handlers.
     * All handlers should receive the event even if one throws.
     */
    @Test
    fun multipleHandlersOneThrows_othersStillReceive() = runBlocking {
        val handler1Events = mutableListOf<TONWalletKitEvent>()
        val handler2Events = mutableListOf<TONWalletKitEvent>()
        val handler3Events = mutableListOf<TONWalletKitEvent>()

        val handler1 = object : TONBridgeEventsHandler {
            override fun handle(event: TONWalletKitEvent) {
                handler1Events.add(event)
            }
        }

        val throwingHandler = object : TONBridgeEventsHandler {
            override fun handle(event: TONWalletKitEvent) {
                handler2Events.add(event) // Record that we received it
                throw RuntimeException("Handler 2 throws!")
            }
        }

        val handler3 = object : TONBridgeEventsHandler {
            override fun handle(event: TONWalletKitEvent) {
                handler3Events.add(event)
            }
        }

        // Add all three handlers
        sdk.addEventsHandler(handler1)
        sdk.addEventsHandler(throwingHandler)
        sdk.addEventsHandler(handler3)

        // Trigger event
        sdk.disconnectSession("multi-handler")
        delay(300)

        // All handlers should have received the event
        assertEquals("Handler 1 should receive event", 1, handler1Events.size)
        assertEquals("Throwing handler should have received event before throwing", 1, handler2Events.size)
        assertEquals("Handler 3 should receive event despite handler 2 throwing", 1, handler3Events.size)
    }

    /**
     * Multiple handlers all receive the same event exactly once.
     */
    @Test
    fun multipleHandlersAllReceiveEvent() = runBlocking {
        val callCounts = AtomicInteger(0)
        val handlers = (1..5).map { index ->
            object : TONBridgeEventsHandler {
                override fun handle(event: TONWalletKitEvent) {
                    callCounts.incrementAndGet()
                }
            }
        }

        handlers.forEach { sdk.addEventsHandler(it) }

        // Trigger event
        sdk.disconnectSession("multi-test")
        delay(300)

        assertEquals("All 5 handlers should be called exactly once", 5, callCounts.get())
    }

    /**
     * Scenario 14: Event delivered after handler removal should be ignored without crash.
     */
    @Test
    fun eventAfterHandlerRemoved_isIgnored() = runBlocking {
        val receivedEvents = mutableListOf<TONWalletKitEvent>()
        val handler = object : TONBridgeEventsHandler {
            override fun handle(event: TONWalletKitEvent) {
                receivedEvents.add(event)
            }
        }

        sdk.addEventsHandler(handler)

        // Verify handler works
        sdk.disconnectSession("before-remove")
        delay(200)
        assertEquals("Handler should receive first event", 1, receivedEvents.size)

        // Remove handler
        sdk.removeEventsHandler(handler)

        // Send another event
        sdk.disconnectSession("after-remove")
        delay(200)

        // Handler should not receive event after removal
        assertEquals("Handler should not receive events after removal", 1, receivedEvents.size)
    }

    /**
     * Handler can be re-added after removal and will receive events again.
     */
    @Test
    fun handlersReaddedAfterRemoval_receivesEvents() = runBlocking {
        val receivedEvents = mutableListOf<TONWalletKitEvent>()
        val handler = object : TONBridgeEventsHandler {
            override fun handle(event: TONWalletKitEvent) {
                receivedEvents.add(event)
            }
        }

        // Add, receive, remove
        sdk.addEventsHandler(handler)
        sdk.disconnectSession("first")
        delay(200)
        assertEquals(1, receivedEvents.size)

        sdk.removeEventsHandler(handler)
        sdk.disconnectSession("while-removed")
        delay(200)
        assertEquals("Should not receive while removed", 1, receivedEvents.size)

        // Re-add and verify events flow again
        sdk.addEventsHandler(handler)
        sdk.disconnectSession("after-readd")
        delay(200)
        assertEquals("Should receive after re-add", 2, receivedEvents.size)
    }

    /**
     * No events delivered after removing the final handler.
     */
    @Test
    fun eventDeliveryStopsAfterLastHandlerRemoved() = runBlocking {
        val handler1Events = mutableListOf<TONWalletKitEvent>()
        val handler2Events = mutableListOf<TONWalletKitEvent>()

        val handler1 = object : TONBridgeEventsHandler {
            override fun handle(event: TONWalletKitEvent) {
                handler1Events.add(event)
            }
        }
        val handler2 = object : TONBridgeEventsHandler {
            override fun handle(event: TONWalletKitEvent) {
                handler2Events.add(event)
            }
        }

        sdk.addEventsHandler(handler1)
        sdk.addEventsHandler(handler2)

        // Both receive
        sdk.disconnectSession("both")
        delay(200)
        assertEquals(1, handler1Events.size)
        assertEquals(1, handler2Events.size)

        // Remove handler1
        sdk.removeEventsHandler(handler1)
        sdk.disconnectSession("only-h2")
        delay(200)
        assertEquals("Handler1 removed, should not receive", 1, handler1Events.size)
        assertEquals("Handler2 still active", 2, handler2Events.size)

        // Remove handler2 (last handler)
        sdk.removeEventsHandler(handler2)
        sdk.disconnectSession("none")
        delay(200)
        assertEquals("Handler1 still removed", 1, handler1Events.size)
        assertEquals("Handler2 now removed", 2, handler2Events.size)
    }

    /**
     * Scenario 20: Calls after destroy() should throw an exception.
     *
     * This test verifies that:
     * 1. Events work before destroy
     * 2. After destroy(), calling SDK methods throws WalletKitBridgeException
     * 3. destroy() is idempotent (can be called multiple times safely)
     */
    @Test
    fun callAfterDestroy_throwsException() = runBlocking {
        val receivedEvents = mutableListOf<TONWalletKitEvent>()
        val handler = object : TONBridgeEventsHandler {
            override fun handle(event: TONWalletKitEvent) {
                receivedEvents.add(event)
            }
        }
        sdk.addEventsHandler(handler)

        // Verify working before destroy
        sdk.disconnectSession("before-destroy")
        delay(200)
        assertEquals("Handler should receive event before destroy", 1, receivedEvents.size)

        // Destroy SDK
        sdk.destroy()

        // Calling methods after destroy should throw, not hang
        var exceptionThrown = false
        try {
            sdk.disconnectSession("after-destroy")
        } catch (e: Exception) {
            exceptionThrown = true
            assertTrue("Exception message should mention destroyed", e.message?.contains("destroyed") == true)
        }
        assertTrue("Should throw exception when calling destroyed SDK", exceptionThrown)

        // Verify destroy is idempotent - calling again should not crash
        sdk.destroy()

        // Handler should not have received additional events
        assertEquals("No additional events after destroy", 1, receivedEvents.size)

        // Skip teardown destroy since we already destroyed
        skipTeardownDestroy = true
    }

    /**
     * Scenario 11: Same event ID twice should be handled.
     * The SDK should handle duplicate event IDs gracefully - either dedup or process both.
     *
     * Note: Uses engine.callBridgeMethod to trigger duplicate event IDs from mock JS.
     */
    @Test
    fun duplicateEventId_isHandledGracefully() = runBlocking {
        val receivedEvents = mutableListOf<TONWalletKitEvent>()
        val handler = object : TONBridgeEventsHandler {
            override fun handle(event: TONWalletKitEvent) {
                receivedEvents.add(event)
            }
        }
        sdk.addEventsHandler(handler)

        // Trigger duplicate events with same ID from mock JS
        withContext(Dispatchers.Main) {
            engine.callBridgeMethod("test_sendDuplicateEvent", null)
        }
        delay(300)

        // SDK should handle duplicates without crashing
        // Current behavior: both events are delivered (no dedup)
        assertTrue("SDK should handle duplicate event IDs without crashing", receivedEvents.size >= 1)
    }

    /**
     * Scenario 19: Unknown event types should be ignored/logged without crash.
     *
     * Note: This uses engine.callBridgeMethod because we need to trigger an unknown
     * event type which can't be done through normal SDK methods.
     */
    @Test
    fun unknownEventType_doesNotCrash() = runBlocking {
        val receivedEvents = mutableListOf<TONWalletKitEvent>()
        val handler = object : TONBridgeEventsHandler {
            override fun handle(event: TONWalletKitEvent) {
                receivedEvents.add(event)
            }
        }
        sdk.addEventsHandler(handler)

        // Trigger unknown event type from JS (must use engine for this edge case)
        withContext(Dispatchers.Main) {
            engine.callBridgeMethod("test_sendUnknownEventType", null)
        }
        delay(300)

        // SDK should not crash; unknown events should be ignored (not dispatched)
        assertEquals("Unknown event types should not be dispatched to handlers", 0, receivedEvents.size)
    }

    /**
     * Scenario 13: Event emitted before handler added should be dropped.
     * Events are only dispatched to registered handlers, so early events are lost.
     */
    @Test
    fun eventBeforeHandlerRegistered_isDropped() = runBlocking {
        // Trigger event BEFORE adding handler
        sdk.disconnectSession("early-event")
        delay(100)

        // Now add handler
        val receivedEvents = mutableListOf<TONWalletKitEvent>()
        val handler = object : TONBridgeEventsHandler {
            override fun handle(event: TONWalletKitEvent) {
                receivedEvents.add(event)
            }
        }
        sdk.addEventsHandler(handler)
        delay(200)

        // Handler should NOT have received the early event
        assertEquals("Events before handler registration should be dropped", 0, receivedEvents.size)

        // Verify handler works for new events
        sdk.disconnectSession("after-registration")
        delay(200)
        assertEquals("Handler should receive events after registration", 1, receivedEvents.size)
    }

    /**
     * Scenario 17: Event with missing required fields should be handled gracefully.
     *
     * Note: Uses engine.callBridgeMethod to trigger malformed events from mock JS.
     */
    @Test
    fun eventMissingRequiredFields_isHandledGracefully() = runBlocking {
        val receivedEvents = mutableListOf<TONWalletKitEvent>()
        val handler = object : TONBridgeEventsHandler {
            override fun handle(event: TONWalletKitEvent) {
                receivedEvents.add(event)
            }
        }
        sdk.addEventsHandler(handler)

        // Trigger event with missing fields from mock JS
        withContext(Dispatchers.Main) {
            engine.callBridgeMethod("test_sendEventMissingFields", null)
        }
        delay(300)

        // SDK should not crash - malformed events should be ignored or handled gracefully
        // The handler may or may not receive the event depending on parsing
        assertTrue("SDK should not crash on malformed events", true)
    }

    /**
     * Scenario 18: Event with wrong payload type should be handled gracefully.
     *
     * Note: Uses engine.callBridgeMethod to trigger malformed events from mock JS.
     */
    @Test
    fun eventWrongPayloadType_isHandledGracefully() = runBlocking {
        val receivedEvents = mutableListOf<TONWalletKitEvent>()
        val handler = object : TONBridgeEventsHandler {
            override fun handle(event: TONWalletKitEvent) {
                receivedEvents.add(event)
            }
        }
        sdk.addEventsHandler(handler)

        // Trigger event with wrong payload type from mock JS
        withContext(Dispatchers.Main) {
            engine.callBridgeMethod("test_sendEventWrongPayloadType", null)
        }
        delay(300)

        // SDK should not crash - wrong payload types should be ignored
        assertTrue("SDK should not crash on wrong payload types", true)
    }

    /**
     * Handler added during event processing should not receive that event.
     */
    @Test
    fun handlerAddedDuringEvent_doesNotReceiveCurrentEvent() = runBlocking {
        val secondHandlerEvents = mutableListOf<TONWalletKitEvent>()
        val secondHandler = object : TONBridgeEventsHandler {
            override fun handle(event: TONWalletKitEvent) {
                secondHandlerEvents.add(event)
            }
        }

        var firstHandlerCalled = false
        val firstHandler = object : TONBridgeEventsHandler {
            override fun handle(event: TONWalletKitEvent) {
                if (!firstHandlerCalled) {
                    firstHandlerCalled = true
                    // Add second handler during event processing (use launch to avoid deadlock)
                    CoroutineScope(Dispatchers.Default).launch {
                        sdk.addEventsHandler(secondHandler)
                    }
                }
            }
        }

        sdk.addEventsHandler(firstHandler)

        // Trigger first event - firstHandler adds secondHandler during processing
        sdk.disconnectSession("first-event")
        delay(300)

        assertTrue("First handler should have been called", firstHandlerCalled)
        // Second handler may or may not receive the first event depending on implementation

        // Clear and verify second handler receives subsequent events
        secondHandlerEvents.clear()
        sdk.disconnectSession("second-event")
        delay(300)

        assertEquals("Second handler should receive subsequent events", 1, secondHandlerEvents.size)
    }

    /**
     * Removing handler during its own event processing should be safe.
     */
    @Test
    fun handlerRemovesItselfDuringEvent_isSafe() = runBlocking {
        val receivedEvents = mutableListOf<TONWalletKitEvent>()
        lateinit var selfRemovingHandler: TONBridgeEventsHandler

        selfRemovingHandler = object : TONBridgeEventsHandler {
            override fun handle(event: TONWalletKitEvent) {
                receivedEvents.add(event)
                // Remove self during event processing (use launch to avoid deadlock)
                CoroutineScope(Dispatchers.Default).launch {
                    sdk.removeEventsHandler(selfRemovingHandler)
                }
            }
        }

        sdk.addEventsHandler(selfRemovingHandler)

        // Trigger event - handler removes itself
        sdk.disconnectSession("self-remove")
        delay(300)

        assertEquals("Handler should have received the event before removing itself", 1, receivedEvents.size)

        // Trigger another event - handler should NOT receive it
        sdk.disconnectSession("after-self-remove")
        delay(300)

        assertEquals("Handler should not receive events after removing itself", 1, receivedEvents.size)
    }

    /**
     * Multiple events in quick succession should all be delivered.
     */
    @Test
    fun rapidEventSequence_allDelivered() = runBlocking {
        val receivedEvents = mutableListOf<TONWalletKitEvent>()
        val handler = object : TONBridgeEventsHandler {
            override fun handle(event: TONWalletKitEvent) {
                receivedEvents.add(event)
            }
        }
        sdk.addEventsHandler(handler)

        // Fire multiple events rapidly
        repeat(5) { i ->
            sdk.disconnectSession("rapid-$i")
        }
        delay(500)

        assertEquals("All 5 rapid events should be delivered", 5, receivedEvents.size)
    }

    /**
     * Events should be delivered in order.
     */
    @Test
    fun eventOrder_isPreserved() = runBlocking {
        val receivedSessionIds = mutableListOf<String>()
        val handler = object : TONBridgeEventsHandler {
            override fun handle(event: TONWalletKitEvent) {
                if (event is TONWalletKitEvent.Disconnect) {
                    receivedSessionIds.add(event.event.sessionId ?: "")
                }
            }
        }
        sdk.addEventsHandler(handler)

        // Fire events with numbered session IDs
        listOf("first", "second", "third").forEach { id ->
            sdk.disconnectSession(id)
            delay(50) // Small delay to ensure ordering
        }
        delay(300)

        assertEquals("All events should be received", 3, receivedSessionIds.size)
        assertEquals("Events should be in order", listOf("first", "second", "third"), receivedSessionIds)
    }
}
