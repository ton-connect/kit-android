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
package io.ton.walletkit.demo.presentation.model

import io.ton.walletkit.api.generated.TONJetton
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * UI-friendly jetton summary model for list display.
 *
 * Maps from [TONJetton] SDK model to presentation layer.
 * Mirrors iOS WalletJettonsListItem structure for cross-platform consistency.
 */
data class JettonSummary(
    val name: String,
    val symbol: String,
    val address: String,
    val balance: String,
    val formattedBalance: String,
    val imageUrl: String?,
    val imageData: String?,
    val estimatedValue: String,
    val jetton: TONJetton,
) {
    /**
     * Get the image source, preferring URL over base64 data.
     */
    val imageSource: String?
        get() = imageUrl ?: imageData

    companion object {
        fun formatBalance(rawBalance: String, decimals: Int?, symbol: String): String = try {
            val d = decimals ?: 9
            val divisor = BigDecimal.TEN.pow(d)
            val formatted = BigDecimal(rawBalance)
                .divide(divisor, d, RoundingMode.DOWN)
                .stripTrailingZeros()
                .toPlainString()
            "$formatted $symbol"
        } catch (e: Exception) {
            "$rawBalance $symbol (raw)"
        }

        fun from(jetton: TONJetton): JettonSummary {
            val info = jetton.info
            val symbol = info.symbol ?: "UNKNOWN"
            return JettonSummary(
                name = info.name ?: "Unknown Jetton",
                symbol = symbol,
                address = jetton.walletAddress.value,
                balance = jetton.balance,
                formattedBalance = formatBalance(jetton.balance, jetton.decimalsNumber, symbol),
                imageUrl = info.image?.mediumUrl ?: info.image?.url,
                imageData = info.image?.data,
                estimatedValue = "≈ \$0.00",
                jetton = jetton,
            )
        }
    }
}
