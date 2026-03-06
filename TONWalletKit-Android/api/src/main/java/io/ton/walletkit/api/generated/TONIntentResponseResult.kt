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
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.serializer

/**
 *
 *
 * This is a discriminated union type. Use the appropriate subclass based on the `type` field.
 */
@Serializable(with = TONIntentResponseResult.Serializer::class)
sealed class TONIntentResponseResult {

    /**
     *
     */
    @Serializable
    data class Transaction(
        val value: TONIntentTransactionResponse,
    ) : TONIntentResponseResult()

    /**
     *
     */
    @Serializable
    data class SignData(
        val value: TONIntentSignDataResponse,
    ) : TONIntentResponseResult()

    /**
     *
     */
    @Serializable
    data class Error(
        val value: TONIntentErrorResponse,
    ) : TONIntentResponseResult()

    internal object Serializer : KSerializer<TONIntentResponseResult> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("TONIntentResponseResult")

        @Suppress("UNCHECKED_CAST")
        override fun serialize(encoder: Encoder, value: TONIntentResponseResult) {
            val jsonEncoder = encoder as? JsonEncoder
                ?: throw SerializationException("TONIntentResponseResult can only be serialized with JSON")

            val jsonElement = when (value) {
                is Transaction ->
                    jsonEncoder.json.encodeToJsonElement(serializer<TONIntentTransactionResponse>(), value.value)
                is SignData ->
                    jsonEncoder.json.encodeToJsonElement(serializer<TONIntentSignDataResponse>(), value.value)
                is Error ->
                    jsonEncoder.json.encodeToJsonElement(serializer<TONIntentErrorResponse>(), value.value)
            }
            jsonEncoder.encodeJsonElement(jsonElement)
        }

        override fun deserialize(decoder: Decoder): TONIntentResponseResult {
            val jsonDecoder = decoder as? JsonDecoder
                ?: throw SerializationException("TONIntentResponseResult can only be deserialized from JSON")

            val jsonObject = jsonDecoder.decodeJsonElement().jsonObject
            val discriminatorValue = jsonObject["type"]?.jsonPrimitive?.content
                ?: throw SerializationException("Missing 'type' discriminator for TONIntentResponseResult")

            return when (discriminatorValue) {
                "transaction" ->
                    Transaction(
                        jsonDecoder.json.decodeFromJsonElement(serializer<TONIntentTransactionResponse>(), jsonObject),
                    )
                "signData" ->
                    SignData(
                        jsonDecoder.json.decodeFromJsonElement(serializer<TONIntentSignDataResponse>(), jsonObject),
                    )
                "error" ->
                    Error(
                        jsonDecoder.json.decodeFromJsonElement(serializer<TONIntentErrorResponse>(), jsonObject),
                    )
                else -> throw SerializationException("Unknown discriminator '$discriminatorValue' for TONIntentResponseResult")
            }
        }
    }
}
