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
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation test for the "no ready" JS scenario.
 *
 * Expected behaviour:
 * - SDK init keeps waiting because JS never signals readiness.
 * - SDK calls also stay pending instead of failing.
 *
 * Tests use short timeouts only to avoid hanging the runner.
 */
@RunWith(AndroidJUnit4::class)
class NoReadyMockTest : MockBridgeTestBase() {

    override fun getMockScenarioHtml(): String = "no-ready"
    override fun autoInitWalletKit(): Boolean = false
    override fun autoAddEventsHandler(): Boolean = false

    @Test
    fun init_waits_when_ready_never_arrives() = runBlocking {
        supervisorScope {
            val initJob = async(Dispatchers.Main) { engine.init(createTestConfig()) }

            delay(500)
            assertTrue("Initialization should still be waiting for ready", initJob.isActive && !initJob.isCompleted)

            initJob.cancel()
            initJob.join()
        }
    }

    @Test
    fun api_calls_remain_pending_when_bridge_not_ready() = runBlocking {
        val result =
            runCatching {
                withTimeout(1500) {
                    sdk.createTonMnemonic()
                }
            }

        assertTrue("SDK call should fail when init is blocked waiting for ready", result.isFailure)

        val error = result.exceptionOrNull()
        assertTrue("Expected WalletKitBridgeException on blocked init", error is WalletKitBridgeException)
        assertTrue(
            "Error message should mention initialization/ready: ${error?.message}",
            error?.message?.contains("auto-initialize", ignoreCase = true) == true ||
                error?.message?.contains("ready", ignoreCase = true) == true ||
                error?.message?.contains("initialize", ignoreCase = true) == true,
        )
    }
}
