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
@Serializable(with = TONIntentRequestEvent.Serializer::class)
sealed class TONIntentRequestEvent {

    /**
     * The discriminator value for this union type
     */
    abstract val type: String

    /**
     *
     */
    @Serializable
    data class Transaction(
        @SerialName("value")
        val value: TONTransactionIntentRequestEvent,
    ) : TONIntentRequestEvent() {
        override val type: String = "transaction"
    }

    /**
     *
     */
    @Serializable
    data class SignData(
        @SerialName("value")
        val value: TONSignDataIntentRequestEvent,
    ) : TONIntentRequestEvent() {
        override val type: String = "signData"
    }

    /**
     *
     */
    @Serializable
    data class Action(
        @SerialName("value")
        val value: TONActionIntentRequestEvent,
    ) : TONIntentRequestEvent() {
        override val type: String = "action"
    }

    internal object Serializer : KSerializer<TONIntentRequestEvent> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("TONIntentRequestEvent")

        @Suppress("UNCHECKED_CAST")
        override fun serialize(encoder: Encoder, value: TONIntentRequestEvent) {
            val jsonEncoder = encoder as? JsonEncoder
                ?: throw SerializationException("TONIntentRequestEvent can only be serialized with JSON")

            val jsonObject = when (value) {
                is Transaction -> {
                    // Use explicit type serializer to avoid runtime class serialization issues (e.g., LinkedHashMap)
                    val valueJson = jsonEncoder.json.encodeToJsonElement(serializer<TONTransactionIntentRequestEvent>(), value.value)
                    buildJsonObject {
                        put("type", JsonPrimitive("transaction"))
                        put("value", valueJson)
                    }
                }
                is SignData -> {
                    // Use explicit type serializer to avoid runtime class serialization issues (e.g., LinkedHashMap)
                    val valueJson = jsonEncoder.json.encodeToJsonElement(serializer<TONSignDataIntentRequestEvent>(), value.value)
                    buildJsonObject {
                        put("type", JsonPrimitive("signData"))
                        put("value", valueJson)
                    }
                }
                is Action -> {
                    // Use explicit type serializer to avoid runtime class serialization issues (e.g., LinkedHashMap)
                    val valueJson = jsonEncoder.json.encodeToJsonElement(serializer<TONActionIntentRequestEvent>(), value.value)
                    buildJsonObject {
                        put("type", JsonPrimitive("action"))
                        put("value", valueJson)
                    }
                }
            }
            jsonEncoder.encodeJsonElement(jsonObject)
        }

        override fun deserialize(decoder: Decoder): TONIntentRequestEvent {
            val jsonDecoder = decoder as? JsonDecoder
                ?: throw SerializationException("TONIntentRequestEvent can only be deserialized from JSON")

            val jsonObject = jsonDecoder.decodeJsonElement().jsonObject
            val typeValue = jsonObject["type"]?.jsonPrimitive?.content
                ?: throw SerializationException("Missing 'type' discriminator for TONIntentRequestEvent")

            return when (typeValue) {
                "transaction" -> {
                    val valueJson = jsonObject["value"]
                        ?: throw SerializationException("Missing 'value' for TONIntentRequestEvent.Transaction")
                    Transaction(
                        jsonDecoder.json.decodeFromJsonElement(serializer<TONTransactionIntentRequestEvent>(), valueJson),
                    )
                }
                "signData" -> {
                    val valueJson = jsonObject["value"]
                        ?: throw SerializationException("Missing 'value' for TONIntentRequestEvent.SignData")
                    SignData(
                        jsonDecoder.json.decodeFromJsonElement(serializer<TONSignDataIntentRequestEvent>(), valueJson),
                    )
                }
                "action" -> {
                    val valueJson = jsonObject["value"]
                        ?: throw SerializationException("Missing 'value' for TONIntentRequestEvent.Action")
                    Action(
                        jsonDecoder.json.decodeFromJsonElement(serializer<TONActionIntentRequestEvent>(), valueJson),
                    )
                }
                else -> throw SerializationException("Unknown type '$typeValue' for TONIntentRequestEvent")
            }
        }
    }
}
