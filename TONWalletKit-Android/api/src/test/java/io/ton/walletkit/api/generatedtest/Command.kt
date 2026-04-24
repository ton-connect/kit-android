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
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.serializer
import io.ton.walletkit.model.TONBase64
import io.ton.walletkit.model.TONUserFriendlyAddress

/**
 * 
 *
 * This is a discriminated union type. Use the appropriate subclass based on the `type` field.
 */
@Serializable(with = Command.Serializer::class)
sealed class Command {

    companion object {
        internal const val DISCRIMINATOR_FIELD = "type"
    }

    /**
     * 
     */
    @Serializable
    data class Start(
        val timeout: kotlin.Int? = null
    ) : Command()


    /**
     * 
     */
    object Stop : Command()


    internal object Serializer : KSerializer<Command> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Command")

        @Suppress("UNCHECKED_CAST")
        override fun serialize(encoder: Encoder, value: Command) {
            val jsonEncoder = encoder as? JsonEncoder
                ?: throw SerializationException("Command can only be serialized with JSON")

            val jsonElement = when (value) {
                is Start ->
                    buildJsonObject {
                        put(DISCRIMINATOR_FIELD, JsonPrimitive("start"))
                        value.timeout?.let { put("timeout", jsonEncoder.json.encodeToJsonElement(serializer<kotlin.Int>(), it)) }
                    }

                is Stop ->
                    buildJsonObject {
                        put(DISCRIMINATOR_FIELD, JsonPrimitive("stop"))
                    }

            }
            jsonEncoder.encodeJsonElement(jsonElement)
        }

        override fun deserialize(decoder: Decoder): Command {
            val jsonDecoder = decoder as? JsonDecoder
                ?: throw SerializationException("Command can only be deserialized from JSON")

            val jsonObject = jsonDecoder.decodeJsonElement().jsonObject
            val discriminatorValue = jsonObject[DISCRIMINATOR_FIELD]?.jsonPrimitive?.content
                ?: throw SerializationException("Missing '$DISCRIMINATOR_FIELD' discriminator for Command")

            return when (discriminatorValue) {
                "start" -> {
                    val fieldElement = jsonObject["timeout"]
                    if (fieldElement != null) {
                        Start(
                            timeout = jsonDecoder.json.decodeFromJsonElement(serializer<kotlin.Int>(), fieldElement)
                        )
                    } else {
                        Start()
                    }
                }

                "stop" ->
                    Stop

                else -> throw SerializationException("Unknown discriminator '$discriminatorValue' for Command")
            }
        }
    }
}

