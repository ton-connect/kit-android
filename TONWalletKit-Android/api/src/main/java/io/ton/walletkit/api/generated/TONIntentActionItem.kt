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
@Serializable(with = TONIntentActionItem.Serializer::class)
sealed class TONIntentActionItem {

    /**
     * The discriminator value for this union type
     */
    abstract val type: String

    /**
     *
     */
    @Serializable
    data class SendTon(
        @SerialName("value")
        val value: TONSendTonAction,
    ) : TONIntentActionItem() {
        override val type: String = "sendTon"
    }

    /**
     *
     */
    @Serializable
    data class SendJetton(
        @SerialName("value")
        val value: TONSendJettonAction,
    ) : TONIntentActionItem() {
        override val type: String = "sendJetton"
    }

    /**
     *
     */
    @Serializable
    data class SendNft(
        @SerialName("value")
        val value: TONSendNftAction,
    ) : TONIntentActionItem() {
        override val type: String = "sendNft"
    }

    internal object Serializer : KSerializer<TONIntentActionItem> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("TONIntentActionItem")

        @Suppress("UNCHECKED_CAST")
        override fun serialize(encoder: Encoder, value: TONIntentActionItem) {
            val jsonEncoder = encoder as? JsonEncoder
                ?: throw SerializationException("TONIntentActionItem can only be serialized with JSON")

            val jsonObject = when (value) {
                is SendTon -> {
                    // Use explicit type serializer to avoid runtime class serialization issues (e.g., LinkedHashMap)
                    val valueJson = jsonEncoder.json.encodeToJsonElement(serializer<TONSendTonAction>(), value.value)
                    buildJsonObject {
                        put("type", JsonPrimitive("sendTon"))
                        put("value", valueJson)
                    }
                }
                is SendJetton -> {
                    // Use explicit type serializer to avoid runtime class serialization issues (e.g., LinkedHashMap)
                    val valueJson = jsonEncoder.json.encodeToJsonElement(serializer<TONSendJettonAction>(), value.value)
                    buildJsonObject {
                        put("type", JsonPrimitive("sendJetton"))
                        put("value", valueJson)
                    }
                }
                is SendNft -> {
                    // Use explicit type serializer to avoid runtime class serialization issues (e.g., LinkedHashMap)
                    val valueJson = jsonEncoder.json.encodeToJsonElement(serializer<TONSendNftAction>(), value.value)
                    buildJsonObject {
                        put("type", JsonPrimitive("sendNft"))
                        put("value", valueJson)
                    }
                }
            }
            jsonEncoder.encodeJsonElement(jsonObject)
        }

        override fun deserialize(decoder: Decoder): TONIntentActionItem {
            val jsonDecoder = decoder as? JsonDecoder
                ?: throw SerializationException("TONIntentActionItem can only be deserialized from JSON")

            val jsonObject = jsonDecoder.decodeJsonElement().jsonObject
            val typeValue = jsonObject["type"]?.jsonPrimitive?.content
                ?: throw SerializationException("Missing 'type' discriminator for TONIntentActionItem")

            return when (typeValue) {
                "sendTon" -> {
                    val valueJson = jsonObject["value"]
                        ?: throw SerializationException("Missing 'value' for TONIntentActionItem.SendTon")
                    SendTon(
                        jsonDecoder.json.decodeFromJsonElement(serializer<TONSendTonAction>(), valueJson),
                    )
                }
                "sendJetton" -> {
                    val valueJson = jsonObject["value"]
                        ?: throw SerializationException("Missing 'value' for TONIntentActionItem.SendJetton")
                    SendJetton(
                        jsonDecoder.json.decodeFromJsonElement(serializer<TONSendJettonAction>(), valueJson),
                    )
                }
                "sendNft" -> {
                    val valueJson = jsonObject["value"]
                        ?: throw SerializationException("Missing 'value' for TONIntentActionItem.SendNft")
                    SendNft(
                        jsonDecoder.json.decodeFromJsonElement(serializer<TONSendNftAction>(), valueJson),
                    )
                }
                else -> throw SerializationException("Unknown type '$typeValue' for TONIntentActionItem")
            }
        }
    }
}
