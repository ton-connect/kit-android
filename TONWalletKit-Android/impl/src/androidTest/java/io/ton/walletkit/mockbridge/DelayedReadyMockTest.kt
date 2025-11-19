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
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.seconds

/**
 * Tests delayed ready event scenario using REAL SDK with MOCK JavaScript.
 *
 * Scenario: JavaScript sends ready event after 5 seconds (slow initialization).
 * Tests that the SDK properly waits for bridge initialization and handles
 * delayed ready events without timing out prematurely.
 */
@RunWith(AndroidJUnit4::class)
class DelayedReadyMockTest : MockBridgeTestBase() {

    override fun getMockScenarioHtml(): String = "delayed-ready"

    @Test(timeout = 10000)
    fun delayedReadySdkWaitsForSlowInitialization() = runBlocking {
        withContext(Dispatchers.Main) {
            withTimeout(8.seconds) {
                // The mock sends ready after 5 seconds
                // Try to create mnemonic - should wait for ready then succeed
                val mnemonic = sdk.createTonMnemonic()

                // Verify mnemonic was generated successfully after ready arrived
                assertEquals("Mnemonic should have 24 words after delayed ready", 24, mnemonic.size)
                assertTrue("Mnemonic should contain valid words", mnemonic.isNotEmpty())
            }
        }
    }

    @Test(timeout = 10000)
    fun delayedReadyMultipleCallsAfterReady() = runBlocking {
        withContext(Dispatchers.Main) {
            withTimeout(8.seconds) {
                // Wait for delayed ready, then make multiple calls
                val mnemonic1 = sdk.createTonMnemonic()
                val mnemonic2 = sdk.createTonMnemonic()

                // Both should succeed
                assertEquals(24, mnemonic1.size)
                assertEquals(24, mnemonic2.size)
            }
        }
    }
}
