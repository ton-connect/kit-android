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
package io.ton.walletkit.engine.infrastructure

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Internal bridge envelope DTOs. Replaces the hand-rolled
 * `JSONObject().put(KEY_KIND, ...)`-style construction in [MessageDispatcher],
 * [BridgeRpcClient], and friends. The bridge transport stays on `JSONObject` —
 * encode at the boundary via `json.toJSONObject(...)`.
 */

/** `{ "message": "<error text>" }` — body of any bridge error. */
@Serializable
internal data class BridgeErrorPayload(val message: String)

/**
 * `{ "kind": "response", "id": "...", "error": {message: "..."} }` — response envelope
 * we synthesise from Kotlin (e.g. when a malformed JS message is rejected) and feed
 * back to [BridgeRpcClient.handleResponse] to fail a specific pending call.
 */
@Serializable
internal data class BridgeResponseEnvelope(
    val kind: String,
    val id: String,
    val error: BridgeErrorPayload? = null,
    val result: JsonElement? = null,
)

/** `{ "items": [...] }` — wraps an array result so `handleResponse` can return JSONObject. */
@Serializable
internal data class BridgeItemsWrap(val items: JsonElement)

/** `{ "value": <scalar> }` — wraps a non-object/non-array result for the same reason. */
@Serializable
internal data class BridgeValueWrap(val value: JsonElement)

/** `{ "type": "ready", "data": {...} }` — internal ready-event envelope. */
@Serializable
internal data class BridgeReadyEvent(
    val type: String,
    val data: JsonElement,
)
