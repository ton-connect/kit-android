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
 * Transaction on the TON blockchain.
 *
 * @param account
 * @param hash
 * @param logicalTime
 * @param now Unix timestamp of the transaction
 * @param mcBlockSeqno Masterchain block sequence number
 * @param traceExternalHash
 * @param outMessages The list of outgoing messages produced by the transaction
 * @param isEmulated Emulated state of the transaction
 * @param accountStateBefore
 * @param accountStateAfter
 * @param description
 * @param traceId ID of the trace
 * @param previousTransactionHash The hash of the previous transaction
 * @param previousTransactionLogicalTime
 * @param origStatus
 * @param endStatus
 * @param totalFees
 * @param totalFeesExtraCurrencies Map of extra currency IDs to their amounts. Extra currencies are additional tokens that can be attached to TON messages.
 * @param blockRef
 * @param inMessage
 */
@Serializable
data class TONTransaction(

    @Contextual @SerialName(value = "account")
    val account: io.ton.walletkit.model.TONUserFriendlyAddress,

    @Contextual @SerialName(value = "hash")
    val hash: io.ton.walletkit.model.TONHex,

    @SerialName(value = "logicalTime")
    val logicalTime: kotlin.String,

    /* Unix timestamp of the transaction */
    @SerialName(value = "now")
    val now: kotlin.Int,

    /* Masterchain block sequence number */
    @SerialName(value = "mcBlockSeqno")
    val mcBlockSeqno: kotlin.Int,

    @Contextual @SerialName(value = "traceExternalHash")
    val traceExternalHash: io.ton.walletkit.model.TONHex,

    /* The list of outgoing messages produced by the transaction */
    @SerialName(value = "outMessages")
    val outMessages: kotlin.collections.List<TONTransactionMessage>,

    /* Emulated state of the transaction */
    @SerialName(value = "isEmulated")
    val isEmulated: kotlin.Boolean,

    @SerialName(value = "accountStateBefore")
    val accountStateBefore: TONAccountState? = null,

    @SerialName(value = "accountStateAfter")
    val accountStateAfter: TONAccountState? = null,

    @SerialName(value = "description")
    val description: TONTransactionDescription? = null,

    /* ID of the trace */
    @SerialName(value = "traceId")
    val traceId: kotlin.String? = null,

    /* The hash of the previous transaction */
    @SerialName(value = "previousTransactionHash")
    val previousTransactionHash: kotlin.String? = null,

    @SerialName(value = "previousTransactionLogicalTime")
    val previousTransactionLogicalTime: kotlin.String? = null,

    @SerialName(value = "origStatus")
    val origStatus: TONAccountStatus? = null,

    @SerialName(value = "endStatus")
    val endStatus: TONAccountStatus? = null,

    @SerialName(value = "totalFees")
    val totalFees: kotlin.String? = null,

    /* Map of extra currency IDs to their amounts. Extra currencies are additional tokens that can be attached to TON messages. */
    @SerialName(value = "totalFeesExtraCurrencies")
    val totalFeesExtraCurrencies: kotlin.collections.Map<kotlin.String, kotlin.String>? = null,

    @SerialName(value = "blockRef")
    val blockRef: TONTransactionBlockRef? = null,

    @SerialName(value = "inMessage")
    val inMessage: TONTransactionMessage? = null,

) {

    companion object
}
