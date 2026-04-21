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
 * Provider-specific options for DeDust swap operations
 *
 * @param referralAddress The address of the referrer
 * @param referralFeeBps Referral fee in basis points (max 100 = 1%)
 * @param protocols Protocols to use for routing Available: 'dedust', 'dedust_v3', 'dedust_v3_memepad', 'stonfi_v1', 'stonfi_v2', 'tonco', 'memeslab', 'tonfun'
 * @param excludeProtocols Protocols to exclude from routing
 * @param onlyVerifiedPools Only use verified pools
 * @param maxSplits Maximum number of route splits
 * @param maxLength Maximum route length (hops)
 * @param excludeVolatilePools Exclude volatile pools
 */
@Serializable
data class TONDeDustProviderOptions(

    /* The address of the referrer */
    @SerialName(value = "referralAddress")
    val referralAddress: kotlin.String? = null,

    /* Referral fee in basis points (max 100 = 1%) */
    @SerialName(value = "referralFeeBps")
    val referralFeeBps: kotlin.Int? = null,

    /* Protocols to use for routing Available: 'dedust', 'dedust_v3', 'dedust_v3_memepad', 'stonfi_v1', 'stonfi_v2', 'tonco', 'memeslab', 'tonfun' */
    @SerialName(value = "protocols")
    val protocols: kotlin.collections.List<kotlin.String>? = null,

    /* Protocols to exclude from routing */
    @SerialName(value = "excludeProtocols")
    val excludeProtocols: kotlin.collections.List<kotlin.String>? = null,

    /* Only use verified pools */
    @SerialName(value = "onlyVerifiedPools")
    val onlyVerifiedPools: kotlin.Boolean? = null,

    /* Maximum number of route splits */
    @SerialName(value = "maxSplits")
    val maxSplits: kotlin.Int? = null,

    /* Maximum route length (hops) */
    @SerialName(value = "maxLength")
    val maxLength: kotlin.Int? = null,

    /* Exclude volatile pools */
    @SerialName(value = "excludeVolatilePools")
    val excludeVolatilePools: kotlin.Boolean? = null,

) {

    companion object
}
