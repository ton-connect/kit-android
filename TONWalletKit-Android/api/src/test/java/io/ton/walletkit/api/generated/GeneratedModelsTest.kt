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
package io.ton.walletkit.api.generated

import io.ton.walletkit.model.TONUserFriendlyAddress
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contract tests for generated models.
 *
 * Each test decodes a known JSON literal and asserts field values, or encodes a value and
 * asserts the JSON output. This catches regressions where model regeneration silently changes
 * a @SerialName, flips nullability, or produces a wrong enum wire value — none of which are
 * caught by compilation alone.
 *
 * The JSON strings in each test ARE the contract: they must match exactly what the JS bridge
 * sends and expects.
 */
class GeneratedModelsTest {

    private val json = Json { ignoreUnknownKeys = true }

    // ─────────────────────────────────────────────────────────────────────────────
    // TONAssetType — string enum
    // ─────────────────────────────────────────────────────────────────────────────

    @Test fun `TONAssetType ton decodes from JSON ton`() = assertEquals(TONAssetType.ton, json.decodeFromString<TONAssetType>("\"ton\""))

    @Test fun `TONAssetType jetton decodes from JSON jetton`() = assertEquals(TONAssetType.jetton, json.decodeFromString<TONAssetType>("\"jetton\""))

    @Test fun `TONAssetType nft decodes from JSON nft`() = assertEquals(TONAssetType.nft, json.decodeFromString<TONAssetType>("\"nft\""))

    // ─────────────────────────────────────────────────────────────────────────────
    // TONResult — string enum
    // ─────────────────────────────────────────────────────────────────────────────

    @Test fun `TONResult success decodes from JSON success`() = assertEquals(TONResult.success, json.decodeFromString<TONResult>("\"success\""))

    @Test fun `TONResult failure decodes from JSON failure`() = assertEquals(TONResult.failure, json.decodeFromString<TONResult>("\"failure\""))

    // ─────────────────────────────────────────────────────────────────────────────
    // TONSettlementMethod — string enum (name != value)
    // ─────────────────────────────────────────────────────────────────────────────

    @Test fun `TONSettlementMethod swap decodes from JSON SETTLEMENT_METHOD_SWAP`() =
        assertEquals(TONSettlementMethod.swap, json.decodeFromString<TONSettlementMethod>("\"SETTLEMENT_METHOD_SWAP\""))

    @Test fun `TONSettlementMethod swap encodes to JSON SETTLEMENT_METHOD_SWAP`() =
        assertEquals("\"SETTLEMENT_METHOD_SWAP\"", json.encodeToString(TONSettlementMethod.swap))

    @Test fun `TONSettlementMethod escrow decodes from JSON SETTLEMENT_METHOD_ESCROW`() =
        assertEquals(TONSettlementMethod.escrow, json.decodeFromString<TONSettlementMethod>("\"SETTLEMENT_METHOD_ESCROW\""))

    @Test fun `TONSettlementMethod htlc decodes from JSON SETTLEMENT_METHOD_HTLC`() =
        assertEquals(TONSettlementMethod.htlc, json.decodeFromString<TONSettlementMethod>("\"SETTLEMENT_METHOD_HTLC\""))

    // ─────────────────────────────────────────────────────────────────────────────
    // TONStakingQuoteDirection — string enum
    // ─────────────────────────────────────────────────────────────────────────────

    @Test fun `TONStakingQuoteDirection stake decodes from JSON stake`() =
        assertEquals(TONStakingQuoteDirection.stake, json.decodeFromString<TONStakingQuoteDirection>("\"stake\""))

    @Test fun `TONStakingQuoteDirection unstake decodes from JSON unstake`() =
        assertEquals(TONStakingQuoteDirection.unstake, json.decodeFromString<TONStakingQuoteDirection>("\"unstake\""))

    // ─────────────────────────────────────────────────────────────────────────────
    // TONStreamingUpdateStatus — string enum
    // ─────────────────────────────────────────────────────────────────────────────

    @Test fun `TONStreamingUpdateStatus pending decodes from JSON pending`() =
        assertEquals(TONStreamingUpdateStatus.pending, json.decodeFromString<TONStreamingUpdateStatus>("\"pending\""))

    @Test fun `TONStreamingUpdateStatus confirmed decodes from JSON confirmed`() =
        assertEquals(TONStreamingUpdateStatus.confirmed, json.decodeFromString<TONStreamingUpdateStatus>("\"confirmed\""))

    @Test fun `TONStreamingUpdateStatus finalized decodes from JSON finalized`() =
        assertEquals(TONStreamingUpdateStatus.finalized, json.decodeFromString<TONStreamingUpdateStatus>("\"finalized\""))

    @Test fun `TONStreamingUpdateStatus invalidated decodes from JSON invalidated`() =
        assertEquals(TONStreamingUpdateStatus.invalidated, json.decodeFromString<TONStreamingUpdateStatus>("\"invalidated\""))

    // ─────────────────────────────────────────────────────────────────────────────
    // TONStreamingWatchType — string enum
    // ─────────────────────────────────────────────────────────────────────────────

    @Test fun `TONStreamingWatchType balance decodes from JSON balance`() =
        assertEquals(TONStreamingWatchType.balance, json.decodeFromString<TONStreamingWatchType>("\"balance\""))

    @Test fun `TONStreamingWatchType transactions decodes from JSON transactions`() =
        assertEquals(TONStreamingWatchType.transactions, json.decodeFromString<TONStreamingWatchType>("\"transactions\""))

    @Test fun `TONStreamingWatchType jettons decodes from JSON jettons`() =
        assertEquals(TONStreamingWatchType.jettons, json.decodeFromString<TONStreamingWatchType>("\"jettons\""))

    // ─────────────────────────────────────────────────────────────────────────────
    // TONTransactionStatus — string enum
    // ─────────────────────────────────────────────────────────────────────────────

    @Test fun `TONTransactionStatus unknown decodes from JSON unknown`() =
        assertEquals(TONTransactionStatus.unknown, json.decodeFromString<TONTransactionStatus>("\"unknown\""))

    @Test fun `TONTransactionStatus pending decodes from JSON pending`() =
        assertEquals(TONTransactionStatus.pending, json.decodeFromString<TONTransactionStatus>("\"pending\""))

    @Test fun `TONTransactionStatus completed decodes from JSON completed`() =
        assertEquals(TONTransactionStatus.completed, json.decodeFromString<TONTransactionStatus>("\"completed\""))

    @Test fun `TONTransactionStatus failed decodes from JSON failed`() =
        assertEquals(TONTransactionStatus.failed, json.decodeFromString<TONTransactionStatus>("\"failed\""))

    // ─────────────────────────────────────────────────────────────────────────────
    // TONUnstakeMode — string enum where Kotlin name != JSON value
    // ─────────────────────────────────────────────────────────────────────────────

    @Test fun `TONUnstakeMode instant decodes from JSON INSTANT not instant`() =
        assertEquals(TONUnstakeMode.instant, json.decodeFromString<TONUnstakeMode>("\"INSTANT\""))

    @Test fun `TONUnstakeMode instant encodes to JSON INSTANT`() =
        assertEquals("\"INSTANT\"", json.encodeToString(TONUnstakeMode.instant))

    @Test fun `TONUnstakeMode whenAvailable decodes from JSON WHEN_AVAILABLE`() =
        assertEquals(TONUnstakeMode.whenAvailable, json.decodeFromString<TONUnstakeMode>("\"WHEN_AVAILABLE\""))

    @Test fun `TONUnstakeMode whenAvailable encodes to JSON WHEN_AVAILABLE`() =
        assertEquals("\"WHEN_AVAILABLE\"", json.encodeToString(TONUnstakeMode.whenAvailable))

    @Test fun `TONUnstakeMode roundEnd decodes from JSON ROUND_END`() =
        assertEquals(TONUnstakeMode.roundEnd, json.decodeFromString<TONUnstakeMode>("\"ROUND_END\""))

    @Test fun `TONUnstakeMode roundEnd encodes to JSON ROUND_END`() =
        assertEquals("\"ROUND_END\"", json.encodeToString(TONUnstakeMode.roundEnd))

    // ─────────────────────────────────────────────────────────────────────────────
    // TONSendModeBase — int enum with custom serializer
    // ─────────────────────────────────────────────────────────────────────────────

    @Test fun `TONSendModeBase ordinary decodes from JSON 0`() =
        assertEquals(TONSendModeBase.ordinary, json.decodeFromString<TONSendModeBase>("0"))

    @Test fun `TONSendModeBase ordinary encodes to JSON 0`() =
        assertEquals("0", json.encodeToString(TONSendModeBase.ordinary))

    @Test fun `TONSendModeBase carryAllRemainingIncomingValue decodes from JSON 64`() =
        assertEquals(TONSendModeBase.carryAllRemainingIncomingValue, json.decodeFromString<TONSendModeBase>("64"))

    @Test fun `TONSendModeBase carryAllRemainingIncomingValue encodes to JSON 64`() =
        assertEquals("64", json.encodeToString(TONSendModeBase.carryAllRemainingIncomingValue))

    @Test fun `TONSendModeBase carryAllRemainingBalance decodes from JSON 128`() =
        assertEquals(TONSendModeBase.carryAllRemainingBalance, json.decodeFromString<TONSendModeBase>("128"))

    @Test fun `TONSendModeBase carryAllRemainingBalance encodes to JSON 128`() =
        assertEquals("128", json.encodeToString(TONSendModeBase.carryAllRemainingBalance))

    // ─────────────────────────────────────────────────────────────────────────────
    // TONSendModeFlag — int enum with custom serializer
    // ─────────────────────────────────────────────────────────────────────────────

    @Test fun `TONSendModeFlag destroyAccountIfZero decodes from JSON 32`() =
        assertEquals(TONSendModeFlag.destroyAccountIfZero, json.decodeFromString<TONSendModeFlag>("32"))

    @Test fun `TONSendModeFlag destroyAccountIfZero encodes to JSON 32`() =
        assertEquals("32", json.encodeToString(TONSendModeFlag.destroyAccountIfZero))

    @Test fun `TONSendModeFlag bounceIfFailure decodes from JSON 16`() =
        assertEquals(TONSendModeFlag.bounceIfFailure, json.decodeFromString<TONSendModeFlag>("16"))

    @Test fun `TONSendModeFlag ignoreErrors decodes from JSON 2`() =
        assertEquals(TONSendModeFlag.ignoreErrors, json.decodeFromString<TONSendModeFlag>("2"))

    @Test fun `TONSendModeFlag payGasSeparately decodes from JSON 1`() =
        assertEquals(TONSendModeFlag.payGasSeparately, json.decodeFromString<TONSendModeFlag>("1"))

    // ─────────────────────────────────────────────────────────────────────────────
    // TONTransferRequest — bridge boundary model
    // ─────────────────────────────────────────────────────────────────────────────

    @Test fun `TONTransferRequest required fields map to correct JSON keys`() {
        val decoded = json.decodeFromString<TONTransferRequest>(
            """{"transferAmount":"1000000000","recipientAddress":"$VALID_FRIENDLY_ADDRESS"}""",
        )
        assertEquals("1000000000", decoded.transferAmount)
        assertEquals(VALID_FRIENDLY_ADDRESS, decoded.recipientAddress.value)
    }

    @Test fun `TONTransferRequest comment field maps to correct JSON key`() {
        val decoded = json.decodeFromString<TONTransferRequest>(
            """{"transferAmount":"500000000","recipientAddress":"$VALID_FRIENDLY_ADDRESS","comment":"hello"}""",
        )
        assertEquals("hello", decoded.comment)
    }

    @Test fun `TONTransferRequest absent optional fields are null`() {
        val decoded = json.decodeFromString<TONTransferRequest>(
            """{"transferAmount":"1","recipientAddress":"$VALID_FRIENDLY_ADDRESS"}""",
        )
        assertNull(decoded.comment)
        assertNull(decoded.stateInit)
        assertNull(decoded.payload)
        assertNull(decoded.mode)
        assertNull(decoded.extraCurrency)
    }

    @Test fun `TONTransferRequest extraCurrency field maps to correct JSON key`() {
        val decoded = json.decodeFromString<TONTransferRequest>(
            """{"transferAmount":"1","recipientAddress":"$VALID_FRIENDLY_ADDRESS","extraCurrency":{"1":"500"}}""",
        )
        assertEquals(mapOf("1" to "500"), decoded.extraCurrency)
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // TONTransactionRequest — bridge boundary model
    // ─────────────────────────────────────────────────────────────────────────────

    @Test fun `TONTransactionRequest messages field maps to correct JSON key`() {
        val decoded = json.decodeFromString<TONTransactionRequest>(
            """{"messages":[{"address":"$VALID_FRIENDLY_ADDRESS","amount":"1000000000"}]}""",
        )
        assertEquals(1, decoded.messages.size)
        assertEquals(VALID_FRIENDLY_ADDRESS, decoded.messages[0].address)
        assertEquals("1000000000", decoded.messages[0].amount)
    }

    @Test fun `TONTransactionRequest validUntil field maps to correct JSON key`() {
        val decoded = json.decodeFromString<TONTransactionRequest>(
            """{"messages":[{"address":"$VALID_FRIENDLY_ADDRESS","amount":"1"}],"validUntil":1704067200}""",
        )
        assertEquals(1704067200, decoded.validUntil)
    }

    @Test fun `TONTransactionRequest fromAddress field maps to correct JSON key`() {
        val decoded = json.decodeFromString<TONTransactionRequest>(
            """{"messages":[{"address":"$VALID_FRIENDLY_ADDRESS","amount":"1"}],"fromAddress":"$VALID_FRIENDLY_ADDRESS"}""",
        )
        assertEquals(VALID_FRIENDLY_ADDRESS, decoded.fromAddress)
    }

    @Test fun `TONTransactionRequest absent optional fields are null`() {
        val decoded = json.decodeFromString<TONTransactionRequest>(
            """{"messages":[{"address":"$VALID_FRIENDLY_ADDRESS","amount":"1"}]}""",
        )
        assertNull(decoded.network)
        assertNull(decoded.validUntil)
        assertNull(decoded.fromAddress)
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // TONTransactionEmulatedPreview — bridge boundary model
    // ─────────────────────────────────────────────────────────────────────────────

    @Test fun `TONTransactionEmulatedPreview result success maps to correct JSON key`() {
        val decoded = json.decodeFromString<TONTransactionEmulatedPreview>("""{"result":"success"}""")
        assertEquals(TONResult.success, decoded.result)
    }

    @Test fun `TONTransactionEmulatedPreview absent optional fields are null`() {
        val decoded = json.decodeFromString<TONTransactionEmulatedPreview>("""{"result":"success"}""")
        assertNull(decoded.error)
        assertNull(decoded.trace)
        assertNull(decoded.moneyFlow)
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // TONPreparedSignData — bridge boundary model
    // ─────────────────────────────────────────────────────────────────────────────

    @Test fun `TONPreparedSignData all fields map to correct JSON keys`() {
        val decoded = json.decodeFromString<TONPreparedSignData>(
            """{"address":"$VALID_FRIENDLY_ADDRESS","timestamp":1704067200,"domain":"example.com","payload":{"data":{"type":"text","value":{"content":"hello"}}},"hash":"$VALID_HEX"}""",
        )
        assertEquals(VALID_FRIENDLY_ADDRESS, decoded.address.value)
        assertEquals(1704067200, decoded.timestamp)
        assertEquals("example.com", decoded.domain)
        assertEquals(VALID_HEX, decoded.hash.value)
    }

    @Test fun `TONPreparedSignData payload data text content maps correctly`() {
        val decoded = json.decodeFromString<TONPreparedSignData>(
            """{"address":"$VALID_FRIENDLY_ADDRESS","timestamp":1,"domain":"d.com","payload":{"data":{"type":"text","value":{"content":"sign me"}}},"hash":"$VALID_HEX"}""",
        )
        val text = decoded.payload.data as TONSignData.Text
        assertEquals("sign me", text.value.content)
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // TONProofMessage — bridge boundary model
    // ─────────────────────────────────────────────────────────────────────────────

    @Test fun `TONProofMessage all required fields map to correct JSON keys`() {
        val decoded = json.decodeFromString<TONProofMessage>(
            """{"workchain":0,"addressHash":"$VALID_HEX","timestamp":1704067200,"domain":{"lengthBytes":11,"value":"example.com"},"payload":"deadbeef","stateInit":"$VALID_BASE64"}""",
        )
        assertEquals(0, decoded.workchain)
        assertEquals(VALID_HEX, decoded.addressHash.value)
        assertEquals(1704067200, decoded.timestamp)
        assertEquals(11, decoded.domain.lengthBytes)
        assertEquals("example.com", decoded.domain.value)
        assertEquals("deadbeef", decoded.payload)
        assertEquals(VALID_BASE64, decoded.stateInit.value)
        assertNull(decoded.signature)
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // TONSignData — discriminated union (type field selects variant)
    // ─────────────────────────────────────────────────────────────────────────────

    @Test fun `TONSignData text variant deserializes by type discriminator`() {
        val decoded = json.decodeFromString<TONSignData>(
            """{"type":"text","value":{"content":"hello world"}}""",
        )
        assertTrue(decoded is TONSignData.Text)
        assertEquals("hello world", (decoded as TONSignData.Text).value.content)
    }

    @Test fun `TONSignData text variant serializes with type discriminator`() {
        val data: TONSignData = TONSignData.Text(TONSignDataText(content = "hello"))
        val encoded = json.encodeToString(data)
        assertEquals("""{"type":"text","value":{"content":"hello"}}""", encoded)
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // TONAccountStatus — type/value wrapper: object variants (no data) and string value
    // ─────────────────────────────────────────────────────────────────────────────

    @Test fun `TONAccountStatus active decodes from JSON type active`() =
        assertEquals(TONAccountStatus.Active, json.decodeFromString<TONAccountStatus>("""{"type":"active"}"""))

    @Test fun `TONAccountStatus active serializes without value field`() {
        val status: TONAccountStatus = TONAccountStatus.Active
        assertEquals("""{"type":"active"}""", json.encodeToString(status))
    }

    @Test fun `TONAccountStatus frozen decodes from JSON type frozen`() =
        assertEquals(TONAccountStatus.Frozen, json.decodeFromString<TONAccountStatus>("""{"type":"frozen"}"""))

    @Test fun `TONAccountStatus uninit decodes from JSON type uninit`() =
        assertEquals(TONAccountStatus.Uninit, json.decodeFromString<TONAccountStatus>("""{"type":"uninit"}"""))

    @Test fun `TONAccountStatus unknown variant wraps string in value field`() {
        val decoded = json.decodeFromString<TONAccountStatus>("""{"type":"unknown","value":"custom_status"}""")
        assertTrue(decoded is TONAccountStatus.Unknown)
        assertEquals("custom_status", (decoded as TONAccountStatus.Unknown).value)
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // TONRawStackItem — type/value wrapper: string value, object case, recursive list
    // ─────────────────────────────────────────────────────────────────────────────

    @Test fun `TONRawStackItem num decodes string value`() {
        val decoded = json.decodeFromString<TONRawStackItem>("""{"type":"num","value":"42"}""")
        assertTrue(decoded is TONRawStackItem.Num)
        assertEquals("42", (decoded as TONRawStackItem.Num).value)
    }

    @Test fun `TONRawStackItem null decodes without value field`() =
        assertEquals(TONRawStackItem.Null, json.decodeFromString<TONRawStackItem>("""{"type":"null"}"""))

    @Test fun `TONRawStackItem null serializes without value field`() {
        val item: TONRawStackItem = TONRawStackItem.Null
        assertEquals("""{"type":"null"}""", json.encodeToString(item))
    }

    @Test fun `TONRawStackItem tuple decodes nested items recursively`() {
        val decoded = json.decodeFromString<TONRawStackItem>(
            """{"type":"tuple","value":[{"type":"num","value":"1"},{"type":"null"}]}""",
        )
        assertTrue(decoded is TONRawStackItem.Tuple)
        val items = (decoded as TONRawStackItem.Tuple).value
        assertEquals(2, items.size)
        assertTrue(items[0] is TONRawStackItem.Num)
        assertEquals(TONRawStackItem.Null, items[1])
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // TONSignatureDomain — interface union: single required field + empty variant
    //
    // Contrast with TONSignData (type/value wrapper): here the field is a sibling
    // of the discriminator in JSON, not nested under a "value" key.
    // ─────────────────────────────────────────────────────────────────────────────

    @Test fun `TONSignatureDomain l2 decodes single field directly from object not from value key`() {
        val decoded = json.decodeFromString<TONSignatureDomain>("""{"type":"l2","globalId":42}""")
        assertTrue(decoded is TONSignatureDomain.L2)
        assertEquals(42, (decoded as TONSignatureDomain.L2).globalId)
    }

    @Test fun `TONSignatureDomain l2 serializes field as sibling of discriminator`() {
        val domain: TONSignatureDomain = TONSignatureDomain.L2(globalId = 42)
        assertEquals("""{"type":"l2","globalId":42}""", json.encodeToString(domain))
    }

    @Test fun `TONSignatureDomain empty decodes from JSON type empty`() =
        assertEquals(TONSignatureDomain.Empty, json.decodeFromString<TONSignatureDomain>("""{"type":"empty"}"""))

    @Test fun `TONSignatureDomain empty serializes as type only`() {
        val domain: TONSignatureDomain = TONSignatureDomain.Empty
        assertEquals("""{"type":"empty"}""", json.encodeToString(domain))
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // TONStreamingUpdate — interface union: discriminator embedded in the full object
    //
    // Contrast with TONSignData (type/value wrapper): here the whole object is the
    // payload — no "value" wrapper — and the discriminator comes from a constant
    // field on the member type (TONBalanceUpdate.type = "balance").
    // ─────────────────────────────────────────────────────────────────────────────

    @Test fun `TONStreamingUpdate balance decodes discriminator from embedded object`() {
        val decoded = json.decodeFromString<TONStreamingUpdate>(
            """{"type":"balance","address":"$VALID_FRIENDLY_ADDRESS","rawBalance":"5000","balance":"5.0","status":"confirmed"}""",
        )
        assertTrue(decoded is TONStreamingUpdate.Balance)
        assertEquals("5000", (decoded as TONStreamingUpdate.Balance).value.rawBalance)
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // TONBalanceUpdate — constant field always serializes to the correct value
    //
    // The generator emits `val type: kotlin.String = "balance"` so it always
    // carries its wire value. A regression in the generator could change this to
    // "transactions" or omit it entirely, breaking TONStreamingUpdate dispatch.
    // ─────────────────────────────────────────────────────────────────────────────

    @Test fun `TONBalanceUpdate type constant field deserializes to balance when absent from JSON`() {
        val decoded = json.decodeFromString<TONBalanceUpdate>(
            """{"address":"$VALID_FRIENDLY_ADDRESS","rawBalance":"5000","balance":"5.0","status":"confirmed"}""",
        )
        assertEquals("balance", decoded.type)
    }

    @Test fun `TONBalanceUpdate type constant field serializes as balance with encodeDefaults`() {
        val jsonWithDefaults = Json { encodeDefaults = true }
        val update = TONBalanceUpdate(
            status = TONStreamingUpdateStatus.confirmed,
            address = TONUserFriendlyAddress(VALID_FRIENDLY_ADDRESS),
            rawBalance = "5000",
            balance = "5.0",
        )
        assertTrue(jsonWithDefaults.encodeToString<TONBalanceUpdate>(update).contains("\"type\":\"balance\""))
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // TONOmnistonQuoteMetadata — frozen field (private val JsonElement)
    //
    // A frozen field stores an opaque JSON subtree as JsonElement. It's private,
    // so consumers cannot inspect it, but it must survive a serialize round-trip
    // so the SDK can forward the payload unchanged.
    // ─────────────────────────────────────────────────────────────────────────────

    @Test fun `TONOmnistonQuoteMetadata frozen field survives serialize round-trip`() {
        val input = """{"omnistonQuote":{"quoteId":"abc123","rate":"1.5"}}"""
        val decoded = json.decodeFromString<TONOmnistonQuoteMetadata>(input)
        val encoded = json.encodeToString(decoded)
        assertTrue(encoded.contains("\"omnistonQuote\""))
        assertTrue(encoded.contains("\"quoteId\":\"abc123\""))
    }

    companion object {
        private const val VALID_FRIENDLY_ADDRESS = "EQCD39VS5jcptHL8vMjEXrzGaRcCVYto7HUn4bpAOg8xqB2N"
        private const val VALID_HEX = "0xabcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890ab"
        private const val VALID_BASE64 = "te6cckEBAQEAAgAAAEysuc0="
    }
}
