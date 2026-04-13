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
 * Configuration for the DeDust Swap Provider
 *
 * @param referralAddress The address of the referrer
 * @param referralFeeBps Referral fee in basis points (max 100 = 1%)
 * @param providerId Custom provider ID (defaults to 'dedust')
 * @param defaultSlippageBps Default slippage tolerance in basis points (1 bp = 0.01%)
 * @param apiUrl API base URL
 * @param onlyVerifiedPools Only use verified pools
 * @param maxSplits Maximum number of route splits
 * @param maxLength Maximum route length (hops)
 * @param minPoolUsdTvl Minimum pool TVL in USD
 */
@Serializable
data class TONDeDustSwapProviderConfig(

    /* The address of the referrer */
    @SerialName(value = "referralAddress")
    val referralAddress: kotlin.String? = null,

    /* Referral fee in basis points (max 100 = 1%) */
    @SerialName(value = "referralFeeBps")
    val referralFeeBps: kotlin.Int? = null,

    /* Custom provider ID (defaults to 'dedust') */
    @SerialName(value = "providerId")
    val providerId: kotlin.String? = null,

    /* Default slippage tolerance in basis points (1 bp = 0.01%) */
    @SerialName(value = "defaultSlippageBps")
    val defaultSlippageBps: kotlin.Int? = null,

    /* API base URL */
    @SerialName(value = "apiUrl")
    val apiUrl: kotlin.String? = null,

    /* Only use verified pools */
    @SerialName(value = "onlyVerifiedPools")
    val onlyVerifiedPools: kotlin.Boolean? = null,

    /* Maximum number of route splits */
    @SerialName(value = "maxSplits")
    val maxSplits: kotlin.Int? = null,

    /* Maximum route length (hops) */
    @SerialName(value = "maxLength")
    val maxLength: kotlin.Int? = null,

    /* Minimum pool TVL in USD */
    @SerialName(value = "minPoolUsdTvl")
    val minPoolUsdTvl: kotlin.String? = null,

) {

    companion object
}
