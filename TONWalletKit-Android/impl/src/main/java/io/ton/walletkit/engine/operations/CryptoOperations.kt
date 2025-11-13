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
import io.ton.walletkit.engine.infrastructure.BridgeRpcClient
import io.ton.walletkit.engine.infrastructure.toJSONObject
import io.ton.walletkit.engine.operations.requests.CreateMnemonicRequest
import io.ton.walletkit.engine.operations.requests.MnemonicToKeyPairRequest
import io.ton.walletkit.engine.operations.requests.SignRequest
import io.ton.walletkit.internal.constants.BridgeMethodConstants
import io.ton.walletkit.internal.constants.LogConstants
import io.ton.walletkit.internal.constants.ResponseConstants
import io.ton.walletkit.internal.util.Logger
import io.ton.walletkit.model.KeyPair
import kotlinx.serialization.json.Json
import org.json.JSONArray

/**
 * Handles cryptographic bridge operations such as mnemonic generation, key derivation,
 * and data signing. Each call ensures the bridge is initialised before delegating to
 * the JavaScript transport.
 *
 * Behaviour and exception semantics match the legacy [WebViewWalletKitEngine] directly.
 *
 * @property ensureInitialized Suspended function that guarantees WalletKit initialisation.
 * @property rpcClient RPC client used for bridge communication.
 *
 * @suppress Internal component consumed by [WebViewWalletKitEngine].
 */
internal class CryptoOperations(
    private val ensureInitialized: suspend () -> Unit,
    private val rpcClient: BridgeRpcClient,
    private val json: Json,
) {

    /**
     * Generate a TON mnemonic of the given size.
     *
     * @param wordCount Number of mnemonic words to generate.
     * @return Generated mnemonic words. Returns an empty list if bridge response omits items.
     * @throws WalletKitBridgeException If the bridge call fails.
     */
    suspend fun createTonMnemonic(wordCount: Int): List<String> {
        ensureInitialized()

        val request = CreateMnemonicRequest(count = wordCount)
        val result = rpcClient.call(BridgeMethodConstants.METHOD_CREATE_TON_MNEMONIC, json.toJSONObject(request))

        // JS now returns array directly (not wrapped in { items: [...] })
        val items = if (result is JSONArray) {
            result
        } else {
            result.optJSONArray(ResponseConstants.KEY_ITEMS)
        }

        if (items == null) {
            Logger.w(TAG, "Mnemonic generation returned no items (wordCount=$wordCount)")
            return emptyList()
        }

        return List(items.length()) { index -> items.optString(index) }
    }

    /**
     * Convert a mnemonic phrase to an Ed25519 key pair.
     *
     * @param words Mnemonic seed words (12 or 24 words).
     * @param mnemonicType Derivation type: "ton" (default) or "bip39".
     * @return KeyPair containing public key (32 bytes) and secret key (64 bytes).
     * @throws WalletKitBridgeException If the bridge call fails.
     */
    suspend fun mnemonicToKeyPair(
        words: List<String>,
        mnemonicType: String = "ton",
    ): KeyPair {
        ensureInitialized()

        val request = MnemonicToKeyPairRequest(mnemonic = words, mnemonicType = mnemonicType)
        val result = rpcClient.call(BridgeMethodConstants.METHOD_MNEMONIC_TO_KEY_PAIR, json.toJSONObject(request))

        // JS now returns raw keyPair object with Uint8Array properties (serialized as JSONArray)
        val publicKeyArray = result.optJSONArray(ResponseConstants.KEY_PUBLIC_KEY)
            ?: throw WalletKitBridgeException("Missing publicKey in mnemonicToKeyPair response")
        val secretKeyArray = result.optJSONArray(ResponseConstants.KEY_SECRET_KEY)
            ?: throw WalletKitBridgeException("Missing secretKey in mnemonicToKeyPair response")

        val publicKey = ByteArray(publicKeyArray.length()) { i -> publicKeyArray.optInt(i).toByte() }
        val secretKey = ByteArray(secretKeyArray.length()) { i -> secretKeyArray.optInt(i).toByte() }

        return KeyPair(publicKey, secretKey)
    }

    /**
     * Sign arbitrary data using a secret key via the bridge.
     *
     * @param data Data bytes to sign.
     * @param secretKey Secret key bytes for signing.
     * @return Signature bytes returned by the bridge.
     * @throws WalletKitBridgeException If the bridge call fails or omits the signature.
     */
    suspend fun sign(
        data: ByteArray,
        secretKey: ByteArray,
    ): ByteArray {
        ensureInitialized()

        val request = SignRequest(
            data = data.map { it.toInt() and 0xFF },
            secretKey = secretKey.map { it.toInt() and 0xFF },
        )
        val result = rpcClient.call(BridgeMethodConstants.METHOD_SIGN, json.toJSONObject(request))

        // JS now returns hex string directly (not wrapped in { signature: ... })
        val signatureHex = when {
            result is String -> result
            result.has(ResponseConstants.KEY_SIGNATURE) -> result.optString(ResponseConstants.KEY_SIGNATURE)
            else -> result.toString()
        }.takeIf { it.isNotEmpty() && it != "null" }
            ?: throw WalletKitBridgeException(ERROR_SIGNATURE_MISSING_SIGN_RESULT)

        // Convert hex string to ByteArray
        return hexToByteArray(signatureHex)
    }

    /**
     * Converts a hex string (with or without "0x" prefix) to a ByteArray.
     */
    private fun hexToByteArray(hex: String): ByteArray {
        val cleanHex = if (hex.startsWith("0x", ignoreCase = true)) {
            hex.substring(2)
        } else {
            hex
        }

        require(cleanHex.length % 2 == 0) { "Hex string must have even length: $hex" }

        return ByteArray(cleanHex.length / 2) { i ->
            cleanHex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }

    companion object {
        private const val TAG = "${LogConstants.TAG_WEBVIEW_ENGINE}:CryptoOps"
        private const val ERROR_SIGNATURE_MISSING_SIGN_RESULT =
            "Signature missing from sign result"
    }
}
