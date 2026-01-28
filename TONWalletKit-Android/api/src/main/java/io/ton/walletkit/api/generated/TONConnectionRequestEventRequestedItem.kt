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
@Serializable(with = TONConnectionRequestEventRequestedItem.Serializer::class)
sealed class TONConnectionRequestEventRequestedItem {

    /**
     * The discriminator value for this union type
     */
    abstract val type: String

    /**
     *
     */
    @Serializable
    data class TonProof(
        @SerialName("value")
        val value: TONConnectionRequestTonProofRequestedItem,
    ) : TONConnectionRequestEventRequestedItem() {
        override val type: String = "ton_proof"
    }

    /**
     *
     */
    @Serializable
    data class Unknown(
        @SerialName("value")
        val value: kotlinx.serialization.json.JsonElement,
    ) : TONConnectionRequestEventRequestedItem() {
        override val type: String = "unknown"
    }

    /**
     * Case without associated value: tonAddr
     */
    @Serializable
    object TonAddr : TONConnectionRequestEventRequestedItem() {
        override val type: String = "ton_addr"
    }

    internal object Serializer : KSerializer<TONConnectionRequestEventRequestedItem> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("TONConnectionRequestEventRequestedItem")

        @Suppress("UNCHECKED_CAST")
        override fun serialize(encoder: Encoder, value: TONConnectionRequestEventRequestedItem) {
            val jsonEncoder = encoder as? JsonEncoder
                ?: throw SerializationException("TONConnectionRequestEventRequestedItem can only be serialized with JSON")

            val jsonObject = when (value) {
                is TonProof -> {
                    // Use explicit type serializer to avoid runtime class serialization issues (e.g., LinkedHashMap)
                    val valueJson = jsonEncoder.json.encodeToJsonElement(serializer<TONConnectionRequestTonProofRequestedItem>(), value.value)
                    buildJsonObject {
                        put("type", JsonPrimitive("ton_proof"))
                        put("value", valueJson)
                    }
                }
                is Unknown -> {
                    // Use explicit type serializer to avoid runtime class serialization issues (e.g., LinkedHashMap)
                    val valueJson = jsonEncoder.json.encodeToJsonElement(serializer<kotlinx.serialization.json.JsonElement>(), value.value)
                    buildJsonObject {
                        put("type", JsonPrimitive("unknown"))
                        put("value", valueJson)
                    }
                }
                is TonAddr -> JsonObject(mapOf("type" to JsonPrimitive("ton_addr")))
            }
            jsonEncoder.encodeJsonElement(jsonObject)
        }

        override fun deserialize(decoder: Decoder): TONConnectionRequestEventRequestedItem {
            val jsonDecoder = decoder as? JsonDecoder
                ?: throw SerializationException("TONConnectionRequestEventRequestedItem can only be deserialized from JSON")

            val jsonObject = jsonDecoder.decodeJsonElement().jsonObject
            val typeValue = jsonObject["type"]?.jsonPrimitive?.content
                ?: throw SerializationException("Missing 'type' discriminator for TONConnectionRequestEventRequestedItem")

            return when (typeValue) {
                "ton_proof" -> {
                    val valueJson = jsonObject["value"]
                        ?: throw SerializationException("Missing 'value' for TONConnectionRequestEventRequestedItem.TonProof")
                    TonProof(
                        jsonDecoder.json.decodeFromJsonElement(serializer<TONConnectionRequestTonProofRequestedItem>(), valueJson),
                    )
                }
                "unknown" -> {
                    val valueJson = jsonObject["value"]
                        ?: throw SerializationException("Missing 'value' for TONConnectionRequestEventRequestedItem.Unknown")
                    Unknown(
                        jsonDecoder.json.decodeFromJsonElement(serializer<kotlinx.serialization.json.JsonElement>(), valueJson),
                    )
                }
                "ton_addr" -> TonAddr
                else -> throw SerializationException("Unknown type '$typeValue' for TONConnectionRequestEventRequestedItem")
            }
        }
    }
}
