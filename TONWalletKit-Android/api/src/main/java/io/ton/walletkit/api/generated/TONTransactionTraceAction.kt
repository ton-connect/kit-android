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
 * High-level action extracted from a transaction trace.
 *
 * @param transactions List of transaction hashes involved in this action
 * @param accounts List of accounts involved in this action
 * @param details
 * @param traceId Trace identifier
 * @param actionId Action identifier
 * @param startLt
 * @param endLt
 * @param startUtime Unix time when the action started
 * @param endUtime Unix time when the action ended
 * @param traceEndLt
 * @param traceEndUtime Unix time when the trace ended
 * @param traceMcSeqnoEnd Masterchain block sequence number when the trace ended
 * @param isSuccess Indicates if the action was successful
 * @param traceExternalHash
 */
@Serializable
data class TONTransactionTraceAction(

    /* List of transaction hashes involved in this action */
    @SerialName(value = "transactions")
    val transactions: kotlin.collections.List<@Contextual io.ton.walletkit.model.TONHex>,

    /* List of accounts involved in this action */
    @SerialName(value = "accounts")
    val accounts: kotlin.collections.List<@Contextual io.ton.walletkit.model.TONUserFriendlyAddress>,

    @SerialName(value = "details")
    val details: TONTransactionTraceActionDetails,

    /* Trace identifier */
    @SerialName(value = "traceId")
    val traceId: kotlin.String? = null,

    /* Action identifier */
    @SerialName(value = "actionId")
    val actionId: kotlin.String? = null,

    @SerialName(value = "startLt")
    val startLt: kotlin.String? = null,

    @SerialName(value = "endLt")
    val endLt: kotlin.String? = null,

    /* Unix time when the action started */
    @SerialName(value = "startUtime")
    val startUtime: kotlin.Int? = null,

    /* Unix time when the action ended */
    @SerialName(value = "endUtime")
    val endUtime: kotlin.Int? = null,

    @SerialName(value = "traceEndLt")
    val traceEndLt: kotlin.String? = null,

    /* Unix time when the trace ended */
    @SerialName(value = "traceEndUtime")
    val traceEndUtime: kotlin.Int? = null,

    /* Masterchain block sequence number when the trace ended */
    @SerialName(value = "traceMcSeqnoEnd")
    val traceMcSeqnoEnd: kotlin.Int? = null,

    /* Indicates if the action was successful */
    @SerialName(value = "isSuccess")
    val isSuccess: kotlin.Boolean? = null,

    @Contextual @SerialName(value = "traceExternalHash")
    val traceExternalHash: io.ton.walletkit.model.TONHex? = null,

) {

    companion object
}
