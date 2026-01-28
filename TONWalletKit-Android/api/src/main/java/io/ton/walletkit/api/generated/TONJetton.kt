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
 * Jetton fungible token on the TON blockchain (TEP-74 standard).
 *
 * @param address
 * @param walletAddress
 * @param balance
 * @param info
 * @param isVerified Indicates if the jetton is verified
 * @param prices Current prices of the jetton in various currencies
 * @param decimalsNumber The number of decimal places used by the token
 * @param extra Additional arbitrary data related to the jetton
 */
@Serializable
data class TONJetton(

    @Contextual @SerialName(value = "address")
    val address: io.ton.walletkit.model.TONUserFriendlyAddress,

    @Contextual @SerialName(value = "walletAddress")
    val walletAddress: io.ton.walletkit.model.TONUserFriendlyAddress,

    @SerialName(value = "balance")
    val balance: kotlin.String,

    @SerialName(value = "info")
    val info: TONTokenInfo,

    /* Indicates if the jetton is verified */
    @SerialName(value = "isVerified")
    val isVerified: kotlin.Boolean,

    /* Current prices of the jetton in various currencies */
    @SerialName(value = "prices")
    val prices: kotlin.collections.List<TONJettonPrice>,

    /* The number of decimal places used by the token */
    @SerialName(value = "decimalsNumber")
    val decimalsNumber: kotlin.Int? = null,

    /* Additional arbitrary data related to the jetton */
    @Contextual @SerialName(value = "extra")
    val extra: kotlin.collections.Map<kotlin.String, kotlinx.serialization.json.JsonElement>? = null,

) {

    companion object
}
