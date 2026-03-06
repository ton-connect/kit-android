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

import io.ton.walletkit.model.TONBase64
import io.ton.walletkit.model.TONUserFriendlyAddress
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Successful response for sign data intent.
 *
 * @param signature
 * @param address
 * @param timestamp UNIX timestamp (seconds, UTC)
 * @param domain App domain
 * @param payload
 */
@Serializable
data class TONIntentSignDataResponse(

    @Contextual @SerialName(value = "signature")
    val signature: io.ton.walletkit.model.TONBase64,

    @Contextual @SerialName(value = "address")
    val address: io.ton.walletkit.model.TONUserFriendlyAddress,

    /* UNIX timestamp (seconds, UTC) */
    @SerialName(value = "timestamp")
    val timestamp: kotlin.Int,

    /* App domain */
    @SerialName(value = "domain")
    val domain: kotlin.String,

    @SerialName(value = "payload")
    val payload: TONSignDataPayload,
    @SerialName("type")
    val type: kotlin.String = "signData",
) {

    companion object
}
