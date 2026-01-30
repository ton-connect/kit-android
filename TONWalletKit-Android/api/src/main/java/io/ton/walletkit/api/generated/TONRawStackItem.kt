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
@file:Suppress(
    "ArrayInDataClass",
    "EnumEntryName",
    "RemoveRedundantQualifierName",
    "UnusedImport",
)

package io.ton.walletkit.api.generated

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.serializer

/**
 *
 *
 * This is a discriminated union type. Use the appropriate subclass based on the `type` field.
 */
@Serializable(with = TONRawStackItem.Serializer::class)
sealed class TONRawStackItem {

    /**
     * The discriminator value for this union type
     */
    abstract val type: String

    /**
     *
     */
    @Serializable
    data class Num(
        @SerialName("value")
        val value: kotlin.String,
    ) : TONRawStackItem() {
        override val type: String = "num"
    }

    /**
     *
     */
    @Serializable
    data class Cell(
        @SerialName("value")
        val value: kotlin.String,
    ) : TONRawStackItem() {
        override val type: String = "cell"
    }

    /**
     *
     */
    @Serializable
    data class Slice(
        @SerialName("value")
        val value: kotlin.String,
    ) : TONRawStackItem() {
        override val type: String = "slice"
    }

    /**
     *
     */
    @Serializable
    data class Builder(
        @SerialName("value")
        val value: kotlin.String,
    ) : TONRawStackItem() {
        override val type: String = "builder"
    }

    /**
     *
     */
    @Serializable
    data class Tuple(
        @SerialName("value")
        val value: kotlin.collections.List<TONRawStackItem>,
    ) : TONRawStackItem() {
        override val type: String = "tuple"
    }

    /**
     *
     */
    @Serializable
    data class List(
        @SerialName("value")
        val value: kotlin.collections.List<TONRawStackItem>,
    ) : TONRawStackItem() {
        override val type: String = "list"
    }

    /**
     * Case without associated value: null
     */
    @Serializable
    object Null : TONRawStackItem() {
        override val type: String = "null"
    }

    internal object Serializer : KSerializer<TONRawStackItem> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("TONRawStackItem")

        @Suppress("UNCHECKED_CAST")
        override fun serialize(encoder: Encoder, value: TONRawStackItem) {
            val jsonEncoder = encoder as? JsonEncoder
                ?: throw SerializationException("TONRawStackItem can only be serialized with JSON")

            val jsonObject = when (value) {
                is Num -> {
                    // Use explicit type serializer to avoid runtime class serialization issues (e.g., LinkedHashMap)
                    val valueJson = jsonEncoder.json.encodeToJsonElement(serializer<kotlin.String>(), value.value)
                    buildJsonObject {
                        put("type", JsonPrimitive("num"))
                        put("value", valueJson)
                    }
                }
                is Cell -> {
                    // Use explicit type serializer to avoid runtime class serialization issues (e.g., LinkedHashMap)
                    val valueJson = jsonEncoder.json.encodeToJsonElement(serializer<kotlin.String>(), value.value)
                    buildJsonObject {
                        put("type", JsonPrimitive("cell"))
                        put("value", valueJson)
                    }
                }
                is Slice -> {
                    // Use explicit type serializer to avoid runtime class serialization issues (e.g., LinkedHashMap)
                    val valueJson = jsonEncoder.json.encodeToJsonElement(serializer<kotlin.String>(), value.value)
                    buildJsonObject {
                        put("type", JsonPrimitive("slice"))
                        put("value", valueJson)
                    }
                }
                is Builder -> {
                    // Use explicit type serializer to avoid runtime class serialization issues (e.g., LinkedHashMap)
                    val valueJson = jsonEncoder.json.encodeToJsonElement(serializer<kotlin.String>(), value.value)
                    buildJsonObject {
                        put("type", JsonPrimitive("builder"))
                        put("value", valueJson)
                    }
                }
                is Tuple -> {
                    // Use explicit type serializer to avoid runtime class serialization issues (e.g., LinkedHashMap)
                    val valueJson = jsonEncoder.json.encodeToJsonElement(serializer<kotlin.collections.List<TONRawStackItem>>(), value.value)
                    buildJsonObject {
                        put("type", JsonPrimitive("tuple"))
                        put("value", valueJson)
                    }
                }
                is List -> {
                    // Use explicit type serializer to avoid runtime class serialization issues (e.g., LinkedHashMap)
                    val valueJson = jsonEncoder.json.encodeToJsonElement(serializer<kotlin.collections.List<TONRawStackItem>>(), value.value)
                    buildJsonObject {
                        put("type", JsonPrimitive("list"))
                        put("value", valueJson)
                    }
                }
                is Null -> JsonObject(mapOf("type" to JsonPrimitive("null")))
            }
            jsonEncoder.encodeJsonElement(jsonObject)
        }

        override fun deserialize(decoder: Decoder): TONRawStackItem {
            val jsonDecoder = decoder as? JsonDecoder
                ?: throw SerializationException("TONRawStackItem can only be deserialized from JSON")

            val jsonObject = jsonDecoder.decodeJsonElement().jsonObject
            val typeValue = jsonObject["type"]?.jsonPrimitive?.content
                ?: throw SerializationException("Missing 'type' discriminator for TONRawStackItem")

            return when (typeValue) {
                "num" -> {
                    val valueJson = jsonObject["value"]
                        ?: throw SerializationException("Missing 'value' for TONRawStackItem.Num")
                    Num(
                        jsonDecoder.json.decodeFromJsonElement(serializer<kotlin.String>(), valueJson),
                    )
                }
                "cell" -> {
                    val valueJson = jsonObject["value"]
                        ?: throw SerializationException("Missing 'value' for TONRawStackItem.Cell")
                    Cell(
                        jsonDecoder.json.decodeFromJsonElement(serializer<kotlin.String>(), valueJson),
                    )
                }
                "slice" -> {
                    val valueJson = jsonObject["value"]
                        ?: throw SerializationException("Missing 'value' for TONRawStackItem.Slice")
                    Slice(
                        jsonDecoder.json.decodeFromJsonElement(serializer<kotlin.String>(), valueJson),
                    )
                }
                "builder" -> {
                    val valueJson = jsonObject["value"]
                        ?: throw SerializationException("Missing 'value' for TONRawStackItem.Builder")
                    Builder(
                        jsonDecoder.json.decodeFromJsonElement(serializer<kotlin.String>(), valueJson),
                    )
                }
                "tuple" -> {
                    val valueJson = jsonObject["value"]
                        ?: throw SerializationException("Missing 'value' for TONRawStackItem.Tuple")
                    Tuple(
                        jsonDecoder.json.decodeFromJsonElement(serializer<kotlin.collections.List<TONRawStackItem>>(), valueJson),
                    )
                }
                "list" -> {
                    val valueJson = jsonObject["value"]
                        ?: throw SerializationException("Missing 'value' for TONRawStackItem.List")
                    List(
                        jsonDecoder.json.decodeFromJsonElement(serializer<kotlin.collections.List<TONRawStackItem>>(), valueJson),
                    )
                }
                "null" -> Null
                else -> throw SerializationException("Unknown type '$typeValue' for TONRawStackItem")
            }
        }
    }
}
