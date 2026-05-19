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

import io.ton.walletkit.model.TONUserFriendlyAddress
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Static metadata for a staking provider
 *
 * @param name Human-readable provider name (e.g. \"Tonstakers\")
 * @param supportedUnstakeModes Supported unstake modes for this provider
 * @param supportsReversedQuote Whether provider supports reversed quote format (e.g., passing TON instead of tsTON for unstake)
 * @param stakeToken
 * @param receiveToken
 * @param contractAddress
 */
@Serializable
data class TONStakingProviderMetadata(

    /* Human-readable provider name (e.g. \"Tonstakers\") */
    @SerialName(value = "name")
    val name: kotlin.String,

    /* Supported unstake modes for this provider */
    @SerialName(value = "supportedUnstakeModes")
    val supportedUnstakeModes: kotlin.collections.List<@Contextual TONUnstakeMode>,

    /* Whether provider supports reversed quote format (e.g., passing TON instead of tsTON for unstake) */
    @SerialName(value = "supportsReversedQuote")
    val supportsReversedQuote: kotlin.Boolean,

    @SerialName(value = "stakeToken")
    val stakeToken: TONStakingTokenInfo,

    @SerialName(value = "receiveToken")
    val receiveToken: TONStakingTokenInfo? = null,

    @Contextual @SerialName(value = "contractAddress")
    val contractAddress: io.ton.walletkit.model.TONUserFriendlyAddress? = null,

) {

    companion object
}
