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
package io.ton.walletkit.testfixtures

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.json.JSONObject

/**
 * Shared `@Serializable` DTOs for unit-test fixtures. Each fixture mirrors the
 * exact JSON shape that the production code path expects, so building it via
 * `kotlinx.serialization` and then wrapping in `JSONObject(...)` gives the same
 * payload as the legacy hand-rolled `JSONObject().apply { put(...) }` chains.
 *
 * Helpers also live here so tests do not have to repeat the boilerplate
 * `JSONObject(json.encodeToString(dto))` conversion. Callers rely on a single
 * shared [Json] instance that lenient-parses unknown keys, matching the runtime
 * config used by the production code.
 *
 * @suppress Internal test utility only.
 */

internal val testFixtureJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
    explicitNulls = true
}

/** Convert any `@Serializable` value to a [JSONObject] for test fixture wiring. */
internal inline fun <reified T> jsonObjectOf(value: T): JSONObject {
    return JSONObject(testFixtureJson.encodeToString(value))
}

// ─── Bridge envelope shapes ───────────────────────────────────────────────────

/**
 * Wire shape `{ "kind": "response", "id": "...", "result": <element> }`. Used by
 * [io.ton.walletkit.engine.infrastructure.BridgeRpcClientTest] to feed synthetic
 * bridge responses into [io.ton.walletkit.engine.infrastructure.BridgeRpcClient].
 * `result` is a [JsonElement] so the same DTO can carry primitives, arrays, or
 * nested objects.
 */
@Serializable
internal data class TestBridgeResponseEnvelope(
    val kind: String = "response",
    val id: String,
    val result: JsonElement? = null,
    val error: TestBridgeError? = null,
)

/** Wire shape `{ "message": "..." }`. Optional inside [TestBridgeResponseEnvelope]. */
@Serializable
internal data class TestBridgeError(val message: String? = null)

/** Wire shape `{ "value": "..." }`. Common nested-result shape. */
@Serializable
internal data class TestValueWrap(val value: String)

// ─── Event-parser fixtures ────────────────────────────────────────────────────

/** Wire shape `{ "sessionId": "..." }` — single-string body for disconnect events. */
@Serializable
internal data class TestSessionIdBody(val sessionId: String)

/** Wire shape `{ "id": "..." }` — id-only body, used by `disconnect`'s `id` fallback. */
@Serializable
internal data class TestIdBody(val id: String)

/**
 * Wire shape `{ "sessionId": "...", "id": "..." }` — disconnect payload exercising
 * the `sessionId` precedence rule.
 */
@Serializable
internal data class TestSessionIdAndIdBody(val sessionId: String, val id: String)

/** Wire shape `{ "url": "..." }` — browser-page event body. */
@Serializable
internal data class TestUrlBody(val url: String)

/** Wire shape `{ "message": "..." }` — browser-error event body. */
@Serializable
internal data class TestMessageBody(val message: String)

/**
 * Wire shape used by the EventParser browser-bridge-request fixture:
 * `{ "messageId": "...", "method": "...", "request": "..." }`.
 */
@Serializable
internal data class TestBrowserBridgeRequestBody(
    val messageId: String,
    val method: String,
    val request: String,
)

/**
 * Wire shape for the connect-request preview's dApp metadata:
 * `{ "name": "...", "url": "...", "iconUrl"?: "..." }`.
 */
@Serializable
internal data class TestDAppInfo(
    val name: String,
    val url: String,
    val iconUrl: String? = null,
)

/**
 * Wire shape `{ "permissions": [...], "dAppInfo"?: {...} }` — connect-request
 * preview body. `permissions` is left as `JsonElement` so empty / populated
 * arrays both round-trip without ceremony.
 */
@Serializable
internal data class TestConnectRequestPreview(
    val permissions: JsonElement,
    val dAppInfo: TestDAppInfo? = null,
)

/**
 * Wire shape for the full `connectRequest` event payload:
 * `{ "id", "sessionId", "requestedItems", "preview" }`. The
 * [requestedItems] slot is a [JsonElement] so the parser sees the same array
 * literal it does in production.
 */
@Serializable
internal data class TestConnectRequestData(
    val id: String,
    val sessionId: String,
    val requestedItems: JsonElement,
    val preview: TestConnectRequestPreview,
)

/** Wire shape `{ "result": "success" }` — minimal transaction-preview data. */
@Serializable
internal data class TestPreviewResultData(val result: String)

/** Wire shape `{ "data": {...} }` — transaction-preview wrapper. */
@Serializable
internal data class TestPreviewDataWrap(val data: TestPreviewResultData)

/** Wire shape `{ "messages": [...] }` — minimal transaction request. */
@Serializable
internal data class TestTransactionMessagesBody(val messages: JsonElement)

/**
 * Wire shape for the full `transactionRequest` event payload:
 * `{ "id", "sessionId", "walletAddress", "preview", "request" }`.
 */
@Serializable
internal data class TestTransactionRequestData(
    val id: String,
    val sessionId: String,
    val walletAddress: String,
    val preview: TestPreviewDataWrap,
    val request: TestTransactionMessagesBody,
)

/** Wire shape `{ "content": "Hello World" }` — sign-data inner value. */
@Serializable
internal data class TestSignDataValue(val content: String)

/** Wire shape `{ "type": "text", "value": {...} }` — sign-data inner data. */
@Serializable
internal data class TestSignDataInner(
    val type: String,
    val value: TestSignDataValue,
)

/** Wire shape `{ "data": {...} }` — sign-data outer wrapper. */
@Serializable
internal data class TestSignDataWrap(val data: TestSignDataInner)

/**
 * Wire shape for the full `signDataRequest` event payload:
 * `{ "id", "sessionId", "walletAddress", "payload", "preview" }`.
 */
@Serializable
internal data class TestSignDataRequestData(
    val id: String,
    val sessionId: String,
    val walletAddress: String,
    val payload: TestSignDataWrap,
    val preview: TestSignDataWrap,
)

// ─── Crypto / wallet operation fixtures ───────────────────────────────────────

/** Wire shape `{ "items": [<string>, …] }` — mnemonic word list. */
@Serializable
internal data class TestStringItemsBody(val items: List<String>)

/** Wire shape `{ "publicKey": [<int>], "secretKey": [<int>] }` — array form. */
@Serializable
internal data class TestKeyPairArrays(
    val publicKey: List<Int>,
    val secretKey: List<Int>,
)

/** Wire shape `{ "publicKey": {"0":N,"1":N,…}, "secretKey": {…} }` — indexed-object form. */
@Serializable
internal data class TestKeyPairIndexedObjects(
    val publicKey: Map<String, Int>,
    val secretKey: Map<String, Int>,
)

/** Wire shape `{ "secretKey": [<int>] }` — partial fixture (no publicKey). */
@Serializable
internal data class TestSecretKeyOnly(val secretKey: List<Int>)

/** Wire shape `{ "publicKey": [<int>] }` — partial fixture (no secretKey). */
@Serializable
internal data class TestPublicKeyOnly(val publicKey: List<Int>)

/** Wire shape `{ "signature": "<hex>" }`. */
@Serializable
internal data class TestSignatureBody(val signature: String)

// ─── Asset operation fixtures ─────────────────────────────────────────────────

/** Wire shape `{ "address": "..." }` — minimal address-only owner block. */
@Serializable
internal data class TestAddressBody(val address: String)

/**
 * Wire shape for an NFT entry the bridge returns:
 * `{ "address": "...", "owner": { "address": "..." } }`.
 */
@Serializable
internal data class TestNftEntry(
    val address: String,
    val owner: TestAddressBody,
)

/** Wire shape `{ "nfts": [...] }` — top-level NFT list response. */
@Serializable
internal data class TestNftsResponse(val nfts: List<TestNftEntry>)

/** Wire shape `{ "address": "...", "ownerAddress": "..." }` — single NFT response. */
@Serializable
internal data class TestSingleNftResponse(
    val address: String,
    val ownerAddress: String,
)

/** Wire shape `{ "name": "...", "symbol": "..." }` — jetton metadata sub-block. */
@Serializable
internal data class TestJettonInfo(
    val name: String,
    val symbol: String,
)

/**
 * Wire shape for a jetton wallet entry:
 * `{ "address", "walletAddress", "balance", "info", "isVerified", "prices" }`.
 */
@Serializable
internal data class TestJettonEntry(
    val address: String,
    val walletAddress: String,
    val balance: String,
    val info: TestJettonInfo,
    val isVerified: Boolean,
    val prices: JsonElement,
)

/**
 * Wire shape `{ "addressBook": {…}, "jettons": [...] }` — top-level jetton list
 * response. `addressBook` is left as [JsonElement] so populated/empty maps both
 * round-trip without an extra DTO layer.
 */
@Serializable
internal data class TestJettonsResponse(
    val addressBook: JsonElement,
    val jettons: List<TestJettonEntry>,
)

/** Wire shape `{ "balance": "..." }` — getJettonBalance fixture. */
@Serializable
internal data class TestBalanceBody(val balance: String)

/** Wire shape `{ "jettonWalletAddress": "..." }` — getJettonWalletAddress fixture. */
@Serializable
internal data class TestJettonWalletAddressBody(val jettonWalletAddress: String)

// ─── Wallet operation fixtures ────────────────────────────────────────────────

/**
 * Wire shape `{ "publicKey": "...", "version"?: "..." }` — wallet sub-object the
 * production code reads off `wallet.publicKey` / `wallet.version`. Both fields
 * are optional so the same DTO covers `JSONObject()` empty variants.
 */
@Serializable
internal data class TestWalletInner(
    val publicKey: String? = null,
    val version: String? = null,
)

/**
 * Wire shape `{ "walletId": "...", "wallet": {…} }` — entry inside a `getWallets`
 * response or the body of a `getWallet` / `addWallet` response.
 */
@Serializable
internal data class TestWalletEntry(
    val walletId: String,
    val wallet: TestWalletInner,
)

/** Wire shape `{ "items": [<TestWalletEntry>, …] }` — list response top-level. */
@Serializable
internal data class TestWalletItemsResponse(val items: List<TestWalletEntry>)

// ─── Transaction fixtures ─────────────────────────────────────────────────────

/** Wire shape `{ "address": "...", "amount": "..." }` — single transfer message. */
@Serializable
internal data class TestTransferMessage(
    val address: String,
    val amount: String,
)

/**
 * Wire shape `{ "messages": [...], "fromAddress": "..." }` — top-level transfer
 * response. `messages` is left as [JsonElement] so empty / populated lists can
 * share one DTO without ceremony.
 */
@Serializable
internal data class TestTransferResponse(
    val messages: JsonElement,
    val fromAddress: String,
)

/**
 * Wire shape for the `result` key on a transaction-preview response:
 * `{ "result": "success" }`. Mirrors a separate field name from the
 * [TestPreviewResultData] above which sits inside a nested `data` block.
 */
@Serializable
internal data class TestPreviewResultBody(val result: String)

// ─── TonConnect session fixtures ──────────────────────────────────────────────

/**
 * Wire shape for the dApp metadata embedded in a session entry:
 * `{ "name", "url"?, "iconUrl"? }`. Optional fields use `null` rather than
 * being absent so the same DTO covers both populated and minimal payloads.
 */
@Serializable
internal data class TestSessionDAppInfo(
    val name: String,
    val url: String? = null,
    val iconUrl: String? = null,
)

/**
 * Wire shape for a single session entry in `listSessions`:
 * `{ sessionId, walletId, walletAddress, createdAt, lastActivityAt, privateKey,
 *    publicKey, domain, dAppInfo }`.
 */
@Serializable
internal data class TestSessionEntry(
    val sessionId: String,
    val walletId: String,
    val walletAddress: String,
    val createdAt: String,
    val lastActivityAt: String,
    val privateKey: String,
    val publicKey: String,
    val domain: String,
    val dAppInfo: TestSessionDAppInfo,
)

/** Wire shape `{ "items": [<TestSessionEntry>, …] }` — `listSessions` response. */
@Serializable
internal data class TestSessionItemsResponse(val items: List<TestSessionEntry>)

/** Wire shape `{ "result": "..." }` — handleTonConnectRequest reply fixture. */
@Serializable
internal data class TestResultBody(val result: String)

/** Wire shape `{ "success": true }` — generic mock-RPC ok response. */
@Serializable
internal data class TestSuccessFlag(val success: Boolean)
