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

import io.ton.walletkit.WalletKitBridgeException
import io.ton.walletkit.config.TONWalletKitConfiguration
import io.ton.walletkit.mockbridge.infra.DefaultMockScenario
import io.ton.walletkit.mockbridge.infra.MockBridgeTestBase
import io.ton.walletkit.mockbridge.infra.MockScenario
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests "no ready" scenario using mocked WalletKitEngine.
 *
 * Scenario: Engine init never completes (simulating JS never signaling readiness).
 *
 * Mock Behavior:
 * - handleInit() hangs indefinitely (delay of Long.MAX_VALUE / 2)
 * - All other methods throw WalletKitBridgeException since SDK was never initialized
 *
 * Expected Test Behavior:
 * - SDK init keeps waiting because engine init never completes
 * - SDK calls fail with WalletKitBridgeException because init never completed
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class NoReadyMockTest : MockBridgeTestBase() {

    /**
     * Scenario that simulates "no ready" - init hangs forever,
     * and all other methods fail because SDK is not initialized.
     */
    class NoReadyScenario : DefaultMockScenario() {
        override suspend fun handleInit(configuration: TONWalletKitConfiguration) {
            // Simulate JS never sending ready - hang indefinitely
            delay(Long.MAX_VALUE / 2)
        }

        // All other methods fail because init never completed
        override fun handleCreateTonMnemonic(wordCount: Int): List<String> {
            throw WalletKitBridgeException("Failed to auto-initialize WalletKit: SDK not ready")
        }
    }

    override fun getMockScenario(): MockScenario = NoReadyScenario()

    // Don't auto-init because we want to test init behavior
    override fun autoInitWalletKit(): Boolean = false
    override fun autoAddEventsHandler(): Boolean = false

    @Test
    fun init_waits_when_ready_never_arrives() = runBlocking {
        supervisorScope {
            // Call engine.init directly - this simulates what happens during SDK initialization
            val initJob = async { mockEngine.init(createTestConfig()) }

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
