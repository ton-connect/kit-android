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
 *
 *
 * @param amount
 * @param fromToken
 * @param toToken
 * @param network
 * @param slippageBps Slippage tolerance in basis points (1 bp = 0.01%)
 * @param maxOutgoingMessages Maximum number of outgoing messages
 * @param providerOptions Provider-specific options
 * @param isReverseSwap If true, amount is the amount to receive (buy). If false, amount is the amount to spend (sell).
 */
@Serializable
data class TONSwapQuoteParams(

    @SerialName(value = "amount")
    val amount: kotlin.String,

    @SerialName(value = "fromToken")
    val fromToken: TONSwapToken,

    @SerialName(value = "toToken")
    val toToken: TONSwapToken,

    @SerialName(value = "network")
    val network: TONNetwork,

    /* Slippage tolerance in basis points (1 bp = 0.01%) */
    @SerialName(value = "slippageBps")
    val slippageBps: kotlin.Int? = null,

    /* Maximum number of outgoing messages */
    @SerialName(value = "maxOutgoingMessages")
    val maxOutgoingMessages: kotlin.Int? = null,

    /* Provider-specific options */
    @Contextual @SerialName(value = "providerOptions")
    val providerOptions: kotlinx.serialization.json.JsonElement? = null,

    /* If true, amount is the amount to receive (buy). If false, amount is the amount to spend (sell). */
    @SerialName(value = "isReverseSwap")
    val isReverseSwap: kotlin.Boolean? = null,

) {

    companion object
}
