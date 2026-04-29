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
package io.ton.walletkit.browser

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Internal DTOs for the [TonConnectInjector] / WebView side of the bridge. Replaces
 * the hand-rolled `JSONObject().apply { put(...) }` payloads that were sent to the
 * dApp WebView (or to the engine when emitting browser-side events). Encode through
 * `Json.encodeToString(...)` or via the bridge's `toJSONObject(...)` helper.
 *
 * @suppress Internal browser plumbing only.
 */

/** `{ "url": "..." }` — METHOD_EMIT_BROWSER_PAGE_STARTED / FINISHED. */
@Serializable
internal data class BrowserUrlEvent(val url: String)

/** `{ "message": "..." }` — METHOD_EMIT_BROWSER_ERROR. */
@Serializable
internal data class BrowserErrorEvent(val message: String)

/** `{ "messageId": "...", "method": "...", "request": "..." }` — METHOD_EMIT_BROWSER_BRIDGE_REQUEST. */
@Serializable
internal data class BrowserBridgeRequestEvent(
    val messageId: String,
    val method: String,
    val request: String,
)

/** `{ "type": "BRIDGE_EVENT", "event": {...} }` — TonConnect event sent into the dApp. */
@Serializable
internal data class TonConnectEventMessage(
    val type: String,
    val event: JsonElement,
)

/** `{ "type": ..., "messageId": ..., "success": true, "payload": {...} }` — TonConnect response envelope. */
@Serializable
internal data class TonConnectResponseEnvelope(
    val type: String,
    val messageId: String,
    val success: Boolean,
    val payload: JsonElement,
)

/** `{ "message": "...", "code": N }` — body of a TonConnect error response. */
@Serializable
internal data class TonConnectErrorBody(
    val message: String,
    val code: Int,
)

/** `{ "error": { message, code } }` — full TonConnect error response. */
@Serializable
internal data class TonConnectErrorResponse(
    val error: TonConnectErrorBody,
)
