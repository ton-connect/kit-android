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
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * SDK bridge communication tests - verifies how SDK serializes/deserializes bridge messages.
 */
@RunWith(AndroidJUnit4::class)
class SDKBridgeCommunicationTests : MockBridgeTestBase() {
    override fun getMockScenarioHtml(): String = "normal-flow"
    override fun autoInitWalletKit(): Boolean = false
    override fun autoAddEventsHandler(): Boolean = false

    @Test
    fun sdkForwardsMethodParamsCorrectlyToBridge_placeholder() = runBlocking {
        // TODO: Verify SDK serializes and sends params to JS correctly.
        assertTrue(true)
    }

    @Test
    fun sdkParsesStandardResponseCorrectly_placeholder() = runBlocking {
        // TODO: Verify SDK correctly deserializes normal bridge responses.
        assertTrue(true)
    }

    @Test
    fun sdkMaintainsWalletListAcrossOperations_placeholder() = runBlocking {
        // TODO: Verify internal wallet list stays consistent across operations.
        assertTrue(true)
    }
}
