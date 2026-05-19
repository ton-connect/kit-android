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
package io.ton.walletkit.engine.operations.responses

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive

/**
 * Decodes a JS Uint8Array, which the bridge transports as either a JSON array of bytes
 * `[1,2,3]` or an indexed object `{"0":1,"1":2,"2":3}` depending on serialization path.
 */
internal object BridgeByteArraySerializer : KSerializer<ByteArray> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("BridgeByteArray")

    override fun deserialize(decoder: Decoder): ByteArray {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw SerializationException("BridgeByteArraySerializer requires a JSON decoder")
        return when (val element = jsonDecoder.decodeJsonElement()) {
            is JsonArray -> ByteArray(element.size) { i -> element[i].jsonPrimitive.int.toByte() }
            is JsonObject -> ByteArray(element.size) { i ->
                element[i.toString()]?.jsonPrimitive?.int?.toByte()
                    ?: throw SerializationException("ByteArray missing index $i")
            }
            else -> throw SerializationException("Expected JsonArray or indexed JsonObject for ByteArray, got $element")
        }
    }

    override fun serialize(encoder: Encoder, value: ByteArray): Unit =
        throw SerializationException("BridgeByteArraySerializer is decode-only")
}

@Serializable
internal data class KeyPairResponse(
    @Serializable(with = BridgeByteArraySerializer::class) val publicKey: ByteArray,
    @Serializable(with = BridgeByteArraySerializer::class) val secretKey: ByteArray,
)

@Serializable
internal data class SignerInfoResponse(
    val signerId: String? = null,
    val publicKey: String? = null,
)

@Serializable
internal data class AdapterInfoResponse(
    val adapterId: String? = null,
    val address: String? = null,
)

@Serializable
internal data class WalletInfoBridge(
    val publicKey: String? = null,
    val version: String? = null,
)

@Serializable
internal data class AddWalletResponse(
    val walletId: String? = null,
    val wallet: WalletInfoBridge? = null,
)

@Serializable
internal data class ProviderIdResponse(val providerId: String)

@Serializable
internal data class ProviderIdsResponse(val providerIds: List<String> = emptyList())

@Serializable
internal data class HasProviderResponse(val result: Boolean = false)

@Serializable
internal data class SendTransactionResponse(
    val boc: String? = null,
    val signedBoc: String? = null,
)
