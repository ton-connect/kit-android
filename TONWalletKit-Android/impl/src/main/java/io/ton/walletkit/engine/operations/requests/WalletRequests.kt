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
package io.ton.walletkit.engine.operations.requests

import kotlinx.serialization.Serializable

/**
 * Internal bridge request models for wallet operations.
 * These DTOs represent the exact JSON structure sent to the JavaScript bridge.
 *
 * @suppress Internal bridge communication only.
 */

@Serializable
internal data class CreateSignerRequest(
    val mnemonic: List<String>? = null,
    val secretKey: String? = null,
    val mnemonicType: String = "ton",
)

@Serializable
internal data class CreateAdapterRequest(
    val signerId: String,
    val walletVersion: String,
    val network: String? = null,
    val workchain: Int = 0,
    val walletId: Long,
    // Public key as hex string (for custom signers)
    val publicKey: String? = null,
    val isCustom: Boolean = false,
)

@Serializable
internal data class AddWalletRequest(
    val adapterId: String,
)

@Serializable
internal data class AddressRequest(
    val address: String,
)
