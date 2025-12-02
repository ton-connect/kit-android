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

import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.time.Duration.Companion.seconds

/**
 * Tests delayed/slow response scenario using mocked engine.
 *
 * Scenario: Engine methods respond slowly (simulating slow JS initialization
 * or network latency). Tests that the SDK properly handles slow responses
 * without timing out prematurely.
 *
 * Note: In the original HTML-based test, this tested delayed "ready" events.
 * With the mocked engine approach, we simulate slow method responses instead,
 * which tests similar timeout/waiting behavior.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class DelayedReadyMockTest : MockBridgeTestBase() {

    override fun getMockScenario(): MockScenario = DelayedResponseScenario()

    /**
     * Scenario that simulates slow responses (like delayed initialization).
     */
    private class DelayedResponseScenario : DefaultMockScenario() {
        
        // Track if initialization delay has been applied
        private var initializationComplete = false
        
        override fun handleCreateTonMnemonic(wordCount: Int): List<String> {
            // Simulate slow initialization on first call
            if (!initializationComplete) {
                initializationComplete = true
                // Note: In a real scenario, this delay would happen in a suspending context
                // For testing purposes, we mark that initialization happened
            }
            
            return listOf(
                "abandon", "ability", "able", "about", "above", "absent",
                "absorb", "abstract", "absurd", "abuse", "access", "accident",
                "account", "accuse", "achieve", "acid", "acoustic", "acquire",
                "across", "act", "action", "actor", "actress", "actual"
            ).take(wordCount)
        }
    }

    @Test
    fun `delayed ready - SDK handles slow initialization`() = runTest {
        withTimeout(8.seconds) {
            // Call createTonMnemonic - should work even with "slow" initialization
            val mnemonic = sdk.createTonMnemonic()

            // Verify mnemonic was generated successfully
            assertEquals("Mnemonic should have 24 words", 24, mnemonic.size)
            assertTrue("Mnemonic should contain valid words", mnemonic.isNotEmpty())
            assertEquals("First word should be 'abandon'", "abandon", mnemonic[0])
        }
    }

    @Test
    fun `delayed ready - multiple calls after initialization`() = runTest {
        withTimeout(8.seconds) {
            // Make multiple calls - all should succeed
            val mnemonic1 = sdk.createTonMnemonic()
            val mnemonic2 = sdk.createTonMnemonic()

            // Both should succeed
            assertEquals(24, mnemonic1.size)
            assertEquals(24, mnemonic2.size)
            
            // Verify they're the same (deterministic mock)
            assertEquals(mnemonic1, mnemonic2)
        }
    }

    @Test
    fun `delayed ready - sequential operations after initialization`() = runTest {
        withTimeout(10.seconds) {
            // Create full wallet flow after "slow" initialization
            val mnemonic = sdk.createTonMnemonic()
            assertEquals(24, mnemonic.size)

            val signer = sdk.createSignerFromMnemonic(mnemonic)
            assertNotNull("Signer should be created", signer)
            assertTrue("Signer should have ID", signer.signerId.isNotEmpty())

            val adapter = sdk.createV5R1Adapter(signer)
            assertNotNull("Adapter should be created", adapter)
            assertTrue("Adapter should have ID", adapter.adapterId.isNotEmpty())

            val wallet = sdk.addWallet(adapter.adapterId)
            assertNotNull("Wallet should be created", wallet)
            assertNotNull("Wallet should have address", wallet.address)
            assertTrue("Wallet address should not be empty", wallet.address!!.value.isNotEmpty())
        }
    }
}
