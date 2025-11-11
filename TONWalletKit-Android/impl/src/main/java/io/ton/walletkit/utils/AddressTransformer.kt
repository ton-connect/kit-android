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

/**
 * AddressTransformer - Address formatting and conversion utilities
 *
 * Handles base64 to hex conversion, user-friendly address formatting,
 * and other address transformations.
 */
object AddressTransformer {

    /**
     * Converts a base64 string to hexadecimal format.
     */
    fun base64ToHex(base64: String?): String? {
        if (base64.isNullOrBlank()) return null

        return try {
            val bytes = Base64.decode(base64, Base64.NO_WRAP)
            bytesToHex(bytes)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Converts a byte array to hexadecimal string.
     */
    fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Converts a hexadecimal string to byte array.
     */
    fun hexToBytes(hex: String): ByteArray {
        val cleaned = if (hex.startsWith("0x", ignoreCase = true)) {
            hex.substring(2)
        } else {
            hex
        }

        return cleaned.chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
    }

    /**
     * Converts a raw address to user-friendly format.
     *
     * This is a placeholder for the actual TON address formatting logic.
     * In production, this should use the TON SDK's address parsing and formatting.
     *
     * @param rawAddress The raw address string
     * @param isTestnet Whether to format for testnet or mainnet
     * @return User-friendly formatted address (EQ... or UQ...)
     */
    fun toUserFriendly(rawAddress: String?, isTestnet: Boolean = false): String? {
        if (rawAddress.isNullOrBlank()) return null

        // If already in user-friendly format, return as-is
        if (rawAddress.matches(Regex("^(EQ|UQ)[0-9a-zA-Z_-]{46}$"))) {
            return rawAddress
        }

        // TODO: Implement actual TON address parsing and formatting
        // This requires the TON SDK's Address class
        // For now, return the raw address as fallback
        return rawAddress
    }

    /**
     * Validates if a string is a valid TON address format.
     */
    fun isValidAddress(address: String?): Boolean {
        if (address.isNullOrBlank()) return false

        // User-friendly format (base64url, 48 chars, EQ/UQ prefix)
        if (address.matches(Regex("^(EQ|UQ)[0-9a-zA-Z_-]{46}$"))) {
            return true
        }

        // Raw format (base64, typically 48 chars)
        if (address.length == 48 && address.matches(Regex("^[0-9a-zA-Z+/=]+$"))) {
            return true
        }

        return false
    }

    /**
     * Normalizes an address to a standard format.
     * Trims whitespace and validates format.
     */
    fun normalize(address: String?): String? {
        val trimmed = address?.trim()

        if (trimmed.isNullOrBlank() || !isValidAddress(trimmed)) {
            return null
        }

        return trimmed
    }

    /**
     * Shortens an address for display purposes.
     * Example: EQ...abc -> EQ12...abc
     */
    fun shorten(address: String?, prefixLength: Int = 6, suffixLength: Int = 4): String? {
        if (address.isNullOrBlank() || address.length <= prefixLength + suffixLength) {
            return address
        }

        val prefix = address.substring(0, prefixLength)
        val suffix = address.substring(address.length - suffixLength)

        return "$prefix...$suffix"
    }
}
