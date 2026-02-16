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
import io.ton.walletkit.api.TESTNET
import io.ton.walletkit.api.WalletVersions
import io.ton.walletkit.api.generated.TONNetwork
import io.ton.walletkit.api.isTestnet
import io.ton.walletkit.engine.infrastructure.BridgeRpcClient
import io.ton.walletkit.engine.infrastructure.toJSONObject
import io.ton.walletkit.engine.model.WalletAccount
import io.ton.walletkit.engine.operations.requests.MnemonicToKeyPairRequest
import io.ton.walletkit.engine.operations.requests.WalletIdRequest
import io.ton.walletkit.engine.state.SignerManager
import io.ton.walletkit.internal.constants.BridgeMethodConstants
import io.ton.walletkit.internal.constants.LogConstants
import io.ton.walletkit.internal.constants.ResponseConstants
import io.ton.walletkit.internal.util.IDGenerator
import io.ton.walletkit.internal.util.JsonUtils
import io.ton.walletkit.model.TONHex
import io.ton.walletkit.model.TONUserFriendlyAddress
import io.ton.walletkit.model.WalletAdapterInfo
import io.ton.walletkit.model.WalletSigner
import io.ton.walletkit.model.WalletSignerInfo
import kotlinx.serialization.json.Json
import org.json.JSONArray
import org.json.JSONObject

/**
 * Encapsulates wallet lifecycle and account state operations.
 *
 * Signer and adapter state is managed entirely on the Kotlin side.
 * The JS bridge only provides stateless helpers — no JS-side stores.
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
    /** signerId → secret key hex (no 0x prefix). Managed in Kotlin, never sent to JS stores. */
    private val secretKeyStore = mutableMapOf<String, String>()

    /** signerId → public key hex (no 0x prefix). */
    private val publicKeyStore = mutableMapOf<String, String>()

    /** adapterId → adapter metadata created by createV5R1Adapter / createV4R2Adapter. */
    private val internalAdapters = mutableMapOf<String, InternalAdapterInfo>()

    private data class InternalAdapterInfo(
        val adapterId: String,
        val signerId: String,
        val version: String,
        val network: TONNetwork,
        val workchain: Int,
        val walletId: Long,
        val address: TONUserFriendlyAddress,
        val publicKey: String,
        val isCustom: Boolean,
    )

    // ── Signer factory methods (matches iOS signer(mnemonic:) / signer(privateKey:)) ──

    suspend fun createSignerFromMnemonic(
        mnemonic: List<String>,
        mnemonicType: String = "ton",
    ): WalletSignerInfo {
        ensureInitialized()

        val request = MnemonicToKeyPairRequest(mnemonic = mnemonic, mnemonicType = mnemonicType)
        val result = rpcClient.call(BridgeMethodConstants.METHOD_MNEMONIC_TO_KEY_PAIR, json.toJSONObject(request))

        val publicKeyJson = result.opt(ResponseConstants.KEY_PUBLIC_KEY)
            ?: throw WalletKitBridgeException("Missing publicKey in mnemonicToKeyPair response")
        val secretKeyJson = result.opt(ResponseConstants.KEY_SECRET_KEY)
            ?: throw WalletKitBridgeException("Missing secretKey in mnemonicToKeyPair response")

        val publicKeyBytes = JsonUtils.jsonToByteArray(publicKeyJson, "publicKey")
        val secretKeyBytes = JsonUtils.jsonToByteArray(secretKeyJson, "secretKey")

        val signerId = IDGenerator.generateSignerId()
        val publicKeyHex = WalletKitUtils.byteArrayToHexNoPrefix(publicKeyBytes)
        val secretKeyHex = WalletKitUtils.byteArrayToHexNoPrefix(secretKeyBytes)

        secretKeyStore[signerId] = secretKeyHex
        publicKeyStore[signerId] = publicKeyHex

        return WalletSignerInfo(signerId = signerId, publicKey = TONHex(publicKeyHex))
    }

    suspend fun createSignerFromSecretKey(secretKey: ByteArray): WalletSignerInfo {
        ensureInitialized()

        val secretKeyHex = WalletKitUtils.byteArrayToHexNoPrefix(secretKey)
        val request = JSONObject().apply { put("secretKey", secretKeyHex) }
        val result = rpcClient.call(BridgeMethodConstants.METHOD_PUBLIC_KEY_FROM_SECRET_KEY, request)

        val rawPublicKey = result.optString(ResponseConstants.KEY_VALUE, "")
        val publicKeyHex = WalletKitUtils.stripHexPrefix(rawPublicKey)

        val signerId = IDGenerator.generateSignerId()
        secretKeyStore[signerId] = secretKeyHex
        publicKeyStore[signerId] = publicKeyHex

        return WalletSignerInfo(signerId = signerId, publicKey = TONHex(publicKeyHex))
    }

    suspend fun createSignerFromCustom(signer: WalletSigner): WalletSignerInfo {
        ensureInitialized()
        val signerId = signerManager.registerSigner(signer)
        val publicKeyHex = WalletKitUtils.stripHexPrefix(signer.publicKey().value)
        publicKeyStore[signerId] = publicKeyHex
        return WalletSignerInfo(signerId = signerId, publicKey = signer.publicKey())
    }

    // ── Adapter factory methods (matches iOS walletV5R1Adapter / walletV4R2Adapter) ──

    suspend fun createV5R1Adapter(
        signerId: String,
        network: TONNetwork?,
        workchain: Int,
        walletId: Long,
        publicKey: String?,
        isCustom: Boolean,
    ): WalletAdapterInfo = createAdapterInternal(signerId, WalletVersions.V5R1, network, workchain, walletId, publicKey, isCustom)

    suspend fun createV4R2Adapter(
        signerId: String,
        network: TONNetwork?,
        workchain: Int,
        walletId: Long,
        publicKey: String?,
        isCustom: Boolean,
    ): WalletAdapterInfo = createAdapterInternal(signerId, WalletVersions.V4R2, network, workchain, walletId, publicKey, isCustom)

    private suspend fun createAdapterInternal(
        signerId: String,
        walletVersion: String,
        network: TONNetwork?,
        workchain: Int,
        walletId: Long,
        publicKey: String?,
        isCustom: Boolean,
    ): WalletAdapterInfo {
        ensureInitialized()

        val resolvedPublicKey = publicKey
            ?: publicKeyStore[signerId]
            ?: throw WalletKitBridgeException("No public key found for signer: $signerId")

        val resolvedNetwork = network ?: TONNetwork.TESTNET

        // Stateless JS call — computes address without storing anything
        val request = JSONObject().apply {
            put("publicKey", resolvedPublicKey)
            put("version", walletVersion)
            put(
                "network",
                JSONObject().apply {
                    put("chainId", resolvedNetwork.chainId)
                },
            )
            put("workchain", workchain)
            put("walletId", walletId)
        }
        val result = rpcClient.call(BridgeMethodConstants.METHOD_COMPUTE_WALLET_ADDRESS, request)
        val address = result.optString(ResponseConstants.KEY_VALUE, "")

        val adapterId = IDGenerator.generateAdapterId()

        internalAdapters[adapterId] = InternalAdapterInfo(
            adapterId = adapterId,
            signerId = signerId,
            version = walletVersion,
            network = resolvedNetwork,
            workchain = workchain,
            walletId = walletId,
            address = TONUserFriendlyAddress(address),
            publicKey = resolvedPublicKey,
            isCustom = isCustom,
        )

        return WalletAdapterInfo(
            adapterId = adapterId,
            address = TONUserFriendlyAddress(address),
            network = resolvedNetwork,
        )
    }

    // ── Add wallet from stored adapter (3-step factory pattern) ──

    suspend fun addWallet(adapterId: String): WalletAccount {
        ensureInitialized()

        val adapterInfo = internalAdapters.remove(adapterId)
            ?: throw WalletKitBridgeException("No adapter found for ID: $adapterId")

        // Single stateless JS call — creates signer + adapter + adds wallet in one shot
        val request = JSONObject().apply {
            if (adapterInfo.isCustom) {
                put("publicKey", adapterInfo.publicKey)
                put("signerId", adapterInfo.signerId)
                put("isCustom", true)
            } else {
                val secretKey = secretKeyStore[adapterInfo.signerId]
                    ?: throw WalletKitBridgeException("No secret key found for signer: ${adapterInfo.signerId}")
                put("secretKey", secretKey)
            }
            put("version", adapterInfo.version)
            put(
                "network",
                JSONObject().apply {
                    put("chainId", adapterInfo.network.chainId)
                },
            )
            put("workchain", adapterInfo.workchain)
            put("walletId", adapterInfo.walletId)
        }

        val result = rpcClient.call(BridgeMethodConstants.METHOD_ADD_WALLET_WITH_SIGNER, request)

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

    // ── Add wallet from proxy adapter (Tonkeeper pattern) ──

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

        val result = rpcClient.call("addWallet", request)

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

    /**
     * Fetch a list of wallets registered on the bridge.
     */
    suspend fun getWallets(): List<WalletAccount> {
        ensureInitialized()

        val result = rpcClient.call(BridgeMethodConstants.METHOD_GET_WALLETS)

        // JS returns array of { walletId, wallet } objects, wrapped as { items: [...] }
        val items = result.optJSONArray(ResponseConstants.KEY_ITEMS) ?: JSONArray()

        return buildList(items.length()) {
            for (index in 0 until items.length()) {
                val entry = items.optJSONObject(index) ?: continue

                // Get walletId from the wrapper object
                val walletId = entry.optString("walletId").takeIf { it.isNotEmpty() } ?: continue

                // Get wallet properties from the nested wallet object
                val walletObj = entry.optJSONObject("wallet")
                val rawPublicKey = walletObj?.optString("publicKey")?.takeIf { it.isNotEmpty() }
                val publicKey = rawPublicKey?.let { WalletKitUtils.stripHexPrefix(it) }
                val version = walletObj?.optString("version")?.takeIf { it.isNotEmpty() } ?: "unknown"

                // Call getWalletAddress RPC to get current address
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

    /**
     * Get wallet address by calling getAddress() on the JS wallet object.
     * JS returns raw address string, BridgeRpcClient wraps it as { value: "address" }
     */
    suspend fun getWalletAddress(walletId: String): String {
        val request = JSONObject().apply {
            put("walletId", walletId)
        }
        val result = rpcClient.call("getWalletAddress", request)
        // JS returns raw string, wrapped by BridgeRpcClient as { value: "..." }
        return result.optString(ResponseConstants.KEY_VALUE, "")
    }

    /**
     * Get a single wallet by walletId using RPC call.
     *
     * @param walletId Wallet ID
     */
    suspend fun getWallet(walletId: String): WalletAccount? {
        ensureInitialized()

        val request = WalletIdRequest(walletId = walletId)
        val result = rpcClient.call(BridgeMethodConstants.METHOD_GET_WALLET, json.toJSONObject(request))

        // JS returns { walletId, wallet } or null (empty object when null)
        if (result.length() == 0) {
            return null
        }

        // Get walletId from the wrapper object
        val returnedWalletId = result.optString("walletId").takeIf { it.isNotEmpty() } ?: walletId

        // Get wallet properties from the nested wallet object
        val walletObj = result.optJSONObject("wallet")
        val rawPublicKey = walletObj?.optString("publicKey")
        val publicKey = rawPublicKey?.let { WalletKitUtils.stripHexPrefix(it) }
        val version = walletObj?.optString("version")?.takeIf { it.isNotEmpty() } ?: "unknown"

        // Get address via RPC
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

    /**
     * Remove a wallet from the bridge layer.
     *
     * @param walletId Wallet ID
     */
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

    /**
     * Retrieve wallet balance in nanoTON.
     *
     * @param walletId Wallet ID
     */
    suspend fun getBalance(walletId: String): String {
        ensureInitialized()

        val request = WalletIdRequest(walletId = walletId)
        val result = rpcClient.call(BridgeMethodConstants.METHOD_GET_BALANCE, json.toJSONObject(request))

        // JS now returns raw balance value (bigint/number) directly
        return when {
            // Try to get as string first
            result is String -> result
            // If it's a JSONObject, check for balance key (legacy)
            result.has(ResponseConstants.KEY_BALANCE) -> result.optString(ResponseConstants.KEY_BALANCE)
            result.has(ResponseConstants.KEY_VALUE) -> result.optString(ResponseConstants.KEY_VALUE)
            // Try to convert the result itself to string
            else -> result.toString().takeIf { it != "null" && it.isNotEmpty() }
        } ?: "0"
    }

    private fun JSONObject.optNullableString(key: String): String? {
        val value = opt(key)
        return when (value) {
            null, JSONObject.NULL -> null
            else -> value.toString()
        }
    }

    companion object {
        private const val TAG = "${LogConstants.TAG_WEBVIEW_ENGINE}:WalletOps"

        internal const val ERROR_NEW_WALLET_NOT_FOUND = "Failed to retrieve newly added wallet"
        internal const val ERROR_FAILED_REMOVE_WALLET = "Failed to remove wallet: "
    }
}
