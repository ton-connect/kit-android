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

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Token information for Jetton master contracts.
 *
 * @param isValid Indicates if the token contract is valid
 * @param type Type of token
 * @param extra Additional metadata for the token, such as image sizes, decimal precision, external links, and marketplaces
 * @param name Display name of the Jetton
 * @param symbol Ticker symbol of the Jetton
 * @param description Human-readable description of the Jetton
 * @param decimalsCount Number of decimal places for the Jetton amount
 * @param social Social media links for the Jetton project
 * @param uri Metadata URI for the Jetton
 * @param websites Official website URLs for the Jetton project
 * @param image
 */
@Serializable
data class TONTransactionTokenInfoJettonMasters(

    /* Indicates if the token contract is valid */
    @SerialName(value = "isValid")
    val isValid: kotlin.Boolean,

    /* Type of token */
    @SerialName(value = "type")
    val type: kotlin.String,

    /* Additional metadata for the token, such as image sizes, decimal precision, external links, and marketplaces */
    @Contextual @SerialName(value = "extra")
    val extra: kotlin.collections.Map<kotlin.String, kotlinx.serialization.json.JsonElement>,

    /* Display name of the Jetton */
    @SerialName(value = "name")
    val name: kotlin.String,

    /* Ticker symbol of the Jetton */
    @SerialName(value = "symbol")
    val symbol: kotlin.String,

    /* Human-readable description of the Jetton */
    @SerialName(value = "description")
    val description: kotlin.String,

    /* Number of decimal places for the Jetton amount */
    @SerialName(value = "decimalsCount")
    val decimalsCount: kotlin.Int,

    /* Social media links for the Jetton project */
    @SerialName(value = "social")
    val social: kotlin.collections.List<kotlin.String>,

    /* Metadata URI for the Jetton */
    @SerialName(value = "uri")
    val uri: kotlin.String,

    /* Official website URLs for the Jetton project */
    @SerialName(value = "websites")
    val websites: kotlin.collections.List<kotlin.String>,

    @SerialName(value = "image")
    val image: TONTokenImage? = null,

) {

    companion object
}
