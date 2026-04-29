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
    "UnusedImport"
)

package io.ton.walletkit.api.generatedtest


import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
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
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.serializer
import io.ton.walletkit.model.TONBase64
import io.ton.walletkit.model.TONUserFriendlyAddress

/**
 * 
 * 
 * This is a discriminated union type. Use the appropriate subclass based on the `type` field.
 */
@Serializable(with = StackItem.Serializer::class)
sealed class StackItem {

    /**
     * The discriminator value for this union type
     */
    abstract val type: String

    /**
     * 
     */
    @Serializable
    data class Int(
        @SerialName("value")
        val value: kotlin.Int
    ) : StackItem() {
        override val type: String = "int"
    }

    /**
     * 
     */
    @Serializable
    data class Str(
        @SerialName("value")
        val value: kotlin.String
    ) : StackItem() {
        override val type: String = "str"
    }

    /**
     * Case without associated value: empty
     */
    @Serializable
    object Empty : StackItem() {
        override val type: String = "empty"
    }

    internal object Serializer : KSerializer<StackItem> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("StackItem")

        @Suppress("UNCHECKED_CAST")
        override fun serialize(encoder: Encoder, value: StackItem) {
            val jsonEncoder = encoder as? JsonEncoder
                ?: throw SerializationException("StackItem can only be serialized with JSON")
            
            val jsonObject = when (value) {
                is Int -> {
                    // Use explicit type serializer to avoid runtime class serialization issues (e.g., LinkedHashMap)
                    val valueJson = jsonEncoder.json.encodeToJsonElement(serializer<kotlin.Int>(), value.value)
                    buildJsonObject {
                        put("type", JsonPrimitive("int"))
                        put("value", valueJson)
                    }
                }
                is Str -> {
                    // Use explicit type serializer to avoid runtime class serialization issues (e.g., LinkedHashMap)
                    val valueJson = jsonEncoder.json.encodeToJsonElement(serializer<kotlin.String>(), value.value)
                    buildJsonObject {
                        put("type", JsonPrimitive("str"))
                        put("value", valueJson)
                    }
                }
                is Empty -> JsonObject(mapOf("type" to JsonPrimitive("empty")))
            }
            jsonEncoder.encodeJsonElement(jsonObject)
        }

        override fun deserialize(decoder: Decoder): StackItem {
            val jsonDecoder = decoder as? JsonDecoder
                ?: throw SerializationException("StackItem can only be deserialized from JSON")
            
            val jsonObject = jsonDecoder.decodeJsonElement().jsonObject
            val typeValue = jsonObject["type"]?.jsonPrimitive?.content
                ?: throw SerializationException("Missing 'type' discriminator for StackItem")
            
            return when (typeValue) {
                "int" -> {
                    val valueJson = jsonObject["value"]
                        ?: throw SerializationException("Missing 'value' for StackItem.Int")
                    Int(
                        jsonDecoder.json.decodeFromJsonElement(serializer<kotlin.Int>(), valueJson)
                    )
                }
                "str" -> {
                    val valueJson = jsonObject["value"]
                        ?: throw SerializationException("Missing 'value' for StackItem.Str")
                    Str(
                        jsonDecoder.json.decodeFromJsonElement(serializer<kotlin.String>(), valueJson)
                    )
                }
                "empty" -> Empty
                else -> throw SerializationException("Unknown type '$typeValue' for StackItem")
            }
        }
    }
}

