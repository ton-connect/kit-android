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
import io.ton.walletkit.WalletKitBridgeException
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
 * Tests malformed JSON scenario using REAL SDK with MOCK JavaScript.
 *
 * Scenario: JavaScript sends malformed/invalid JSON messages that can't be parsed.
 *
 * Mock Behavior:
 * - Sends random malformed JSON in background (truncated, invalid syntax, non-JSON strings)
 * - Every 3rd RPC call gets malformed response: `{"kind":"response","id":"...","result":INVALID_JSON}`
 * - Other calls get valid responses
 *
 * SDK Behavior (WebViewManager.postMessage):
 * - Line 266: `JSONObject(json)` throws JSONException for malformed JSON
 * - Extracts call ID using regex from malformed string
 * - If ID found: Creates error response, fails only that specific call
 * - If no ID: Background malformed message, logs and ignores
 * - **Other pending calls continue normally**
 *
 * Expected Test Behavior:
 * - Background malformed messages are ignored (no ID, just logged)
 * - Calls with malformed responses fail with error message
 * - Other calls succeed normally - SDK continues working
 */
@RunWith(AndroidJUnit4::class)
class MalformedJsonMockTest : MockBridgeTestBase() {

    override fun getMockScenarioHtml(): String = "malformed-json"

    @Test(timeout = 10000)
    fun malformedJsonBackgroundMessagesAreIgnored() = runBlocking {
        withContext(Dispatchers.Main) {
            withTimeout(8.seconds) {
                events.clear()

                // Wait for malformed background messages (sent at 200ms)
                delay(500)

                // Make calls - should succeed despite background malformed messages
                val mnemonic1 = sdk.createTonMnemonic()
                assertEquals("Call should succeed (background malformed ignored)", 24, mnemonic1.size)

                val mnemonic2 = sdk.createTonMnemonic()
                assertEquals("Second call should also succeed", 24, mnemonic2.size)

                // No events expected
                assertEquals("No events for background malformed messages", 0, events.size)
            }
        }
    }

    @Test(timeout = 15000)
    fun malformedJsonEveryThirdCallFailsOthersSucceed() = runBlocking {
        withContext(Dispatchers.Main) {
            withTimeout(12.seconds) {
                events.clear()
                delay(500) // Wait past background malformed messages

                // NOTE: Counter persists from previous test!
                // First test did 2 createTonMnemonic calls, so this test starts at call #3

                val results = mutableListOf<Result<List<String>>>()

                // Make 9 calls to see the pattern:
                // If counter is at 2 from previous test:
                // Call #3 - MALFORMED ❌
                // Call #4 - valid ✅
                // Call #5 - valid ✅
                // Call #6 - MALFORMED ❌
                // Call #7 - valid ✅
                // Call #8 - valid ✅
                // Call #9 - MALFORMED ❌
                // Call #10 - valid ✅
                // Call #11 - valid ✅

                repeat(9) { i ->
                    try {
                        val mnemonic = sdk.createTonMnemonic()
                        results.add(Result.success(mnemonic))
                        println("Call #${i + 1}: SUCCESS - ${mnemonic.size} words")
                    } catch (e: WalletKitBridgeException) {
                        results.add(Result.failure(e))
                        println("Call #${i + 1}: FAILED - ${e.message}")
                    }
                    delay(100)
                }

                // At least 6 calls should succeed (valid responses)
                val successCount = results.count { it.isSuccess }
                val failCount = results.count { it.isFailure }

                println("Success: $successCount, Failed: $failCount")
                assertTrue("At least 6 calls should succeed", successCount >= 6)
                assertTrue("At least 2 calls should fail (malformed)", failCount >= 2)

                // Verify failed calls have malformed JSON error
                results.filter { it.isFailure }.forEach { result ->
                    val error = result.exceptionOrNull()
                    assertTrue(
                        "Failed call should mention malformed JSON",
                        error?.message?.contains("malformed", ignoreCase = true) == true ||
                            error?.message?.contains("INVALID_JSON", ignoreCase = true) == true,
                    )
                }
            }
        }
    }
}
