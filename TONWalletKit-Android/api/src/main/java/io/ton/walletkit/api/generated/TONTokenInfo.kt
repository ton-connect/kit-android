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
@file:Suppress(
    "ArrayInDataClass",
    "EnumEntryName",
    "RemoveRedundantQualifierName",
    "UnusedImport",
)

package io.ton.walletkit.api.generated

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Display information for a token (TON, Jetton, or NFT).
 *
 * @param name Display name of the token
 * @param description Human-readable description of the token
 * @param image
 * @param animation
 * @param symbol Ticker symbol of the token (e.g., \"TON\", \"USDT\")
 */
@Serializable
data class TONTokenInfo(

    /* Display name of the token */
    @SerialName(value = "name")
    val name: kotlin.String? = null,

    /* Human-readable description of the token */
    @SerialName(value = "description")
    val description: kotlin.String? = null,

    @SerialName(value = "image")
    val image: TONTokenImage? = null,

    @SerialName(value = "animation")
    val animation: TONTokenAnimation? = null,

    /* Ticker symbol of the token (e.g., \"TON\", \"USDT\") */
    @SerialName(value = "symbol")
    val symbol: kotlin.String? = null,

) {

    companion object
}
