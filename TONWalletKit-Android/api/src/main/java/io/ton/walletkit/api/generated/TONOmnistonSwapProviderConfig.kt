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
 * Configuration for the Omniston Swap Provider
 *
 * @param referrerAddress The address of the referrer
 * @param referrerFeeBps Referrer fee in basis points (1 bp = 0.01%)
 * @param flexibleReferrerFee Whether a flexible referrer fee is allowed
 * @param apiUrl Optional URL for the Omniston API
 * @param defaultSlippageBps Default slippage tolerance in basis points (1 bp = 0.01%)
 * @param quoteTimeoutMs Timeout for quote requests in milliseconds
 * @param providerId Identifier for the provider
 */
@Serializable
data class TONOmnistonSwapProviderConfig(

    /* The address of the referrer */
    @SerialName(value = "referrerAddress")
    val referrerAddress: kotlin.String? = null,

    /* Referrer fee in basis points (1 bp = 0.01%) */
    @SerialName(value = "referrerFeeBps")
    val referrerFeeBps: kotlin.Int? = null,

    /* Whether a flexible referrer fee is allowed */
    @SerialName(value = "flexibleReferrerFee")
    val flexibleReferrerFee: kotlin.Boolean? = null,

    /* Optional URL for the Omniston API */
    @SerialName(value = "apiUrl")
    val apiUrl: kotlin.String? = null,

    /* Default slippage tolerance in basis points (1 bp = 0.01%) */
    @SerialName(value = "defaultSlippageBps")
    val defaultSlippageBps: kotlin.Int? = null,

    /* Timeout for quote requests in milliseconds */
    @SerialName(value = "quoteTimeoutMs")
    val quoteTimeoutMs: kotlin.Int? = null,

    /* Identifier for the provider */
    @SerialName(value = "providerId")
    val providerId: kotlin.String? = null,

) {

    companion object
}
