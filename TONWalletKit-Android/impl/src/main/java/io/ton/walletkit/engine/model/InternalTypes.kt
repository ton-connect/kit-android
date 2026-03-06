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
package io.ton.walletkit.engine.model

import io.ton.walletkit.model.TONUserFriendlyAddress
import kotlinx.serialization.Serializable

/**
 * Internal wallet account representation.
 * Represents a wallet stored in WalletKit.
 *
 * Only contains fields that come from JS:
 * - walletId: from getWalletId() method result included in wrapper
 * - address: from getWalletAddress() RPC call
 * - publicKey: serialized property on wallet object
 * - version: serialized property on wallet object (e.g., "v5r1", "v4r2")
 */
@Serializable
data class WalletAccount(
    val walletId: String,
    val address: TONUserFriendlyAddress,
    val publicKey: String? = null,
    val version: String? = null,
)
