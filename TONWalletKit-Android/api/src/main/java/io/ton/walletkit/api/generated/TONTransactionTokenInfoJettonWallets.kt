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
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Token information for Jetton wallet contracts.
 *
 * @param isValid Indicates if the token contract is valid
 * @param type Type of token
 * @param extra Additional metadata for the token, such as image sizes, decimal precision, external links, and marketplaces
 * @param balance
 * @param jetton
 * @param owner
 */
@Serializable
data class TONTransactionTokenInfoJettonWallets(

    /* Indicates if the token contract is valid */
    @SerialName(value = "isValid")
    val isValid: kotlin.Boolean,

    /* Type of token */
    @SerialName(value = "type")
    val type: kotlin.String,

    /* Additional metadata for the token, such as image sizes, decimal precision, external links, and marketplaces */
    @Contextual @SerialName(value = "extra")
    val extra: kotlin.collections.Map<kotlin.String, kotlinx.serialization.json.JsonElement>,

    @SerialName(value = "balance")
    val balance: kotlin.String,

    @Contextual @SerialName(value = "jetton")
    val jetton: io.ton.walletkit.model.TONUserFriendlyAddress,

    @Contextual @SerialName(value = "owner")
    val owner: io.ton.walletkit.model.TONUserFriendlyAddress,

) {

    companion object
}
