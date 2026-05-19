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
 * Action phase of transaction execution (outgoing message sending).
 *
 * @param isSuccess Whether the action phase succeeded
 * @param isValid Whether the action list was valid
 * @param hasNoFunds Whether the transaction failed due to insufficient funds
 * @param statusChange Account status change applied during the action phase
 * @param resultCode Result code of the action phase
 * @param totalActions Total number of actions processed
 * @param specActions Number of special actions executed
 * @param skippedActions Number of actions skipped
 * @param msgsCreated Number of messages created
 * @param totalMsgSize
 * @param totalFwdFees
 * @param totalActionFees
 * @param actionListHash
 */
@Serializable
data class TONEmulationActionPhase(

    /* Whether the action phase succeeded */
    @SerialName(value = "isSuccess")
    val isSuccess: kotlin.Boolean,

    /* Whether the action list was valid */
    @SerialName(value = "isValid")
    val isValid: kotlin.Boolean,

    /* Whether the transaction failed due to insufficient funds */
    @SerialName(value = "hasNoFunds")
    val hasNoFunds: kotlin.Boolean,

    /* Account status change applied during the action phase */
    @SerialName(value = "statusChange")
    val statusChange: kotlin.String,

    /* Result code of the action phase */
    @SerialName(value = "resultCode")
    val resultCode: kotlin.Int,

    /* Total number of actions processed */
    @SerialName(value = "totalActions")
    val totalActions: kotlin.Int,

    /* Number of special actions executed */
    @SerialName(value = "specActions")
    val specActions: kotlin.Int,

    /* Number of actions skipped */
    @SerialName(value = "skippedActions")
    val skippedActions: kotlin.Int,

    /* Number of messages created */
    @SerialName(value = "msgsCreated")
    val msgsCreated: kotlin.Int,

    @SerialName(value = "totalMsgSize")
    val totalMsgSize: TONEmulationActionMessageSize,

    @SerialName(value = "totalFwdFees")
    val totalFwdFees: kotlin.String? = null,

    @SerialName(value = "totalActionFees")
    val totalActionFees: kotlin.String? = null,

    @Contextual @SerialName(value = "actionListHash")
    val actionListHash: io.ton.walletkit.model.TONHex? = null,

) {

    companion object
}
