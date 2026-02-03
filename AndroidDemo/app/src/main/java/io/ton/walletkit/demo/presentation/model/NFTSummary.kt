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
import io.ton.walletkit.demo.data.api.NftItem

/**
 * UI-friendly NFT summary model for list display.
 * Supports both SDK's TONNFT and native API's NftItem.
 */
data class NFTSummary(
    val address: String,
    val name: String?,
    val description: String?,
    val imageUrl: String?,
    val collectionName: String?,
    val collectionAddress: String?,
) {
    companion object {
        /**
         * Create NFTSummary from SDK's TONNFT.
         */
        fun from(nft: TONNFT): NFTSummary = NFTSummary(
            address = nft.address.value,
            name = nft.info?.name,
            description = nft.info?.description,
            imageUrl = nft.info?.image?.mediumUrl ?: nft.info?.image?.url,
            collectionName = nft.collection?.name,
            collectionAddress = nft.collection?.address?.value,
        )

        /**
         * Create NFTSummary from native API's NftItem.
         */
        fun from(nft: NftItem): NFTSummary {
            // Prefer preview images (smaller, faster to load)
            val imageUrl = nft.previews?.firstOrNull { it.resolution == "500x500" }?.url
                ?: nft.previews?.firstOrNull()?.url
                ?: nft.metadata?.image

            return NFTSummary(
                address = nft.address,
                name = nft.metadata?.name,
                description = nft.metadata?.description,
                imageUrl = imageUrl,
                collectionName = nft.collection?.name,
                collectionAddress = nft.collection?.address,
            )
        }
    }
}
