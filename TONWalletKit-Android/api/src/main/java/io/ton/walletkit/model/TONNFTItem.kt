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
package io.ton.walletkit.model

import kotlinx.serialization.Serializable

/**
 * NFT item (token) information.
 *
 * @property address NFT contract address (user-friendly format: UQ... or EQ...)
 * @property auctionContractAddress Auction contract address (user-friendly format, if NFT is on auction)
 * @property codeHash Code hash of the NFT contract (hex string with 0x prefix)
 * @property dataHash Data hash of the NFT contract (hex string with 0x prefix)
 * @property collection Collection information (if part of a collection)
 * @property collectionAddress Collection contract address (user-friendly format)
 * @property metadata Token metadata (name, image, description, etc.)
 * @property index NFT index in the collection
 * @property initFlag Whether the NFT contract is initialized
 * @property lastTransactionLt Last transaction logical time
 * @property onSale Whether the NFT is currently on sale
 * @property ownerAddress Current owner address (user-friendly format)
 * @property realOwner Real owner address (user-friendly format, may differ from ownerAddress for sale contracts)
 * @property saleContractAddress Sale contract address (user-friendly format, if NFT is on sale)
 */
@Serializable
data class TONNFTItem(
    /** NFT contract address (user-friendly format: UQ... or EQ...) */
    val address: String,
    /** Auction contract address (user-friendly format) */
    val auctionContractAddress: String? = null,
    /** Code hash of the NFT contract (hex string with 0x prefix) */
    val codeHash: String? = null,
    /** Data hash of the NFT contract (hex string with 0x prefix) */
    val dataHash: String? = null,
    val collection: TONNFTCollection? = null,
    /** Collection contract address (user-friendly format) */
    val collectionAddress: String? = null,
    val metadata: TONTokenInfo? = null,
    val index: String? = null,
    val initFlag: Boolean? = null,
    val lastTransactionLt: String? = null,
    val onSale: Boolean? = null,
    /** Current owner address (user-friendly format) */
    val ownerAddress: String? = null,
    /** Real owner address (user-friendly format) */
    val realOwner: String? = null,
    /** Sale contract address (user-friendly format) */
    val saleContractAddress: String? = null,
)
