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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.seconds

/**
 * Tests duplicate call ID scenario using REAL SDK with MOCK JavaScript.
 *
 * Scenario: JavaScript sends multiple responses with the same call ID
 * (simulating buggy backend or network duplication).
 *
 * SDK Implementation (BridgeRpcClient.handleResponse):
 * - Line 115: `val deferred = pending.remove(id)`
 * - First response: remove() returns deferred and removes from map -> completes deferred
 * - Duplicate responses: remove() returns null -> logs warning and returns early
 *
 * CompletableDeferred Behavior:
 * - complete() returns true on first call (successfully completed)
 * - complete() returns false on subsequent calls (already completed, ignores new value)
 * - Does NOT throw exception, just returns false and ignores duplicate
 *
 * Test Verification:
 * - SDK uses pending.remove(id) so duplicates never reach deferred.complete()
 * - Even if they did, deferred.complete() returns false and ignores them
 * - Tests verify SDK handles duplicates gracefully without exceptions
 */
@RunWith(AndroidJUnit4::class)
class DuplicateCallIdMockTest : MockBridgeTestBase() {

    override fun getMockScenarioHtml(): String = "duplicate-callid"

    @Test(timeout = 5000)
    fun duplicateCallIdSdkIgnoresDuplicateResponses() = runBlocking {
        withContext(Dispatchers.Main) {
            withTimeout(3.seconds) {
                events.clear()

                // Mock sends same response 3 times (0ms, 100ms, 200ms)
                // SDK should process only the first response
                val mnemonic = sdk.createTonMnemonic()

                // Verify first response was processed correctly
                assertEquals("Mnemonic should be generated correctly", 24, mnemonic.size)
                assertTrue("Mnemonic should contain valid words", mnemonic.isNotEmpty())

                // Wait for all 3 duplicate responses to arrive
                delay(500)

                // Verify NO events were triggered (createTonMnemonic doesn't emit events)
                // If duplicates were processed, we'd see multiple completions or errors
                assertEquals("No events should be triggered by createTonMnemonic", 0, events.size)
            }
        }
    }

    @Test(timeout = 5000)
    fun duplicateCallIdMultipleCallsHandledCorrectly() = runBlocking {
        withContext(Dispatchers.Main) {
            withTimeout(3.seconds) {
                events.clear()

                // Each call gets 3 duplicate responses
                // Test that EACH call only processes its first response
                val mnemonic1 = sdk.createTonMnemonic()
                delay(100)
                val mnemonic2 = sdk.createTonMnemonic()

                // Both calls should complete successfully with first response only
                assertEquals(24, mnemonic1.size)
                assertEquals(24, mnemonic2.size)

                // Wait for all duplicates (3 per call = 6 total)
                delay(500)

                // No events expected - if duplicates processed, would see errors or multiple completions
                assertEquals("No events for createTonMnemonic calls", 0, events.size)
            }
        }
    }
}
