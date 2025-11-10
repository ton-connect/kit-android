/**
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
package io.ton.walletkit.utils

/**
 * ByteArrayUtils - Byte array conversion utilities
 *
 * Provides conversion between byte arrays and hex strings.
 * Replaces publicKey formatting previously done in TypeScript bridge.
 */
object ByteArrayUtils {
    /**
     * Converts a byte array to a lowercase hex string.
     *
     * @param bytes The byte array to convert
     * @return Hex string representation (lowercase, no prefix)
     */
    fun toHexString(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Converts a hex string to a byte array.
     *
     * @param hex The hex string to convert (with or without 0x prefix)
     * @return Byte array representation
     * @throws IllegalArgumentException if hex string is invalid
     */
    fun fromHexString(hex: String): ByteArray {
        val cleanHex = hex.removePrefix("0x").removePrefix("0X")
        
        if (cleanHex.length % 2 != 0) {
            throw IllegalArgumentException("Hex string must have even length")
        }
        
        return try {
            cleanHex.chunked(2)
                .map { it.toInt(16).toByte() }
                .toByteArray()
        } catch (e: NumberFormatException) {
            throw IllegalArgumentException("Invalid hex string: $hex", e)
        }
    }

    /**
     * Converts a byte array to an uppercase hex string.
     *
     * @param bytes The byte array to convert
     * @return Hex string representation (uppercase, no prefix)
     */
    fun toHexStringUppercase(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02X".format(it) }
    }
}
