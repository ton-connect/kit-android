package io.ton.walletkit.demo.presentation.util

import android.os.Build
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Locale
import java.util.TimeZone

/**
 * Utility for parsing various timestamp formats.
 */
object TimestampParser {
    
    private val NUMERIC_PATTERN = Regex("^-?\\d+")
    private val ISO8601_PATTERNS = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
    )
    
    /**
     * Parse a timestamp from various formats:
     * - Unix timestamp (numeric string)
     * - ISO 8601 timestamp (e.g., "2023-10-23T12:34:56.789Z")
     * 
     * Returns null if parsing fails.
     */
    fun parse(value: String?): Long? {
        if (value.isNullOrBlank()) return null
        return runCatching {
            when {
                value.matches(NUMERIC_PATTERN) -> value.toLong()
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> Instant.parse(value).toEpochMilli()
                else -> parseIso8601(value)
            }
        }.getOrNull()
    }
    
    /**
     * Parse ISO 8601 timestamp using SimpleDateFormat.
     * Tries multiple formats and normalizes fractional seconds if needed.
     */
    private fun parseIso8601(value: String): Long {
        val candidates = buildList {
            add(value)
            normalizeIso8601Fraction(value)?.let { add(it) }
        }

        candidates.forEach { candidate ->
            ISO8601_PATTERNS.forEach { pattern ->
                val parsed = runCatching {
                    SimpleDateFormat(pattern, Locale.US).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }.parse(candidate)?.time
                }.getOrNull()

                if (parsed != null) {
                    return parsed
                }
            }
        }

        throw IllegalArgumentException("Unsupported timestamp format: $value")
    }
    
    /**
     * Normalize ISO 8601 fractional seconds to 3 digits.
     * 
     * SimpleDateFormat expects exactly 3 digits for SSS pattern,
     * so we need to normalize timestamps with different precision.
     * 
     * Examples:
     * - "2023-10-23T12:34:56.78Z" -> "2023-10-23T12:34:56.780Z"
     * - "2023-10-23T12:34:56.123456Z" -> "2023-10-23T12:34:56.123Z"
     */
    private fun normalizeIso8601Fraction(value: String): String? {
        val timeSeparator = value.indexOf('T')
        if (timeSeparator == -1) return null

        val fractionStart = value.indexOf('.', startIndex = timeSeparator)
        if (fractionStart == -1) return null

        val zoneStart = value.indexOfAny(charArrayOf('Z', '+', '-'), startIndex = fractionStart)
        if (zoneStart == -1) return null

        val fraction = value.substring(fractionStart + 1, zoneStart)
        if (fraction.isEmpty()) return null

        val normalized = when {
            fraction.length == 3 -> return null // Already normalized
            fraction.length < 3 -> fraction.padEnd(3, '0')
            else -> fraction.substring(0, 3)
        }

        val prefix = value.substring(0, fractionStart)
        val suffix = value.substring(zoneStart)
        return "$prefix.$normalized$suffix"
    }
}
