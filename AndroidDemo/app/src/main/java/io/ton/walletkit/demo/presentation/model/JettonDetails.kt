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
 * UI-friendly detailed jetton information model.
 *
 * Maps from [TONJetton] SDK model to presentation layer for detail view.
 */
data class JettonDetails(
    val name: String,
    val symbol: String,
    val description: String?,
    val jettonAddress: String,
    val walletAddress: String,
    val balance: String,
    val formattedBalance: String,
    val decimals: Int,
    val totalSupply: String?,
    val imageUrl: String?,
    val imageData: String?,
    val verified: Boolean,
) {
    /**
     * Get the image source, preferring URL over base64 data.
     */
    val imageSource: String?
        get() = imageUrl ?: imageData

    companion object {
        /**
         * Create JettonDetails from SDK's TONJetton.
         *
         * @param jetton Jetton from SDK
         * @return UI-friendly jetton details
         */
        fun from(jetton: TONJetton): JettonDetails {
            val info = jetton.info

            val name = info.name ?: "Unknown Jetton"
            val symbol = info.symbol ?: "UNKNOWN"
            val description = info.description
            val jettonAddress = jetton.address.value
            val walletAddress = jetton.walletAddress.value
            val balance = jetton.balance
            val decimals = jetton.decimalsNumber ?: 9

            // Format balance with decimals
            val formattedBalance = try {
                val balanceBigInt = BigDecimal(balance)
                val divisor = BigDecimal.TEN.pow(decimals)
                val formattedValue = balanceBigInt.divide(divisor, decimals, RoundingMode.DOWN)
                formattedValue.stripTrailingZeros().toPlainString()
            } catch (e: Exception) {
                balance
            }

            val imageUrl = info.image?.mediumUrl ?: info.image?.url
            val imageData = info.image?.data?.let { String(it, Charsets.UTF_8) }

            return JettonDetails(
                name = name,
                symbol = symbol,
                description = description,
                jettonAddress = jettonAddress,
                walletAddress = walletAddress,
                balance = balance,
                formattedBalance = formattedBalance,
                decimals = decimals,
                totalSupply = null, // Not available in new API
                imageUrl = imageUrl,
                imageData = imageData,
                verified = jetton.isVerified,
            )
        }
    }
}
