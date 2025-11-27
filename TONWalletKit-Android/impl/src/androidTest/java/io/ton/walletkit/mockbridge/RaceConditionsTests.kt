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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

/**
 * Concurrency & race conditions tests.
 *
 * These tests verify the SDK handles concurrent operations correctly:
 * - Handler registration from multiple threads
 * - Concurrent RPC calls
 * - Destroy during event processing
 * - Init/destroy race conditions
 */
@RunWith(AndroidJUnit4::class)
class RaceConditionsTests : MockBridgeTestBase() {
    override fun getMockScenarioHtml(): String = "race-conditions-scenarios"
    override fun autoInitWalletKit(): Boolean = true
    override fun autoAddEventsHandler(): Boolean = false

    /**
     * Scenario 96: Handler registration from background thread should be safe.
     * EventRouter uses mutex for thread-safety.
     */
    @Test
    fun handlerRegisteredFromBackgroundThread_isSafe() = runBlocking {
        val receivedEvents = CopyOnWriteArrayList<TONWalletKitEvent>()
        val latch = CountDownLatch(1)

        // Register handler from a background thread
        thread {
            runBlocking {
                val handler = object : TONBridgeEventsHandler {
                    override fun handle(event: TONWalletKitEvent) {
                        receivedEvents.add(event)
                    }
                }
                sdk.addEventsHandler(handler)
                latch.countDown()
            }
        }

        // Wait for handler to be registered
        assertTrue("Handler registration should complete", latch.await(2, TimeUnit.SECONDS))

        // Trigger event and verify handler receives it
        sdk.disconnectSession("background-thread-test")
        delay(300)

        assertEquals("Handler registered from background thread should receive events", 1, receivedEvents.size)
    }

    /**
     * Scenario 97: Concurrent RPC calls should all complete correctly.
     * BridgeRpcClient uses ConcurrentHashMap for pending calls.
     */
    @Test
    fun concurrentRpcCalls_allComplete() = runBlocking {
        val numCalls = 10
        val results = CopyOnWriteArrayList<Boolean>()

        // Launch multiple concurrent RPC calls
        val jobs = (1..numCalls).map { i ->
            async(Dispatchers.Default) {
                try {
                    sdk.disconnectSession("concurrent-$i")
                    results.add(true)
                } catch (e: Exception) {
                    results.add(false)
                }
            }
        }

        // Wait for all to complete
        jobs.awaitAll()
        delay(300)

        assertEquals("All $numCalls concurrent RPC calls should complete", numCalls, results.size)
        assertTrue("All calls should succeed", results.all { it })
    }

    /**
     * Scenario 98: Multiple handlers registered concurrently should all be added.
     */
    @Test
    fun concurrentHandlerRegistration_allAdded() = runBlocking {
        val numHandlers = 10
        val registeredCount = AtomicInteger(0)
        val receivedCounts = CopyOnWriteArrayList<AtomicInteger>()

        // Create handlers and track them
        val handlers = (1..numHandlers).map {
            val counter = AtomicInteger(0)
            receivedCounts.add(counter)
            object : TONBridgeEventsHandler {
                override fun handle(event: TONWalletKitEvent) {
                    counter.incrementAndGet()
                }
            }
        }

        // Register all handlers concurrently
        val jobs = handlers.map { handler ->
            async(Dispatchers.Default) {
                sdk.addEventsHandler(handler)
                registeredCount.incrementAndGet()
            }
        }

        jobs.awaitAll()
        assertEquals("All handlers should be registered", numHandlers, registeredCount.get())

        // Trigger an event
        sdk.disconnectSession("concurrent-registration-test")
        delay(300)

        // All handlers should receive the event
        val totalReceived = receivedCounts.sumOf { it.get() }
        assertEquals("All $numHandlers handlers should receive the event", numHandlers, totalReceived)
    }

    /**
     * Scenario 99: Calling destroy during event handler execution should be safe.
     */
    @Test
    fun destroyDuringEventHandler_isSafe() = runBlocking {
        val handlerCalled = AtomicInteger(0)

        val handler = object : TONBridgeEventsHandler {
            override fun handle(event: TONWalletKitEvent) {
                handlerCalled.incrementAndGet()
                // Simulate slow handler processing
                Thread.sleep(100)
            }
        }
        sdk.addEventsHandler(handler)

        // Fire event
        CoroutineScope(Dispatchers.Default).launch {
            sdk.disconnectSession("destroy-during-handler")
        }

        // Give time for event to start processing, then destroy
        delay(50)
        skipTeardownDestroy = true
        sdk.destroy()

        delay(200)

        // Handler should have been called (may or may not complete depending on timing)
        assertTrue("Handler should have started processing", handlerCalled.get() >= 1)
    }

    /**
     * Scenario 100: Coroutine cancellation should not hang pending calls.
     */
    @Test
    fun cancellationDoesNotHangPendingCalls() = runBlocking {
        // Use timeout to ensure we don't hang
        val result = withTimeoutOrNull(3000) {
            // This should complete or timeout, but not hang forever
            val job = CoroutineScope(Dispatchers.Default).launch {
                try {
                    // Make an RPC call
                    sdk.disconnectSession("cancellation-test")
                } catch (e: Exception) {
                    // Expected if cancelled
                }
            }

            delay(50)
            job.cancel()
            job.join()

            "completed"
        }

        assertNotNull("Operation should complete within timeout", result)
    }

    /**
     * Scenario 127: Handler removal during event dispatch should be safe.
     */
    @Test
    fun handlerRemovedDuringDispatch_isSafe() = runBlocking {
        val handler1Events = CopyOnWriteArrayList<TONWalletKitEvent>()
        val handler2Events = CopyOnWriteArrayList<TONWalletKitEvent>()

        lateinit var handler1: TONBridgeEventsHandler

        handler1 = object : TONBridgeEventsHandler {
            override fun handle(event: TONWalletKitEvent) {
                handler1Events.add(event)
                // Remove self during dispatch
                CoroutineScope(Dispatchers.Default).launch {
                    sdk.removeEventsHandler(handler1)
                }
            }
        }

        val handler2 = object : TONBridgeEventsHandler {
            override fun handle(event: TONWalletKitEvent) {
                handler2Events.add(event)
            }
        }

        sdk.addEventsHandler(handler1)
        sdk.addEventsHandler(handler2)

        // Fire event
        sdk.disconnectSession("remove-during-dispatch")
        delay(300)

        // Both handlers should have received the first event
        assertEquals("Handler1 should receive event before removal", 1, handler1Events.size)
        assertEquals("Handler2 should receive event", 1, handler2Events.size)

        // Fire another event - handler1 should not receive it
        sdk.disconnectSession("after-removal")
        delay(300)

        assertEquals("Handler1 should not receive events after self-removal", 1, handler1Events.size)
        assertEquals("Handler2 should receive second event", 2, handler2Events.size)
    }

    /**
     * Scenario 128: Init completing after destroy should not revive instance.
     */
    @Test
    fun initAfterDestroy_throwsException() = runBlocking {
        // Destroy the SDK
        skipTeardownDestroy = true
        sdk.destroy()
        delay(100)

        // Trying to use the SDK after destroy should throw
        var exceptionThrown = false
        try {
            sdk.disconnectSession("after-destroy")
        } catch (e: Exception) {
            exceptionThrown = true
            assertTrue("Exception should mention destroyed", e.message?.contains("destroyed") == true)
        }

        assertTrue("Should throw exception when calling destroyed SDK", exceptionThrown)
    }

    /**
     * Rapid add/remove of handlers should be safe.
     */
    @Test
    fun rapidAddRemoveHandlers_isSafe() = runBlocking {
        val operations = AtomicInteger(0)
        val errors = CopyOnWriteArrayList<Exception>()

        // Rapidly add and remove handlers
        val jobs = (1..20).map { i ->
            async(Dispatchers.Default) {
                try {
                    val handler = object : TONBridgeEventsHandler {
                        override fun handle(event: TONWalletKitEvent) {}
                    }
                    sdk.addEventsHandler(handler)
                    operations.incrementAndGet()
                    delay(10)
                    sdk.removeEventsHandler(handler)
                    operations.incrementAndGet()
                } catch (e: Exception) {
                    errors.add(e)
                }
            }
        }

        jobs.awaitAll()

        assertTrue("No errors during rapid add/remove", errors.isEmpty())
        assertEquals("All add/remove operations should complete", 40, operations.get())
    }

    /**
     * Concurrent events should all be delivered to handlers.
     */
    @Test
    fun concurrentEvents_allDelivered() = runBlocking {
        val receivedEvents = CopyOnWriteArrayList<TONWalletKitEvent>()
        val handler = object : TONBridgeEventsHandler {
            override fun handle(event: TONWalletKitEvent) {
                receivedEvents.add(event)
            }
        }
        sdk.addEventsHandler(handler)

        val numEvents = 10

        // Fire multiple events concurrently
        val jobs = (1..numEvents).map { i ->
            async(Dispatchers.Default) {
                sdk.disconnectSession("concurrent-event-$i")
            }
        }

        jobs.awaitAll()
        delay(500)

        assertEquals("All $numEvents concurrent events should be delivered", numEvents, receivedEvents.size)
    }

    /**
     * SDK handles main thread requirement for WebView operations.
     */
    @Test
    fun sdkHandlesMainThreadRequirement() = runBlocking {
        // Calling SDK methods from background thread should work
        // (SDK internally handles dispatching to main thread)
        var result: Boolean? = null

        withContext(Dispatchers.Default) {
            try {
                sdk.disconnectSession("main-thread-test")
                result = true
            } catch (e: Exception) {
                result = false
            }
        }

        delay(200)
        assertTrue("SDK should handle main thread requirement internally", result == true)
    }
}
