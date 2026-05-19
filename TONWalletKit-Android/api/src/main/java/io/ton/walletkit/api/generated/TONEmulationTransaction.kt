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
 * Transaction within an emulated trace.
 *
 * @param account
 * @param hash
 * @param lt
 * @param now Unix timestamp of the transaction
 * @param mcBlockSeqno Masterchain block sequence number
 * @param traceExternalHash
 * @param origStatus
 * @param endStatus
 * @param totalFees
 * @param totalFeesExtraCurrencies Map of extra currency IDs to their amounts. Extra currencies are additional tokens that can be attached to TON messages.
 * @param description
 * @param blockRef
 * @param outMsgs Outgoing messages produced by this transaction
 * @param accountStateBefore
 * @param accountStateAfter
 * @param isEmulated Whether this transaction was produced by emulation rather than executed on-chain
 * @param prevTransHash
 * @param prevTransLt
 * @param inMsg
 * @param traceId Trace identifier, if available
 */
@Serializable
data class TONEmulationTransaction(

    @Contextual @SerialName(value = "account")
    val account: io.ton.walletkit.model.TONUserFriendlyAddress,

    @Contextual @SerialName(value = "hash")
    val hash: io.ton.walletkit.model.TONHex,

    @SerialName(value = "lt")
    val lt: kotlin.String,

    /* Unix timestamp of the transaction */
    @SerialName(value = "now")
    val now: kotlin.Int,

    /* Masterchain block sequence number */
    @SerialName(value = "mcBlockSeqno")
    val mcBlockSeqno: kotlin.Int,

    @Contextual @SerialName(value = "traceExternalHash")
    val traceExternalHash: io.ton.walletkit.model.TONHex,

    @Contextual @SerialName(value = "origStatus")
    val origStatus: TONAccountStatus,

    @Contextual @SerialName(value = "endStatus")
    val endStatus: TONAccountStatus,

    @SerialName(value = "totalFees")
    val totalFees: kotlin.String,

    /* Map of extra currency IDs to their amounts. Extra currencies are additional tokens that can be attached to TON messages. */
    @SerialName(value = "totalFeesExtraCurrencies")
    val totalFeesExtraCurrencies: kotlin.collections.Map<kotlin.String, kotlin.String>,

    @SerialName(value = "description")
    val description: TONEmulationTransactionDescription,

    @SerialName(value = "blockRef")
    val blockRef: TONEmulationBlockRef,

    /* Outgoing messages produced by this transaction */
    @SerialName(value = "outMsgs")
    val outMsgs: kotlin.collections.List<TONEmulationMessage>,

    @SerialName(value = "accountStateBefore")
    val accountStateBefore: TONEmulationAccountState,

    @SerialName(value = "accountStateAfter")
    val accountStateAfter: TONEmulationAccountState,

    /* Whether this transaction was produced by emulation rather than executed on-chain */
    @SerialName(value = "isEmulated")
    val isEmulated: kotlin.Boolean,

    @Contextual @SerialName(value = "prevTransHash")
    val prevTransHash: io.ton.walletkit.model.TONHex? = null,

    @SerialName(value = "prevTransLt")
    val prevTransLt: kotlin.String? = null,

    @SerialName(value = "inMsg")
    val inMsg: TONEmulationMessage? = null,

    /* Trace identifier, if available */
    @SerialName(value = "traceId")
    val traceId: kotlin.String? = null,

) {

    companion object
}
