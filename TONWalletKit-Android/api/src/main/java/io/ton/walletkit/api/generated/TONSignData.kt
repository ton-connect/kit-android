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
@Serializable(with = TONSignData.Serializer::class)
sealed class TONSignData {

    /**
     * The discriminator value for this union type
     */
    abstract val type: String

    /**
     *
     */
    @Serializable
    data class Text(
        @SerialName("value")
        val value: TONSignDataText,
    ) : TONSignData() {
        override val type: String = "text"
    }

    /**
     *
     */
    @Serializable
    data class Binary(
        @SerialName("value")
        val value: TONSignDataBinary,
    ) : TONSignData() {
        override val type: String = "binary"
    }

    /**
     *
     */
    @Serializable
    data class Cell(
        @SerialName("value")
        val value: TONSignDataCell,
    ) : TONSignData() {
        override val type: String = "cell"
    }

    internal object Serializer : KSerializer<TONSignData> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("TONSignData")

        @Suppress("UNCHECKED_CAST")
        override fun serialize(encoder: Encoder, value: TONSignData) {
            val jsonEncoder = encoder as? JsonEncoder
                ?: throw SerializationException("TONSignData can only be serialized with JSON")

            val jsonObject = when (value) {
                is Text -> {
                    // Use explicit type serializer to avoid runtime class serialization issues (e.g., LinkedHashMap)
                    val valueJson = jsonEncoder.json.encodeToJsonElement(serializer<TONSignDataText>(), value.value)
                    buildJsonObject {
                        put("type", JsonPrimitive("text"))
                        put("value", valueJson)
                    }
                }
                is Binary -> {
                    // Use explicit type serializer to avoid runtime class serialization issues (e.g., LinkedHashMap)
                    val valueJson = jsonEncoder.json.encodeToJsonElement(serializer<TONSignDataBinary>(), value.value)
                    buildJsonObject {
                        put("type", JsonPrimitive("binary"))
                        put("value", valueJson)
                    }
                }
                is Cell -> {
                    // Use explicit type serializer to avoid runtime class serialization issues (e.g., LinkedHashMap)
                    val valueJson = jsonEncoder.json.encodeToJsonElement(serializer<TONSignDataCell>(), value.value)
                    buildJsonObject {
                        put("type", JsonPrimitive("cell"))
                        put("value", valueJson)
                    }
                }
            }
            jsonEncoder.encodeJsonElement(jsonObject)
        }

        override fun deserialize(decoder: Decoder): TONSignData {
            val jsonDecoder = decoder as? JsonDecoder
                ?: throw SerializationException("TONSignData can only be deserialized from JSON")

            val jsonObject = jsonDecoder.decodeJsonElement().jsonObject
            val typeValue = jsonObject["type"]?.jsonPrimitive?.content
                ?: throw SerializationException("Missing 'type' discriminator for TONSignData")

            return when (typeValue) {
                "text" -> {
                    val valueJson = jsonObject["value"]
                        ?: throw SerializationException("Missing 'value' for TONSignData.Text")
                    Text(
                        jsonDecoder.json.decodeFromJsonElement(serializer<TONSignDataText>(), valueJson),
                    )
                }
                "binary" -> {
                    val valueJson = jsonObject["value"]
                        ?: throw SerializationException("Missing 'value' for TONSignData.Binary")
                    Binary(
                        jsonDecoder.json.decodeFromJsonElement(serializer<TONSignDataBinary>(), valueJson),
                    )
                }
                "cell" -> {
                    val valueJson = jsonObject["value"]
                        ?: throw SerializationException("Missing 'value' for TONSignData.Cell")
                    Cell(
                        jsonDecoder.json.decodeFromJsonElement(serializer<TONSignDataCell>(), valueJson),
                    )
                }
                else -> throw SerializationException("Unknown type '$typeValue' for TONSignData")
            }
        }
    }
}
