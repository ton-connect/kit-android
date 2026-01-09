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
package io.ton.walletkit.model

import android.util.Base64
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Represents a Base64-encoded string value.
 *
 * This is a lightweight wrapper that provides:
 * - Type safety for Base64 data in the API
 * - Conversion to/from raw byte arrays
 * - Custom serialization as a plain string
 *
 * @property value The base64-encoded string
 */
@Serializable(with = TONBase64Serializer::class)
data class TONBase64(
    val value: String,
) {
    /**
     * Decodes the Base64 string to raw bytes.
     * Returns null if the value is not valid Base64.
     */
    val data: ByteArray?
        get() = try {
            Base64.decode(value, Base64.DEFAULT)
        } catch (e: IllegalArgumentException) {
            null
        }

    companion object {
        /**
         * Creates a TONBase64 from raw bytes.
         */
        fun fromData(data: ByteArray): TONBase64 {
            return TONBase64(Base64.encodeToString(data, Base64.NO_WRAP))
        }

        /**
         * Creates a TONBase64 from a UTF-8 string.
         */
        fun fromString(string: String): TONBase64 {
            return fromData(string.toByteArray(Charsets.UTF_8))
        }
    }

    override fun toString(): String = value
}

/**
 * Serializer for [TONBase64] that serializes/deserializes as a plain string.
 */
object TONBase64Serializer : KSerializer<TONBase64> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        "io.ton.walletkit.model.TONBase64",
        PrimitiveKind.STRING,
    )

    override fun serialize(encoder: Encoder, value: TONBase64) {
        encoder.encodeString(value.value)
    }

    override fun deserialize(decoder: Decoder): TONBase64 {
        return TONBase64(decoder.decodeString())
    }
}
