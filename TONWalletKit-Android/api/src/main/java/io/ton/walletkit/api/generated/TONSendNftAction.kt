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
 * NFT transfer action (TEP-62).
 *
 * @param type Action type discriminator
 * @param nftAddress NFT item address
 * @param newOwnerAddress New owner address
 * @param responseDestination Response destination (defaults to sender)
 * @param customPayload
 * @param forwardTonAmount Forward TON amount (nanotons)
 * @param forwardPayload
 * @param queryId Query ID
 */
@Serializable
data class TONSendNftAction(

    /* Action type discriminator */
    @SerialName(value = "type")
    val type: TONSendNftAction.Type,

    /* NFT item address */
    @SerialName(value = "nftAddress")
    val nftAddress: kotlin.String,

    /* New owner address */
    @SerialName(value = "newOwnerAddress")
    val newOwnerAddress: kotlin.String,

    /* Response destination (defaults to sender) */
    @SerialName(value = "responseDestination")
    val responseDestination: kotlin.String? = null,

    @Contextual @SerialName(value = "customPayload")
    val customPayload: io.ton.walletkit.model.TONBase64? = null,

    /* Forward TON amount (nanotons) */
    @SerialName(value = "forwardTonAmount")
    val forwardTonAmount: kotlin.String? = null,

    @Contextual @SerialName(value = "forwardPayload")
    val forwardPayload: io.ton.walletkit.model.TONBase64? = null,

    /* Query ID */
    @SerialName(value = "queryId")
    val queryId: kotlin.Int? = null,

) {

    companion object

    /**
     * Action type discriminator
     *
     * Values: sendNft
     */
    @Serializable
    enum class Type(val value: kotlin.String) {
        @SerialName(value = "sendNft")
        sendNft("sendNft"),
    }
}
