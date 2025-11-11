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
package io.ton.walletkit.utils

import android.util.Base64
import android.util.Log

/**
 * EncodingUtils - Encoding conversion utilities
 *
 * Provides conversion between different encoding formats (Base64, hex, etc.).
 * Moved from TypeScript bridge for better performance and reliability.
 */
object EncodingUtils {
    private const val TAG = "EncodingUtils"

    /**
     * Converts a Base64 string to a hex string.
     *
     * @param base64 The Base64 encoded string to convert
     * @return Hex string representation (lowercase, no prefix)
     */
    fun base64ToHex(base64: String): String {
        return try {
            val bytes = Base64.decode(base64, Base64.NO_WRAP)
            ByteArrayUtils.toHexString(bytes)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Failed to convert base64 to hex: $base64", e)
            base64 // Return original on error (backward compatibility)
        }
    }

    /**
     * Converts a hex string to a Base64 string.
     *
     * @param hex The hex string to convert (with or without 0x prefix)
     * @return Base64 encoded string
     */
    fun hexToBase64(hex: String): String {
        val bytes = ByteArrayUtils.fromHexString(hex)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    /**
     * Strips the 0x or 0X prefix from a hex string.
     *
     * @param hex The hex string to process
     * @return Hex string without prefix
     */
    fun stripHexPrefix(hex: String): String {
        return hex.removePrefix("0x").removePrefix("0X")
    }

    /**
     * Ensures a hex string has the 0x prefix.
     *
     * @param hex The hex string to process
     * @return Hex string with 0x prefix
     */
    fun ensureHexPrefix(hex: String): String {
        return if (hex.startsWith("0x") || hex.startsWith("0X")) {
            hex
        } else {
            "0x$hex"
        }
    }
}
