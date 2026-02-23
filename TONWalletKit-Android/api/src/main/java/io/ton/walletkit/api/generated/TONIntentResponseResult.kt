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
 * Union of all intent responses.
 *
 * @param resultType Result type discriminator
 * @param boc Signed BoC (base64)
 * @param signature Signature (base64)
 * @param address Signer address (raw format: 0:hex)
 * @param timestamp UNIX timestamp (seconds, UTC)
 * @param domain App domain
 * @param payload
 * @param error
 */
@Serializable
data class TONIntentResponseResult(

    /* Result type discriminator */
    @SerialName(value = "resultType")
    val resultType: TONIntentResponseResult.ResultType,

    /* Signed BoC (base64) */
    @SerialName(value = "boc")
    val boc: kotlin.String,

    /* Signature (base64) */
    @SerialName(value = "signature")
    val signature: kotlin.String,

    /* Signer address (raw format: 0:hex) */
    @SerialName(value = "address")
    val address: kotlin.String,

    /* UNIX timestamp (seconds, UTC) */
    @SerialName(value = "timestamp")
    val timestamp: kotlin.Int,

    /* App domain */
    @SerialName(value = "domain")
    val domain: kotlin.String,

    @SerialName(value = "payload")
    val payload: TONSignDataPayload,

    @SerialName(value = "error")
    val error: TONIntentError,

) {

    companion object

    /**
     * Result type discriminator
     *
     * Values: error
     */
    @Serializable
    enum class ResultType(val value: kotlin.String) {
        @SerialName(value = "error")
        error("error"),
    }
}
