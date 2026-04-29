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
package io.ton.walletkit.presentation.impl

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Internal bridge request models for the QuickJS engine. These DTOs represent the
 * exact JSON payloads sent to the embedded JavaScript runtime via
 * `globalThis.__walletkitCall(id, method, paramsJson)`.
 *
 * The wire-format is byte-identical to the legacy hand-rolled
 * `JSONObject().put(...)` builder — same field names, same nullability emission
 * rules. Encoded at the bridge boundary via `Json.toJSONObject(...)`.
 *
 * @suppress Internal bridge communication only.
 */

/** `{ "address": "..." }` — address-only request body. */
@Serializable
internal data class QuickJsAddressRequest(val address: String)

/** `{ "url": "..." }` — TON Connect URL request body. */
@Serializable
internal data class QuickJsUrlRequest(val url: String)

/**
 * `{ "address": "...", "limit": N, "offset": N }` — paged-listing request body.
 * Used by `getNfts` / `getJettons`.
 */
@Serializable
internal data class QuickJsPagedRequest(
    val address: String,
    val limit: Int,
    val offset: Int,
)

/**
 * `{ "address": "...", "jettonAddress": "..." }` — jetton-specific lookup body.
 */
@Serializable
internal data class QuickJsJettonAddressRequest(
    val address: String,
    val jettonAddress: String,
)

/**
 * `{ "walletId": "...", "transactionContent": <object> }` — transaction
 * request body. `transactionContent` is a [JsonElement] so it preserves the
 * caller-supplied shape verbatim.
 */
@Serializable
internal data class QuickJsTransactionRequest(
    val walletId: String,
    val transactionContent: JsonElement,
)

/**
 * Connect-approve params: `{ "requestId", "walletAddress", "walletId", "response"? }`.
 * Optional `response` is a [JsonElement] (encoded TONConnectionApprovalResponse).
 */
@Serializable
internal data class QuickJsApproveConnectParams(
    val requestId: String,
    val walletAddress: String,
    val walletId: String,
    val response: JsonElement? = null,
)

/**
 * Transaction-approve params: `{ "requestId", "walletAddress", "walletId", "response"? }`.
 * Same shape as connect-approve but for `approveTransactionRequest`.
 */
@Serializable
internal data class QuickJsApproveTransactionParams(
    val requestId: String,
    val walletAddress: String,
    val walletId: String? = null,
    val response: JsonElement? = null,
)

/**
 * Sign-data approve params: `{ "requestId", "walletAddress", "walletId", "response"? }`.
 */
@Serializable
internal data class QuickJsApproveSignDataParams(
    val requestId: String,
    val walletAddress: String,
    val walletId: String? = null,
    val response: JsonElement? = null,
)

/**
 * Reject params shared by connect / transaction / sign-data rejection. Wire shape:
 * `{ "requestId": "...", "reason"?: "...", "errorCode"?: N }`. Absent fields are
 * omitted from the encoded JSON (handled via `explicitNulls = false`).
 */
@Serializable
internal data class QuickJsRejectParams(
    val requestId: String,
    val reason: String? = null,
    val errorCode: Int? = null,
)

/**
 * `{ "sessionId": "..." }` — disconnect-session request body. Wrapped by
 * `disconnectSession` and only emitted when `sessionId` is non-null.
 */
@Serializable
internal data class QuickJsDisconnectSessionParams(
    val sessionId: String,
)

/**
 * `{ "status": <int>, "statusText": "...", "headers": [[name,value],…] }` —
 * fetch-response metadata used by the QuickJS HTTP shim.
 */
@Serializable
internal data class QuickJsFetchResponseMeta(
    val status: Int,
    val statusText: String,
    val headers: List<List<String>>,
)
