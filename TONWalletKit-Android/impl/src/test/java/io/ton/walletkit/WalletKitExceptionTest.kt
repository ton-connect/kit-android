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
package io.ton.walletkit

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class WalletKitExceptionTest {

    @Test
    fun testWalletKitBridgeExceptionExtendsException() {
        val exception = WalletKitBridgeException("Test error")
        assertTrue(exception is Exception)
    }

    @Test
    fun testExceptionPreservesMessage() {
        val message = "Wallet initialization failed"
        val exception = WalletKitBridgeException(message)
        assertEquals(message, exception.message)
    }

    @Test
    fun testExceptionCanBeCaughtAsException() {
        try {
            throw WalletKitBridgeException("Test")
        } catch (e: Exception) {
            assertNotNull(e)
            assertTrue(e is WalletKitBridgeException)
        }
    }

    @Test
    fun testExceptionCanBeCaughtAsWalletKitBridgeException() {
        try {
            throw WalletKitBridgeException("Specific error")
        } catch (e: WalletKitBridgeException) {
            assertEquals("Specific error", e.message)
        }
    }

    @Test
    fun testExceptionSupportsStackTraces() {
        val exception = WalletKitBridgeException("Test error")
        assertNotNull(exception.stackTrace)
        assertTrue(exception.stackTrace.isNotEmpty())
    }
}
