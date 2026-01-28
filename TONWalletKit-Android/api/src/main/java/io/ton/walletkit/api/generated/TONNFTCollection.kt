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
 * NFT collection on the TON blockchain (TEP-62 standard).
 *
 * @param address
 * @param name The name of the NFT collection
 * @param image
 * @param description A brief description of the NFT collection
 * @param nextItemIndex The index value for the next item to be minted in the collection
 * @param codeHash
 * @param dataHash
 * @param ownerAddress
 * @param extra Additional arbitrary data related to the NFT collection
 */
@Serializable
data class TONNFTCollection(

    @Contextual @SerialName(value = "address")
    val address: io.ton.walletkit.model.TONUserFriendlyAddress,

    /* The name of the NFT collection */
    @SerialName(value = "name")
    val name: kotlin.String? = null,

    @SerialName(value = "image")
    val image: TONTokenImage? = null,

    /* A brief description of the NFT collection */
    @SerialName(value = "description")
    val description: kotlin.String? = null,

    /* The index value for the next item to be minted in the collection */
    @SerialName(value = "nextItemIndex")
    val nextItemIndex: kotlin.String? = null,

    @Contextual @SerialName(value = "codeHash")
    val codeHash: io.ton.walletkit.model.TONHex? = null,

    @Contextual @SerialName(value = "dataHash")
    val dataHash: io.ton.walletkit.model.TONHex? = null,

    @Contextual @SerialName(value = "ownerAddress")
    val ownerAddress: io.ton.walletkit.model.TONUserFriendlyAddress? = null,

    /* Additional arbitrary data related to the NFT collection */
    @Contextual @SerialName(value = "extra")
    val extra: kotlin.collections.Map<kotlin.String, kotlinx.serialization.json.JsonElement>? = null,

) {

    companion object
}
