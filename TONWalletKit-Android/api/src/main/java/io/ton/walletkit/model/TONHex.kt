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

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Represents a hexadecimal-encoded string value.
 *
 * This is a lightweight wrapper that provides:
 * - Type safety for hex data in the API
 * - Conversion to/from raw byte arrays
 * - Custom serialization as a plain string
 *
 * @property value The hex-encoded string (may or may not include "0x" prefix)
 */
@Serializable(with = TONHexSerializer::class)
data class TONHex(
    val value: String,
) {
    /**
     * The hex string without the "0x" prefix.
     */
    val rawValue: String
        get() = value.removePrefix("0x").removePrefix("0X")

    /**
     * Decodes the hex string to raw bytes.
     * Returns null if the value is not valid hex.
     */
    val data: ByteArray?
        get() = try {
            val hex = rawValue
            require(hex.length % 2 == 0) { "Hex string must have even length" }
            hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        } catch (e: Exception) {
            null
        }

    /**
     * Creates a TONHex with "0x" prefix.
     */
    val withPrefix: String
        get() = if (value.startsWith("0x") || value.startsWith("0X")) value else "0x$value"

    companion object {
        /**
         * Creates a TONHex from raw bytes.
         *
         * @param data The raw byte array to encode
         * @param withPrefix Whether to include "0x" prefix (default: true)
         */
        fun fromData(data: ByteArray, withPrefix: Boolean = true): TONHex {
            val hex = data.joinToString("") { "%02x".format(it) }
            return TONHex(if (withPrefix) "0x$hex" else hex)
        }

        /**
         * Creates a TONHex from a UTF-8 string.
         *
         * @param string The UTF-8 string to encode
         * @param withPrefix Whether to include "0x" prefix (default: true)
         */
        fun fromString(string: String, withPrefix: Boolean = true): TONHex {
            return fromData(string.toByteArray(Charsets.UTF_8), withPrefix)
        }
    }

    override fun toString(): String = value
}

/**
 * Serializer for [TONHex] that encodes/decodes as a plain string.
 *
 * JSON representation: "0x1234abcd" or "1234abcd"
 */
object TONHexSerializer : KSerializer<TONHex> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("TONHex", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: TONHex) {
        encoder.encodeString(value.value)
    }

    override fun deserialize(decoder: Decoder): TONHex {
        return TONHex(decoder.decodeString())
    }
}
