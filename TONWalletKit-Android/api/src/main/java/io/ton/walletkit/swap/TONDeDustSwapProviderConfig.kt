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
package io.ton.walletkit.swap

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Configuration for the DeDust swap provider.
 *
 * All fields are optional; omitting them uses provider defaults.
 */
@Serializable
data class TONDeDustSwapProviderConfig(
    /** Custom provider identifier. Defaults to "dedust". */
    @SerialName("providerId") val providerId: String? = null,
    /** Default slippage tolerance in basis points (1 bp = 0.01%). Default: 100 (1%). */
    @SerialName("defaultSlippageBps") val defaultSlippageBps: Int? = null,
    /** DeDust API base URL. Default: https://api-mainnet.dedust.io */
    @SerialName("apiUrl") val apiUrl: String? = null,
    /** Only route through verified pools. Default: true. */
    @SerialName("onlyVerifiedPools") val onlyVerifiedPools: Boolean? = null,
    /** Maximum number of route splits. Default: 4. */
    @SerialName("maxSplits") val maxSplits: Int? = null,
    /** Maximum route length (hops). Default: 3. */
    @SerialName("maxLength") val maxLength: Int? = null,
    /** Minimum pool TVL in USD. Default: "5000". */
    @SerialName("minPoolUsdTvl") val minPoolUsdTvl: String? = null,
    /** Referral address. */
    @SerialName("referralAddress") val referralAddress: String? = null,
    /** Referral fee in basis points (max 100 = 1%). */
    @SerialName("referralFeeBps") val referralFeeBps: Int? = null,
)
