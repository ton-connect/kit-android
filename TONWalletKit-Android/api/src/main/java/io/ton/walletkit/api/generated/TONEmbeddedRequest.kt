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
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.serializer

/**
 *
 *
 * This is a discriminated union type. Use the appropriate subclass based on the `method` field.
 */
@Serializable(with = TONEmbeddedRequest.Serializer::class)
sealed class TONEmbeddedRequest {

    companion object {
        internal const val DISCRIMINATOR_FIELD = "method"
    }

    /**
     *
     */
    @Serializable
    data class SendTransaction(
        val transactionRequest: TONTransactionRequest,
    ) : TONEmbeddedRequest()

    /**
     *
     */
    @Serializable
    data class SignMessage(
        val transactionRequest: TONTransactionRequest,
    ) : TONEmbeddedRequest()

    /**
     *
     */
    @Serializable
    data class SignData(
        val payload: TONSignDataPayload,
    ) : TONEmbeddedRequest()

    internal object Serializer : KSerializer<TONEmbeddedRequest> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("TONEmbeddedRequest")

        @Suppress("UNCHECKED_CAST")
        override fun serialize(encoder: Encoder, value: TONEmbeddedRequest) {
            val jsonEncoder = encoder as? JsonEncoder
                ?: throw SerializationException("TONEmbeddedRequest can only be serialized with JSON")

            val jsonElement = when (value) {
                is SendTransaction ->
                    buildJsonObject {
                        put(DISCRIMINATOR_FIELD, JsonPrimitive("sendTransaction"))
                        put("transactionRequest", jsonEncoder.json.encodeToJsonElement(serializer<TONTransactionRequest>(), value.transactionRequest))
                    }

                is SignMessage ->
                    buildJsonObject {
                        put(DISCRIMINATOR_FIELD, JsonPrimitive("signMessage"))
                        put("transactionRequest", jsonEncoder.json.encodeToJsonElement(serializer<TONTransactionRequest>(), value.transactionRequest))
                    }

                is SignData ->
                    buildJsonObject {
                        put(DISCRIMINATOR_FIELD, JsonPrimitive("signData"))
                        put("payload", jsonEncoder.json.encodeToJsonElement(serializer<TONSignDataPayload>(), value.payload))
                    }
            }
            jsonEncoder.encodeJsonElement(jsonElement)
        }

        override fun deserialize(decoder: Decoder): TONEmbeddedRequest {
            val jsonDecoder = decoder as? JsonDecoder
                ?: throw SerializationException("TONEmbeddedRequest can only be deserialized from JSON")

            // Cases without an associated value arrive as bare strings on the wire
            // (e.g. "empty" rather than { "type": "empty" }). Accept both shapes so the
            // discriminated-union decode doesn't crash with "JsonLiteral is not a JsonObject".
            val element = jsonDecoder.decodeJsonElement()
            val asPrimitive = element as? JsonPrimitive
            val jsonObject: JsonObject? = if (asPrimitive != null && asPrimitive.isString) null else element.jsonObject
            val discriminatorValue: String = jsonObject?.get(DISCRIMINATOR_FIELD)?.jsonPrimitive?.content
                ?: asPrimitive?.content
                ?: throw SerializationException("Missing '$DISCRIMINATOR_FIELD' discriminator for TONEmbeddedRequest")

            return when (discriminatorValue) {
                "sendTransaction" ->
                    SendTransaction(
                        transactionRequest = jsonDecoder.json.decodeFromJsonElement(serializer<TONTransactionRequest>(), jsonObject?.get("transactionRequest") ?: throw SerializationException("Missing 'transactionRequest' for TONEmbeddedRequest")),
                    )

                "signMessage" ->
                    SignMessage(
                        transactionRequest = jsonDecoder.json.decodeFromJsonElement(serializer<TONTransactionRequest>(), jsonObject?.get("transactionRequest") ?: throw SerializationException("Missing 'transactionRequest' for TONEmbeddedRequest")),
                    )

                "signData" ->
                    SignData(
                        payload = jsonDecoder.json.decodeFromJsonElement(serializer<TONSignDataPayload>(), jsonObject?.get("payload") ?: throw SerializationException("Missing 'payload' for TONEmbeddedRequest")),
                    )

                else -> throw SerializationException("Unknown discriminator '$discriminatorValue' for TONEmbeddedRequest")
            }
        }
    }
}
