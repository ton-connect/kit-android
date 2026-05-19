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
package io.ton.walletkit.bridge

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [BridgeCodec.encode] that prove the `when` branch order is safe.
 *
 * Specifically, `is JsonElement` is checked before `is List<*>` and `is Map<*, *>`.
 * `JsonObject` is-a `Map<String, JsonElement>` and `JsonArray` is-a `List<JsonElement>`,
 * so without that ordering the codec would re-wrap them. These tests fail if anyone
 * reorders the branches or removes the `is JsonElement` arm.
 */
class BridgeCodecTest {

    private val codec = BridgeCodec(Json)

    // --- JsonElement subtypes are returned as-is (identity) ---

    @Test
    fun encode_jsonObject_returnsSameInstance() {
        val input = buildJsonObject {
            put("a", 1)
            put("b", "two")
        }
        val result = codec.encode(input)
        assertSame("JsonObject must pass through without being re-wrapped", input, result)
    }

    @Test
    fun encode_jsonArray_returnsSameInstance() {
        val input = buildJsonArray {
            add(JsonPrimitive(1))
            add(JsonPrimitive("two"))
        }
        val result = codec.encode(input)
        assertSame("JsonArray must pass through without being re-wrapped", input, result)
    }

    @Test
    fun encode_jsonPrimitive_returnsSameInstance() {
        val input = JsonPrimitive("hello")
        val result = codec.encode(input)
        assertSame("JsonPrimitive must pass through", input, result)
    }

    @Test
    fun encode_jsonNullInstance_returnsJsonNull() {
        // The kotlinx `JsonNull` singleton (not Kotlin `null`) should hit the
        // `is JsonElement` branch and be returned as-is.
        val result = codec.encode(JsonNull)
        assertSame(JsonNull, result)
    }

    // --- Kotlin `null` becomes JsonNull ---

    @Test
    fun encode_kotlinNull_returnsJsonNull() {
        assertSame(JsonNull, codec.encode(null))
    }

    // --- Plain Kotlin types are wrapped ---

    @Test
    fun encode_string_wrapsAsJsonPrimitive() {
        assertEquals(JsonPrimitive("hello"), codec.encode("hello"))
    }

    @Test
    fun encode_boolean_wrapsAsJsonPrimitive() {
        assertEquals(JsonPrimitive(true), codec.encode(true))
    }

    @Test
    fun encode_number_wrapsAsJsonPrimitive() {
        assertEquals(JsonPrimitive(42), codec.encode(42))
        assertEquals(JsonPrimitive(3.14), codec.encode(3.14))
    }

    @Test
    fun encode_listOfPrimitives_wrapsAsJsonArray() {
        val result = codec.encode(listOf(1, "two", true))
        assertTrue(result is JsonArray)
        val array = result as JsonArray
        assertEquals(3, array.size)
        assertEquals(JsonPrimitive(1), array[0])
        assertEquals(JsonPrimitive("two"), array[1])
        assertEquals(JsonPrimitive(true), array[2])
    }

    @Test
    fun encode_mapOfPrimitives_wrapsAsJsonObject() {
        val result = codec.encode(mapOf("a" to 1, "b" to "two"))
        assertTrue(result is JsonObject)
        val obj = result as JsonObject
        assertEquals(JsonPrimitive(1), obj["a"])
        assertEquals(JsonPrimitive("two"), obj["b"])
    }

    // --- Nested case: a plain list containing JsonElements ---

    @Test
    fun encode_listContainingJsonElements_recursesAndKeepsThemAsIs() {
        val nestedObject = buildJsonObject { put("nested", true) }
        val result = codec.encode(listOf("plain", nestedObject))
        assertTrue(result is JsonArray)
        val array = result as JsonArray
        assertEquals(JsonPrimitive("plain"), array[0])
        // The inner JsonObject must survive recursion intact, not be re-wrapped.
        assertSame(nestedObject, array[1])
    }
}
