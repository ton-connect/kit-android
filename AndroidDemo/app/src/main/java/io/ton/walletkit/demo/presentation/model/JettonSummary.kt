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
import io.ton.walletkit.demo.data.api.JettonBalance
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * UI-friendly jetton summary model for list display.
 *
 * Supports both SDK's TONJetton and native API's JettonBalance.
 */
data class JettonSummary(
    val name: String,
    val symbol: String,
    val address: String,
    val masterAddress: String,
    val balance: String,
    val formattedBalance: String,
    val imageUrl: String?,
    val estimatedValue: String,
    val decimals: Int,
) {
    companion object {
        /**
         * Create JettonSummary from SDK's TONJetton.
         */
        fun from(jetton: TONJetton): JettonSummary {
            val info = jetton.info
            val name = info.name ?: "Unknown Jetton"
            val symbol = info.symbol ?: "UNKNOWN"
            val walletAddress = jetton.walletAddress.value
            val masterAddress = jetton.address.value
            val balance = jetton.balance
            val decimals = jetton.decimalsNumber ?: 9

            val formattedBalance = formatBalance(balance, decimals, symbol)
            val imageUrl = info.image?.mediumUrl ?: info.image?.url

            return JettonSummary(
                name = name,
                symbol = symbol,
                address = walletAddress,
                masterAddress = masterAddress,
                balance = balance,
                formattedBalance = formattedBalance,
                imageUrl = imageUrl,
                estimatedValue = "≈ \$0.00",
                decimals = decimals,
            )
        }

        /**
         * Create JettonSummary from native API's JettonBalance.
         */
        fun from(jetton: JettonBalance): JettonSummary {
            val info = jetton.jetton
            val name = info.name
            val symbol = info.symbol
            val address = jetton.walletAddress.address
            val masterAddress = info.address
            val balance = jetton.balance
            val decimals = info.decimals

            val formattedBalance = formatBalance(balance, decimals, symbol)

            return JettonSummary(
                name = name,
                symbol = symbol,
                address = address,
                masterAddress = masterAddress,
                balance = balance,
                formattedBalance = formattedBalance,
                imageUrl = info.image,
                estimatedValue = "≈ \$0.00",
                decimals = decimals,
            )
        }

        private fun formatBalance(balance: String, decimals: Int, symbol: String): String = try {
            val balanceBigInt = BigDecimal(balance)
            val divisor = BigDecimal.TEN.pow(decimals)
            val formattedValue = balanceBigInt.divide(divisor, decimals, RoundingMode.DOWN)
            val strippedValue = formattedValue.stripTrailingZeros()
            val plainString = strippedValue.toPlainString()
            "$plainString $symbol"
        } catch (e: Exception) {
            "$balance $symbol (raw)"
        }
    }
}
