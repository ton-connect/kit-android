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

import io.ton.walletkit.engine.infrastructure.BridgeRpcClient
import io.ton.walletkit.engine.infrastructure.toJSONObject
import io.ton.walletkit.engine.operations.requests.CreateTransferJettonRequest
import io.ton.walletkit.engine.operations.requests.CreateTransferNftRawRequest
import io.ton.walletkit.engine.operations.requests.CreateTransferNftRequest
import io.ton.walletkit.engine.operations.requests.GetJettonBalanceRequest
import io.ton.walletkit.engine.operations.requests.GetJettonWalletAddressRequest
import io.ton.walletkit.engine.operations.requests.GetJettonsRequest
import io.ton.walletkit.engine.operations.requests.GetNftRequest
import io.ton.walletkit.engine.operations.requests.GetNftsRequest
import io.ton.walletkit.internal.constants.BridgeMethodConstants
import io.ton.walletkit.model.TONJettonTransferParams
import io.ton.walletkit.model.TONJettonWallets
import io.ton.walletkit.model.TONNFTItem
import io.ton.walletkit.model.TONNFTItems
import io.ton.walletkit.model.TONNFTTransferParamsHuman
import io.ton.walletkit.model.TONNFTTransferParamsRaw
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

    suspend fun getNfts(walletAddress: String, limit: Int, offset: Int): TONNFTItems {
        ensureInitialized()

        val request = GetNftsRequest(address = walletAddress, limit = limit, offset = offset)
        val result = rpcClient.call(BridgeMethodConstants.METHOD_GET_NFTS, json.toJSONObject(request))
        return json.decodeFromString(result.toString())
    }

    suspend fun getNft(nftAddress: String): TONNFTItem? {
        ensureInitialized()

        val request = GetNftRequest(address = nftAddress)
        val result = rpcClient.call(BridgeMethodConstants.METHOD_GET_NFT, json.toJSONObject(request))
        return if (result.has("address")) {
            json.decodeFromString(result.toString())
        } else {
            null
        }
    }

    suspend fun createTransferNftTransaction(
        walletAddress: String,
        params: TONNFTTransferParamsHuman,
    ): String {
        ensureInitialized()

        val request = CreateTransferNftRequest(
            address = walletAddress,
            nftAddress = params.nftAddress,
            transferAmount = params.transferAmount,
            toAddress = params.toAddress,
            comment = params.comment,
        )
        val result = rpcClient.call(BridgeMethodConstants.METHOD_CREATE_TRANSFER_NFT_TRANSACTION, json.toJSONObject(request))
        return result.toString()
    }

    suspend fun createTransferNftRawTransaction(
        walletAddress: String,
        params: TONNFTTransferParamsRaw,
    ): String {
        ensureInitialized()

        val request = CreateTransferNftRawRequest(
            address = walletAddress,
            nftAddress = params.nftAddress,
            transferAmount = params.transferAmount,
            transferMessage = params.transferMessage,
        )
        val result = rpcClient.call(BridgeMethodConstants.METHOD_CREATE_TRANSFER_NFT_RAW_TRANSACTION, json.toJSONObject(request))
        return result.toString()
    }

    suspend fun getJettons(walletAddress: String, limit: Int, offset: Int): TONJettonWallets {
        ensureInitialized()

        val request = GetJettonsRequest(address = walletAddress, limit = limit, offset = offset)
        val result = rpcClient.call(BridgeMethodConstants.METHOD_GET_JETTONS, json.toJSONObject(request))
        return json.decodeFromString(result.toString())
    }

    suspend fun createTransferJettonTransaction(
        walletAddress: String,
        params: TONJettonTransferParams,
    ): String {
        ensureInitialized()

        val request = CreateTransferJettonRequest(
            address = walletAddress,
            toAddress = params.toAddress,
            jettonAddress = params.jettonAddress,
            amount = params.amount,
            comment = params.comment,
        )
        val result = rpcClient.call(BridgeMethodConstants.METHOD_CREATE_TRANSFER_JETTON_TRANSACTION, json.toJSONObject(request))
        return result.toString()
    }

    suspend fun getJettonBalance(walletAddress: String, jettonAddress: String): String {
        ensureInitialized()

        val request = GetJettonBalanceRequest(address = walletAddress, jettonAddress = jettonAddress)
        val result = rpcClient.call(BridgeMethodConstants.METHOD_GET_JETTON_BALANCE, json.toJSONObject(request))
        return result.optString("balance", "0")
    }

    suspend fun getJettonWalletAddress(walletAddress: String, jettonAddress: String): String {
        ensureInitialized()

        val request = GetJettonWalletAddressRequest(address = walletAddress, jettonAddress = jettonAddress)
        val result = rpcClient.call(BridgeMethodConstants.METHOD_GET_JETTON_WALLET_ADDRESS, json.toJSONObject(request))
        return result.optString("jettonWalletAddress", "")
    }
}
