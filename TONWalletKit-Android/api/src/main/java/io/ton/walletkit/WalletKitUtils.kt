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

/**
 * Utility functions for TON Wallet Kit.
 *
 * Provides helpers for common operations like hex encoding/decoding,
 * matching the JS WalletKit utility functions.
 *
 * ## Common Usage Patterns
 *
 * ### Converting signatures to hex for display:
 * ```kotlin
 * val signature: ByteArray = wallet.signTransaction(...)
 * val hexSignature = WalletKitUtils.byteArrayToHex(signature)
 * // Display: "0x1234abcd..."
 * ```
 *
 * ### Converting public keys for comparison:
 * ```kotlin
 * val publicKey1 = signer1.getPublicKey()
 * val publicKey2 = signer2.getPublicKey()
 *
 * // Compare as hex strings
 * if (WalletKitUtils.byteArrayToHexNoPrefix(publicKey1) ==
 *     WalletKitUtils.byteArrayToHexNoPrefix(publicKey2)) {
 *     println("Same key!")
 * }
 * ```
 *
 * ### Parsing hex input from users:
 * ```kotlin
 * // Accepts both "0x..." and raw hex
 * val userInput = "0x1234abcd"
 * val bytes = WalletKitUtils.hexToByteArray(userInput)
 * ```
 *
 * ### Round-trip conversion:
 * ```kotlin
 * val originalData = byteArrayOf(0x01, 0x02, 0x03)
 * val hex = WalletKitUtils.byteArrayToHex(originalData)
 * val restored = WalletKitUtils.hexToByteArray(hex)
 * // originalData == restored
 * ```
 */
object WalletKitUtils {
    private val HEX_CHARS = "0123456789abcdef".toCharArray()

    /**
     * Convert a byte array to a hex string with 0x prefix.
     *
     * This matches the JS WalletKit's `Uint8ArrayToHex` utility function.
     * The output is always lowercase hex with a `0x` prefix.
     *
     * Example:
     * ```kotlin
     * val bytes = byteArrayOf(0x12, 0x34, 0x56)
     * val hex = WalletKitUtils.byteArrayToHex(bytes)
     * // hex = "0x123456"
     * ```
     *
     * @param bytes The byte array to convert
     * @return Hex string with 0x prefix (e.g., "0x123abc")
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
     *
     * This is the inverse of [byteArrayToHex].
     * The input can optionally start with "0x" prefix, which will be stripped.
     * Both uppercase and lowercase hex characters are supported.
     *
     * Example:
     * ```kotlin
     * val hex = "0x123456"
     * val bytes = WalletKitUtils.hexToByteArray(hex)
     * // bytes = [0x12, 0x34, 0x56]
     * ```
     *
     * @param hex Hex string with optional 0x prefix
     * @return Byte array
     * @throws IllegalArgumentException if hex string is invalid
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
     *
     * This is useful when you need raw hex without the prefix.
     * The output is always lowercase hex.
     *
     * Example:
     * ```kotlin
     * val bytes = byteArrayOf(0x12, 0x34, 0x56)
     * val hex = WalletKitUtils.byteArrayToHexNoPrefix(bytes)
     * // hex = "123456"
     * ```
     *
     * @param bytes The byte array to convert
     * @return Hex string without prefix (e.g., "123abc")
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
}
