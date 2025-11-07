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
package io.ton.walletkit.model

import kotlinx.serialization.Serializable

/**
 * NFT collection information.
 *
 * @property address Collection contract address (user-friendly format: UQ... or EQ...)
 * @property codeHash Code hash of the collection contract (hex string with 0x prefix)
 * @property dataHash Data hash of the collection contract (hex string with 0x prefix)
 * @property lastTransactionLt Last transaction logical time
 * @property nextItemIndex Next item index in the collection
 * @property ownerAddress Collection owner address (user-friendly format)
 */
@Serializable
data class TONNFTCollection(
    /** Collection contract address (user-friendly format: UQ... or EQ...) */
    val address: String,
    /** Code hash of the collection contract (hex string with 0x prefix) */
    val codeHash: String? = null,
    /** Data hash of the collection contract (hex string with 0x prefix) */
    val dataHash: String? = null,
    val lastTransactionLt: String? = null,
    val nextItemIndex: String,
    /** Collection owner address (user-friendly format) */
    val ownerAddress: String? = null,
)
