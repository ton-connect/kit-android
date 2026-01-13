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
import io.ton.walletkit.engine.infrastructure.BridgeRpcClient
import io.ton.walletkit.engine.infrastructure.toJSONObject
import io.ton.walletkit.engine.model.WalletAccount
import io.ton.walletkit.engine.operations.requests.AddWalletRequest
import io.ton.walletkit.engine.operations.requests.CreateAdapterRequest
import io.ton.walletkit.engine.operations.requests.CreateSignerRequest
import io.ton.walletkit.engine.operations.requests.WalletIdRequest
import io.ton.walletkit.engine.state.SignerManager
import io.ton.walletkit.internal.constants.BridgeMethodConstants
import io.ton.walletkit.internal.constants.LogConstants
import io.ton.walletkit.internal.constants.ResponseConstants
import io.ton.walletkit.internal.util.IDGenerator
import io.ton.walletkit.internal.util.Logger
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
 * Methods here mirror the behaviour of the previous monolithic engine including logging,
 * error contracts, and result handling.
 *
 * @property ensureInitialized Suspended lambda that ensures bridge initialisation.
 * @property rpcClient Bridge RPC client wrapper.
 * @property signerManager Tracks wallet signers to maintain bridge affinity.
 * @property currentNetworkProvider Provides the latest network identifier for defaulting fields.
 *
 * @suppress Internal component used by [WebViewWalletKitEngine].
 */
internal class WalletOperations(
    private val ensureInitialized: suspend () -> Unit,
    private val rpcClient: BridgeRpcClient,
    private val signerManager: SignerManager,
    private val currentNetworkProvider: () -> String,
    private val json: Json,
) {
    // Store adapter info by adapterId so we can compute walletId in addWallet
    private val adapterStore = mutableMapOf<String, WalletAdapterInfo>()

    /**
     * Create a signer from mnemonic phrase.
     * Step 1 of the wallet creation pattern from JS docs.
     *
     * Example:
     * ```
     * val signer = walletOperations.createSignerFromMnemonic(mnemonic)
     * ```
     */

    /**
     * Create a signer from mnemonic phrase.
     * Step 1 of the wallet creation pattern from JS docs.
     *
     * This is the Android equivalent of JS WalletKit's `Signer.fromMnemonic()`.
     *
     * Example:
     * ```
     * val signer = walletOperations.createSignerFromMnemonic(mnemonic)
     * ```
     */
    suspend fun createSignerFromMnemonic(
        mnemonic: List<String>,
        mnemonicType: String = "ton",
    ): WalletSignerInfo {
        ensureInitialized()

        val request = CreateSignerRequest(mnemonic = mnemonic, mnemonicType = mnemonicType)
        val result = rpcClient.call(BridgeMethodConstants.METHOD_CREATE_SIGNER, json.toJSONObject(request))

        // JS now returns { _tempId, signer } - extract signer object
        val tempId = result.optString("_tempId")
        val signerObj = result.optJSONObject("signer") ?: result

        // Generate signerId in Kotlin
        val signerId = tempId.takeIf { it.isNotEmpty() } ?: IDGenerator.generateSignerId()

        // Extract publicKey from signer object
        val rawPublicKey = signerObj.optString("publicKey")
        val publicKey = TONHex(WalletKitUtils.stripHexPrefix(rawPublicKey))

        return WalletSignerInfo(
            signerId = signerId,
            publicKey = publicKey,
        )
    }

    /**
     * Create a signer from a secret key.
     * Step 1 of the wallet creation pattern from JS docs.
     *
     * This is the Android equivalent of JS WalletKit's `Signer.fromPrivateKey()`.
     *
     * Example:
     * ```
     * val signer = walletOperations.createSignerFromSecretKey(secretKey)
     * ```
     */
    suspend fun createSignerFromSecretKey(secretKey: ByteArray): WalletSignerInfo {
        ensureInitialized()

        val request = CreateSignerRequest(secretKey = WalletKitUtils.byteArrayToHexNoPrefix(secretKey))
        val result = rpcClient.call(BridgeMethodConstants.METHOD_CREATE_SIGNER, json.toJSONObject(request))

        // JS now returns { _tempId, signer } - extract signer object
        val tempId = result.optString("_tempId")
        val signerObj = result.optJSONObject("signer") ?: result

        // Generate signerId in Kotlin
        val signerId = tempId.takeIf { it.isNotEmpty() } ?: IDGenerator.generateSignerId()

        // Extract publicKey from signer object
        val rawPublicKey = signerObj.optString("publicKey")
        val publicKey = TONHex(WalletKitUtils.stripHexPrefix(rawPublicKey))

        return WalletSignerInfo(
            signerId = signerId,
            publicKey = publicKey,
        )
    }

    /**
     * Create a signer from a custom WalletSigner implementation.
     * Step 1 of the wallet creation pattern, enabling hardware wallet integration.
     *
     * This is the Android equivalent of JS WalletKit's `Signer.custom()`.
     *
     * Example:
     * ```
     * val customSigner = object : WalletSigner {
     *     override val publicKey = "0x..."
     *     override suspend fun sign(data: ByteArray) = hardwareDevice.sign(data)
     * }
     * val signer = walletOperations.createSignerFromCustom(customSigner)
     * ```
     */
    suspend fun createSignerFromCustom(signer: WalletSigner): WalletSignerInfo {
        ensureInitialized()

        // Register the custom signer in the SignerManager to handle sign requests from JavaScript
        val signerId = signerManager.registerSigner(signer)

        // Return signer info with the registered ID and public key from the custom signer
        return WalletSignerInfo(
            signerId = signerId,
            publicKey = signer.publicKey(),
        )
    }

    /**
     * Create a V5R1 wallet adapter from a signer.
     * Step 2 of the wallet creation pattern from JS docs.
     *
     * Example:
     * ```
     * val adapter = walletOperations.createV5R1Adapter(signer.signerId, network, workchain, walletId)
     * ```
     *
     * @param signerId Signer ID from createSigner
     * @param network Network string ("mainnet" or "testnet")
     * @param workchain Workchain ID (0 for basechain, -1 for masterchain)
     * @param walletId Wallet ID for address uniqueness
     * @param publicKey Public key hex string (required for custom signers)
     * @param isCustom Whether this is a custom signer (hardware wallet)
     */
    suspend fun createV5R1Adapter(
        signerId: String,
        network: TONNetwork?,
        workchain: Int,
        walletId: Long,
        publicKey: String?,
        isCustom: Boolean,
    ): WalletAdapterInfo {
        ensureInitialized()

        val request = CreateAdapterRequest(
            signerId = signerId,
            walletVersion = WalletVersions.V5R1,
            network = network,
            workchain = workchain,
            walletId = walletId,
            publicKey = publicKey,
            isCustom = isCustom,
        )
        val result = rpcClient.call(BridgeMethodConstants.METHOD_CREATE_ADAPTER, json.toJSONObject(request))

        // JavaScript returns { _tempId, adapter } where adapter is the raw object
        // Since adapter properties are now methods (getAddress(), getPublicKey(), etc.),
        // we need to call getAddress() on the stored adapter to get the address
        val tempId = result.optString("_tempId")

        // Use the tempId from JavaScript as the adapterId
        val adapterId = tempId.takeIf { it.isNotEmpty() } ?: IDGenerator.generateAdapterId()

        // Call getAddress() on the adapter through the bridge
        // JS returns raw string, BridgeRpcClient wraps it as { value: "..." }
        val getAddressRequest = JSONObject().apply {
            put("adapterId", adapterId)
        }
        val addressResult = rpcClient.call("getAdapterAddress", getAddressRequest)
        val address = addressResult.optString(ResponseConstants.KEY_VALUE, "")

        // Use provided network or default to testnet
        val resolvedNetwork = network ?: TONNetwork.TESTNET

        val adapterInfo = WalletAdapterInfo(
            adapterId = adapterId,
            address = TONUserFriendlyAddress(address),
            network = resolvedNetwork,
        )

        // Store adapter info for walletId computation in addWallet
        adapterStore[adapterId] = adapterInfo

        return adapterInfo
    }

    /**
     * Create a V4R2 wallet adapter from a signer.
     * Step 2 of the wallet creation pattern from JS docs.
     *
     * Example:
     * ```
     * val adapter = walletOperations.createV4R2Adapter(signer.signerId, network, workchain, walletId)
     * ```
     *
     * @param signerId Signer ID from createSigner
     * @param network Network string ("mainnet" or "testnet")
     * @param workchain Workchain ID (0 for basechain, -1 for masterchain)
     * @param walletId Wallet ID for address uniqueness
     * @param publicKey Public key hex string (required for custom signers)
     * @param isCustom Whether this is a custom signer (hardware wallet)
     */
    suspend fun createV4R2Adapter(
        signerId: String,
        network: TONNetwork?,
        workchain: Int = 0,
        walletId: Long = 698983191L,
        publicKey: String? = null,
        isCustom: Boolean = false,
    ): WalletAdapterInfo {
        ensureInitialized()

        val request = CreateAdapterRequest(
            signerId = signerId,
            walletVersion = WalletVersions.V4R2,
            network = network,
            workchain = workchain,
            walletId = walletId,
            publicKey = publicKey,
            isCustom = isCustom,
        )
        val result = rpcClient.call(BridgeMethodConstants.METHOD_CREATE_ADAPTER, json.toJSONObject(request))

        // JavaScript returns { _tempId, adapter } where adapter is the raw object
        // Since adapter properties are now methods (getAddress(), getPublicKey(), etc.),
        // we need to call getAddress() on the stored adapter to get the address
        val tempId = result.optString("_tempId")

        // Use the tempId from JavaScript as the adapterId
        val adapterId = tempId.takeIf { it.isNotEmpty() } ?: IDGenerator.generateAdapterId()

        // Call getAddress() on the adapter through the bridge
        // JS returns raw string, BridgeRpcClient wraps it as { value: "..." }
        val getAddressRequest = JSONObject().apply {
            put("adapterId", adapterId)
        }
        val addressResult = rpcClient.call("getAdapterAddress", getAddressRequest)
        val address = addressResult.optString(ResponseConstants.KEY_VALUE, "")

        // Use provided network or default to testnet
        val resolvedNetwork = network ?: TONNetwork.TESTNET

        val adapterInfo = WalletAdapterInfo(
            adapterId = adapterId,
            address = TONUserFriendlyAddress(address),
            network = resolvedNetwork,
        )

        // Store adapter info for walletId computation in addWallet
        adapterStore[adapterId] = adapterInfo

        return adapterInfo
    }

    /**
     * Add a wallet using the adapter.
     * Step 3 of the wallet creation pattern from JS docs.
     *
     * Example:
     * ```
     * val wallet = walletOperations.addWallet(adapter.adapterId)
     * ```
     *
     * @param adapterId Adapter ID from createV5R1Adapter or createV4R2Adapter
     */
    suspend fun addWallet(adapterId: String): WalletAccount {
        ensureInitialized()

        // Remove adapter from store since JS will delete it too
        adapterStore.remove(adapterId)

        val request = AddWalletRequest(adapterId = adapterId)
        val result = rpcClient.call(BridgeMethodConstants.METHOD_ADD_WALLET, json.toJSONObject(request))

        // JS returns { walletId, wallet } where wallet has publicKey, version as properties
        val walletId = result.optString("walletId").takeIf { it.isNotEmpty() }
            ?: throw WalletKitBridgeException(ERROR_NEW_WALLET_NOT_FOUND)

        val walletObj = result.optJSONObject("wallet")
        val rawPublicKey = walletObj?.optString("publicKey") ?: ""
        val publicKey = WalletKitUtils.stripHexPrefix(rawPublicKey)
        val version = walletObj?.optString("version")?.takeIf { it.isNotEmpty() } ?: "unknown"

        // Get address via RPC (since getAddress() is a method, not a serialized property)
        val address = getWalletAddress(walletId)

        return WalletAccount(
            walletId = walletId,
            address = TONUserFriendlyAddress(address),
            publicKey = publicKey.takeIf { it.isNotEmpty() },
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
     * @param walletId Wallet ID in format "chainId:address" (e.g., "-239:UQDtFp...")
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
     * @param walletId Wallet ID in format "chainId:address" (e.g., "-239:UQDtFp...")
     */
    suspend fun removeWallet(walletId: String) {
        ensureInitialized()

        val request = WalletIdRequest(walletId = walletId)
        val result = rpcClient.call(BridgeMethodConstants.METHOD_REMOVE_WALLET, json.toJSONObject(request))
        Logger.d(TAG, "removeWallet result: $result")

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
     * @param walletId Wallet ID in format "chainId:address" (e.g., "-239:UQDtFp...")
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
