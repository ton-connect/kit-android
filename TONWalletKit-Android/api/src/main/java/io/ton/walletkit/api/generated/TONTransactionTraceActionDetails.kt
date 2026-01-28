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
@Serializable(with = TONTransactionTraceActionDetails.Serializer::class)
sealed class TONTransactionTraceActionDetails {

    /**
     * The discriminator value for this union type
     */
    abstract val type: String

    /**
     *
     */
    @Serializable
    data class JettonSwap(
        @SerialName("value")
        val value: TONTransactionTraceActionJettonSwapDetails,
    ) : TONTransactionTraceActionDetails() {
        override val type: String = "jetton_swap"
    }

    /**
     *
     */
    @Serializable
    data class CallContract(
        @SerialName("value")
        val value: TONTransactionTraceActionCallContractDetails,
    ) : TONTransactionTraceActionDetails() {
        override val type: String = "call_contract"
    }

    /**
     *
     */
    @Serializable
    data class TonTransfer(
        @SerialName("value")
        val value: TONTransactionTraceActionTONTransferDetails,
    ) : TONTransactionTraceActionDetails() {
        override val type: String = "ton_transfer"
    }

    /**
     *
     */
    @Serializable
    data class Unknown(
        @SerialName("value")
        val value: kotlin.collections.Map<kotlin.String, kotlinx.serialization.json.JsonElement>,
    ) : TONTransactionTraceActionDetails() {
        override val type: String = "unknown"
    }

    internal object Serializer : KSerializer<TONTransactionTraceActionDetails> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("TONTransactionTraceActionDetails")

        @Suppress("UNCHECKED_CAST")
        override fun serialize(encoder: Encoder, value: TONTransactionTraceActionDetails) {
            val jsonEncoder = encoder as? JsonEncoder
                ?: throw SerializationException("TONTransactionTraceActionDetails can only be serialized with JSON")

            val jsonObject = when (value) {
                is JettonSwap -> {
                    // Use explicit type serializer to avoid runtime class serialization issues (e.g., LinkedHashMap)
                    val valueJson = jsonEncoder.json.encodeToJsonElement(serializer<TONTransactionTraceActionJettonSwapDetails>(), value.value)
                    buildJsonObject {
                        put("type", JsonPrimitive("jetton_swap"))
                        put("value", valueJson)
                    }
                }
                is CallContract -> {
                    // Use explicit type serializer to avoid runtime class serialization issues (e.g., LinkedHashMap)
                    val valueJson = jsonEncoder.json.encodeToJsonElement(serializer<TONTransactionTraceActionCallContractDetails>(), value.value)
                    buildJsonObject {
                        put("type", JsonPrimitive("call_contract"))
                        put("value", valueJson)
                    }
                }
                is TonTransfer -> {
                    // Use explicit type serializer to avoid runtime class serialization issues (e.g., LinkedHashMap)
                    val valueJson = jsonEncoder.json.encodeToJsonElement(serializer<TONTransactionTraceActionTONTransferDetails>(), value.value)
                    buildJsonObject {
                        put("type", JsonPrimitive("ton_transfer"))
                        put("value", valueJson)
                    }
                }
                is Unknown -> {
                    // Use explicit type serializer to avoid runtime class serialization issues (e.g., LinkedHashMap)
                    val valueJson = jsonEncoder.json.encodeToJsonElement(serializer<kotlin.collections.Map<kotlin.String, kotlinx.serialization.json.JsonElement>>(), value.value)
                    buildJsonObject {
                        put("type", JsonPrimitive("unknown"))
                        put("value", valueJson)
                    }
                }
            }
            jsonEncoder.encodeJsonElement(jsonObject)
        }

        override fun deserialize(decoder: Decoder): TONTransactionTraceActionDetails {
            val jsonDecoder = decoder as? JsonDecoder
                ?: throw SerializationException("TONTransactionTraceActionDetails can only be deserialized from JSON")

            val jsonObject = jsonDecoder.decodeJsonElement().jsonObject
            val typeValue = jsonObject["type"]?.jsonPrimitive?.content
                ?: throw SerializationException("Missing 'type' discriminator for TONTransactionTraceActionDetails")

            return when (typeValue) {
                "jetton_swap" -> {
                    val valueJson = jsonObject["value"]
                        ?: throw SerializationException("Missing 'value' for TONTransactionTraceActionDetails.JettonSwap")
                    JettonSwap(
                        jsonDecoder.json.decodeFromJsonElement(serializer<TONTransactionTraceActionJettonSwapDetails>(), valueJson),
                    )
                }
                "call_contract" -> {
                    val valueJson = jsonObject["value"]
                        ?: throw SerializationException("Missing 'value' for TONTransactionTraceActionDetails.CallContract")
                    CallContract(
                        jsonDecoder.json.decodeFromJsonElement(serializer<TONTransactionTraceActionCallContractDetails>(), valueJson),
                    )
                }
                "ton_transfer" -> {
                    val valueJson = jsonObject["value"]
                        ?: throw SerializationException("Missing 'value' for TONTransactionTraceActionDetails.TonTransfer")
                    TonTransfer(
                        jsonDecoder.json.decodeFromJsonElement(serializer<TONTransactionTraceActionTONTransferDetails>(), valueJson),
                    )
                }
                "unknown" -> {
                    val valueJson = jsonObject["value"]
                        ?: throw SerializationException("Missing 'value' for TONTransactionTraceActionDetails.Unknown")
                    Unknown(
                        jsonDecoder.json.decodeFromJsonElement(serializer<kotlin.collections.Map<kotlin.String, kotlinx.serialization.json.JsonElement>>(), valueJson),
                    )
                }
                else -> throw SerializationException("Unknown type '$typeValue' for TONTransactionTraceActionDetails")
            }
        }
    }
}
