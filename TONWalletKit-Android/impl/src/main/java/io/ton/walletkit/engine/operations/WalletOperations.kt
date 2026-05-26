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

import io.ton.walletkit.WalletKitBridgeException
import io.ton.walletkit.api.generated.TONNetwork
import io.ton.walletkit.api.generated.TONSignatureDomain
import io.ton.walletkit.engine.infrastructure.BridgeRpcClient
import io.ton.walletkit.engine.infrastructure.callTyped
import io.ton.walletkit.engine.infrastructure.callTypedOrNull
import io.ton.walletkit.engine.operations.requests.AdapterIdRequest
import io.ton.walletkit.engine.operations.requests.CreateAdapterRequest
import io.ton.walletkit.engine.operations.requests.CreateSignerFromCustomRequest
import io.ton.walletkit.engine.operations.requests.CreateSignerFromMnemonicRequest
import io.ton.walletkit.engine.operations.requests.CreateSignerFromSecretKeyRequest
import io.ton.walletkit.engine.operations.requests.WalletIdRequest
import io.ton.walletkit.engine.operations.responses.AdapterInfoResponse
import io.ton.walletkit.engine.operations.responses.AddWalletResponse
import io.ton.walletkit.engine.operations.responses.SignerInfoResponse
import io.ton.walletkit.internal.constants.BridgeMethodConstants

internal suspend fun BridgeRpcClient.createSignerFromMnemonic(
    mnemonic: List<String>,
    mnemonicType: String,
): SignerInfoResponse = callTyped(
    BridgeMethodConstants.METHOD_CREATE_SIGNER_FROM_MNEMONIC,
    CreateSignerFromMnemonicRequest(mnemonic = mnemonic, mnemonicType = mnemonicType),
)

internal suspend fun BridgeRpcClient.createSignerFromSecretKey(secretKeyHex: String): SignerInfoResponse =
    callTyped(
        BridgeMethodConstants.METHOD_CREATE_SIGNER_FROM_PRIVATE_KEY,
        CreateSignerFromSecretKeyRequest(secretKey = secretKeyHex),
    )

internal suspend fun BridgeRpcClient.createSignerFromCustom(signerId: String, publicKeyHex: String) {
    send(
        BridgeMethodConstants.METHOD_CREATE_SIGNER_FROM_CUSTOM,
        CreateSignerFromCustomRequest(signerId = signerId, publicKey = publicKeyHex),
    )
}

internal suspend fun BridgeRpcClient.createWalletAdapter(
    version: String,
    signerId: String,
    network: TONNetwork,
    workchain: Int,
    walletId: Long,
    domain: TONSignatureDomain?,
): AdapterInfoResponse {
    val method = when (version) {
        "v5r1" -> BridgeMethodConstants.METHOD_CREATE_V5R1_WALLET_ADAPTER
        "v4r2" -> BridgeMethodConstants.METHOD_CREATE_V4R2_WALLET_ADAPTER
        else -> throw WalletKitBridgeException("Unsupported wallet version: $version")
    }
    return callTyped(
        method,
        CreateAdapterRequest(
            signerId = signerId,
            network = network,
            workchain = workchain,
            walletId = walletId,
            domain = domain,
        ),
    )
}

internal suspend fun BridgeRpcClient.addWallet(adapterId: String): AddWalletResponse =
    callTyped(BridgeMethodConstants.METHOD_ADD_WALLET, AdapterIdRequest(adapterId = adapterId))

internal suspend fun BridgeRpcClient.getWallets(): List<AddWalletResponse> =
    callTyped(BridgeMethodConstants.METHOD_GET_WALLETS)

internal suspend fun BridgeRpcClient.getWallet(walletId: String): AddWalletResponse? =
    callTypedOrNull(BridgeMethodConstants.METHOD_GET_WALLET, WalletIdRequest(walletId = walletId))

internal suspend fun BridgeRpcClient.getWalletAddress(walletId: String): String =
    callTyped(BridgeMethodConstants.METHOD_GET_WALLET_ADDRESS, WalletIdRequest(walletId = walletId))

internal suspend fun BridgeRpcClient.getWalletNetwork(walletId: String): TONNetwork =
    callTyped(BridgeMethodConstants.METHOD_GET_WALLET_NETWORK, WalletIdRequest(walletId = walletId))

internal suspend fun BridgeRpcClient.removeWallet(walletId: String) {
    send(BridgeMethodConstants.METHOD_REMOVE_WALLET, WalletIdRequest(walletId = walletId))
}

internal suspend fun BridgeRpcClient.getBalance(walletId: String): String =
    callTyped(BridgeMethodConstants.METHOD_GET_BALANCE, WalletIdRequest(walletId = walletId))
