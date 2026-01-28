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

import android.util.Base64

/**
 * Utility functions for TON Wallet Kit.
 *
 * Provides helpers for hex encoding/decoding and base64 conversions.
 */
object WalletKitUtils {
    private val HEX_CHARS = "0123456789abcdef".toCharArray()

    /**
     * Convert a byte array to a hex string with 0x prefix.
     * Matches the JS WalletKit's `Uint8ArrayToHex` utility function.
     *
     * @param bytes The byte array to convert
     * @return Hex string with 0x prefix (lowercase), e.g. "0x123abc"
     *
     * Example:
     * ```kotlin
     * val signature: ByteArray = wallet.signTransaction(...)
     * val hex = WalletKitUtils.byteArrayToHex(signature)
     * // hex = "0x1234abcd..."
     * ```
     */
    fun byteArrayToHex(bytes: ByteArray): String {
        if (bytes.isEmpty()) return "0x"
        val result = CharArray(bytes.size * 2 + 2)
        result[0] = '0'
        result[1] = 'x'
        for (i in bytes.indices) {
            val v = bytes[i].toInt() and 0xFF
            result[2 + i * 2] = HEX_CHARS[v ushr 4]
            result[3 + i * 2] = HEX_CHARS[v and 0x0F]
        }
        return String(result)
    }

    /**
     * Convert a hex string to a byte array.
     * Accepts hex with or without 0x prefix. Both uppercase and lowercase supported.
     *
     * @param hex Hex string with optional 0x prefix
     * @return Byte array
     * @throws IllegalArgumentException if hex string is invalid
     *
     * Example:
     * ```kotlin
     * val userInput = "0x1234abcd"
     * val bytes = WalletKitUtils.hexToByteArray(userInput)
     * ```
     */
    fun hexToByteArray(hex: String): ByteArray {
        val cleanHex = if (hex.startsWith("0x") || hex.startsWith("0X")) {
            hex.substring(2)
        } else {
            hex
        }

        if (cleanHex.length % 2 != 0) {
            throw IllegalArgumentException("Hex string must have even length")
        }

        val result = ByteArray(cleanHex.length / 2)
        for (i in result.indices) {
            val index = i * 2
            val byte = cleanHex.substring(index, index + 2).toInt(16)
            result[i] = byte.toByte()
        }
        return result
    }

    /**
     * Convert a byte array to a hex string WITHOUT the 0x prefix.
     * Useful for comparing public keys or when raw hex is needed.
     *
     * @param bytes The byte array to convert
     * @return Hex string without prefix (lowercase), e.g. "123abc"
     *
     * Example:
     * ```kotlin
     * val publicKey1 = signer1.getPublicKey()
     * val publicKey2 = signer2.getPublicKey()
     * if (WalletKitUtils.byteArrayToHexNoPrefix(publicKey1) ==
     *     WalletKitUtils.byteArrayToHexNoPrefix(publicKey2)) {
     *     println("Same key!")
     * }
     * ```
     */
    fun byteArrayToHexNoPrefix(bytes: ByteArray): String {
        if (bytes.isEmpty()) return ""
        val result = CharArray(bytes.size * 2)
        for (i in bytes.indices) {
            val v = bytes[i].toInt() and 0xFF
            result[i * 2] = HEX_CHARS[v ushr 4]
            result[i * 2 + 1] = HEX_CHARS[v and 0x0F]
        }
        return String(result)
    }

    /**
     * Convert a Base64 string to hex (without 0x prefix).
     * Returns null if conversion fails.
     *
     * @param base64 The Base64 encoded string to convert
     * @return Hex string (lowercase, no prefix) or null on error
     *
     * Example:
     * ```kotlin
     * val base64 = "EjRWeA=="
     * val hex = WalletKitUtils.base64ToHex(base64)
     * // hex = "12345678"
     * ```
     */
    fun base64ToHex(base64: String?): String? {
        if (base64.isNullOrBlank()) return null

        return try {
            val bytes = Base64.decode(base64, Base64.NO_WRAP)
            byteArrayToHexNoPrefix(bytes)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    /**
     * Convert a hex string to Base64.
     * Accepts hex with or without 0x prefix.
     *
     * @param hex The hex string to convert
     * @return Base64 encoded string
     * @throws IllegalArgumentException if hex string is invalid
     *
     * Example:
     * ```kotlin
     * val hex = "12345678"
     * val base64 = WalletKitUtils.hexToBase64(hex)
     * // base64 = "EjRWeA=="
     * ```
     */
    fun hexToBase64(hex: String): String {
        val bytes = hexToByteArray(hex)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    /**
     * Strip the 0x or 0X prefix from a hex string.
     *
     * @param hex The hex string to process
     * @return Hex string without prefix
     *
     * Example:
     * ```kotlin
     * val hex = "0x123abc"
     * val stripped = WalletKitUtils.stripHexPrefix(hex)
     * // stripped = "123abc"
     * ```
     */
    fun stripHexPrefix(hex: String): String {
        return hex.removePrefix("0x").removePrefix("0X")
    }
}
