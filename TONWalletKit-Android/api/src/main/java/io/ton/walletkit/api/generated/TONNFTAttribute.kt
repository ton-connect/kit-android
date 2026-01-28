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
 * Attribute/trait of an NFT, commonly used for rarity and properties.
 *
 * @param traitType Category or type of the trait (e.g., \"Background\", \"Eyes\")
 * @param displayType How the attribute should be displayed (e.g., \"string\", \"number\", \"date\")
 * @param `value` Value of the attribute (e.g., \"Blue\", \"Rare\")
 */
@Serializable
data class TONNFTAttribute(

    /* Category or type of the trait (e.g., \"Background\", \"Eyes\") */
    @SerialName(value = "traitType")
    val traitType: kotlin.String? = null,

    /* How the attribute should be displayed (e.g., \"string\", \"number\", \"date\") */
    @SerialName(value = "displayType")
    val displayType: kotlin.String? = null,

    /* Value of the attribute (e.g., \"Blue\", \"Rare\") */
    @SerialName(value = "value")
    val `value`: kotlin.String? = null,

) {

    companion object
}
