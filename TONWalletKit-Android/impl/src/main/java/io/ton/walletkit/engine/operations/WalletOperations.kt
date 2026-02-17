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
import io.ton.walletkit.WalletKitUtils
import io.ton.walletkit.api.generated.TONNetwork
import io.ton.walletkit.api.isTestnet
import io.ton.walletkit.engine.infrastructure.BridgeRpcClient
import io.ton.walletkit.engine.infrastructure.toJSONObject
import io.ton.walletkit.engine.model.WalletAccount
import io.ton.walletkit.engine.operations.requests.WalletIdRequest
import io.ton.walletkit.engine.state.SignerManager
import io.ton.walletkit.internal.constants.BridgeMethodConstants
import io.ton.walletkit.internal.constants.ResponseConstants
import io.ton.walletkit.model.TONHex
import io.ton.walletkit.model.TONUserFriendlyAddress
import io.ton.walletkit.model.WalletAdapterInfo
import io.ton.walletkit.model.WalletSigner
import io.ton.walletkit.model.WalletSignerInfo
import kotlinx.serialization.json.Json
import org.json.JSONArray
import org.json.JSONObject

/**
 * Wallet lifecycle and account state operations.
 *
 * Live JS objects (signers, adapters) are held in a JS-side registry by string ID.
 * Kotlin holds only the IDs.
 *
 * @suppress Internal component used by [WebViewWalletKitEngine].
 */
internal class WalletOperations(
    private val ensureInitialized: suspend () -> Unit,
    private val rpcClient: BridgeRpcClient,
    private val signerManager: SignerManager,
    private val adapterManager: io.ton.walletkit.engine.state.AdapterManager,
    private val currentNetworkProvider: () -> String,
    private val json: Json,
) {

    suspend fun createSignerFromMnemonic(
        mnemonic: List<String>,
        mnemonicType: String = "ton",
    ): WalletSignerInfo {
        ensureInitialized()

        val request = JSONObject().apply {
            put("mnemonic", JSONArray(mnemonic))
            put("mnemonicType", mnemonicType)
        }
        val result = rpcClient.call(BridgeMethodConstants.METHOD_CREATE_SIGNER_FROM_MNEMONIC, request)
        val signerId = result.optString("signerId").takeIf { it.isNotEmpty() }
            ?: throw WalletKitBridgeException("JS did not return signerId")
        val publicKeyHex = WalletKitUtils.stripHexPrefix(result.optString("publicKey", ""))

        return WalletSignerInfo(signerId = signerId, publicKey = TONHex(publicKeyHex))
    }

    suspend fun createSignerFromSecretKey(secretKeyHex: String): WalletSignerInfo {
        ensureInitialized()

        val request = JSONObject().apply { put("secretKey", secretKeyHex) }
        val result = rpcClient.call(BridgeMethodConstants.METHOD_CREATE_SIGNER_FROM_PRIVATE_KEY, request)
        val signerId = result.optString("signerId").takeIf { it.isNotEmpty() }
            ?: throw WalletKitBridgeException("JS did not return signerId")
        val publicKeyHex = WalletKitUtils.stripHexPrefix(result.optString("publicKey", ""))

        return WalletSignerInfo(signerId = signerId, publicKey = TONHex(publicKeyHex))
    }

    suspend fun createSignerFromCustom(signer: WalletSigner): WalletSignerInfo {
        ensureInitialized()

        val signerId = signerManager.registerSigner(signer)
        val publicKeyHex = WalletKitUtils.stripHexPrefix(signer.publicKey().value)

        val request = JSONObject().apply {
            put("signerId", signerId)
            put("publicKey", publicKeyHex)
        }
        rpcClient.call(BridgeMethodConstants.METHOD_CREATE_SIGNER_FROM_CUSTOM, request)

        return WalletSignerInfo(signerId = signerId, publicKey = signer.publicKey())
    }

    suspend fun createAdapter(
        signerId: String,
        version: String,
        network: TONNetwork?,
        workchain: Int,
        walletId: Long,
    ): WalletAdapterInfo {
        ensureInitialized()

        val resolvedNetwork = network ?: TONNetwork(chainId = "-239")

        val method = when (version) {
            "v5r1" -> BridgeMethodConstants.METHOD_CREATE_V5R1_WALLET_ADAPTER
            "v4r2" -> BridgeMethodConstants.METHOD_CREATE_V4R2_WALLET_ADAPTER
            else -> throw WalletKitBridgeException("Unsupported wallet version: $version")
        }

        val request = JSONObject().apply {
            put("signerId", signerId)
            put("network", JSONObject().apply { put("chainId", resolvedNetwork.chainId) })
            put("workchain", workchain)
            put("walletId", walletId)
        }
        val result = rpcClient.call(method, request)
        val adapterId = result.optString("adapterId").takeIf { it.isNotEmpty() }
            ?: throw WalletKitBridgeException("JS did not return adapterId")
        val address = result.optString("address", "")

        return WalletAdapterInfo(
            adapterId = adapterId,
            address = TONUserFriendlyAddress(address),
            network = resolvedNetwork,
        )
    }

    suspend fun addWallet(adapter: WalletAdapterInfo): WalletAccount {
        ensureInitialized()

        val request = JSONObject().apply {
            put("adapterId", adapter.adapterId)
        }
        val result = rpcClient.call(BridgeMethodConstants.METHOD_ADD_WALLET, request)

        val walletId = result.optString("walletId").takeIf { it.isNotEmpty() }
            ?: throw WalletKitBridgeException(ERROR_NEW_WALLET_NOT_FOUND)

        val walletObj = result.optJSONObject("wallet")
        val rawPublicKey = walletObj?.optString("publicKey") ?: ""
        val pubKey = WalletKitUtils.stripHexPrefix(rawPublicKey)
        val version = walletObj?.optString("version")?.takeIf { it.isNotEmpty() } ?: "unknown"
        val address = getWalletAddress(walletId)

        return WalletAccount(
            walletId = walletId,
            address = TONUserFriendlyAddress(address),
            publicKey = pubKey.takeIf { it.isNotEmpty() },
            version = version,
        )
    }

    suspend fun addWallet(adapter: io.ton.walletkit.model.TONWalletAdapter): WalletAccount {
        ensureInitialized()

        val adapterId = adapterManager.registerAdapter(adapter)

        val network = adapter.network()
        val request = JSONObject().apply {
            put("adapterId", adapterId)
            put("walletId", adapter.identifier())
            put("publicKey", adapter.publicKey().value)
            put(
                "network",
                JSONObject().apply {
                    put("chainId", network.chainId)
                },
            )
            put("address", adapter.address(network.isTestnet).value)
        }

        val result = rpcClient.call(BridgeMethodConstants.METHOD_ADD_WALLET, request)

        val returnedWalletId = result.optString("walletId").takeIf { it.isNotEmpty() }
            ?: throw WalletKitBridgeException(ERROR_NEW_WALLET_NOT_FOUND)

        val walletObj = result.optJSONObject("wallet")
        val rawPublicKey = walletObj?.optString("publicKey") ?: ""
        val pubKey = WalletKitUtils.stripHexPrefix(rawPublicKey)
        val version = walletObj?.optString("version")?.takeIf { it.isNotEmpty() } ?: "unknown"

        val address = getWalletAddress(returnedWalletId)

        return WalletAccount(
            walletId = returnedWalletId,
            address = TONUserFriendlyAddress(address),
            publicKey = pubKey.takeIf { it.isNotEmpty() },
            version = version,
        )
    }

    suspend fun getWallets(): List<WalletAccount> {
        ensureInitialized()

        val result = rpcClient.call(BridgeMethodConstants.METHOD_GET_WALLETS)
        val items = result.optJSONArray(ResponseConstants.KEY_ITEMS) ?: JSONArray()

        return buildList(items.length()) {
            for (index in 0 until items.length()) {
                val entry = items.optJSONObject(index) ?: continue
                val walletId = entry.optString("walletId").takeIf { it.isNotEmpty() } ?: continue
                val walletObj = entry.optJSONObject("wallet")
                val rawPublicKey = walletObj?.optString("publicKey")?.takeIf { it.isNotEmpty() }
                val publicKey = rawPublicKey?.let { WalletKitUtils.stripHexPrefix(it) }
                val version = walletObj?.optString("version")?.takeIf { it.isNotEmpty() } ?: "unknown"

                val address = getWalletAddress(walletId)

                add(
                    WalletAccount(
                        walletId = walletId,
                        address = TONUserFriendlyAddress(address),
                        publicKey = publicKey,
                        version = version,
                    ),
                )
            }
        }
    }

    suspend fun getWalletAddress(walletId: String): String {
        val request = JSONObject().apply {
            put("walletId", walletId)
        }
        val result = rpcClient.call("getWalletAddress", request)
        return result.optString(ResponseConstants.KEY_VALUE, "")
    }

    suspend fun getWallet(walletId: String): WalletAccount? {
        ensureInitialized()

        val request = WalletIdRequest(walletId = walletId)
        val result = rpcClient.call(BridgeMethodConstants.METHOD_GET_WALLET, json.toJSONObject(request))
        if (result.length() == 0) return null

        val returnedWalletId = result.optString("walletId").takeIf { it.isNotEmpty() } ?: walletId
        val walletObj = result.optJSONObject("wallet")
        val rawPublicKey = walletObj?.optString("publicKey")
        val publicKey = rawPublicKey?.let { WalletKitUtils.stripHexPrefix(it) }
        val version = walletObj?.optString("version")?.takeIf { it.isNotEmpty() } ?: "unknown"

        val address = getWalletAddress(returnedWalletId)
        if (address.isEmpty()) {
            return null
        }

        return WalletAccount(
            walletId = returnedWalletId,
            address = TONUserFriendlyAddress(address),
            publicKey = publicKey,
            version = version,
        )
    }

    suspend fun removeWallet(walletId: String) {
        ensureInitialized()

        val request = WalletIdRequest(walletId = walletId)
        val result = rpcClient.call(BridgeMethodConstants.METHOD_REMOVE_WALLET, json.toJSONObject(request))

        val removed =
            when {
                result.has(ResponseConstants.KEY_REMOVED) -> result.optBoolean(ResponseConstants.KEY_REMOVED, false)
                result.has(ResponseConstants.KEY_OK) -> result.optBoolean(ResponseConstants.KEY_OK, true)
                result.has(ResponseConstants.KEY_VALUE) -> result.optBoolean(ResponseConstants.KEY_VALUE, true)
                else -> true
            }

        if (!removed) {
            throw WalletKitBridgeException(ERROR_FAILED_REMOVE_WALLET + walletId)
        }
    }

    suspend fun getBalance(walletId: String): String {
        ensureInitialized()

        val request = WalletIdRequest(walletId = walletId)
        val result = rpcClient.call(BridgeMethodConstants.METHOD_GET_BALANCE, json.toJSONObject(request))

        return when {
            result is String -> result
            result.has(ResponseConstants.KEY_BALANCE) -> result.optString(ResponseConstants.KEY_BALANCE)
            result.has(ResponseConstants.KEY_VALUE) -> result.optString(ResponseConstants.KEY_VALUE)
            else -> result.toString().takeIf { it != "null" && it.isNotEmpty() }
        } ?: "0"
    }

    companion object {
        internal const val ERROR_NEW_WALLET_NOT_FOUND = "Failed to retrieve newly added wallet"
        internal const val ERROR_FAILED_REMOVE_WALLET = "Failed to remove wallet: "
    }
}
