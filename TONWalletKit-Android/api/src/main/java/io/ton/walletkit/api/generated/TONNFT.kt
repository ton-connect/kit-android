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
 * Non-fungible token (NFT) on the TON blockchain.
 *
 * @param address
 * @param index Index of the item within its collection
 * @param info
 * @param attributes Custom attributes/traits of the NFT (e.g., rarity, properties)
 * @param collection
 * @param auctionContractAddress
 * @param codeHash
 * @param dataHash
 * @param isInited Whether the NFT contract has been initialized
 * @param isSoulbound Whether the NFT is soulbound (non-transferable)
 * @param isOnSale Whether the NFT is currently listed for sale
 * @param ownerAddress
 * @param realOwnerAddress
 * @param saleContractAddress
 * @param extra Off-chain metadata of the NFT (key-value pairs)
 */
@Serializable
data class TONNFT(

    @Contextual @SerialName(value = "address")
    val address: io.ton.walletkit.model.TONUserFriendlyAddress,

    /* Index of the item within its collection */
    @SerialName(value = "index")
    val index: kotlin.String? = null,

    @SerialName(value = "info")
    val info: TONTokenInfo? = null,

    /* Custom attributes/traits of the NFT (e.g., rarity, properties) */
    @SerialName(value = "attributes")
    val attributes: kotlin.collections.List<TONNFTAttribute>? = null,

    @SerialName(value = "collection")
    val collection: TONNFTCollection? = null,

    @Contextual @SerialName(value = "auctionContractAddress")
    val auctionContractAddress: io.ton.walletkit.model.TONUserFriendlyAddress? = null,

    @Contextual @SerialName(value = "codeHash")
    val codeHash: io.ton.walletkit.model.TONHex? = null,

    @Contextual @SerialName(value = "dataHash")
    val dataHash: io.ton.walletkit.model.TONHex? = null,

    /* Whether the NFT contract has been initialized */
    @SerialName(value = "isInited")
    val isInited: kotlin.Boolean? = null,

    /* Whether the NFT is soulbound (non-transferable) */
    @SerialName(value = "isSoulbound")
    val isSoulbound: kotlin.Boolean? = null,

    /* Whether the NFT is currently listed for sale */
    @SerialName(value = "isOnSale")
    val isOnSale: kotlin.Boolean? = null,

    @Contextual @SerialName(value = "ownerAddress")
    val ownerAddress: io.ton.walletkit.model.TONUserFriendlyAddress? = null,

    @Contextual @SerialName(value = "realOwnerAddress")
    val realOwnerAddress: io.ton.walletkit.model.TONUserFriendlyAddress? = null,

    @Contextual @SerialName(value = "saleContractAddress")
    val saleContractAddress: io.ton.walletkit.model.TONUserFriendlyAddress? = null,

    /* Off-chain metadata of the NFT (key-value pairs) */
    @Contextual @SerialName(value = "extra")
    val extra: kotlin.collections.Map<kotlin.String, kotlinx.serialization.json.JsonElement>? = null,

) {

    companion object
}
