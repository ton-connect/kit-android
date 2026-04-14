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
 * Staking quote response with pricing information
 *
 * @param direction
 * @param rawAmountIn
 * @param rawAmountOut
 * @param amountIn Formatted amount of tokens being provided
 * @param amountOut Formatted estimated amount of tokens to be received
 * @param network
 * @param providerId Identifier of the staking provider
 * @param apy Annual Percentage Yield as a percentage value
 * @param unstakeMode
 * @param metadata Provider-specific metadata for the quote
 */
@Serializable
data class TONStakingQuote(

    @Contextual @SerialName(value = "direction")
    val direction: TONStakingQuoteDirection,

    @SerialName(value = "rawAmountIn")
    val rawAmountIn: kotlin.String,

    @SerialName(value = "rawAmountOut")
    val rawAmountOut: kotlin.String,

    /* Formatted amount of tokens being provided */
    @SerialName(value = "amountIn")
    val amountIn: kotlin.String,

    /* Formatted estimated amount of tokens to be received */
    @SerialName(value = "amountOut")
    val amountOut: kotlin.String,

    @SerialName(value = "network")
    val network: TONNetwork,

    /* Identifier of the staking provider */
    @SerialName(value = "providerId")
    val providerId: kotlin.String,

    /* Annual Percentage Yield as a percentage value */
    @SerialName(value = "apy")
    val apy: kotlin.Double? = null,

    @Contextual @SerialName(value = "unstakeMode")
    val unstakeMode: TONUnstakeMode? = null,

    /* Provider-specific metadata for the quote */
    @Contextual @SerialName(value = "metadata")
    val metadata: kotlinx.serialization.json.JsonElement? = null,

) {

    companion object
}
