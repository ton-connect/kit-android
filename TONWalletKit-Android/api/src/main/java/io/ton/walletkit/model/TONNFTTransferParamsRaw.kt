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
 * Low-level NFT transfer message.
 *
 * @property queryId Query ID for the transfer
 * @property newOwner New owner address
 * @property responseDestination Response destination address (for excess funds)
 * @property customPayload Custom payload (optional)
 * @property forwardAmount Amount to forward to the new owner
 * @property forwardPayload Forward payload (optional)
 */
@Serializable
data class TONNFTTransferMessageDTO(
    val queryId: String,
    val newOwner: String,
    val responseDestination: String? = null,
    val customPayload: String? = null,
    val forwardAmount: String,
    val forwardPayload: String? = null,
)

/**
 * Raw (advanced) NFT transfer parameters.
 *
 * Matches the shared TONNFTTransferParamsRaw structure for cross-platform consistency.
 *
 * @property nftAddress NFT contract address to transfer
 * @property transferAmount Amount of TON to attach to the transfer (for gas fees)
 * @property transferMessage Detailed transfer message with all parameters
 */
@Serializable
data class TONNFTTransferParamsRaw(
    val nftAddress: String,
    val transferAmount: String,
    val transferMessage: TONNFTTransferMessageDTO,
)
