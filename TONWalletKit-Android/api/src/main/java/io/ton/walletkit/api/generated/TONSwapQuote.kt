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

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Swap quote response with pricing information
 *
 * @param fromToken
 * @param toToken
 * @param fromAmount
 * @param toAmount
 * @param minReceived
 * @param network
 * @param providerId Identifier of the swap provider
 * @param priceImpact Price impact of the swap in basis points (100 = 1%)
 * @param fee Fees associated with the swap
 * @param expiresAt Unix timestamp in seconds when the quote expires
 * @param metadata Provider-specific metadata for the quote
 */
@Serializable
data class TONSwapQuote(

    @SerialName(value = "fromToken")
    val fromToken: TONSwapToken,

    @SerialName(value = "toToken")
    val toToken: TONSwapToken,

    @SerialName(value = "fromAmount")
    val fromAmount: kotlin.String,

    @SerialName(value = "toAmount")
    val toAmount: kotlin.String,

    @SerialName(value = "minReceived")
    val minReceived: kotlin.String,

    @SerialName(value = "network")
    val network: TONNetwork,

    /* Identifier of the swap provider */
    @SerialName(value = "providerId")
    val providerId: kotlin.String,

    /* Price impact of the swap in basis points (100 = 1%) */
    @SerialName(value = "priceImpact")
    val priceImpact: kotlin.Int? = null,

    /* Fees associated with the swap */
    @SerialName(value = "fee")
    val fee: kotlin.collections.List<TONSwapFee>? = null,

    /* Unix timestamp in seconds when the quote expires */
    @SerialName(value = "expiresAt")
    val expiresAt: kotlin.Int? = null,

    /* Provider-specific metadata for the quote */
    @Contextual @SerialName(value = "metadata")
    val metadata: kotlinx.serialization.json.JsonElement? = null,

) {

    companion object
}
