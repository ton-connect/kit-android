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

import io.ton.walletkit.exceptions.JSValueConversionException
import io.ton.walletkit.exceptions.SecureStorageException
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class WalletKitExceptionTest {

    // ========== WalletKitBridgeException Tests ==========

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

    // ========== JSValueConversionException Tests ==========

    @Test
    fun testJSValueConversionUnableToConvert() {
        val exception = JSValueConversionException.UnableToConvertJSValue(
            targetType = "Int",
            jsValueDescription = "string 'hello'",
        )
        assertTrue(exception.message!!.contains("Unable to cast JS value"))
        assertTrue(exception.message!!.contains("Int"))
        assertTrue(exception.message!!.contains("hello"))
    }

    @Test
    fun testJSValueConversionUndefinedValue() {
        val exception = JSValueConversionException.UndefinedValue("String")
        assertTrue(exception.message!!.contains("undefined"))
        assertTrue(exception.message!!.contains("String"))
    }

    @Test
    fun testJSValueConversionNullValue() {
        val exception = JSValueConversionException.NullValue("Int")
        assertTrue(exception.message!!.contains("null"))
        assertTrue(exception.message!!.contains("Int"))
    }

    @Test
    fun testJSValueConversionDecodingError() {
        val cause = IllegalArgumentException("Invalid JSON")
        val exception = JSValueConversionException.DecodingError(
            message = "Expected START_OBJECT",
            cause = cause,
        )
        assertTrue(exception.message!!.contains("Decoding error"))
        assertEquals(cause, exception.cause)
    }

    @Test
    fun testJSValueConversionEncodingError() {
        val cause = IllegalStateException("Serializer not found")
        val exception = JSValueConversionException.EncodingError(
            message = "Cannot serialize Wallet",
            cause = cause,
        )
        assertTrue(exception.message!!.contains("Encoding error"))
        assertEquals(cause, exception.cause)
    }

    @Test
    fun testJSValueConversionUnknown() {
        val exception = JSValueConversionException.Unknown("BigInt conversion failed")
        assertTrue(exception.message!!.contains("BigInt"))
    }

    // ========== SecureStorageException Tests ==========

    @Test
    fun testSecureStorageSaveFailed() {
        val cause = Exception("KeyStore error")
        val exception = SecureStorageException.SaveFailed(cause)
        assertTrue(exception.message!!.contains("save"))
        assertEquals(cause, exception.cause)
    }

    @Test
    fun testSecureStorageGetFailed() {
        val exception = SecureStorageException.GetFailed()
        assertTrue(exception.message!!.contains("get"))
    }

    @Test
    fun testSecureStorageDeleteFailed() {
        val exception = SecureStorageException.DeleteFailed()
        assertTrue(exception.message!!.contains("delete"))
    }

    @Test
    fun testSecureStorageClearFailed() {
        val exception = SecureStorageException.ClearFailed()
        assertTrue(exception.message!!.contains("clear"))
    }

    @Test
    fun testSecureStorageInvalidData() {
        val exception = SecureStorageException.InvalidData()
        assertTrue(exception.message!!.contains("Invalid data"))
    }

    // ========== Exception Hierarchy Tests ==========

    @Test
    fun testAllExceptionsAreThrowable() {
        val exceptions = listOf(
            WalletKitBridgeException("test"),
            JSValueConversionException.Unknown("test"),
            SecureStorageException.SaveFailed(),
        )

        exceptions.forEach { exception ->
            assertTrue(exception is Throwable)
            assertTrue(exception is Exception)
        }
    }

    @Test
    fun testSealedClassPolymorphism() {
        val conversions: List<JSValueConversionException> = listOf(
            JSValueConversionException.UnableToConvertJSValue("Int", "test"),
            JSValueConversionException.UndefinedValue("String"),
            JSValueConversionException.NullValue("Boolean"),
            JSValueConversionException.DecodingError("test", Exception()),
            JSValueConversionException.EncodingError("test", Exception()),
            JSValueConversionException.Unknown("test"),
        )

        conversions.forEach { exception ->
            assertTrue(exception is JSValueConversionException)
        }

        val storageExceptions: List<SecureStorageException> = listOf(
            SecureStorageException.SaveFailed(),
            SecureStorageException.GetFailed(),
            SecureStorageException.DeleteFailed(),
            SecureStorageException.ClearFailed(),
            SecureStorageException.InvalidData(),
        )

        storageExceptions.forEach { exception ->
            assertTrue(exception is SecureStorageException)
        }
    }
}
