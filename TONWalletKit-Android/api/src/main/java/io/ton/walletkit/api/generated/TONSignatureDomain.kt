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
@Serializable(with = TONSignatureDomain.Serializer::class)
sealed class TONSignatureDomain {

    companion object {
        internal const val DISCRIMINATOR_FIELD = "type"
    }

    /**
     *
     */
    @Serializable
    data class L2(
        val globalId: kotlin.Int,
    ) : TONSignatureDomain()

    /**
     *
     */
    object Empty : TONSignatureDomain()

    internal object Serializer : KSerializer<TONSignatureDomain> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("TONSignatureDomain")

        @Suppress("UNCHECKED_CAST")
        override fun serialize(encoder: Encoder, value: TONSignatureDomain) {
            val jsonEncoder = encoder as? JsonEncoder
                ?: throw SerializationException("TONSignatureDomain can only be serialized with JSON")

            val jsonElement = when (value) {
                is L2 ->
                    buildJsonObject {
                        put(DISCRIMINATOR_FIELD, JsonPrimitive("l2"))
                        put("globalId", jsonEncoder.json.encodeToJsonElement(serializer<kotlin.Int>(), value.globalId))
                    }

                is Empty ->
                    buildJsonObject {
                        put(DISCRIMINATOR_FIELD, JsonPrimitive("empty"))
                    }
            }
            jsonEncoder.encodeJsonElement(jsonElement)
        }

        override fun deserialize(decoder: Decoder): TONSignatureDomain {
            val jsonDecoder = decoder as? JsonDecoder
                ?: throw SerializationException("TONSignatureDomain can only be deserialized from JSON")

            val jsonObject = jsonDecoder.decodeJsonElement().jsonObject
            val discriminatorValue = jsonObject[DISCRIMINATOR_FIELD]?.jsonPrimitive?.content
                ?: throw SerializationException("Missing '$DISCRIMINATOR_FIELD' discriminator for TONSignatureDomain")

            return when (discriminatorValue) {
                "l2" ->
                    L2(
                        globalId = jsonDecoder.json.decodeFromJsonElement(serializer<kotlin.Int>(), jsonObject["globalId"] ?: throw SerializationException("Missing 'globalId' for TONSignatureDomain")),
                    )

                "empty" ->
                    Empty

                else -> throw SerializationException("Unknown discriminator '$discriminatorValue' for TONSignatureDomain")
            }
        }
    }
}
