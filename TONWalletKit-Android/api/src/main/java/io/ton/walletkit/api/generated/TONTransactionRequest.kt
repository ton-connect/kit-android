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
 * Request to send a transaction on the TON blockchain.
 *
 * @param messages List of messages to include in the transaction
 * @param network
 * @param validUntil Unix timestamp after which the transaction becomes invalid
 * @param fromAddress Sender wallet address in received format(raw, user friendly)
 */
@Serializable
data class TONTransactionRequest(

    /* List of messages to include in the transaction */
    @SerialName(value = "messages")
    val messages: kotlin.collections.List<TONTransactionRequestMessage>,

    @SerialName(value = "network")
    val network: TONNetwork? = null,

    /* Unix timestamp after which the transaction becomes invalid */
    @SerialName(value = "validUntil")
    val validUntil: kotlin.Int? = null,

    /* Sender wallet address in received format(raw, user friendly) */
    @SerialName(value = "fromAddress")
    val fromAddress: kotlin.String? = null,

) {

    companion object
}
