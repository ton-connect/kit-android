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
 * @param amountIn
 * @param amountOut
 * @param network
 * @param providerId Identifier of the staking provider
 * @param apy Annual Percentage Yield in basis points (100 = 1%)
 * @param unstakeMode
 * @param estimatedUnstakeDelayHours Estimated delay in hours for unstaking
 * @param instantUnstakeAvailable
 * @param metadata Provider-specific metadata for the quote
 */
@Serializable
data class TONStakingQuote(

    @Contextual @SerialName(value = "direction")
    val direction: TONStakingQuoteDirection,

    @SerialName(value = "amountIn")
    val amountIn: kotlin.String,

    @SerialName(value = "amountOut")
    val amountOut: kotlin.String,

    @SerialName(value = "network")
    val network: TONNetwork,

    /* Identifier of the staking provider */
    @SerialName(value = "providerId")
    val providerId: kotlin.String,

    /* Annual Percentage Yield in basis points (100 = 1%) */
    @SerialName(value = "apy")
    val apy: kotlin.Int? = null,

    @Contextual @SerialName(value = "unstakeMode")
    val unstakeMode: TONUnstakeMode? = null,

    /* Estimated delay in hours for unstaking */
    @SerialName(value = "estimatedUnstakeDelayHours")
    val estimatedUnstakeDelayHours: kotlin.Int? = null,

    @SerialName(value = "instantUnstakeAvailable")
    val instantUnstakeAvailable: kotlin.String? = null,

    /* Provider-specific metadata for the quote */
    @Contextual @SerialName(value = "metadata")
    val metadata: kotlinx.serialization.json.JsonElement? = null,

) {

    companion object
}
