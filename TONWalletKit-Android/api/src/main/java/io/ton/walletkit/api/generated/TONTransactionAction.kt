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
 * Action phase of transaction execution (message sending).
 *
 * @param isSuccess The flag indicating whether the action phase succeeded
 * @param isValid The flag indicating whether the action phase was valid
 * @param hasNoFunds The flag indicating if the transaction had insufficient funds
 * @param statusChange The status change applied to the account during the action phase
 * @param totalForwardingFees
 * @param totalActionFees
 * @param resultCode The result code returned from the action phase
 * @param totalActionsNumber The total number of actions processed
 * @param specActionsNumber The number of special actions executed
 * @param skippedActionsNumber The number of skipped actions during execution
 * @param messagesCreatedNumber The number of messages created in the action phase
 * @param actionListHash
 * @param totalMessagesSize
 */
@Serializable
data class TONTransactionAction(

    /* The flag indicating whether the action phase succeeded */
    @SerialName(value = "isSuccess")
    val isSuccess: kotlin.Boolean? = null,

    /* The flag indicating whether the action phase was valid */
    @SerialName(value = "isValid")
    val isValid: kotlin.Boolean? = null,

    /* The flag indicating if the transaction had insufficient funds */
    @SerialName(value = "hasNoFunds")
    val hasNoFunds: kotlin.Boolean? = null,

    /* The status change applied to the account during the action phase */
    @SerialName(value = "statusChange")
    val statusChange: kotlin.String? = null,

    @SerialName(value = "totalForwardingFees")
    val totalForwardingFees: kotlin.String? = null,

    @SerialName(value = "totalActionFees")
    val totalActionFees: kotlin.String? = null,

    /* The result code returned from the action phase */
    @SerialName(value = "resultCode")
    val resultCode: kotlin.Int? = null,

    /* The total number of actions processed */
    @SerialName(value = "totalActionsNumber")
    val totalActionsNumber: kotlin.Int? = null,

    /* The number of special actions executed */
    @SerialName(value = "specActionsNumber")
    val specActionsNumber: kotlin.Int? = null,

    /* The number of skipped actions during execution */
    @SerialName(value = "skippedActionsNumber")
    val skippedActionsNumber: kotlin.Int? = null,

    /* The number of messages created in the action phase */
    @SerialName(value = "messagesCreatedNumber")
    val messagesCreatedNumber: kotlin.Int? = null,

    @Contextual @SerialName(value = "actionListHash")
    val actionListHash: io.ton.walletkit.model.TONHex? = null,

    @SerialName(value = "totalMessagesSize")
    val totalMessagesSize: TONTransactionActionMessageSize? = null,

) {

    companion object
}
