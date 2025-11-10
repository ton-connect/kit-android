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

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for WalletKitUtils utility functions.
 * Verifies hex encoding/decoding behavior matches JS WalletKit expectations.
 */
class WalletKitUtilsTest {

    @Test
    fun `byteArrayToHex returns hex with 0x prefix`() {
        val bytes = byteArrayOf(0x12, 0x34, 0x56, 0x78)
        val hex = WalletKitUtils.byteArrayToHex(bytes)

        assertEquals("0x12345678", hex)
    }

    @Test
    fun `byteArrayToHex returns lowercase hex`() {
        val bytes = byteArrayOf(0xAB.toByte(), 0xCD.toByte(), 0xEF.toByte())
        val hex = WalletKitUtils.byteArrayToHex(bytes)

        assertEquals("0xabcdef", hex)
        assertFalse(hex.contains("A"))
        assertFalse(hex.contains("B"))
        assertFalse(hex.contains("C"))
    }

    @Test
    fun `byteArrayToHex handles empty array`() {
        val bytes = byteArrayOf()
        val hex = WalletKitUtils.byteArrayToHex(bytes)

        assertEquals("0x", hex)
    }

    @Test
    fun `byteArrayToHex handles single byte`() {
        val bytes = byteArrayOf(0x0F)
        val hex = WalletKitUtils.byteArrayToHex(bytes)

        assertEquals("0x0f", hex)
    }

    @Test
    fun `byteArrayToHex pads single digit hex`() {
        val bytes = byteArrayOf(0x00, 0x01, 0x0A)
        val hex = WalletKitUtils.byteArrayToHex(bytes)

        assertEquals("0x00010a", hex)
    }

    @Test
    fun `byteArrayToHexNoPrefix returns hex without prefix`() {
        val bytes = byteArrayOf(0x12, 0x34, 0x56, 0x78)
        val hex = WalletKitUtils.byteArrayToHexNoPrefix(bytes)

        assertEquals("12345678", hex)
        assertFalse(hex.startsWith("0x"))
    }

    @Test
    fun `byteArrayToHexNoPrefix handles empty array`() {
        val bytes = byteArrayOf()
        val hex = WalletKitUtils.byteArrayToHexNoPrefix(bytes)

        assertEquals("", hex)
    }

    @Test
    fun `hexToByteArray parses hex with 0x prefix`() {
        val hex = "0x12345678"
        val bytes = WalletKitUtils.hexToByteArray(hex)

        assertArrayEquals(byteArrayOf(0x12, 0x34, 0x56, 0x78), bytes)
    }

    @Test
    fun `hexToByteArray parses hex without prefix`() {
        val hex = "12345678"
        val bytes = WalletKitUtils.hexToByteArray(hex)

        assertArrayEquals(byteArrayOf(0x12, 0x34, 0x56, 0x78), bytes)
    }

    @Test
    fun `hexToByteArray handles uppercase hex`() {
        val hex = "0xABCDEF"
        val bytes = WalletKitUtils.hexToByteArray(hex)

        assertArrayEquals(byteArrayOf(0xAB.toByte(), 0xCD.toByte(), 0xEF.toByte()), bytes)
    }

    @Test
    fun `hexToByteArray handles lowercase hex`() {
        val hex = "0xabcdef"
        val bytes = WalletKitUtils.hexToByteArray(hex)

        assertArrayEquals(byteArrayOf(0xAB.toByte(), 0xCD.toByte(), 0xEF.toByte()), bytes)
    }

    @Test
    fun `hexToByteArray handles mixed case hex`() {
        val hex = "0xAbCdEf"
        val bytes = WalletKitUtils.hexToByteArray(hex)

        assertArrayEquals(byteArrayOf(0xAB.toByte(), 0xCD.toByte(), 0xEF.toByte()), bytes)
    }

    @Test
    fun `hexToByteArray handles empty string`() {
        val hex = ""
        val bytes = WalletKitUtils.hexToByteArray(hex)

        assertEquals(0, bytes.size)
    }

    @Test
    fun `hexToByteArray handles 0x only`() {
        val hex = "0x"
        val bytes = WalletKitUtils.hexToByteArray(hex)

        assertEquals(0, bytes.size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `hexToByteArray throws on odd length hex`() {
        val hex = "0x123" // Odd number of hex digits
        WalletKitUtils.hexToByteArray(hex)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `hexToByteArray throws on invalid hex characters`() {
        val hex = "0x12XY56"
        WalletKitUtils.hexToByteArray(hex)
    }

    @Test
    fun `round trip conversion preserves data`() {
        val originalBytes = byteArrayOf(0x01, 0x23, 0x45, 0x67, 0x89.toByte(), 0xAB.toByte(), 0xCD.toByte(), 0xEF.toByte())

        // With prefix
        val hexWithPrefix = WalletKitUtils.byteArrayToHex(originalBytes)
        val bytesFromHex = WalletKitUtils.hexToByteArray(hexWithPrefix)
        assertArrayEquals(originalBytes, bytesFromHex)

        // Without prefix
        val hexNoPrefix = WalletKitUtils.byteArrayToHexNoPrefix(originalBytes)
        val bytesFromHexNoPrefix = WalletKitUtils.hexToByteArray(hexNoPrefix)
        assertArrayEquals(originalBytes, bytesFromHexNoPrefix)
    }

    @Test
    fun `round trip conversion handles all byte values`() {
        // Test all possible byte values (0x00 to 0xFF)
        val allBytes = ByteArray(256) { it.toByte() }

        val hex = WalletKitUtils.byteArrayToHex(allBytes)
        val decoded = WalletKitUtils.hexToByteArray(hex)

        assertArrayEquals(allBytes, decoded)
    }

    @Test
    fun `byteArrayToHex matches JS WalletKit behavior`() {
        // Test cases matching JS Uint8ArrayToHex expected output

        // Empty array
        assertEquals("0x", WalletKitUtils.byteArrayToHex(byteArrayOf()))

        // Single byte
        assertEquals("0x00", WalletKitUtils.byteArrayToHex(byteArrayOf(0x00)))
        assertEquals("0xff", WalletKitUtils.byteArrayToHex(byteArrayOf(0xFF.toByte())))

        // Multiple bytes
        assertEquals("0x0102030405", WalletKitUtils.byteArrayToHex(byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05)))

        // All lowercase
        val hexResult = WalletKitUtils.byteArrayToHex(byteArrayOf(0xAA.toByte(), 0xBB.toByte()))
        assertEquals("0xaabb", hexResult)
        assertTrue(hexResult.substring(2).all { it.isDigit() || it in 'a'..'f' })
    }

    @Test
    fun `hexToByteArray handles public key format`() {
        // 32-byte public key (64 hex chars + 0x prefix)
        val publicKeyHex = "0x" + "a".repeat(64)
        val bytes = WalletKitUtils.hexToByteArray(publicKeyHex)

        assertEquals(32, bytes.size)
        assertTrue(bytes.all { it == 0xAA.toByte() })
    }

    @Test
    fun `hexToByteArray handles signature format`() {
        // 64-byte signature (128 hex chars + 0x prefix)
        val signatureHex = "0x" + "f".repeat(128)
        val bytes = WalletKitUtils.hexToByteArray(signatureHex)

        assertEquals(64, bytes.size)
        assertTrue(bytes.all { it == 0xFF.toByte() })
    }

    @Test
    fun `byteArrayToHexNoPrefix useful for key comparison`() {
        val key1 = byteArrayOf(0x01, 0x02, 0x03)
        val key2 = byteArrayOf(0x01, 0x02, 0x03)
        val key3 = byteArrayOf(0x01, 0x02, 0x04)

        val hex1 = WalletKitUtils.byteArrayToHexNoPrefix(key1)
        val hex2 = WalletKitUtils.byteArrayToHexNoPrefix(key2)
        val hex3 = WalletKitUtils.byteArrayToHexNoPrefix(key3)

        assertEquals(hex1, hex2)
        assertNotEquals(hex1, hex3)
    }
}
