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
package io.ton.walletkit.api.generationtests

import io.ton.walletkit.api.generatedtest.AddressRef
import io.ton.walletkit.api.generatedtest.AnnotatedFields
import io.ton.walletkit.api.generatedtest.Command
import io.ton.walletkit.api.generatedtest.Direction
import io.ton.walletkit.api.generatedtest.Flags
import io.ton.walletkit.api.generatedtest.GitEvent
import io.ton.walletkit.api.generatedtest.InfoNotification
import io.ton.walletkit.api.generatedtest.Int64Fields
import io.ton.walletkit.api.generatedtest.Notification
import io.ton.walletkit.api.generatedtest.Paginated
import io.ton.walletkit.api.generatedtest.PushEvent
import io.ton.walletkit.api.generatedtest.SettlementMethod
import io.ton.walletkit.api.generatedtest.StackItem
import io.ton.walletkit.api.generatedtest.Theme
import io.ton.walletkit.api.generatedtest.ThemeDark
import io.ton.walletkit.api.generatedtest.ThemeLight
import io.ton.walletkit.api.generatedtest.Versioned
import io.ton.walletkit.api.generatedtest.WalletAlias
import io.ton.walletkit.api.generatedtest.WidthFormats
import io.ton.walletkit.api.generatedtest.Wire
import io.ton.walletkit.api.generatedtest.WithDefaults
import io.ton.walletkit.api.generatedtest.WithUrl
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.jvmErasure

/**
 * Validates the Kotlin models generated from `model-patterns.fixture.ts`.
 *
 * Each test pins one fixture pattern (numbered 1-18 in the fixture file) so any
 * regression in the TS→JSON-Schema→OpenAPI→Kotlin pipeline surfaces here. To
 * regenerate the models being tested, run:
 *
 *     Scripts/generate-api/generate-test-models.sh <path-to-kit>/packages/walletkit
 */
class GeneratedModelsTest {

    // encodeDefaults = true so constant fields (e.g. `type = "info"`) appear on the
    // wire. Without it, kotlinx.serialization skips properties backed by defaults,
    // and the discriminator never reaches JSON.
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // ── 1. TypeScript enum → member-name → camelCase, raw value preserved ─────

    @Test
    fun `enum preserves raw value and camelCases member name`() {
        assertEquals("north", Direction.north.value)
        assertEquals("south_west", Direction.southWest.value)
        // Round-trip: JSON uses rawValue, not the Kotlin identifier.
        assertEquals("\"south_west\"", json.encodeToString(Direction.serializer(), Direction.southWest))
        assertEquals(Direction.southWest, json.decodeFromString(Direction.serializer(), "\"south_west\""))
    }

    // ── 2. Const-object enum → prefix stripped from Kotlin name, full rawValue kept ─

    @Test
    fun `const-object enum strips common prefix from Kotlin names`() {
        assertEquals("SETTLEMENT_METHOD_SWAP", SettlementMethod.swap.value)
        assertEquals("SETTLEMENT_METHOD_ESCROW", SettlementMethod.escrow.value)
        // Kotlin member name must be the stripped short form, not the full rawValue.
        assertTrue(SettlementMethod.values().map { it.name }.containsAll(listOf("swap", "escrow")))
        assertFalse(SettlementMethod.values().any { it.name.startsWith("SETTLEMENT_METHOD_") })
    }

    // ── 3. @format int → kotlin.Int; @format frozen → opaque JsonElement ──────

    @Test
    fun `format int coerces number to Int and frozen renders as JsonElement`() {
        val ctor = AnnotatedFields::class.primaryConstructor!!
        val params = ctor.parameters.associateBy { it.name }
        assertEquals("kotlin.Int", params["count"]!!.type.jvmErasure.qualifiedName)
        assertEquals("kotlin.Int", params["ratio"]!!.type.jvmErasure.qualifiedName)
        assertEquals(
            "kotlinx.serialization.json.JsonElement",
            params["extra"]!!.type.jvmErasure.qualifiedName,
        )
    }

    // ── 4. @discriminator interface union → sealed class + DISCRIMINATOR_FIELD ─

    @Test
    fun `discriminator interface union builds sealed hierarchy with variants`() {
        val subclasses = Notification::class.sealedSubclasses.map { it.simpleName }
        assertTrue(subclasses.containsAll(listOf("Info", "Alert", "Ping")))
        // Full variant Info must wrap the original interface (promoted to a ref).
        val infoCtor = Notification.Info::class.primaryConstructor!!
        assertEquals(
            "io.ton.walletkit.api.generatedtest.InfoNotification",
            infoCtor.parameters.single { it.name == "value" }.type.jvmErasure.qualifiedName,
        )
        // Empty variant Ping is a kotlin object (no fields).
        assertTrue(Notification.Ping::class.objectInstance != null)

        // Round-trip: discriminator field plus inline fields.
        val alert = Notification.Alert(level = "warn")
        val encoded = json.encodeToString(Notification.serializer(), alert).let { json.parseToJsonElement(it).jsonObject }
        assertEquals("alert", encoded["type"]?.jsonPrimitive?.content)
        assertEquals("warn", encoded["level"]?.jsonPrimitive?.content)
    }

    // ── 5. Inline type-literal union → sealed class with abstract `type` field ─

    @Test
    fun `inline union exposes abstract type field and per-variant hasValue shape`() {
        val intItem: StackItem = StackItem.Int(value = 42)
        val strItem: StackItem = StackItem.Str(value = "hi")
        val empty: StackItem = StackItem.Empty
        assertEquals("int", intItem.type)
        assertEquals("str", strItem.type)
        assertEquals("empty", empty.type)

        val roundTrip = json.decodeFromString(
            StackItem.serializer(),
            json.encodeToString(StackItem.serializer(), intItem),
        )
        assertEquals(intItem, roundTrip)
    }

    // ── 6. Generic interface → type-parameterised Kotlin data class ───────────

    @Test
    fun `generic interface renders as parameterised data class`() {
        val typeParams = Paginated::class.typeParameters
        assertEquals(1, typeParams.size)
        val page = Paginated(items = listOf("a", "b"), total = 2)
        assertEquals(listOf("a", "b"), page.items)
        assertEquals(2, page.total)
    }

    // ── 7. Pure $ref type alias → Kotlin typealias ────────────────────────────

    @Test
    fun `pure ref type alias becomes Kotlin typealias`() {
        // WalletAlias must be usable interchangeably with AddressRef.
        val alias: WalletAlias = AddressRef(addr = "EQ...")
        assertEquals("EQ...", alias.addr)
    }

    // ── 8. Standalone literal property → moved into constant field with default ─

    @Test
    fun `standalone literal property becomes constant field with default value`() {
        val v = Versioned(payload = "p")
        assertEquals("v2", v.version)
        val encoded = json.parseToJsonElement(json.encodeToString(Versioned.serializer(), v)).jsonObject
        assertEquals("v2", encoded["version"]?.jsonPrimitive?.content)
    }

    // ── 9. @default → stripped at schema-emit time, not baked into Kotlin ─────

    @Test
    fun `default annotation does not create a Kotlin default value`() {
        // Both params remain required — no default leaked from @default true.
        val ctor = WithDefaults::class.primaryConstructor!!
        assertTrue(ctor.parameters.none { it.isOptional })
    }

    // ── 10. ALLCAPS enum members → lowercased Kotlin identifiers ──────────────

    @Test
    fun `ALLCAPS enum members become lowercase Kotlin identifiers`() {
        assertEquals(listOf("low", "high"), Flags.values().map { it.name })
        assertEquals(0, Flags.low.value)
        assertEquals(1, Flags.high.value)
    }

    // ── 11. Multiple literal properties → multiple constant fields ────────────

    @Test
    fun `multiple literal properties produce multiple constant fields`() {
        val w = Wire(payload = "x")
        assertEquals("0xff", w.magic)
        assertEquals("2", w.version)
    }

    // ── 12. @discriminator with a non-'type' field → discriminator field respected ─

    @Test
    fun `discriminator field name is configurable, not hardcoded to 'type'`() {
        assertEquals("event", GitEvent.DISCRIMINATOR_FIELD)
        val push = GitEvent.Push(value = PushEvent(branch = "main", ref = "refs/heads/main"))
        val encoded = json.parseToJsonElement(json.encodeToString(GitEvent.serializer(), push)).jsonObject
        assertEquals("push", encoded["event"]?.jsonPrimitive?.content)
    }

    // ── 13. Discriminator rawValue with underscore → camelCased case name ─────

    @Test
    fun `underscored discriminator rawValue camelCases the Kotlin case name`() {
        val subclassNames = Theme::class.sealedSubclasses.map { it.simpleName }
        assertTrue(subclassNames.contains("DarkMode"))
        assertTrue(subclassNames.contains("LightMode"))
        // rawValue on the wire stays underscored.
        val dark: Theme = Theme.DarkMode(value = ThemeDark(hex = "#000", opacity = 1))
        val encoded = json.parseToJsonElement(json.encodeToString(Theme.serializer(), dark)).jsonObject
        assertEquals("dark_mode", encoded["type"]?.jsonPrimitive?.content)
    }

    // ── 14. Optional single-field variant → nullable param with default ───────

    @Test
    fun `optional single-field variant becomes nullable with default`() {
        val ctor = Command.Start::class.primaryConstructor!!
        val timeoutParam = ctor.parameters.single { it.name == "timeout" }
        assertTrue(timeoutParam.type.isMarkedNullable)
        assertTrue(timeoutParam.isOptional)
        // Round-trip the empty Stop variant (no fields beyond the discriminator).
        val stop: Command = Command.Stop
        val obj = json.parseToJsonElement(json.encodeToString(Command.serializer(), stop)).jsonObject
        assertEquals("stop", obj["type"]?.jsonPrimitive?.content)
    }

    // ── 15. @format int32/uint32 → kotlin.Int ─────────────────────────────────

    @Test
    fun `int32 and uint32 format both map to kotlin Int`() {
        val ctor = WidthFormats::class.primaryConstructor!!
        val params = ctor.parameters.associateBy { it.name }
        assertEquals("kotlin.Int", params["seqno"]!!.type.jvmErasure.qualifiedName)
        assertEquals("kotlin.Int", params["timestampSec"]!!.type.jvmErasure.qualifiedName)
    }

    // ── 16. @format int64 → kotlin.Long ───────────────────────────────────────

    @Test
    fun `int64 format maps to kotlin Long`() {
        val ctor = Int64Fields::class.primaryConstructor!!
        val bigAmount = ctor.parameters.single { it.name == "bigAmount" }
        assertEquals("kotlin.Long", bigAmount.type.jvmErasure.qualifiedName)
    }

    // ── 17. @format url → kotlin.String (type preserved, format passed through) ─

    @Test
    fun `url format preserves kotlin String and does not coerce to integer`() {
        val ctor = WithUrl::class.primaryConstructor!!
        val params = ctor.parameters.associateBy { it.name }
        assertEquals("kotlin.String", params["iconUrl"]!!.type.jvmErasure.qualifiedName)
        assertEquals("kotlin.String", params["label"]!!.type.jvmErasure.qualifiedName)
    }

    // ── 18. String-literal + ref union (PR #361) → model file present ─────────
    //
    // The `x-string-literal-union` vendor extension is emitted by the JS schema
    // generator but the Kotlin template does not yet render a dedicated sealed
    // hierarchy for it. This test pins the current "stub" behaviour so that if
    // template support is added later, the failure calls out the shape change.

    @Test
    fun `string literal ref union produces a model file (stub shape until template lands)`() {
        // If/when the template renders a real sealed hierarchy for Endpoint, this
        // assertion must be replaced by one that checks each literal / ref case.
        val endpointClass = runCatching { Class.forName("io.ton.walletkit.api.generatedtest.Endpoint") }
        assertNotNull(endpointClass.getOrNull())
    }

    // ── Smoke: round-trip a full Notification.Info to catch wire-format drift ─

    @Test
    fun `Notification Info round trips through InfoNotification promotion`() {
        val info: Notification = Notification.Info(
            value = InfoNotification(title = "hello", body = "world"),
        )
        val encoded = json.encodeToString(Notification.serializer(), info)
        // Variant promotion means the discriminator lives on the wrapped payload,
        // not on a wrapper envelope.
        val obj = json.parseToJsonElement(encoded).jsonObject
        assertEquals("info", obj["type"]?.jsonPrimitive?.content)
        assertEquals("hello", obj["title"]?.jsonPrimitive?.content)

        val decoded = json.decodeFromString(Notification.serializer(), encoded) as Notification.Info
        assertEquals("hello", decoded.value.title)
        assertEquals("world", decoded.value.body)
    }

    // ── Sanity: every expected fixture-pattern class is present on the classpath ─

    @Test
    fun `every fixture pattern produces a generated Kotlin class`() {
        // Typealiases (e.g. WalletAlias = AddressRef) are compile-time only and
        // have no runtime Class — verified separately via the typealias test.
        val expected = listOf(
            "Direction", "SettlementMethod", "AnnotatedFields",
            "Notification", "InfoNotification",
            "StackItem", "Paginated", "AddressRef",
            "Versioned", "WithDefaults", "Flags", "Wire",
            "GitEvent", "PushEvent",
            "Theme", "ThemeDark", "ThemeLight",
            "Command", "WidthFormats", "Int64Fields", "WithUrl", "Endpoint",
        )
        for (name in expected) {
            val fqn = "io.ton.walletkit.api.generatedtest.$name"
            assertNotNull("Expected generated class $fqn", runCatching { Class.forName(fqn) }.getOrNull())
        }
    }

    // ── Sanity: single-field variants are inlined, not emitted as separate files ─
    //
    // AlertNotification, TagEvent, StartCommand have only one non-discriminator
    // field each — the post-processor inlines the field into the parent union,
    // and the boilerplate-only source file is removed by the generation script.

    @Test
    fun `single-field variants are inlined and do not produce standalone files`() {
        for (inlined in listOf("AlertNotification", "TagEvent", "StartCommand")) {
            val fqn = "io.ton.walletkit.api.generatedtest.$inlined"
            assertTrue(
                "Expected $fqn to be inlined (no standalone generated class)",
                runCatching { Class.forName(fqn) }.isFailure,
            )
        }
    }

    // ── Regression guard: nothing accidentally depends on a real-API-only type ─

    @Test
    fun `fixture-derived classes do not reference the prefixed TON namespace`() {
        // The test config drops modelNamePrefix, so none of the generated
        // fixture classes should be named TON*.
        val genericMembers = Notification::class.sealedSubclasses +
            Theme::class.sealedSubclasses +
            GitEvent::class.sealedSubclasses +
            StackItem::class.sealedSubclasses +
            Command::class.sealedSubclasses
        val anyTonPrefixed = genericMembers.any { it.simpleName?.startsWith("TON") == true }
        assertFalse(anyTonPrefixed)
    }

    // ── WithDefaults payload shape ────────────────────────────────────────────

    @Test
    fun `WithDefaults round trips both fields`() {
        val w = WithDefaults(enabled = true, count = 3)
        val obj = json.parseToJsonElement(json.encodeToString(WithDefaults.serializer(), w)).jsonObject
        assertEquals(true, obj["enabled"]?.jsonPrimitive?.content?.toBoolean())
        assertEquals("3", obj["count"]?.jsonPrimitive?.content)
    }

    // ── Constant fields appear in the serialised output ──────────────────────

    @Test
    fun `Wire serialises both magic and version constants`() {
        val encoded = json.encodeToString(Wire.serializer(), Wire(payload = "abc"))
        val obj = json.parseToJsonElement(encoded).jsonObject
        assertEquals("0xff", obj["magic"]?.jsonPrimitive?.content)
        assertEquals("2", obj["version"]?.jsonPrimitive?.content)
        assertEquals("abc", obj["payload"]?.jsonPrimitive?.content)
    }

    // ── Reflection helper: `memberProperties` on data classes is non-empty ───
    //
    // A sanity check that the generator actually produced data-class bodies for
    // the plain object types, not empty classes.

    @Test
    fun `plain object fixture types have declared member properties`() {
        val nonEmpty = listOf(
            AnnotatedFields::class,
            WidthFormats::class,
            Int64Fields::class,
            WithUrl::class,
            WithDefaults::class,
            Versioned::class,
            Wire::class,
            InfoNotification::class,
            PushEvent::class,
            ThemeDark::class,
            ThemeLight::class,
            AddressRef::class,
        )
        for (klass in nonEmpty) {
            assertTrue(
                "Expected ${klass.simpleName} to declare member properties",
                klass.memberProperties.isNotEmpty(),
            )
        }
    }
}
