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
 * Summary of token flows for a transaction.
 *
 * @param outputs
 * @param inputs
 * @param allJettonTransfers List of all token transfers involved in the transaction
 * @param ourTransfers List of token transfers involving our address
 * @param ourAddress
 */
@Serializable
data class TONTransactionTraceMoneyFlow(

    @SerialName(value = "outputs")
    val outputs: kotlin.String,

    @SerialName(value = "inputs")
    val inputs: kotlin.String,

    /* List of all token transfers involved in the transaction */
    @SerialName(value = "allJettonTransfers")
    val allJettonTransfers: kotlin.collections.List<TONTransactionTraceMoneyFlowItem>,

    /* List of token transfers involving our address */
    @SerialName(value = "ourTransfers")
    val ourTransfers: kotlin.collections.List<TONTransactionTraceMoneyFlowItem>,

    @Contextual @SerialName(value = "ourAddress")
    val ourAddress: io.ton.walletkit.model.TONUserFriendlyAddress? = null,

) {

    companion object
}
