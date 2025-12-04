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
package io.ton.walletkit.engine.state

import io.ton.walletkit.event.DisconnectEvent
import io.ton.walletkit.event.TONWalletKitEvent
import io.ton.walletkit.listener.TONBridgeEventsHandler
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

/**
 * Unit tests for EventRouter - handler registration, removal, and event dispatch.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class EventRouterTest {

    private lateinit var router: EventRouter

    @Before
    fun setup() {
        router = EventRouter()
    }

    // --- Add Handler Tests ---

    @Test
    fun addHandler_firstHandler_returnsIsFirstHandlerTrue() = runBlocking {
        val handler = createHandler()

        val outcome = router.addHandler(handler)

        assertTrue("First handler should return isFirstHandler=true", outcome.isFirstHandler)
        assertFalse("Should not be already registered", outcome.alreadyRegistered)
        assertEquals("Handler count should be 1", 1, router.getHandlerCount())
    }

    @Test
    fun addHandler_secondHandler_returnsIsFirstHandlerFalse() = runBlocking {
        val handler1 = createHandler()
        val handler2 = createHandler()

        router.addHandler(handler1)
        val outcome = router.addHandler(handler2)

        assertFalse("Second handler should return isFirstHandler=false", outcome.isFirstHandler)
        assertFalse("Should not be already registered", outcome.alreadyRegistered)
        assertEquals("Handler count should be 2", 2, router.getHandlerCount())
    }

    @Test
    fun addHandler_duplicateHandler_skipsAndReturnsAlreadyRegistered() = runBlocking {
        val handler = createHandler()

        router.addHandler(handler)
        val outcome = router.addHandler(handler)

        assertTrue("Duplicate should return alreadyRegistered=true", outcome.alreadyRegistered)
        assertFalse("Duplicate should not be first handler", outcome.isFirstHandler)
        assertEquals("Handler count should remain 1", 1, router.getHandlerCount())
    }

    // --- Remove Handler Tests ---

    @Test
    fun removeHandler_existingHandler_returnsRemovedTrue() = runBlocking {
        val handler = createHandler()
        router.addHandler(handler)

        val outcome = router.removeHandler(handler)

        assertTrue("Should return removed=true", outcome.removed)
        assertTrue("Should return isEmpty=true", outcome.isEmpty)
        assertEquals("Handler count should be 0", 0, router.getHandlerCount())
    }

    @Test
    fun removeHandler_unknownHandler_returnsRemovedFalse() = runBlocking {
        val handler1 = createHandler()
        val handler2 = createHandler()
        router.addHandler(handler1)

        val outcome = router.removeHandler(handler2)

        assertFalse("Should return removed=false", outcome.removed)
        assertFalse("Should return isEmpty=false", outcome.isEmpty)
        assertEquals("Handler count should remain 1", 1, router.getHandlerCount())
    }

    @Test
    fun removeHandler_oneOfTwo_returnsIsEmptyFalse() = runBlocking {
        val handler1 = createHandler()
        val handler2 = createHandler()
        router.addHandler(handler1)
        router.addHandler(handler2)

        val outcome = router.removeHandler(handler1)

        assertTrue("Should return removed=true", outcome.removed)
        assertFalse("Should return isEmpty=false (one handler left)", outcome.isEmpty)
        assertEquals("Handler count should be 1", 1, router.getHandlerCount())
    }

    // --- Contains Handler Tests ---

    @Test
    fun containsHandler_afterAdd_returnsTrue() = runBlocking {
        val handler = createHandler()
        router.addHandler(handler)

        val contains = router.containsHandler(handler)

        assertTrue("Should contain handler after add", contains)
    }

    @Test
    fun containsHandler_afterRemove_returnsFalse() = runBlocking {
        val handler = createHandler()
        router.addHandler(handler)
        router.removeHandler(handler)

        val contains = router.containsHandler(handler)

        assertFalse("Should not contain handler after remove", contains)
    }

    @Test
    fun containsHandler_neverAdded_returnsFalse() = runBlocking {
        val handler = createHandler()

        val contains = router.containsHandler(handler)

        assertFalse("Should not contain handler never added", contains)
    }

    // --- Dispatch Event Tests ---

    @Test
    fun dispatchEvent_singleHandler_receivesEvent() = runBlocking {
        val receivedEvents = mutableListOf<TONWalletKitEvent>()
        val handler = object : TONBridgeEventsHandler {
            override fun handle(event: TONWalletKitEvent) {
                receivedEvents.add(event)
            }
        }
        router.addHandler(handler)

        val event = TONWalletKitEvent.Disconnect(DisconnectEvent(sessionId = "test-session"))
        router.dispatchEvent("event-1", "disconnect", event)

        assertEquals("Handler should receive 1 event", 1, receivedEvents.size)
        assertEquals("Handler should receive correct event", event, receivedEvents[0])
    }

    @Test
    fun dispatchEvent_multipleHandlers_allReceiveEvent() = runBlocking {
        val received1 = mutableListOf<TONWalletKitEvent>()
        val received2 = mutableListOf<TONWalletKitEvent>()
        val received3 = mutableListOf<TONWalletKitEvent>()

        router.addHandler(object : TONBridgeEventsHandler {
            override fun handle(event: TONWalletKitEvent) {
                received1.add(event)
            }
        })
        router.addHandler(object : TONBridgeEventsHandler {
            override fun handle(event: TONWalletKitEvent) {
                received2.add(event)
            }
        })
        router.addHandler(object : TONBridgeEventsHandler {
            override fun handle(event: TONWalletKitEvent) {
                received3.add(event)
            }
        })

        val event = TONWalletKitEvent.Disconnect(DisconnectEvent(sessionId = "test-session"))
        router.dispatchEvent("event-1", "disconnect", event)

        assertEquals("Handler 1 should receive event", 1, received1.size)
        assertEquals("Handler 2 should receive event", 1, received2.size)
        assertEquals("Handler 3 should receive event", 1, received3.size)
    }

    @Test
    fun dispatchEvent_handlerThrows_otherHandlersContinue() = runBlocking {
        val received1 = mutableListOf<TONWalletKitEvent>()
        val received2 = mutableListOf<TONWalletKitEvent>()

        router.addHandler(object : TONBridgeEventsHandler {
            override fun handle(event: TONWalletKitEvent) {
                received1.add(event)
            }
        })
        router.addHandler(object : TONBridgeEventsHandler {
            override fun handle(event: TONWalletKitEvent) {
                throw RuntimeException("Handler error!")
            }
        })
        router.addHandler(object : TONBridgeEventsHandler {
            override fun handle(event: TONWalletKitEvent) {
                received2.add(event)
            }
        })

        val event = TONWalletKitEvent.Disconnect(DisconnectEvent(sessionId = "test-session"))
        router.dispatchEvent("event-1", "disconnect", event)

        assertEquals("Handler 1 should receive event", 1, received1.size)
        assertEquals("Handler 3 should receive event despite handler 2 throwing", 1, received2.size)
    }

    @Test
    fun dispatchEvent_noHandlers_doesNotThrow() = runBlocking {
        val event = TONWalletKitEvent.Disconnect(DisconnectEvent(sessionId = "test-session"))

        // Should not throw
        router.dispatchEvent("event-1", "disconnect", event)
    }

    // --- Concurrent Operations Tests ---

    @Test
    fun concurrentAddRemove_isSafe() = runBlocking {
        val handlers = (1..20).map { createHandler() }
        val errors = CopyOnWriteArrayList<Exception>()

        val jobs = handlers.mapIndexed { index, handler ->
            async {
                try {
                    router.addHandler(handler)
                    delay(10)
                    router.removeHandler(handler)
                } catch (e: Exception) {
                    errors.add(e)
                }
            }
        }

        jobs.awaitAll()

        assertTrue("No errors during concurrent add/remove", errors.isEmpty())
        assertEquals("All handlers should be removed", 0, router.getHandlerCount())
    }

    @Test
    fun concurrentDispatch_allHandlersReceiveEvents() = runBlocking {
        val eventCount = AtomicInteger(0)
        val handler = object : TONBridgeEventsHandler {
            override fun handle(event: TONWalletKitEvent) {
                eventCount.incrementAndGet()
            }
        }
        router.addHandler(handler)

        val jobs = (1..10).map { i ->
            async {
                val event = TONWalletKitEvent.Disconnect(DisconnectEvent(sessionId = "session-$i"))
                router.dispatchEvent("event-$i", "disconnect", event)
            }
        }

        jobs.awaitAll()

        assertEquals("Handler should receive all 10 events", 10, eventCount.get())
    }

    // --- Helper ---

    private fun createHandler(): TONBridgeEventsHandler = object : TONBridgeEventsHandler {
        override fun handle(event: TONWalletKitEvent) {}
    }
}
