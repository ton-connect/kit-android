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
package io.ton.walletkit.engine.operations

import io.ton.walletkit.api.generated.TONJettonsResponse
import io.ton.walletkit.api.generated.TONJettonsTransferRequest
import io.ton.walletkit.api.generated.TONNFT
import io.ton.walletkit.api.generated.TONNFTRawTransferRequest
import io.ton.walletkit.api.generated.TONNFTTransferRequest
import io.ton.walletkit.api.generated.TONNFTsResponse
import io.ton.walletkit.api.generated.TONPagination
import io.ton.walletkit.api.generated.TONTransactionRequest
import io.ton.walletkit.engine.infrastructure.BridgeRpcClient
import io.ton.walletkit.engine.infrastructure.callTyped
import io.ton.walletkit.engine.infrastructure.callTypedOrNull
import io.ton.walletkit.engine.operations.requests.CreateTransferJettonRequest
import io.ton.walletkit.engine.operations.requests.CreateTransferNftRawRequest
import io.ton.walletkit.engine.operations.requests.CreateTransferNftRequest
import io.ton.walletkit.engine.operations.requests.GetJettonBalanceRequest
import io.ton.walletkit.engine.operations.requests.GetJettonWalletAddressRequest
import io.ton.walletkit.engine.operations.requests.GetJettonsRequest
import io.ton.walletkit.engine.operations.requests.GetNftRequest
import io.ton.walletkit.engine.operations.requests.GetNftsRequest
import io.ton.walletkit.internal.constants.BridgeMethodConstants

internal suspend fun BridgeRpcClient.getNfts(walletId: String, limit: Int, offset: Int): TONNFTsResponse =
    callTyped(
        BridgeMethodConstants.METHOD_GET_NFTS,
        GetNftsRequest(walletId, TONPagination(limit = limit, offset = offset)),
    )

/** JS returns either a TONNFT or null; runCatching also guards against partial/malformed objects. */
internal suspend fun BridgeRpcClient.getNft(nftAddress: String): TONNFT? = runCatching {
    callTypedOrNull<TONNFT>(BridgeMethodConstants.METHOD_GET_NFT, GetNftRequest(address = nftAddress))
}.getOrNull()

internal suspend fun BridgeRpcClient.createTransferNftTransaction(
    walletId: String,
    params: TONNFTTransferRequest,
): TONTransactionRequest = callTyped(
    BridgeMethodConstants.METHOD_CREATE_TRANSFER_NFT_TRANSACTION,
    CreateTransferNftRequest(
        walletId = walletId,
        nftAddress = params.nftAddress.value,
        transferAmount = params.transferAmount,
        recipientAddress = params.recipientAddress.value,
        comment = params.comment,
    ),
)

internal suspend fun BridgeRpcClient.createTransferNftRawTransaction(
    walletId: String,
    params: TONNFTRawTransferRequest,
): TONTransactionRequest = callTyped(
    BridgeMethodConstants.METHOD_CREATE_TRANSFER_NFT_RAW_TRANSACTION,
    CreateTransferNftRawRequest(
        walletId = walletId,
        nftAddress = params.nftAddress.value,
        transferAmount = params.transferAmount,
        message = params.message,
    ),
)

internal suspend fun BridgeRpcClient.getJettons(walletId: String, limit: Int, offset: Int): TONJettonsResponse =
    callTyped(
        BridgeMethodConstants.METHOD_GET_JETTONS,
        GetJettonsRequest(walletId, TONPagination(limit = limit, offset = offset)),
    )

internal suspend fun BridgeRpcClient.createTransferJettonTransaction(
    walletId: String,
    params: TONJettonsTransferRequest,
): TONTransactionRequest = callTyped(
    BridgeMethodConstants.METHOD_CREATE_TRANSFER_JETTON_TRANSACTION,
    CreateTransferJettonRequest(
        walletId = walletId,
        recipientAddress = params.recipientAddress.value,
        jettonAddress = params.jettonAddress.value,
        transferAmount = params.transferAmount,
        comment = params.comment,
    ),
)

internal suspend fun BridgeRpcClient.getJettonBalance(walletId: String, jettonAddress: String): String =
    callTyped(
        BridgeMethodConstants.METHOD_GET_JETTON_BALANCE,
        GetJettonBalanceRequest(walletId, jettonAddress),
    )

internal suspend fun BridgeRpcClient.getJettonWalletAddress(walletId: String, jettonAddress: String): String =
    callTyped(
        BridgeMethodConstants.METHOD_GET_JETTON_WALLET_ADDRESS,
        GetJettonWalletAddressRequest(walletId, jettonAddress),
    )
