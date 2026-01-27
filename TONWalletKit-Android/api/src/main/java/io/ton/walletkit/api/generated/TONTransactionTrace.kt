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
 * Trace of a transaction execution showing the full message chain.
 *
 * @param mcBlockSeqno Masterchain block sequence number where emulation was performed
 * @param trace
 * @param transactions Map of transaction hashes to transaction details
 * @param actions List of high-level actions extracted from the trace
 * @param randSeed
 * @param isIncomplete Whether the trace is incomplete due to limits or errors
 */
@Serializable
data class TONTransactionTrace(

    /* Masterchain block sequence number where emulation was performed */
    @SerialName(value = "mcBlockSeqno")
    val mcBlockSeqno: kotlin.Int,

    @SerialName(value = "trace")
    val trace: TONTransactionTraceNode,

    /* Map of transaction hashes to transaction details */
    @Contextual @SerialName(value = "transactions")
    val transactions: kotlin.collections.Map<kotlin.String, TONTransaction>,

    /* List of high-level actions extracted from the trace */
    @SerialName(value = "actions")
    val actions: kotlin.collections.List<TONTransactionTraceAction>,

    @Contextual @SerialName(value = "randSeed")
    val randSeed: io.ton.walletkit.model.TONHex,

    /* Whether the trace is incomplete due to limits or errors */
    @SerialName(value = "isIncomplete")
    val isIncomplete: kotlin.Boolean,

) {

    companion object
}
