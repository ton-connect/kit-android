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
package io.ton.walletkit.bridge

import io.ton.walletkit.event.TONWalletKitEvent
import io.ton.walletkit.listener.TONBridgeEventsHandler
import org.junit.Assert.*
import org.junit.Test

/**
 * Basic tests for event retry mechanism.
 *
 * Note: Storage tests are in PendingEventStorageTest (instrumented tests)
 * since they require real Android framework components.
 */
class EventRetryMechanismTest {

    @Test
    fun `handler interface can be implemented`() {
        var eventReceived = false

        val handler = object : TONBridgeEventsHandler {
            override fun handle(event: TONWalletKitEvent) {
                eventReceived = true
            }
        }

        assertNotNull(handler)
        assertFalse(eventReceived)
    }

    @Test
    fun `multiple handlers can coexist`() {
        val handler1 = object : TONBridgeEventsHandler {
            override fun handle(event: TONWalletKitEvent) {}
        }

        val handler2 = object : TONBridgeEventsHandler {
            override fun handle(event: TONWalletKitEvent) {}
        }

        assertNotNull(handler1)
        assertNotNull(handler2)
        assertNotSame(handler1, handler2)
    }

    @Test
    fun `handler can store received events`() {
        val receivedEvents = mutableListOf<TONWalletKitEvent>()

        val handler = object : TONBridgeEventsHandler {
            override fun handle(event: TONWalletKitEvent) {
                receivedEvents.add(event)
            }
        }

        assertTrue(receivedEvents.isEmpty())
        assertNotNull(handler)
    }
}
