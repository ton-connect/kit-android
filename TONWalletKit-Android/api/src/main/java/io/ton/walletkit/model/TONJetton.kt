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
package io.ton.walletkit.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Jetton (fungible token) information.
 *
 * Represents the metadata and configuration of a Jetton master contract.
 *
 * @property address Jetton master contract address (user-friendly format: UQ... or EQ...)
 * @property name Jetton name (e.g., "Tether USD")
 * @property symbol Jetton symbol (e.g., "USDT")
 * @property description Jetton description
 * @property decimals Number of decimal places (typically 9)
 * @property totalSupply Total supply of the jetton
 * @property image URL to jetton image/logo
 * @property imageData Base64-encoded image data
 * @property uri URI to jetton metadata
 * @property verification Verification status
 * @property metadata Additional metadata as key-value pairs
 */
@Serializable
data class TONJetton(
    /** Jetton master contract address (user-friendly format: UQ... or EQ...) */
    val address: String? = null,
    val name: String? = null,
    val symbol: String? = null,
    val description: String? = null,
    val decimals: Int? = null,
    @SerialName("total_supply")
    val totalSupply: String? = null,
    val image: String? = null,
    @SerialName("image_data")
    val imageData: String? = null,
    val uri: String? = null,
    val verification: TONJettonVerification? = null,
    val metadata: Map<String, JsonElement>? = null,
)
