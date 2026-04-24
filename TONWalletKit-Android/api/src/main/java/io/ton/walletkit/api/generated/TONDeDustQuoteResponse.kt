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
 * Quote response from DeDust Router API
 *
 * @param inAmount
 * @param outAmount
 * @param swapData
 * @param swapIsPossible
 * @param priceImpact
 * @param improvement
 * @param inMinterPrice
 * @param outMinterPrice
 */
@Serializable
data class TONDeDustQuoteResponse(

    @SerialName(value = "in_amount")
    val inAmount: kotlin.String,

    @SerialName(value = "out_amount")
    val outAmount: kotlin.String,

    @SerialName(value = "swap_data")
    val swapData: TONDeDustSwapData,

    @SerialName(value = "swap_is_possible")
    val swapIsPossible: kotlin.Boolean,

    @SerialName(value = "price_impact")
    val priceImpact: kotlin.Double? = null,

    @SerialName(value = "improvement")
    val improvement: kotlin.String? = null,

    @SerialName(value = "in_minter_price")
    val inMinterPrice: kotlin.String? = null,

    @SerialName(value = "out_minter_price")
    val outMinterPrice: kotlin.String? = null,

) {

    companion object
}
