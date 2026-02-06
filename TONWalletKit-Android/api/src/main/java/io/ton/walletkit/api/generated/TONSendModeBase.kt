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
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 *
 *
 * Values: ordinary,carryAllRemainingIncomingValue,carryAllRemainingBalance
 */
@Serializable(with = TONSendModeBaseSerializer::class)
enum class TONSendModeBase(val value: kotlin.Int) {

    ordinary(0),

    carryAllRemainingIncomingValue(64),

    carryAllRemainingBalance(128),
    ;

    /**
     * Override [toString()] to avoid using the enum variable name as the value, and instead use
     * the actual value defined in the API spec file.
     *
     * This solves a problem when the variable name and its value are different, and ensures that
     * the client sends the correct enum values to the server always.
     */
    override fun toString(): kotlin.String = value.toString()

    companion object {
        /**
         * Converts the provided [data] to a [String] on success, null otherwise.
         */
        fun encode(data: kotlin.Any?): kotlin.String? = if (data is TONSendModeBase) "$data" else null

        /**
         * Returns a valid [TONSendModeBase] for [data], null otherwise.
         */
        fun decode(data: kotlin.Any?): TONSendModeBase? = data?.let {
            val normalizedData = "$it".lowercase()
            values().firstOrNull { value ->
                it == value || normalizedData == "$value".lowercase()
            }
        }
    }
}

internal object TONSendModeBaseSerializer : KSerializer<TONSendModeBase> {
    override val descriptor = kotlin.Int.serializer().descriptor

    override fun deserialize(decoder: Decoder): TONSendModeBase {
        val value = decoder.decodeSerializableValue(kotlin.Int.serializer())
        return TONSendModeBase.values().firstOrNull { it.value == value }
            ?: throw IllegalArgumentException("Unknown enum value: $value")
    }

    override fun serialize(encoder: Encoder, value: TONSendModeBase) {
        encoder.encodeSerializableValue(kotlin.Int.serializer(), value.value)
    }
}
