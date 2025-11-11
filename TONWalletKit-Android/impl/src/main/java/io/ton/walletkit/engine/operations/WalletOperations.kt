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
import io.ton.walletkit.engine.infrastructure.BridgeRpcClient
import io.ton.walletkit.engine.parsing.TransactionParser
import io.ton.walletkit.engine.state.SignerManager
import io.ton.walletkit.internal.constants.BridgeMethodConstants
import io.ton.walletkit.internal.constants.JsonConstants
import io.ton.walletkit.internal.constants.LogConstants
import io.ton.walletkit.internal.constants.ResponseConstants
import io.ton.walletkit.internal.util.Logger
import io.ton.walletkit.model.Transaction
import io.ton.walletkit.model.WalletAccount
import io.ton.walletkit.model.WalletAdapterInfo
import io.ton.walletkit.model.WalletSigner
import io.ton.walletkit.model.WalletSignerInfo
import io.ton.walletkit.utils.EncodingUtils
import io.ton.walletkit.utils.IDGenerator
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
 * @property transactionParser Parser for transaction payloads returned by the bridge.
 * @property currentNetworkProvider Provides the latest network identifier for defaulting fields.
 *
 * @suppress Internal component used by [WebViewWalletKitEngine].
 */
internal class WalletOperations(
    private val ensureInitialized: suspend () -> Unit,
    private val rpcClient: BridgeRpcClient,
    private val signerManager: SignerManager,
    private val transactionParser: TransactionParser,
    private val currentNetworkProvider: () -> String,
) {

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

        val params =
            JSONObject().apply {
                put(JsonConstants.KEY_MNEMONIC, JSONArray(mnemonic))
                put("mnemonicType", mnemonicType)
            }

        val result = rpcClient.call(BridgeMethodConstants.METHOD_CREATE_SIGNER, params)

        // Handle ID generation and publicKey formatting in Kotlin
        val signerId = result.optString(ResponseConstants.KEY_SIGNER_ID).takeIf { it.isNotEmpty() }
            ?: IDGenerator.generateSignerId()
        val rawPublicKey = result.optString(ResponseConstants.KEY_PUBLIC_KEY)
        val publicKey = EncodingUtils.stripHexPrefix(rawPublicKey)

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

        val params =
            JSONObject().apply {
                put(JsonConstants.KEY_SECRET_KEY, WalletKitUtils.byteArrayToHexNoPrefix(secretKey))
            }

        val result = rpcClient.call(BridgeMethodConstants.METHOD_CREATE_SIGNER, params)

        // Handle ID generation and publicKey formatting in Kotlin
        val signerId = result.optString(ResponseConstants.KEY_SIGNER_ID).takeIf { it.isNotEmpty() }
            ?: IDGenerator.generateSignerId()
        val rawPublicKey = result.optString(ResponseConstants.KEY_PUBLIC_KEY)
        val publicKey = EncodingUtils.stripHexPrefix(rawPublicKey)

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
            publicKey = signer.publicKey,
        )
    }

    /**
     * Create a V5R1 wallet adapter from a custom signer.
     * This is used specifically for custom signers (hardware wallets) where signing happens in Kotlin.
     */
    suspend fun createV5R1AdapterFromCustom(
        signerInfo: WalletSignerInfo,
        network: String?,
        workchain: Int = 0,
        walletId: Long = 2147483409L,
    ): WalletAdapterInfo {
        ensureInitialized()

        val params =
            JSONObject().apply {
                put(ResponseConstants.KEY_SIGNER_ID, signerInfo.signerId)
                put("publicKey", signerInfo.publicKey)
                put("isCustom", true)
                put("walletVersion", "v5r1")
                network?.let { put(JsonConstants.KEY_NETWORK, it) }
                put("workchain", workchain)
                put("walletId", walletId)
            }

        val result = rpcClient.call(BridgeMethodConstants.METHOD_CREATE_ADAPTER, params)

        val adapterId = result.optString("adapterId").takeIf { it.isNotEmpty() }
            ?: IDGenerator.generateAdapterId()

        return WalletAdapterInfo(
            adapterId = adapterId,
            address = result.optString(ResponseConstants.KEY_ADDRESS),
        )
    }

    /**
     * Create a V4R2 wallet adapter from a custom signer.
     * This is used specifically for custom signers (hardware wallets) where signing happens in Kotlin.
     */
    suspend fun createV4R2AdapterFromCustom(
        signerInfo: WalletSignerInfo,
        network: String?,
        workchain: Int = 0,
        walletId: Long = 698983191L,
    ): WalletAdapterInfo {
        ensureInitialized()

        val params =
            JSONObject().apply {
                put(ResponseConstants.KEY_SIGNER_ID, signerInfo.signerId)
                put("publicKey", signerInfo.publicKey)
                put("isCustom", true)
                put("walletVersion", "v4r2")
                network?.let { put(JsonConstants.KEY_NETWORK, it) }
                put("workchain", workchain)
                put("walletId", walletId)
            }

        val result = rpcClient.call(BridgeMethodConstants.METHOD_CREATE_ADAPTER, params)

        val adapterId = result.optString("adapterId").takeIf { it.isNotEmpty() }
            ?: IDGenerator.generateAdapterId()

        return WalletAdapterInfo(
            adapterId = adapterId,
            address = result.optString(ResponseConstants.KEY_ADDRESS),
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
     */
    suspend fun createV5R1Adapter(
        signerId: String,
        network: String?,
        workchain: Int = 0,
        walletId: Long = 2147483409L,
    ): WalletAdapterInfo {
        ensureInitialized()

        val params =
            JSONObject().apply {
                put(ResponseConstants.KEY_SIGNER_ID, signerId)
                put("walletVersion", "v5r1")
                network?.let { put(JsonConstants.KEY_NETWORK, it) }
                put("workchain", workchain)
                put("walletId", walletId)
            }

        val result = rpcClient.call(BridgeMethodConstants.METHOD_CREATE_ADAPTER, params)

        // Handle adapter ID generation in Kotlin
        val adapterId = result.optString("adapterId").takeIf { it.isNotEmpty() }
            ?: IDGenerator.generateAdapterId()

        return WalletAdapterInfo(
            adapterId = adapterId,
            address = result.optString(ResponseConstants.KEY_ADDRESS),
        )
    }

    /**
     * Create a V4R2 wallet adapter from a signer.
     * Step 2 of the wallet creation pattern from JS docs.
     *
     * Example:
     * ```
     * val adapter = walletOperations.createV4R2Adapter(signer.signerId, network, workchain, walletId)
     * ```
     */
    suspend fun createV4R2Adapter(
        signerId: String,
        network: String?,
        workchain: Int = 0,
        walletId: Long = 698983191L,
    ): WalletAdapterInfo {
        ensureInitialized()

        val params =
            JSONObject().apply {
                put(ResponseConstants.KEY_SIGNER_ID, signerId)
                put("walletVersion", "v4r2")
                network?.let { put(JsonConstants.KEY_NETWORK, it) }
                put("workchain", workchain)
                put("walletId", walletId)
            }

        val result = rpcClient.call(BridgeMethodConstants.METHOD_CREATE_ADAPTER, params)

        // Handle adapter ID generation in Kotlin
        val adapterId = result.optString("adapterId").takeIf { it.isNotEmpty() }
            ?: IDGenerator.generateAdapterId()

        return WalletAdapterInfo(
            adapterId = adapterId,
            address = result.optString(ResponseConstants.KEY_ADDRESS),
        )
    }

    /**
     * Add a wallet using the adapter.
     * Step 3 of the wallet creation pattern from JS docs.
     *
     * Example:
     * ```
     * val wallet = walletOperations.addWallet(adapter.adapterId)
     * ```
     */
    suspend fun addWallet(adapterId: String): WalletAccount {
        ensureInitialized()

        val params =
            JSONObject().apply {
                put("adapterId", adapterId)
            }

        val result = rpcClient.call(BridgeMethodConstants.METHOD_ADD_WALLET, params)

        // Handle publicKey formatting in Kotlin
        val rawPublicKey = result.optString(ResponseConstants.KEY_PUBLIC_KEY)
        val publicKey = EncodingUtils.stripHexPrefix(rawPublicKey)

        return WalletAccount(
            address = result.optString(ResponseConstants.KEY_ADDRESS),
            publicKey = publicKey,
            name = null,
            // Version is determined by the adapter
            version = "unknown",
            network = currentNetworkProvider(),
            index = 0,
        )
    }

    /**
     * Fetch a list of wallets registered on the bridge.
     */
    suspend fun getWallets(): List<WalletAccount> {
        ensureInitialized()

        val result = rpcClient.call(BridgeMethodConstants.METHOD_GET_WALLETS)
        val items = result.optJSONArray(ResponseConstants.KEY_ITEMS) ?: JSONArray()

        return buildList(items.length()) {
            for (index in 0 until items.length()) {
                val entry = items.optJSONObject(index) ?: continue
                val rawPublicKey = entry.optNullableString(ResponseConstants.KEY_PUBLIC_KEY)
                val publicKey = rawPublicKey?.let { EncodingUtils.stripHexPrefix(it) }
                add(
                    WalletAccount(
                        address = entry.optString(ResponseConstants.KEY_ADDRESS),
                        publicKey = publicKey,
                        name = entry.optNullableString(JsonConstants.KEY_NAME),
                        version = entry.optString(JsonConstants.KEY_VERSION, ResponseConstants.VALUE_UNKNOWN),
                        network = entry.optString(JsonConstants.KEY_NETWORK, currentNetworkProvider()),
                        index = entry.optInt(ResponseConstants.KEY_INDEX, index),
                    ),
                )
            }
        }
    }

    /**
     * Get a single wallet by address using RPC call.
     */
    suspend fun getWallet(address: String): WalletAccount? {
        ensureInitialized()

        val params = JSONObject().apply { put(ResponseConstants.KEY_ADDRESS, address) }
        val result = rpcClient.call(BridgeMethodConstants.METHOD_GET_WALLET, params)

        // If result is null or doesn't have required fields, wallet doesn't exist
        if (result.length() == 0 || !result.has(ResponseConstants.KEY_ADDRESS)) {
            return null
        }

        val rawPublicKey = result.optNullableString(ResponseConstants.KEY_PUBLIC_KEY)
        val publicKey = rawPublicKey?.let { EncodingUtils.stripHexPrefix(it) }

        return WalletAccount(
            address = result.optString(ResponseConstants.KEY_ADDRESS),
            publicKey = publicKey,
            name = result.optNullableString(JsonConstants.KEY_NAME),
            version = result.optString(JsonConstants.KEY_VERSION, ResponseConstants.VALUE_UNKNOWN),
            network = result.optString(JsonConstants.KEY_NETWORK, currentNetworkProvider()),
            index = result.optInt(ResponseConstants.KEY_INDEX, 0),
        )
    }

    /**
     * Remove a wallet from the bridge layer.
     */
    suspend fun removeWallet(address: String) {
        ensureInitialized()

        val params = JSONObject().apply { put(ResponseConstants.KEY_ADDRESS, address) }
        val result = rpcClient.call(BridgeMethodConstants.METHOD_REMOVE_WALLET, params)
        Logger.d(TAG, "removeWallet result: $result")

        val removed =
            when {
                result.has(ResponseConstants.KEY_REMOVED) -> result.optBoolean(ResponseConstants.KEY_REMOVED, false)
                result.has(ResponseConstants.KEY_OK) -> result.optBoolean(ResponseConstants.KEY_OK, true)
                result.has(ResponseConstants.KEY_VALUE) -> result.optBoolean(ResponseConstants.KEY_VALUE, true)
                else -> true
            }

        if (!removed) {
            throw WalletKitBridgeException(ERROR_FAILED_REMOVE_WALLET + address)
        }
    }

    /**
     * Retrieve wallet balance in nanoTON.
     */
    suspend fun getBalance(address: String): String {
        ensureInitialized()

        val params = JSONObject().apply { put(ResponseConstants.KEY_ADDRESS, address) }
        val result = rpcClient.call(BridgeMethodConstants.METHOD_GET_BALANCE, params)

        return when {
            result.has(ResponseConstants.KEY_BALANCE) -> result.optString(ResponseConstants.KEY_BALANCE)
            result.has(ResponseConstants.KEY_VALUE) -> result.optString(ResponseConstants.KEY_VALUE)
            else -> null
        } ?: "0"
    }

    /**
     * Retrieve recent transactions for the requested wallet.
     */
    suspend fun getRecentTransactions(address: String, limit: Int): List<Transaction> {
        ensureInitialized()

        val params =
            JSONObject().apply {
                put(ResponseConstants.KEY_ADDRESS, address)
                put(ResponseConstants.KEY_LIMIT, limit)
            }

        val result = rpcClient.call(BridgeMethodConstants.METHOD_GET_RECENT_TRANSACTIONS, params)
        return transactionParser.parseTransactions(result.optJSONArray(ResponseConstants.KEY_ITEMS))
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
