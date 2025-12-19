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

import io.ton.walletkit.api.generated.TONConnectionRequestEvent
import io.ton.walletkit.api.generated.TONSignDataRequestEvent
import io.ton.walletkit.api.generated.TONTransactionRequestEvent
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Internal bridge request models for TonConnect operations.
 * These DTOs represent the exact JSON structure sent to the JavaScript bridge.
 *
 * @suppress Internal bridge communication only.
 */

@Serializable
internal data class HandleTonConnectUrlRequest(
    val url: String,
)

@Serializable
internal data class ProcessInternalBrowserRequest(
    val messageId: String,
    val method: String,
    // Raw JSON (can be array or object)
    val params: JsonElement? = null,
    val url: String? = null,
)

@Serializable
internal data class ApproveConnectRequest(
    val event: TONConnectionRequestEvent,
    val walletId: String,
)

@Serializable
internal data class RejectConnectRequest(
    val event: TONConnectionRequestEvent,
    val reason: String? = null,
    val errorCode: Int? = null,
)

@Serializable
internal data class ApproveTransactionRequest(
    val event: TONTransactionRequestEvent,
    val walletId: String,
)

@Serializable
internal data class RejectTransactionRequest(
    val event: TONTransactionRequestEvent,
    val reason: String? = null,
    val errorCode: Int? = null,
)

@Serializable
internal data class ApproveSignDataRequest(
    val event: TONSignDataRequestEvent,
    val walletId: String,
)

@Serializable
internal data class RejectSignDataRequest(
    val event: TONSignDataRequestEvent,
    val reason: String? = null,
    val errorCode: Int? = null,
)

@Serializable
internal data class DisconnectSessionRequest(
    val sessionId: String? = null,
)
