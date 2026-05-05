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
import io.ton.walletkit.engine.infrastructure.BridgeRpcClient
import io.ton.walletkit.engine.infrastructure.callTyped
import io.ton.walletkit.engine.operations.requests.CreateTransferJettonRequest
import io.ton.walletkit.engine.operations.requests.CreateTransferNftRawRequest
import io.ton.walletkit.engine.operations.requests.CreateTransferNftRequest
import io.ton.walletkit.engine.operations.requests.GetJettonBalanceRequest
import io.ton.walletkit.engine.operations.requests.GetJettonWalletAddressRequest
import io.ton.walletkit.engine.operations.requests.GetJettonsRequest
import io.ton.walletkit.engine.operations.requests.GetNftRequest
import io.ton.walletkit.engine.operations.requests.GetNftsRequest
import io.ton.walletkit.internal.constants.BridgeMethodConstants
import kotlinx.serialization.json.Json

/**
 * Contains NFT and Jetton related bridge calls such as listing assets and building
 * transfer transactions.
 *
 * @property ensureInitialized Suspended callback to guarantee bridge initialisation.
 * @property rpcClient Bridge RPC transport.
 * @property json Serializer for encoding and decoding bridge payloads.
 *
 * @suppress Internal component used by [WebViewWalletKitEngine].
 */
internal class AssetOperations(
    private val ensureInitialized: suspend () -> Unit,
    private val rpcClient: BridgeRpcClient,
    private val json: Json,
) {

    suspend fun getNfts(walletId: String, limit: Int, offset: Int): TONNFTsResponse {
        ensureInitialized()
        val request = GetNftsRequest(
            walletId = walletId,
            pagination = TONPagination(limit = limit, offset = offset),
        )
        return rpcClient.callTyped(BridgeMethodConstants.METHOD_GET_NFTS, request, json)
    }

    suspend fun getNft(nftAddress: String): TONNFT? {
        ensureInitialized()
        val request = GetNftRequest(address = nftAddress)
        val result = rpcClient.call(BridgeMethodConstants.METHOD_GET_NFT, request)
        return if (result.has("address")) json.decodeFromString(result.toString()) else null
    }

    suspend fun createTransferNftTransaction(
        walletId: String,
        params: TONNFTTransferRequest,
    ): String {
        ensureInitialized()
        val request = CreateTransferNftRequest(
            walletId = walletId,
            nftAddress = params.nftAddress.value,
            transferAmount = params.transferAmount,
            recipientAddress = params.recipientAddress.value,
            comment = params.comment,
        )
        return rpcClient.call(BridgeMethodConstants.METHOD_CREATE_TRANSFER_NFT_TRANSACTION, request).toString()
    }

    suspend fun createTransferNftRawTransaction(
        walletId: String,
        params: TONNFTRawTransferRequest,
    ): String {
        ensureInitialized()
        val request = CreateTransferNftRawRequest(
            walletId = walletId,
            nftAddress = params.nftAddress.value,
            transferAmount = params.transferAmount,
            message = params.message,
        )
        return rpcClient.call(BridgeMethodConstants.METHOD_CREATE_TRANSFER_NFT_RAW_TRANSACTION, request).toString()
    }

    suspend fun getJettons(walletId: String, limit: Int, offset: Int): TONJettonsResponse {
        ensureInitialized()
        val request = GetJettonsRequest(
            walletId = walletId,
            pagination = TONPagination(limit = limit, offset = offset),
        )
        return rpcClient.callTyped(BridgeMethodConstants.METHOD_GET_JETTONS, request, json)
    }

    suspend fun createTransferJettonTransaction(
        walletId: String,
        params: TONJettonsTransferRequest,
    ): String {
        ensureInitialized()
        val request = CreateTransferJettonRequest(
            walletId = walletId,
            recipientAddress = params.recipientAddress.value,
            jettonAddress = params.jettonAddress.value,
            transferAmount = params.transferAmount,
            comment = params.comment,
        )
        return rpcClient.call(BridgeMethodConstants.METHOD_CREATE_TRANSFER_JETTON_TRANSACTION, request).toString()
    }

    suspend fun getJettonBalance(walletId: String, jettonAddress: String): String {
        ensureInitialized()
        val request = GetJettonBalanceRequest(walletId = walletId, jettonAddress = jettonAddress)
        val result = rpcClient.call(BridgeMethodConstants.METHOD_GET_JETTON_BALANCE, request)
        return result.optString("balance", "0")
    }

    suspend fun getJettonWalletAddress(walletId: String, jettonAddress: String): String {
        ensureInitialized()
        val request = GetJettonWalletAddressRequest(walletId = walletId, jettonAddress = jettonAddress)
        val result = rpcClient.call(BridgeMethodConstants.METHOD_GET_JETTON_WALLET_ADDRESS, request)
        return result.optString("jettonWalletAddress", "")
    }
}
