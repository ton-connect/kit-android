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
@Serializable(with = TONAccountStatus.Serializer::class)
sealed class TONAccountStatus {

    /**
     * The discriminator value for this union type
     */
    abstract val type: String

    /**
     *
     */
    @Serializable
    data class Unknown(
        @SerialName("value")
        val value: kotlin.String,
    ) : TONAccountStatus() {
        override val type: String = "unknown"
    }

    /**
     * Case without associated value: active
     */
    @Serializable
    object Active : TONAccountStatus() {
        override val type: String = "active"
    }

    /**
     * Case without associated value: frozen
     */
    @Serializable
    object Frozen : TONAccountStatus() {
        override val type: String = "frozen"
    }

    /**
     * Case without associated value: uninit
     */
    @Serializable
    object Uninit : TONAccountStatus() {
        override val type: String = "uninit"
    }

    internal object Serializer : KSerializer<TONAccountStatus> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("TONAccountStatus")

        @Suppress("UNCHECKED_CAST")
        override fun serialize(encoder: Encoder, value: TONAccountStatus) {
            val jsonEncoder = encoder as? JsonEncoder
                ?: throw SerializationException("TONAccountStatus can only be serialized with JSON")

            val jsonObject = when (value) {
                is Unknown -> {
                    // Use explicit type serializer to avoid runtime class serialization issues (e.g., LinkedHashMap)
                    val valueJson = jsonEncoder.json.encodeToJsonElement(serializer<kotlin.String>(), value.value)
                    buildJsonObject {
                        put("type", JsonPrimitive("unknown"))
                        put("value", valueJson)
                    }
                }
                is Active -> JsonObject(mapOf("type" to JsonPrimitive("active")))
                is Frozen -> JsonObject(mapOf("type" to JsonPrimitive("frozen")))
                is Uninit -> JsonObject(mapOf("type" to JsonPrimitive("uninit")))
            }
            jsonEncoder.encodeJsonElement(jsonObject)
        }

        override fun deserialize(decoder: Decoder): TONAccountStatus {
            val jsonDecoder = decoder as? JsonDecoder
                ?: throw SerializationException("TONAccountStatus can only be deserialized from JSON")

            val jsonObject = jsonDecoder.decodeJsonElement().jsonObject
            val typeValue = jsonObject["type"]?.jsonPrimitive?.content
                ?: throw SerializationException("Missing 'type' discriminator for TONAccountStatus")

            return when (typeValue) {
                "unknown" -> {
                    val valueJson = jsonObject["value"]
                        ?: throw SerializationException("Missing 'value' for TONAccountStatus.Unknown")
                    Unknown(
                        jsonDecoder.json.decodeFromJsonElement(serializer<kotlin.String>(), valueJson),
                    )
                }
                "active" -> Active
                "frozen" -> Frozen
                "uninit" -> Uninit
                else -> throw SerializationException("Unknown type '$typeValue' for TONAccountStatus")
            }
        }
    }
}
