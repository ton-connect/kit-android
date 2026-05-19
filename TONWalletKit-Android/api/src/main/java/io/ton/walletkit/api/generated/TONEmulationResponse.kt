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
 * Unified emulation response model, normalised from either Toncenter or TonAPI sources.
 *
 * @param mcBlockSeqno Masterchain block sequence number used during emulation
 * @param trace
 * @param transactions Map of transaction hashes to transaction details
 * @param actions High-level actions extracted from the trace
 * @param randSeed
 * @param isIncomplete Whether the trace is incomplete due to limits or errors
 * @param codeCells Map of code cell hashes to their BOC base64 representations
 * @param dataCells Map of data cell hashes to their BOC base64 representations
 * @param addressBook Address book mapping raw addresses to human-readable metadata
 */
@Serializable
data class TONEmulationResponse(

    /* Masterchain block sequence number used during emulation */
    @SerialName(value = "mcBlockSeqno")
    val mcBlockSeqno: kotlin.Int,

    @SerialName(value = "trace")
    val trace: TONEmulationTraceNode,

    /* Map of transaction hashes to transaction details */
    @Contextual @SerialName(value = "transactions")
    val transactions: kotlin.collections.Map<kotlin.String, TONEmulationTransaction>,

    /* High-level actions extracted from the trace */
    @SerialName(value = "actions")
    val actions: kotlin.collections.List<TONEmulationAction>,

    @Contextual @SerialName(value = "randSeed")
    val randSeed: io.ton.walletkit.model.TONHex,

    /* Whether the trace is incomplete due to limits or errors */
    @SerialName(value = "isIncomplete")
    val isIncomplete: kotlin.Boolean,

    /* Map of code cell hashes to their BOC base64 representations */
    @Contextual @SerialName(value = "codeCells")
    val codeCells: kotlin.collections.Map<kotlin.String, io.ton.walletkit.model.TONBase64>,

    /* Map of data cell hashes to their BOC base64 representations */
    @Contextual @SerialName(value = "dataCells")
    val dataCells: kotlin.collections.Map<kotlin.String, io.ton.walletkit.model.TONBase64>,

    /* Address book mapping raw addresses to human-readable metadata */
    @Contextual @SerialName(value = "addressBook")
    val addressBook: kotlin.collections.Map<kotlin.String, TONEmulationAddressBookEntry>,

) {

    companion object
}
