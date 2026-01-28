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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Token image URLs in various sizes for display purposes.
 *
 * @param url Original image URL
 * @param smallUrl Small thumbnail URL (typically 64x64 or similar)
 * @param mediumUrl Medium-sized image URL (typically 256x256 or similar)
 * @param largeUrl Large image URL (typically 512x512 or higher)
 * @param `data` Raw image data encoded in Base64
 */
@Serializable
data class TONTokenImage(

    /* Original image URL */
    @SerialName(value = "url")
    val url: kotlin.String? = null,

    /* Small thumbnail URL (typically 64x64 or similar) */
    @SerialName(value = "smallUrl")
    val smallUrl: kotlin.String? = null,

    /* Medium-sized image URL (typically 256x256 or similar) */
    @SerialName(value = "mediumUrl")
    val mediumUrl: kotlin.String? = null,

    /* Large image URL (typically 512x512 or higher) */
    @SerialName(value = "largeUrl")
    val largeUrl: kotlin.String? = null,

    /* Raw image data encoded in Base64 */
    @SerialName(value = "data")
    val `data`: kotlin.String? = null,

) {

    companion object
}
