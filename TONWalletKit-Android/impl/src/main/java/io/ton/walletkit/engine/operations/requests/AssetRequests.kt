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
package io.ton.walletkit.engine.operations.requests

import kotlinx.serialization.Serializable

/**
 * Internal bridge request models for asset operations (NFTs and Jettons).
 * These DTOs represent the exact JSON structure sent to the JavaScript bridge.
 *
 * @suppress Internal bridge communication only.
 */

@Serializable
internal data class GetNftsRequest(
    val address: String,
    val limit: Int,
    val offset: Int,
)

@Serializable
internal data class GetNftRequest(
    val address: String,
)

@Serializable
internal data class CreateTransferNftRequest(
    val address: String,
    val nftAddress: String,
    val transferAmount: String,
    val toAddress: String,
    val comment: String? = null,
)

@Serializable
internal data class CreateTransferNftRawRequest(
    val address: String,
    val nftAddress: String,
    val transferAmount: String,
    val transferMessage: TONNFTTransferMessageDTO,
)

// Re-use public model
typealias TONNFTTransferMessageDTO = io.ton.walletkit.model.TONNFTTransferMessageDTO

@Serializable
internal data class GetJettonsRequest(
    val address: String,
    val limit: Int,
    val offset: Int,
)

@Serializable
internal data class CreateTransferJettonRequest(
    val address: String,
    val toAddress: String,
    val jettonAddress: String,
    val amount: String,
    val comment: String? = null,
)

@Serializable
internal data class GetJettonBalanceRequest(
    val address: String,
    val jettonAddress: String,
)

@Serializable
internal data class GetJettonWalletAddressRequest(
    val address: String,
    val jettonAddress: String,
)
