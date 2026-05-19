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
import io.ton.walletkit.engine.infrastructure.callTyped
import io.ton.walletkit.engine.operations.requests.CreateMnemonicRequest
import io.ton.walletkit.engine.operations.requests.MnemonicToKeyPairRequest
import io.ton.walletkit.engine.operations.requests.SignRequest
import io.ton.walletkit.engine.operations.responses.KeyPairResponse
import io.ton.walletkit.internal.constants.BridgeMethodConstants
import io.ton.walletkit.internal.util.WalletKitUtils
import io.ton.walletkit.model.KeyPair

internal suspend fun BridgeRpcClient.createTonMnemonic(wordCount: Int): List<String> =
    callTyped(BridgeMethodConstants.METHOD_CREATE_TON_MNEMONIC, CreateMnemonicRequest(count = wordCount))

internal suspend fun BridgeRpcClient.mnemonicToKeyPair(words: List<String>, mnemonicType: String = "ton"): KeyPair {
    val response: KeyPairResponse = callTyped(
        BridgeMethodConstants.METHOD_MNEMONIC_TO_KEY_PAIR,
        MnemonicToKeyPairRequest(mnemonic = words, mnemonicType = mnemonicType),
    )
    return KeyPair(response.publicKey, response.secretKey)
}

internal suspend fun BridgeRpcClient.sign(data: ByteArray, secretKey: ByteArray): ByteArray {
    val signatureHex: String = callTyped(
        BridgeMethodConstants.METHOD_SIGN,
        SignRequest(
            data = data.map { it.toInt() and 0xFF },
            secretKey = secretKey.map { it.toInt() and 0xFF },
        ),
    )
    if (signatureHex.isEmpty()) {
        throw WalletKitBridgeException("Signature missing from sign result")
    }
    return WalletKitUtils.hexToByteArray(signatureHex)
}
