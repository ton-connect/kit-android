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
 *
 *
 * @param status
 * @param address
 * @param transactions The array of transactions
 * @param traceHash
 * @param addressBook Map of raw addresses to their metadata entries.
 * @param metadata Metadata about addresses, including indexing and associated token info.
 */
@Serializable
data class TONTransactionsUpdate(

    @Contextual @SerialName(value = "status")
    val status: TONStreamingUpdateStatus,

    @Contextual @SerialName(value = "address")
    val address: io.ton.walletkit.model.TONUserFriendlyAddress,

    /* The array of transactions */
    @SerialName(value = "transactions")
    val transactions: kotlin.collections.List<TONTransaction>,

    @Contextual @SerialName(value = "traceHash")
    val traceHash: io.ton.walletkit.model.TONHex,

    /* Map of raw addresses to their metadata entries. */
    @Contextual @SerialName(value = "addressBook")
    val addressBook: kotlin.collections.Map<kotlin.String, TONAddressBookEntry>? = null,

    /* Metadata about addresses, including indexing and associated token info. */
    @Contextual @SerialName(value = "metadata")
    val metadata: kotlin.collections.Map<kotlin.String, TONTransactionAddressMetadataEntry>? = null,
    @SerialName("type")
    val type: kotlin.String = "transactions",
) {

    companion object
}
