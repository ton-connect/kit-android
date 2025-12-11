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

import io.ton.walletkit.model.TONTransferParams
import kotlinx.serialization.Serializable

/**
 * Internal bridge request models for transaction operations.
 * These DTOs represent the exact JSON structure sent to the JavaScript bridge.
 *
 * @suppress Internal bridge communication only.
 */

@Serializable
internal data class CreateTransferTonRequest(
    val walletId: String,
    val toAddress: String,
    val amount: String,
    val comment: String? = null,
    val body: String? = null,
    val stateInit: String? = null,
)

@Serializable
internal data class CreateTransferMultiTonRequest(
    val walletId: String,
    val messages: List<TONTransferParams>,
)

@Serializable
internal data class HandleNewTransactionRequest(
    val walletId: String,
    val transactionContent: String,
)

@Serializable
internal data class SendTransactionRequest(
    val walletId: String,
    val transactionContent: String,
)

@Serializable
internal data class GetTransactionPreviewRequest(
    val walletId: String,
    // JSON string
    val transactionContent: String,
)
