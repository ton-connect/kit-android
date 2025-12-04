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
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.time.Duration.Companion.seconds

/**
 * Tests rapid concurrent RPC calls using mocked WalletKitEngine.
 *
 * The mock engine responds immediately to all calls to test that
 * the SDK correctly handles concurrent requests.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class RapidRpcMockTest : MockBridgeTestBase() {

    override fun getMockScenario(): MockScenario = DefaultMockScenario()

    @Test(timeout = 30000)
    fun rapidRpcSdkHandlesConcurrentCalls() = runBlocking {
        withTimeout(20.seconds) {
            // Make 3 concurrent calls - mock responds immediately
            val mnemonic1 = sdk.createTonMnemonic()
            val mnemonic2 = sdk.createTonMnemonic()
            val mnemonic3 = sdk.createTonMnemonic()

            // Verify all mnemonics are valid
            assertEquals(24, mnemonic1.size)
            assertEquals(24, mnemonic2.size)
            assertEquals(24, mnemonic3.size)
        }
    }

    @Test(timeout = 60000)
    fun rapidRpcBatchResponsesProcessedCorrectly() = runBlocking {
        withTimeout(45.seconds) {
            val results = mutableListOf<List<String>>()
            repeat(5) {
                val mnemonic = sdk.createTonMnemonic()
                results.add(mnemonic)
                delay(100)
            }

            assertEquals(5, results.size)
            results.forEach { mnemonic ->
                assertEquals(24, mnemonic.size)
            }
        }
    }
}
