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
import java.math.BigInteger

/**
 * Represents a token amount in nano units (smallest indivisible units).
 *
 * This is a lightweight wrapper around BigInteger that provides:
 * - Type safety for token amounts in the API
 * - Custom serialization as a plain string
 * - Proper handling of large numbers that exceed Long range
 *
 * @property nanoUnits The amount in nano units (e.g., nanoTON)
 */
@Serializable(with = TONTokenAmountSerializer::class)
data class TONTokenAmount(
    val nanoUnits: BigInteger,
) {
    /**
     * Creates a TONTokenAmount from a string representation.
     *
     * @param nanoUnitsString The string representation of nano units
     * @throws NumberFormatException if the string is not a valid number
     */
    constructor(nanoUnitsString: String) : this(BigInteger(nanoUnitsString))

    /**
     * Creates a TONTokenAmount from a Long value.
     *
     * @param nanoUnitsLong The amount in nano units as Long
     */
    constructor(nanoUnitsLong: Long) : this(BigInteger.valueOf(nanoUnitsLong))

    /**
     * Returns the string representation of the nano units.
     */
    val value: String
        get() = nanoUnits.toString()

    override fun toString(): String = value

    companion object {
        /**
         * A zero token amount.
         */
        val ZERO = TONTokenAmount(BigInteger.ZERO)

        /**
         * Creates a TONTokenAmount from a string, returning null if invalid.
         */
        fun parseOrNull(value: String): TONTokenAmount? = try {
            TONTokenAmount(value)
        } catch (e: NumberFormatException) {
            null
        }
    }
}

/**
 * Serializer for [TONTokenAmount] that encodes/decodes as a plain string.
 *
 * JSON representation: "1000000000" (1 TON in nano units)
 */
object TONTokenAmountSerializer : KSerializer<TONTokenAmount> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("TONTokenAmount", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: TONTokenAmount) {
        encoder.encodeString(value.value)
    }

    override fun deserialize(decoder: Decoder): TONTokenAmount {
        return TONTokenAmount(decoder.decodeString())
    }
}
