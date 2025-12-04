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

import io.ton.walletkit.mockbridge.infra.DefaultMockScenario
import io.ton.walletkit.mockbridge.infra.MockBridgeTestBase
import io.ton.walletkit.mockbridge.infra.MockScenario
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.seconds

/**
 * Tests handling of multiple/duplicate calls using mocked engine.
 *
 * Original Scenario: JavaScript sends multiple responses with the same call ID
 * (simulating buggy backend or network duplication).
 *
 * Adapted Scenario: With mocked engine, we test that:
 * - Multiple rapid calls to the same method work correctly
 * - Each call gets its own response
 * - Calls don't interfere with each other
 * - Call counting works correctly
 *
 * Note: The original test verified BridgeRpcClient.handleResponse behavior
 * with duplicate call IDs. With mocked engine, we test similar resilience
 * at the SDK API level.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class DuplicateCallIdMockTest : MockBridgeTestBase() {

    override fun getMockScenario(): MockScenario = DuplicateCallScenario()

    /**
     * Scenario that tracks call counts to verify each call is handled independently.
     */
    private class DuplicateCallScenario : DefaultMockScenario() {
        val mnemonicCallCount = AtomicInteger(0)
        val signerCallCount = AtomicInteger(0)

        override fun handleCreateTonMnemonic(wordCount: Int): List<String> {
            val callNumber = mnemonicCallCount.incrementAndGet()

            // Return same mnemonic regardless of call count
            return listOf(
                "abandon", "ability", "able", "about", "above", "absent",
                "absorb", "abstract", "absurd", "abuse", "access", "accident",
                "account", "accuse", "achieve", "acid", "acoustic", "acquire",
                "across", "act", "action", "actor", "actress", "actual",
            ).take(wordCount)
        }
    }

    @Test
    fun `duplicate calls - SDK handles multiple rapid calls correctly`() = runTest {
        val scenario = scenario as DuplicateCallScenario

        withTimeout(3.seconds) {
            events.clear()

            // Make multiple rapid calls - each should complete independently
            val mnemonic1 = sdk.createTonMnemonic()
            val mnemonic2 = sdk.createTonMnemonic()
            val mnemonic3 = sdk.createTonMnemonic()

            // Verify all calls completed successfully
            assertEquals("Mnemonic 1 should have 24 words", 24, mnemonic1.size)
            assertEquals("Mnemonic 2 should have 24 words", 24, mnemonic2.size)
            assertEquals("Mnemonic 3 should have 24 words", 24, mnemonic3.size)

            // Verify each call was tracked
            assertEquals("Should have made 3 mnemonic calls", 3, scenario.mnemonicCallCount.get())

            // No events should be triggered by createTonMnemonic
            assertEquals("No events should be triggered", 0, events.size)
        }
    }

    @Test
    fun `duplicate calls - sequential calls with delay handled correctly`() = runTest {
        val scenario = scenario as DuplicateCallScenario

        withTimeout(3.seconds) {
            events.clear()

            // Make calls with small delays between them
            val mnemonic1 = sdk.createTonMnemonic()
            delay(50)
            val mnemonic2 = sdk.createTonMnemonic()
            delay(50)
            val mnemonic3 = sdk.createTonMnemonic()

            // All should complete successfully
            assertEquals(24, mnemonic1.size)
            assertEquals(24, mnemonic2.size)
            assertEquals(24, mnemonic3.size)

            // All calls should be counted
            assertEquals("Should have made 3 mnemonic calls", 3, scenario.mnemonicCallCount.get())
        }
    }

    @Test
    fun `duplicate calls - multiple different method calls don't interfere`() = runTest {
        withTimeout(3.seconds) {
            events.clear()

            // Make multiple calls to different methods
            val mnemonic = sdk.createTonMnemonic()
            val signer = sdk.createSignerFromMnemonic(mnemonic)
            val adapter = sdk.createV5R1Adapter(signer)

            // All should complete independently
            assertEquals(24, mnemonic.size)
            assertNotNull(signer)
            assertTrue(signer.signerId.isNotEmpty())
            assertNotNull(adapter)
            assertTrue(adapter.adapterId.isNotEmpty())

            // No events expected
            assertEquals(0, events.size)
        }
    }
}
