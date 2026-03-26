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
 * Information about the latest masterchain block.
 *
 * @param seqno Sequence number of the masterchain block
 * @param shard Shard identifier of the block
 * @param workchain Workchain ID of the block
 * @param fileHash
 * @param rootHash
 */
@Serializable
data class TONMasterchainInfo(

    /* Sequence number of the masterchain block */
    @SerialName(value = "seqno")
    val seqno: kotlin.Int,

    /* Shard identifier of the block */
    @SerialName(value = "shard")
    val shard: kotlin.String,

    /* Workchain ID of the block */
    @SerialName(value = "workchain")
    val workchain: kotlin.Int,

    @Contextual @SerialName(value = "fileHash")
    val fileHash: io.ton.walletkit.model.TONHex,

    @Contextual @SerialName(value = "rootHash")
    val rootHash: io.ton.walletkit.model.TONHex,

) {

    companion object
}
