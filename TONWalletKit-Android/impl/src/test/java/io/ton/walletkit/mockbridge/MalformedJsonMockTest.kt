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

import io.mockk.coEvery
import io.ton.walletkit.WalletKitBridgeException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.atomic.AtomicInteger

/**
 * Tests error response handling using mocked engine.
 *
 * Original Scenario: JavaScript sends malformed/invalid JSON messages.
 *
 * Adapted Scenario: With mocked engine, we simulate:
 * - Every Nth call throws an exception (simulating bridge errors)
 * - Other calls succeed normally
 * - SDK continues working after errors
 *
 * This tests the SDK's resilience to engine-level errors.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class MalformedJsonMockTest : MockBridgeTestBase() {

    override fun getMockScenario(): MockScenario = MalformedResponseScenario()

    /**
     * Scenario that throws exceptions on every 3rd call to simulate
     * malformed responses from the bridge.
     */
    private class MalformedResponseScenario : DefaultMockScenario() {
        val callCount = AtomicInteger(0)

        override fun handleCreateTonMnemonic(wordCount: Int): List<String> {
            val count = callCount.incrementAndGet()

            // Every 3rd call "fails" with malformed response
            if (count % 3 == 0) {
                throw WalletKitBridgeException("Malformed JSON response: INVALID_JSON")
            }

            return super.handleCreateTonMnemonic(wordCount)
        }
    }

    @Test
    fun `malformed response - some calls fail, others succeed`() = runTest {
        val scenario = scenario as MalformedResponseScenario

        val results = mutableListOf<Result<List<String>>>()

        // Make 9 calls:
        // Call #1 - valid ✅
        // Call #2 - valid ✅
        // Call #3 - MALFORMED ❌
        // Call #4 - valid ✅
        // Call #5 - valid ✅
        // Call #6 - MALFORMED ❌
        // Call #7 - valid ✅
        // Call #8 - valid ✅
        // Call #9 - MALFORMED ❌

        repeat(9) {
            try {
                val mnemonic = sdk.createTonMnemonic()
                results.add(Result.success(mnemonic))
            } catch (e: WalletKitBridgeException) {
                results.add(Result.failure(e))
            }
        }

        // 6 calls should succeed (calls 1, 2, 4, 5, 7, 8)
        val successCount = results.count { it.isSuccess }
        val failCount = results.count { it.isFailure }

        assertEquals("6 calls should succeed", 6, successCount)
        assertEquals("3 calls should fail", 3, failCount)

        // Verify successful calls returned valid mnemonics
        results.filter { it.isSuccess }.forEach { result ->
            assertEquals(24, result.getOrNull()?.size)
        }

        // Verify failed calls have correct error message
        results.filter { it.isFailure }.forEach { result ->
            val error = result.exceptionOrNull()
            assertTrue(
                "Failed call should mention malformed JSON",
                error?.message?.contains("Malformed", ignoreCase = true) == true ||
                    error?.message?.contains("INVALID_JSON", ignoreCase = true) == true,
            )
        }
    }

    @Test
    fun `malformed response - sdk continues working after errors`() = runTest {
        // First call succeeds
        val mnemonic1 = sdk.createTonMnemonic()
        assertEquals(24, mnemonic1.size)

        // Second call succeeds
        val mnemonic2 = sdk.createTonMnemonic()
        assertEquals(24, mnemonic2.size)

        // Third call fails
        var exceptionThrown = false
        try {
            sdk.createTonMnemonic()
        } catch (e: WalletKitBridgeException) {
            exceptionThrown = true
        }
        assertTrue("Third call should fail", exceptionThrown)

        // Fourth call should succeed - SDK recovered
        val mnemonic4 = sdk.createTonMnemonic()
        assertEquals("SDK should recover after error", 24, mnemonic4.size)
    }

    @Test
    fun `malformed response - other methods unaffected`() = runTest {
        val scenario = scenario as MalformedResponseScenario

        // Make 3 createTonMnemonic calls to trigger the error on 3rd
        sdk.createTonMnemonic() // 1 - success
        sdk.createTonMnemonic() // 2 - success

        // 3rd call will fail
        var failed = false
        try {
            sdk.createTonMnemonic() // 3 - fails
        } catch (e: WalletKitBridgeException) {
            failed = true
        }
        assertTrue(failed)

        // Other SDK methods should still work (they have their own mock handlers)
        val signer = sdk.createSignerFromMnemonic(listOf(
            "abandon", "ability", "able", "about", "above", "absent",
            "absorb", "abstract", "absurd", "abuse", "access", "accident",
            "account", "accuse", "achieve", "acid", "acoustic", "acquire",
            "across", "act", "action", "actor", "actress", "actual"
        ))
        assertNotNull("Other methods should still work", signer)
        assertTrue(signer.signerId.isNotEmpty())
    }

    @Test
    fun `error isolation - failed call doesnt affect pending calls`() = runTest {
        // This test verifies that when one call fails, it doesn't affect other operations
        
        // Create signer first (this uses different mock handler, won't fail)
        val mnemonic = listOf(
            "abandon", "ability", "able", "about", "above", "absent",
            "absorb", "abstract", "absurd", "abuse", "access", "accident",
            "account", "accuse", "achieve", "acid", "acoustic", "acquire",
            "across", "act", "action", "actor", "actress", "actual"
        )
        val signer = sdk.createSignerFromMnemonic(mnemonic)

        // Create adapter
        val adapter = sdk.createV5R1Adapter(signer)

        // Add wallet
        val wallet = sdk.addWallet(adapter.adapterId)
        assertNotNull(wallet)

        // Now trigger error with createTonMnemonic (reset count and make 3 calls)
        val scenario = scenario as MalformedResponseScenario
        scenario.callCount.set(0)

        sdk.createTonMnemonic() // 1
        sdk.createTonMnemonic() // 2
        
        try {
            sdk.createTonMnemonic() // 3 - fails
        } catch (e: WalletKitBridgeException) {
            // Expected
        }

        // getWallets should still work
        val wallets = sdk.getWallets()
        assertNotNull("getWallets should work after createTonMnemonic failure", wallets)
    }
}
