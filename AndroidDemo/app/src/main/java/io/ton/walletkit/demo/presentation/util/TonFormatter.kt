package io.ton.walletkit.demo.presentation.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

/**
 * Utility for formatting TON amounts.
 */
object TonFormatter {
    
    private const val NANOTON_DIVISOR = 1_000_000_000.0
    private const val DEFAULT_FORMAT = "0.0000"
    
    /**
     * Format nanoton (string) to TON with 4 decimal places.
     * 
     * Example: "1000000000" -> "1.0000"
     * 
     * Returns "0.0000" if parsing fails.
     */
    fun formatNanoTon(nanoTon: String): String = try {
        val value = nanoTon.toLongOrNull() ?: 0L
        val ton = value.toDouble() / NANOTON_DIVISOR
        String.format(Locale.US, "%.4f", ton)
    } catch (_: Exception) {
        DEFAULT_FORMAT
    }
    
    /**
     * Format raw nanoton string from BigDecimal to TON with proper precision.
     * 
     * This uses BigDecimal for precise decimal conversion and formats with:
     * - 4 decimal places (scaled down, not rounded up)
     * - Trailing zeros removed
     * - Plain string format (no scientific notation)
     * 
     * Example: "1234567890" -> "1.2345"
     * 
     * Returns the original string if parsing fails.
     */
    fun formatTon(raw: String): String = runCatching {
        BigDecimal(raw)
            .movePointLeft(9) // Divide by 1,000,000,000
            .setScale(4, RoundingMode.DOWN)
            .stripTrailingZeros()
            .toPlainString()
    }.getOrElse { raw }
    
    /**
     * Convert TON to nanoton string.
     * 
     * Example: "1.5" -> "1500000000"
     */
    fun tonToNano(ton: String): String {
        val tonAmount = ton.toBigDecimal()
        return (tonAmount * BigDecimal(1_000_000_000)).toBigInteger().toString()
    }
}
