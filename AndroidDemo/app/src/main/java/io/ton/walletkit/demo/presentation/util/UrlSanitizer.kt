package io.ton.walletkit.demo.presentation.util

/**
 * Utility for sanitizing and validating URLs.
 */
object UrlSanitizer {
    
    /**
     * Sanitize a URL by removing invalid or placeholder values.
     * 
     * Returns null if:
     * - Value is null or blank
     * - Value is the string "null" (case-insensitive)
     * 
     * Otherwise returns the trimmed URL.
     */
    fun sanitize(value: String?): String? {
        if (value.isNullOrBlank()) return null
        if (value.equals("null", ignoreCase = true)) return null
        return value.trim()
    }
}
