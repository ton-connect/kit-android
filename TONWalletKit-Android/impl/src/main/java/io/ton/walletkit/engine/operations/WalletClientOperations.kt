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

import io.ton.walletkit.api.generated.TONAccountState
import io.ton.walletkit.api.generated.TONEmulationResult
import io.ton.walletkit.api.generated.TONGetMethodResult
import io.ton.walletkit.api.generated.TONMasterchainInfo
import io.ton.walletkit.api.generated.TONNFTsRequest
import io.ton.walletkit.api.generated.TONNFTsResponse
import io.ton.walletkit.api.generated.TONRawStackItem
import io.ton.walletkit.api.generated.TONUserNFTsRequest
import io.ton.walletkit.engine.infrastructure.BridgeRpcClient
import io.ton.walletkit.engine.infrastructure.callTyped
import io.ton.walletkit.internal.constants.BridgeMethodConstants
import kotlinx.serialization.Serializable

@Serializable
internal data class WalletClientByIdRequest(val walletId: String)

@Serializable
internal data class WalletClientSendBocRequest(
    val walletId: String,
    val boc: String,
)

@Serializable
internal data class WalletClientRunGetMethodRequest(
    val walletId: String,
    val address: String,
    val method: String,
    val stack: List<TONRawStackItem>? = null,
    val seqno: UInt? = null,
)

@Serializable
internal data class WalletClientGetBalanceRequest(
    val walletId: String,
    val address: String,
    val seqno: UInt? = null,
)

@Serializable
internal data class WalletClientNftItemsByAddressRequest(
    val walletId: String,
    val request: TONNFTsRequest,
)

@Serializable
internal data class WalletClientNftItemsByOwnerRequest(
    val walletId: String,
    val request: TONUserNFTsRequest,
)

@Serializable
internal data class WalletClientFetchEmulationRequest(
    val walletId: String,
    val messageBoc: String,
    val ignoreSignature: Boolean = false,
)

@Serializable
internal data class WalletClientAccountStateRequest(
    val walletId: String,
    val address: String,
    val seqno: UInt? = null,
)

@Serializable
internal data class WalletClientAccountStatesRequest(
    val walletId: String,
    val addresses: List<String>,
)

@Serializable
internal data class WalletClientResolveDnsWalletRequest(
    val walletId: String,
    val domain: String,
)

@Serializable
internal data class WalletClientBackResolveDnsWalletRequest(
    val walletId: String,
    val address: String,
)

@Serializable
internal data class WalletClientResolveDnsWalletResponse(val result: String? = null)

@Serializable
internal data class WalletClientSendBocResponse(val result: String)

internal suspend fun BridgeRpcClient.walletClientSendBoc(walletId: String, boc: String): String {
    val response: WalletClientSendBocResponse = callTyped(
        BridgeMethodConstants.METHOD_WALLET_CLIENT_SEND_BOC,
        WalletClientSendBocRequest(walletId = walletId, boc = boc),
    )
    return response.result
}

internal suspend fun BridgeRpcClient.walletClientRunGetMethod(
    walletId: String,
    address: String,
    method: String,
    stack: List<TONRawStackItem>?,
    seqno: UInt?,
): TONGetMethodResult = callTyped(
    BridgeMethodConstants.METHOD_WALLET_CLIENT_RUN_GET_METHOD,
    WalletClientRunGetMethodRequest(
        walletId = walletId,
        address = address,
        method = method,
        stack = stack,
        seqno = seqno,
    ),
)

internal suspend fun BridgeRpcClient.walletClientGetBalance(
    walletId: String,
    address: String,
    seqno: UInt?,
): String {
    val response: WalletClientSendBocResponse = callTyped(
        BridgeMethodConstants.METHOD_WALLET_CLIENT_GET_BALANCE,
        WalletClientGetBalanceRequest(walletId = walletId, address = address, seqno = seqno),
    )
    return response.result
}

internal suspend fun BridgeRpcClient.walletClientGetMasterchainInfo(walletId: String): TONMasterchainInfo =
    callTyped(
        BridgeMethodConstants.METHOD_WALLET_CLIENT_GET_MASTERCHAIN_INFO,
        WalletClientByIdRequest(walletId = walletId),
    )

internal suspend fun BridgeRpcClient.walletClientNftItemsByAddress(
    walletId: String,
    request: TONNFTsRequest,
): TONNFTsResponse = callTyped(
    BridgeMethodConstants.METHOD_WALLET_CLIENT_NFT_ITEMS_BY_ADDRESS,
    WalletClientNftItemsByAddressRequest(walletId = walletId, request = request),
)

internal suspend fun BridgeRpcClient.walletClientNftItemsByOwner(
    walletId: String,
    request: TONUserNFTsRequest,
): TONNFTsResponse = callTyped(
    BridgeMethodConstants.METHOD_WALLET_CLIENT_NFT_ITEMS_BY_OWNER,
    WalletClientNftItemsByOwnerRequest(walletId = walletId, request = request),
)

internal suspend fun BridgeRpcClient.walletClientFetchEmulation(
    walletId: String,
    messageBoc: String,
    ignoreSignature: Boolean,
): TONEmulationResult = callTyped(
    BridgeMethodConstants.METHOD_WALLET_CLIENT_FETCH_EMULATION,
    WalletClientFetchEmulationRequest(walletId = walletId, messageBoc = messageBoc, ignoreSignature = ignoreSignature),
)

internal suspend fun BridgeRpcClient.walletClientAccountState(
    walletId: String,
    address: String,
    seqno: UInt?,
): TONAccountState = callTyped(
    BridgeMethodConstants.METHOD_WALLET_CLIENT_ACCOUNT_STATE,
    WalletClientAccountStateRequest(walletId = walletId, address = address, seqno = seqno),
)

internal suspend fun BridgeRpcClient.walletClientAccountStates(
    walletId: String,
    addresses: List<String>,
): Map<String, TONAccountState> = callTyped(
    BridgeMethodConstants.METHOD_WALLET_CLIENT_ACCOUNT_STATES,
    WalletClientAccountStatesRequest(walletId = walletId, addresses = addresses),
)

internal suspend fun BridgeRpcClient.walletClientResolveDnsWallet(
    walletId: String,
    domain: String,
): String? {
    val response: WalletClientResolveDnsWalletResponse = callTyped(
        BridgeMethodConstants.METHOD_WALLET_CLIENT_RESOLVE_DNS_WALLET,
        WalletClientResolveDnsWalletRequest(walletId = walletId, domain = domain),
    )
    return response.result
}

internal suspend fun BridgeRpcClient.walletClientBackResolveDnsWallet(
    walletId: String,
    address: String,
): String? {
    val response: WalletClientResolveDnsWalletResponse = callTyped(
        BridgeMethodConstants.METHOD_WALLET_CLIENT_BACK_RESOLVE_DNS_WALLET,
        WalletClientBackResolveDnsWalletRequest(walletId = walletId, address = address),
    )
    return response.result
}
