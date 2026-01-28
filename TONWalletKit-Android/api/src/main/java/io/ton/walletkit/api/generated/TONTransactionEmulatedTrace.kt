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

import io.ton.walletkit.model.TONBase64
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Extended transaction trace with emulation-specific data including code/data cells and address metadata.
 *
 * @param mcBlockSeqno Masterchain block sequence number where emulation was performed
 * @param trace
 * @param transactions Map of transaction hashes to transaction details
 * @param actions List of high-level actions extracted from the trace
 * @param randSeed
 * @param isIncomplete Whether the trace is incomplete due to limits or errors
 * @param codeCells Map of code cell hashes to their Base64-encoded content
 * @param dataCells Map of data cell hashes to their Base64-encoded content
 * @param metadata Metadata about addresses, including indexing and associated token info.
 * @param addressBook Map of raw addresses to their metadata entries.
 */
@Serializable
data class TONTransactionEmulatedTrace(

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

    /* Map of code cell hashes to their Base64-encoded content */
    @Contextual @SerialName(value = "codeCells")
    val codeCells: kotlin.collections.Map<kotlin.String, io.ton.walletkit.model.TONBase64>,

    /* Map of data cell hashes to their Base64-encoded content */
    @Contextual @SerialName(value = "dataCells")
    val dataCells: kotlin.collections.Map<kotlin.String, io.ton.walletkit.model.TONBase64>,

    /* Metadata about addresses, including indexing and associated token info. */
    @Contextual @SerialName(value = "metadata")
    val metadata: kotlin.collections.Map<kotlin.String, TONTransactionAddressMetadataEntry>,

    /* Map of raw addresses to their metadata entries. */
    @Contextual @SerialName(value = "addressBook")
    val addressBook: kotlin.collections.Map<kotlin.String, TONAddressBookEntry>,

) {

    companion object
}
