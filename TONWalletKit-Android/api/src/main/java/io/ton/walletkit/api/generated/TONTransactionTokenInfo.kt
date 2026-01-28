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
@Serializable(with = TONTransactionTokenInfo.Serializer::class)
sealed class TONTransactionTokenInfo {

    /**
     * The discriminator value for this union type
     */
    abstract val type: String

    /**
     *
     */
    @Serializable
    data class JettonWallets(
        @SerialName("value")
        val value: TONTransactionTokenInfoJettonWallets,
    ) : TONTransactionTokenInfo() {
        override val type: String = "jetton_wallets"
    }

    /**
     *
     */
    @Serializable
    data class JettonMasters(
        @SerialName("value")
        val value: TONTransactionTokenInfoJettonMasters,
    ) : TONTransactionTokenInfo() {
        override val type: String = "jetton_masters"
    }

    /**
     *
     */
    @Serializable
    data class Unknown(
        @SerialName("value")
        val value: TONTransactionTokenInfoBase,
    ) : TONTransactionTokenInfo() {
        override val type: String = "unknown"
    }

    internal object Serializer : KSerializer<TONTransactionTokenInfo> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("TONTransactionTokenInfo")

        @Suppress("UNCHECKED_CAST")
        override fun serialize(encoder: Encoder, value: TONTransactionTokenInfo) {
            val jsonEncoder = encoder as? JsonEncoder
                ?: throw SerializationException("TONTransactionTokenInfo can only be serialized with JSON")

            val jsonObject = when (value) {
                is JettonWallets -> {
                    // Use explicit type serializer to avoid runtime class serialization issues (e.g., LinkedHashMap)
                    val valueJson = jsonEncoder.json.encodeToJsonElement(serializer<TONTransactionTokenInfoJettonWallets>(), value.value)
                    buildJsonObject {
                        put("type", JsonPrimitive("jetton_wallets"))
                        put("value", valueJson)
                    }
                }
                is JettonMasters -> {
                    // Use explicit type serializer to avoid runtime class serialization issues (e.g., LinkedHashMap)
                    val valueJson = jsonEncoder.json.encodeToJsonElement(serializer<TONTransactionTokenInfoJettonMasters>(), value.value)
                    buildJsonObject {
                        put("type", JsonPrimitive("jetton_masters"))
                        put("value", valueJson)
                    }
                }
                is Unknown -> {
                    // Use explicit type serializer to avoid runtime class serialization issues (e.g., LinkedHashMap)
                    val valueJson = jsonEncoder.json.encodeToJsonElement(serializer<TONTransactionTokenInfoBase>(), value.value)
                    buildJsonObject {
                        put("type", JsonPrimitive("unknown"))
                        put("value", valueJson)
                    }
                }
            }
            jsonEncoder.encodeJsonElement(jsonObject)
        }

        override fun deserialize(decoder: Decoder): TONTransactionTokenInfo {
            val jsonDecoder = decoder as? JsonDecoder
                ?: throw SerializationException("TONTransactionTokenInfo can only be deserialized from JSON")

            val jsonObject = jsonDecoder.decodeJsonElement().jsonObject
            val typeValue = jsonObject["type"]?.jsonPrimitive?.content
                ?: throw SerializationException("Missing 'type' discriminator for TONTransactionTokenInfo")

            return when (typeValue) {
                "jetton_wallets" -> {
                    val valueJson = jsonObject["value"]
                        ?: throw SerializationException("Missing 'value' for TONTransactionTokenInfo.JettonWallets")
                    JettonWallets(
                        jsonDecoder.json.decodeFromJsonElement(serializer<TONTransactionTokenInfoJettonWallets>(), valueJson),
                    )
                }
                "jetton_masters" -> {
                    val valueJson = jsonObject["value"]
                        ?: throw SerializationException("Missing 'value' for TONTransactionTokenInfo.JettonMasters")
                    JettonMasters(
                        jsonDecoder.json.decodeFromJsonElement(serializer<TONTransactionTokenInfoJettonMasters>(), valueJson),
                    )
                }
                "unknown" -> {
                    val valueJson = jsonObject["value"]
                        ?: throw SerializationException("Missing 'value' for TONTransactionTokenInfo.Unknown")
                    Unknown(
                        jsonDecoder.json.decodeFromJsonElement(serializer<TONTransactionTokenInfoBase>(), valueJson),
                    )
                }
                else -> throw SerializationException("Unknown type '$typeValue' for TONTransactionTokenInfo")
            }
        }
    }
}
