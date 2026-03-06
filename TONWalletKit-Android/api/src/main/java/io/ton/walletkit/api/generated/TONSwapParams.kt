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
 *
 *
 * @param quote
 * @param userAddress
 * @param destinationAddress
 * @param slippageBps Slippage tolerance in basis points (1 bp = 0.01%)
 * @param deadline Transaction deadline in unix timestamp
 * @param providerOptions Provider-specific options
 */
@Serializable
data class TONSwapParams(

    @SerialName(value = "quote")
    val quote: TONSwapQuote,

    @Contextual @SerialName(value = "userAddress")
    val userAddress: io.ton.walletkit.model.TONUserFriendlyAddress,

    @Contextual @SerialName(value = "destinationAddress")
    val destinationAddress: io.ton.walletkit.model.TONUserFriendlyAddress? = null,

    /* Slippage tolerance in basis points (1 bp = 0.01%) */
    @SerialName(value = "slippageBps")
    val slippageBps: kotlin.Int? = null,

    /* Transaction deadline in unix timestamp */
    @SerialName(value = "deadline")
    val deadline: kotlin.Int? = null,

    /* Provider-specific options */
    @Contextual @SerialName(value = "providerOptions")
    val providerOptions: kotlinx.serialization.json.JsonElement? = null,

) {

    companion object
}
