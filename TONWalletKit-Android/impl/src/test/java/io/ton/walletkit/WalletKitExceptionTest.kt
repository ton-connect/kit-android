package io.ton.walletkit

import io.ton.walletkit.WalletKitBridgeException
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
