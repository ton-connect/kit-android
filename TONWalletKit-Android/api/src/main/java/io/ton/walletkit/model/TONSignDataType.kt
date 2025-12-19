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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents the type of data to be signed.
 *
 * Used in sign data requests to indicate how the data should be interpreted
 * and processed before signing.
 */
@Serializable
enum class TONSignDataType {
    /**
     * Plain text data that will be prefixed with a specific marker before signing.
     */
    @SerialName("text")
    TEXT,

    /**
     * Binary data (usually hex-encoded) to be signed directly.
     */
    @SerialName("binary")
    BINARY,

    /**
     * A BOC (Bag of Cells) encoded cell to be signed.
     */
    @SerialName("cell")
    CELL,

    ;

    companion object {
        /**
         * Gets the TONSignDataType from a string value.
         *
         * @param value The string value ("text", "binary", or "cell")
         * @return The corresponding TONSignDataType, or null if invalid
         */
        fun fromValue(value: String): TONSignDataType? =
            entries.find { it.name.equals(value, ignoreCase = true) }
    }
}
