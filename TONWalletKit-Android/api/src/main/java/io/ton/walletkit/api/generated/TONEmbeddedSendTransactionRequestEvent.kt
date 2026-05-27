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

import io.ton.walletkit.model.TONUserFriendlyAddress
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

/**
 *
 *
 * Inherits TONSendTransactionRequestEvent so it is substitutable
 * wherever the base event is expected. kotlinx-serialization cannot auto-serialize this
 * concrete/concrete inheritance (it would raise duplicate serial names), so a custom
 * serializer flat-merges the inherited fields with this type's own fields.
 */
@Serializable(with = TONEmbeddedSendTransactionRequestEvent.Serializer::class)
class TONEmbeddedSendTransactionRequestEvent(
    id: kotlin.String,
    preview: TONSendTransactionRequestEventPreview,
    request: TONTransactionRequest,
    private val connectionResult: kotlinx.serialization.json.JsonElement,
    from: kotlin.String? = null,
    walletAddress: io.ton.walletkit.model.TONUserFriendlyAddress? = null,
    walletId: kotlin.String? = null,
    domain: kotlin.String? = null,
    isJsBridge: kotlin.Boolean? = null,
    tabId: kotlin.String? = null,
    sessionId: kotlin.String? = null,
    isLocal: kotlin.Boolean? = null,
    messageId: kotlin.String? = null,
    traceId: kotlin.String? = null,
    dAppInfo: TONDAppInfo? = null,
    returnStrategy: kotlin.String? = null,
    val type: kotlin.String = "sendTransaction",
) : TONSendTransactionRequestEvent(
    id = id,
    preview = preview,
    request = request,
    from = from,
    walletAddress = walletAddress,
    walletId = walletId,
    domain = domain,
    isJsBridge = isJsBridge,
    tabId = tabId,
    sessionId = sessionId,
    isLocal = isLocal,
    messageId = messageId,
    traceId = traceId,
    dAppInfo = dAppInfo,
    returnStrategy = returnStrategy,
) {

    internal object Serializer : KSerializer<TONEmbeddedSendTransactionRequestEvent> {
        override val descriptor: SerialDescriptor =
            buildClassSerialDescriptor("io.ton.walletkit.api.generated.TONEmbeddedSendTransactionRequestEvent")

        override fun serialize(encoder: Encoder, value: TONEmbeddedSendTransactionRequestEvent) {
            val jsonEncoder = encoder as? JsonEncoder
                ?: throw SerializationException("TONEmbeddedSendTransactionRequestEvent can only be serialized with JSON")
            val base = jsonEncoder.json.encodeToJsonElement(
                TONSendTransactionRequestEvent.serializer(),
                value,
            ).jsonObject
            val merged = JsonObject(
                base + buildMap {
                    put("connectionResult", value.connectionResult)
                    put("type", JsonPrimitive(value.type))
                },
            )
            jsonEncoder.encodeJsonElement(merged)
        }

        override fun deserialize(decoder: Decoder): TONEmbeddedSendTransactionRequestEvent {
            val jsonDecoder = decoder as? JsonDecoder
                ?: throw SerializationException("TONEmbeddedSendTransactionRequestEvent can only be deserialized from JSON")
            val jsonObject = jsonDecoder.decodeJsonElement().jsonObject
            val base = jsonDecoder.json.decodeFromJsonElement(
                TONSendTransactionRequestEvent.serializer(),
                jsonObject,
            )
            return TONEmbeddedSendTransactionRequestEvent(
                id = base.id,
                preview = base.preview,
                request = base.request,
                connectionResult = jsonObject["connectionResult"] ?: throw SerializationException("Missing 'connectionResult' for TONEmbeddedSendTransactionRequestEvent"),
                from = base.from,
                walletAddress = base.walletAddress,
                walletId = base.walletId,
                domain = base.domain,
                isJsBridge = base.isJsBridge,
                tabId = base.tabId,
                sessionId = base.sessionId,
                isLocal = base.isLocal,
                messageId = base.messageId,
                traceId = base.traceId,
                dAppInfo = base.dAppInfo,
                returnStrategy = base.returnStrategy,
            )
        }
    }
}
