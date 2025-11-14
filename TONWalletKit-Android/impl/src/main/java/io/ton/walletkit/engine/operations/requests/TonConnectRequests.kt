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

import io.ton.walletkit.event.ConnectRequestEvent
import io.ton.walletkit.event.SignDataRequestEvent
import io.ton.walletkit.event.TransactionRequestEvent
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import org.json.JSONArray
import org.json.JSONObject

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
    val params: JsonElement? = null, // Raw JSON (can be array or object)
    val url: String? = null,
)

@Serializable
internal data class ApproveConnectRequest(
    val event: ConnectRequestEvent,
    val walletAddress: String,
)

@Serializable
internal data class RejectConnectRequest(
    val event: ConnectRequestEvent,
    val reason: String? = null,
)

@Serializable
internal data class ApproveTransactionRequest(
    val event: TransactionRequestEvent,
)

@Serializable
internal data class RejectTransactionRequest(
    val event: TransactionRequestEvent,
    val reason: String? = null,
)

@Serializable
internal data class ApproveSignDataRequest(
    val event: SignDataRequestEvent,
)

@Serializable
internal data class RejectSignDataRequest(
    val event: SignDataRequestEvent,
    val reason: String? = null,
)

@Serializable
internal data class DisconnectSessionRequest(
    val sessionId: String? = null,
)
