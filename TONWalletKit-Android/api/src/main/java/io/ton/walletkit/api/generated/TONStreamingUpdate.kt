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
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.serializer

/**
 *
 *
 * This is a discriminated union type. Use the appropriate subclass based on the `type` field.
 */
@Serializable(with = TONStreamingUpdate.Serializer::class)
sealed class TONStreamingUpdate {

    companion object {
        internal const val DISCRIMINATOR_FIELD = "type"
    }

    /**
     *
     */
    @Serializable
    data class Balance(
        val value: TONBalanceUpdate,
    ) : TONStreamingUpdate()

    /**
     *
     */
    @Serializable
    data class Transactions(
        val value: TONTransactionsUpdate,
    ) : TONStreamingUpdate()

    /**
     *
     */
    @Serializable
    data class Jettons(
        val value: TONJettonUpdate,
    ) : TONStreamingUpdate()

    internal object Serializer : KSerializer<TONStreamingUpdate> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("TONStreamingUpdate")

        @Suppress("UNCHECKED_CAST")
        override fun serialize(encoder: Encoder, value: TONStreamingUpdate) {
            val jsonEncoder = encoder as? JsonEncoder
                ?: throw SerializationException("TONStreamingUpdate can only be serialized with JSON")

            val jsonElement = when (value) {
                is Balance ->
                    jsonEncoder.json.encodeToJsonElement(serializer<TONBalanceUpdate>(), value.value)

                is Transactions ->
                    jsonEncoder.json.encodeToJsonElement(serializer<TONTransactionsUpdate>(), value.value)

                is Jettons ->
                    jsonEncoder.json.encodeToJsonElement(serializer<TONJettonUpdate>(), value.value)
            }
            jsonEncoder.encodeJsonElement(jsonElement)
        }

        override fun deserialize(decoder: Decoder): TONStreamingUpdate {
            val jsonDecoder = decoder as? JsonDecoder
                ?: throw SerializationException("TONStreamingUpdate can only be deserialized from JSON")

            // Cases without an associated value arrive as bare strings on the wire
            // (e.g. "empty" rather than { "type": "empty" }). Accept both shapes so the
            // discriminated-union decode doesn't crash with "JsonLiteral is not a JsonObject".
            val element = jsonDecoder.decodeJsonElement()
            val asPrimitive = element as? JsonPrimitive
            val jsonObject: JsonObject? = if (asPrimitive != null && asPrimitive.isString) null else element.jsonObject
            val discriminatorValue: String = jsonObject?.get(DISCRIMINATOR_FIELD)?.jsonPrimitive?.content
                ?: asPrimitive?.content
                ?: throw SerializationException("Missing '$DISCRIMINATOR_FIELD' discriminator for TONStreamingUpdate")

            return when (discriminatorValue) {
                "balance" ->
                    Balance(
                        jsonDecoder.json.decodeFromJsonElement(serializer<TONBalanceUpdate>(), jsonObject ?: throw SerializationException("Expected JSON object for TONStreamingUpdate.Balance")),
                    )

                "transactions" ->
                    Transactions(
                        jsonDecoder.json.decodeFromJsonElement(serializer<TONTransactionsUpdate>(), jsonObject ?: throw SerializationException("Expected JSON object for TONStreamingUpdate.Transactions")),
                    )

                "jettons" ->
                    Jettons(
                        jsonDecoder.json.decodeFromJsonElement(serializer<TONJettonUpdate>(), jsonObject ?: throw SerializationException("Expected JSON object for TONStreamingUpdate.Jettons")),
                    )

                else -> throw SerializationException("Unknown discriminator '$discriminatorValue' for TONStreamingUpdate")
            }
        }
    }
}
