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
@Serializable(with = TONIntentActionItem.Serializer::class)
sealed class TONIntentActionItem {

    /**
     *
     */
    @Serializable
    data class SendTon(
        val value: TONSendTonAction,
    ) : TONIntentActionItem()

    /**
     *
     */
    @Serializable
    data class SendJetton(
        val value: TONSendJettonAction,
    ) : TONIntentActionItem()

    /**
     *
     */
    @Serializable
    data class SendNft(
        val value: TONSendNftAction,
    ) : TONIntentActionItem()

    internal object Serializer : KSerializer<TONIntentActionItem> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("TONIntentActionItem")

        @Suppress("UNCHECKED_CAST")
        override fun serialize(encoder: Encoder, value: TONIntentActionItem) {
            val jsonEncoder = encoder as? JsonEncoder
                ?: throw SerializationException("TONIntentActionItem can only be serialized with JSON")

            val jsonElement = when (value) {
                is SendTon ->
                    jsonEncoder.json.encodeToJsonElement(serializer<TONSendTonAction>(), value.value)
                is SendJetton ->
                    jsonEncoder.json.encodeToJsonElement(serializer<TONSendJettonAction>(), value.value)
                is SendNft ->
                    jsonEncoder.json.encodeToJsonElement(serializer<TONSendNftAction>(), value.value)
            }
            jsonEncoder.encodeJsonElement(jsonElement)
        }

        override fun deserialize(decoder: Decoder): TONIntentActionItem {
            val jsonDecoder = decoder as? JsonDecoder
                ?: throw SerializationException("TONIntentActionItem can only be deserialized from JSON")

            val jsonObject = jsonDecoder.decodeJsonElement().jsonObject
            val discriminatorValue = jsonObject["type"]?.jsonPrimitive?.content
                ?: throw SerializationException("Missing 'type' discriminator for TONIntentActionItem")

            return when (discriminatorValue) {
                "sendTon" ->
                    SendTon(
                        jsonDecoder.json.decodeFromJsonElement(serializer<TONSendTonAction>(), jsonObject),
                    )
                "sendJetton" ->
                    SendJetton(
                        jsonDecoder.json.decodeFromJsonElement(serializer<TONSendJettonAction>(), jsonObject),
                    )
                "sendNft" ->
                    SendNft(
                        jsonDecoder.json.decodeFromJsonElement(serializer<TONSendNftAction>(), jsonObject),
                    )
                else -> throw SerializationException("Unknown discriminator '$discriminatorValue' for TONIntentActionItem")
            }
        }
    }
}
