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
package io.ton.walletkit.demo.presentation.model

import io.ton.walletkit.api.generated.TONNFT

/**
 * UI-friendly NFT details model.
 *
 * Maps from [TONNFT] SDK model or [NFTSummary] to presentation layer.
 * Mirrors iOS WalletNFTDetails structure for cross-platform consistency.
 */
data class NFTDetails(
    val name: String,
    val description: String?,
    val imageUrl: String?,
    val collectionName: String?,
    val index: String?,
    val status: String?,
    val contractAddress: String,
    val ownerAddress: String?,
    val canTransfer: Boolean,
) {
    companion object {
        /**
         * Create NFTDetails from SDK's TONNFT.
         *
         * @param nft NFT from SDK
         * @return UI-friendly NFT details
         */
        fun from(nft: TONNFT): NFTDetails {
            val info = nft.info
            val name = info?.name ?: "Unknown NFT"
            val description = info?.description

            // Try to get medium image, fallback to standard image
            val imageUrl = info?.image?.mediumUrl ?: info?.image?.url

            // Collection name from collection object
            val collectionName = nft.collection?.name
                ?: nft.collection?.address?.value?.take(8)?.let { "\$it..." }

            val index = nft.index

            val status = if (nft.isOnSale == true) "On Sale" else "Not on Sale"

            val contractAddress = nft.address.value

            val ownerAddress = nft.realOwnerAddress?.value ?: nft.ownerAddress?.value

            // Can only transfer if not on sale and has owner
            val canTransfer = nft.isOnSale != true && nft.ownerAddress != null

            return NFTDetails(
                name = name,
                description = description,
                imageUrl = imageUrl,
                collectionName = collectionName,
                index = index,
                status = status,
                contractAddress = contractAddress,
                ownerAddress = ownerAddress,
                canTransfer = canTransfer,
            )
        }

        /**
         * Create NFTDetails from NFTSummary (native API model).
         *
         * @param summary NFT summary from native API
         * @return UI-friendly NFT details
         */
        fun from(summary: NFTSummary): NFTDetails = NFTDetails(
            name = summary.name ?: "Unknown NFT",
            description = summary.description,
            imageUrl = summary.imageUrl,
            collectionName = summary.collectionName
                ?: summary.collectionAddress?.take(8)?.let { "$it..." },
            index = null, // Not available from native API
            status = null, // Not available from native API
            contractAddress = summary.address,
            ownerAddress = null, // Not tracked in summary
            canTransfer = true, // Assume transferable since we don't have sale status
        )
    }
}
