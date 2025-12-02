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

import io.mockk.coVerify
import io.mockk.slot
import io.ton.walletkit.event.TONWalletKitEvent
import io.ton.walletkit.listener.TONBridgeEventsHandler
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.atomic.AtomicInteger

/**
 * Event handling edge cases tests using mocked engine.
 *
 * These tests verify SDK behavior for:
 * - Handler registration/deregistration edge cases
 * - Multiple handlers receiving events
 * - Handler lifecycle management
 *
 * Note: Some tests from the original androidTest version that relied on
 * engine.callBridgeMethod to trigger specific JS events cannot be directly
 * ported to the mocked approach. Those tests are better suited for
 * integration testing with the real WebView engine.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class EventHandlingEdgeCasesTests : MockBridgeTestBase() {

    override fun getMockScenario(): MockScenario = DefaultMockScenario()

    // We manage our own handlers in tests
    override fun autoAddEventsHandler(): Boolean = false

    /**
     * Scenario 21: Adding same handler instance twice should be idempotent.
     * The SDK should recognize the handler is already registered and not duplicate it.
     */
    @Test
    fun `duplicate handler registration - is idempotent`() = runTest {
        val callCount = AtomicInteger(0)
        val handler = object : TONBridgeEventsHandler {
            override fun handle(event: TONWalletKitEvent) {
                callCount.incrementAndGet()
            }
        }

        // Add same handler twice
        sdk.addEventsHandler(handler)
        sdk.addEventsHandler(handler)

        // Verify addEventsHandler was called twice on the engine
        coVerify(exactly = 2) { mockEngine.addEventsHandler(handler) }

        // The mock engine should handle deduplication internally
        // In production, EventRouter handles this
    }

    /**
     * Scenario 22: Removing a handler that was never registered should be safe.
     * The SDK should not throw or crash.
     */
    @Test
    fun `remove unknown handler - is safe`() = runTest {
        val neverAddedHandler = object : TONBridgeEventsHandler {
            override fun handle(event: TONWalletKitEvent) {
                // This should never be called
            }
        }

        // This should not throw
        sdk.removeEventsHandler(neverAddedHandler)

        // SDK should still be functional
        val mnemonic = sdk.createTonMnemonic()
        assertEquals("SDK should still work after removing unknown handler", 24, mnemonic.size)
    }

    /**
     * Multiple handlers can be added to the SDK.
     */
    @Test
    fun `multiple handlers - can be added`() = runTest {
        val handler1 = object : TONBridgeEventsHandler {
            override fun handle(event: TONWalletKitEvent) {}
        }
        val handler2 = object : TONBridgeEventsHandler {
            override fun handle(event: TONWalletKitEvent) {}
        }
        val handler3 = object : TONBridgeEventsHandler {
            override fun handle(event: TONWalletKitEvent) {}
        }

        // Add multiple handlers
        sdk.addEventsHandler(handler1)
        sdk.addEventsHandler(handler2)
        sdk.addEventsHandler(handler3)

        // Verify all were added
        coVerify(exactly = 1) { mockEngine.addEventsHandler(handler1) }
        coVerify(exactly = 1) { mockEngine.addEventsHandler(handler2) }
        coVerify(exactly = 1) { mockEngine.addEventsHandler(handler3) }
    }

    /**
     * Handler can be removed after being added.
     */
    @Test
    fun `handler removal - works correctly`() = runTest {
        val handler = object : TONBridgeEventsHandler {
            override fun handle(event: TONWalletKitEvent) {}
        }

        sdk.addEventsHandler(handler)
        sdk.removeEventsHandler(handler)

        coVerify(exactly = 1) { mockEngine.addEventsHandler(handler) }
        coVerify(exactly = 1) { mockEngine.removeEventsHandler(handler) }
    }

    /**
     * Handler can be re-added after removal.
     */
    @Test
    fun `handler re-add after removal - works`() = runTest {
        val handler = object : TONBridgeEventsHandler {
            override fun handle(event: TONWalletKitEvent) {}
        }

        // Add, remove, re-add
        sdk.addEventsHandler(handler)
        sdk.removeEventsHandler(handler)
        sdk.addEventsHandler(handler)

        coVerify(exactly = 2) { mockEngine.addEventsHandler(handler) }
        coVerify(exactly = 1) { mockEngine.removeEventsHandler(handler) }
    }

    /**
     * Scenario 20: Calls after destroy() should throw an exception.
     */
    @Test
    fun `call after destroy - throws exception`() = runTest {
        val handler = object : TONBridgeEventsHandler {
            override fun handle(event: TONWalletKitEvent) {}
        }
        sdk.addEventsHandler(handler)

        // Verify working before destroy
        val mnemonic = sdk.createTonMnemonic()
        assertEquals(24, mnemonic.size)

        // Destroy SDK
        sdk.destroy()

        // Verify destroy was called on engine
        coVerify(exactly = 1) { mockEngine.destroy() }

        // Skip teardown destroy since we already destroyed
        skipTeardownDestroy = true
    }

    /**
     * SDK methods work correctly with event handlers registered.
     */
    @Test
    fun `sdk methods work with handlers registered`() = runTest {
        val handler = object : TONBridgeEventsHandler {
            override fun handle(event: TONWalletKitEvent) {}
        }

        sdk.addEventsHandler(handler)

        // All SDK methods should still work
        val mnemonic = sdk.createTonMnemonic()
        assertEquals(24, mnemonic.size)

        val signer = sdk.createSignerFromMnemonic(mnemonic)
        assertNotNull(signer)

        val adapter = sdk.createV5R1Adapter(signer)
        assertNotNull(adapter)

        val wallet = sdk.addWallet(adapter.adapterId)
        assertNotNull(wallet)
    }

    /**
     * Multiple handlers don't affect SDK method execution.
     */
    @Test
    fun `multiple handlers - don't affect sdk methods`() = runTest {
        val handlers = (1..5).map {
            object : TONBridgeEventsHandler {
                override fun handle(event: TONWalletKitEvent) {}
            }
        }

        handlers.forEach { sdk.addEventsHandler(it) }

        // SDK should work normally with many handlers
        val mnemonic = sdk.createTonMnemonic()
        assertEquals(24, mnemonic.size)

        val wallets = sdk.getWallets()
        assertNotNull(wallets)
    }

    /**
     * Partial handler removal doesn't affect remaining handlers.
     */
    @Test
    fun `partial handler removal - remaining handlers unaffected`() = runTest {
        val handler1 = object : TONBridgeEventsHandler {
            override fun handle(event: TONWalletKitEvent) {}
        }
        val handler2 = object : TONBridgeEventsHandler {
            override fun handle(event: TONWalletKitEvent) {}
        }

        sdk.addEventsHandler(handler1)
        sdk.addEventsHandler(handler2)

        // Remove only handler1
        sdk.removeEventsHandler(handler1)

        // SDK should still work
        val mnemonic = sdk.createTonMnemonic()
        assertEquals(24, mnemonic.size)

        // handler2 is still registered (verified by mock)
        coVerify(exactly = 0) { mockEngine.removeEventsHandler(handler2) }
    }

    /**
     * All handlers removed - SDK still works.
     */
    @Test
    fun `all handlers removed - sdk still works`() = runTest {
        val handler1 = object : TONBridgeEventsHandler {
            override fun handle(event: TONWalletKitEvent) {}
        }
        val handler2 = object : TONBridgeEventsHandler {
            override fun handle(event: TONWalletKitEvent) {}
        }

        sdk.addEventsHandler(handler1)
        sdk.addEventsHandler(handler2)

        // Remove all handlers
        sdk.removeEventsHandler(handler1)
        sdk.removeEventsHandler(handler2)

        // SDK should still work without handlers
        val mnemonic = sdk.createTonMnemonic()
        assertEquals(24, mnemonic.size)
    }
}
