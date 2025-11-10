/**
 * Copyright (c) TonTech.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.ton.walletkit.utils

/**
 * ValidationUtils - Input validation utilities
 *
 * Provides validation methods for addresses, amounts, mnemonics,
 * and other input parameters.
 */
object ValidationUtils {

    /**
     * Validates a TON address format.
     * Returns the trimmed address if valid, throws otherwise.
     */
    fun requireValidAddress(address: String?): String {
        val trimmed = address?.trim()
        
        if (trimmed.isNullOrBlank()) {
            throw IllegalArgumentException("Address is required")
        }
        
        // Basic validation - TON addresses are typically 48 characters (base64)
        // or start with EQ/UQ for user-friendly format
        if (trimmed.length < 48) {
            throw IllegalArgumentException("Invalid address format: too short")
        }
        
        return trimmed
    }

    /**
     * Validates an amount string (should be positive number).
     * Returns the trimmed amount if valid, throws otherwise.
     */
    fun requireValidAmount(amount: String?): String {
        val trimmed = amount?.trim()
        
        if (trimmed.isNullOrBlank()) {
            throw IllegalArgumentException("Amount is required")
        }
        
        // Check if it's a valid number
        val numericValue = trimmed.toBigDecimalOrNull()
            ?: throw IllegalArgumentException("Amount must be a valid number")
        
        if (numericValue <= java.math.BigDecimal.ZERO) {
            throw IllegalArgumentException("Amount must be greater than zero")
        }
        
        return trimmed
    }

    /**
     * Validates a mnemonic phrase.
     * Returns the list of words if valid, throws otherwise.
     */
    fun requireValidMnemonic(mnemonic: List<String>?): List<String> {
        if (mnemonic.isNullOrEmpty()) {
            throw IllegalArgumentException("Mnemonic is required")
        }
        
        val validSizes = setOf(12, 15, 18, 21, 24)
        if (mnemonic.size !in validSizes) {
            throw IllegalArgumentException(
                "Invalid mnemonic length: ${mnemonic.size}. Must be one of $validSizes"
            )
        }
        
        // Check that all words are non-empty
        if (mnemonic.any { it.isBlank() }) {
            throw IllegalArgumentException("Mnemonic contains empty words")
        }
        
        return mnemonic
    }

    /**
     * Validates a non-empty string value.
     * Returns the trimmed value if valid, throws otherwise.
     */
    fun requireNonEmpty(value: String?, fieldName: String = "Value"): String {
        val trimmed = value?.trim()
        
        if (trimmed.isNullOrBlank()) {
            throw IllegalArgumentException("$fieldName is required")
        }
        
        return trimmed
    }

    /**
     * Validates a non-null value.
     * Returns the value if non-null, throws otherwise.
     */
    fun <T> requireNonNull(value: T?, fieldName: String = "Value"): T {
        return value ?: throw IllegalArgumentException("$fieldName is required")
    }

    /**
     * Validates that a list is not empty.
     * Returns the list if valid, throws otherwise.
     */
    fun <T> requireNonEmptyList(list: List<T>?, fieldName: String = "List"): List<T> {
        if (list.isNullOrEmpty()) {
            throw IllegalArgumentException("$fieldName must not be empty")
        }
        return list
    }

    /**
     * Validates a positive integer.
     * Returns the value if valid, throws otherwise.
     */
    fun requirePositiveInt(value: Int?, fieldName: String = "Value"): Int {
        if (value == null || value <= 0) {
            throw IllegalArgumentException("$fieldName must be a positive integer")
        }
        return value
    }

    /**
     * Validates a non-negative integer.
     * Returns the value if valid, throws otherwise.
     */
    fun requireNonNegativeInt(value: Int?, fieldName: String = "Value"): Int {
        if (value == null || value < 0) {
            throw IllegalArgumentException("$fieldName must be non-negative")
        }
        return value
    }

    /**
     * Validates a hex string format.
     * Returns the cleaned hex string (without 0x prefix) if valid, throws otherwise.
     */
    fun requireValidHex(hex: String?, fieldName: String = "Hex value"): String {
        val trimmed = requireNonEmpty(hex, fieldName)
        val cleaned = if (trimmed.startsWith("0x", ignoreCase = true)) {
            trimmed.substring(2)
        } else {
            trimmed
        }
        
        if (!cleaned.matches(Regex("^[0-9a-fA-F]+$"))) {
            throw IllegalArgumentException("$fieldName must be a valid hex string")
        }
        
        return cleaned
    }

    /**
     * Validates a URL string format.
     * Returns the trimmed URL if valid, throws otherwise.
     */
    fun requireValidUrl(url: String?, fieldName: String = "URL"): String {
        val trimmed = requireNonEmpty(url, fieldName)
        
        if (!trimmed.matches(Regex("^https?://.*"))) {
            throw IllegalArgumentException("$fieldName must be a valid HTTP(S) URL")
        }
        
        return trimmed
    }
}
