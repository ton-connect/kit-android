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
 * Request parameters for transferring TON to another address.
 *
 * @param transferAmount
 * @param recipientAddress
 * @param mode
 * @param extraCurrency Map of extra currency IDs to their amounts. Extra currencies are additional tokens that can be attached to TON messages.
 * @param stateInit
 * @param payload
 * @param comment Human-readable text comment attached to the transfer
 */
@Serializable
data class TONTransferRequest(

    @SerialName(value = "transferAmount")
    val transferAmount: kotlin.String,

    @Contextual @SerialName(value = "recipientAddress")
    val recipientAddress: io.ton.walletkit.model.TONUserFriendlyAddress,

    @SerialName(value = "mode")
    val mode: TONSendMode? = null,

    /* Map of extra currency IDs to their amounts. Extra currencies are additional tokens that can be attached to TON messages. */
    @SerialName(value = "extraCurrency")
    val extraCurrency: kotlin.collections.Map<kotlin.String, kotlin.String>? = null,

    @Contextual @SerialName(value = "stateInit")
    val stateInit: io.ton.walletkit.model.TONBase64? = null,

    @Contextual @SerialName(value = "payload")
    val payload: io.ton.walletkit.model.TONBase64? = null,

    /* Human-readable text comment attached to the transfer */
    @SerialName(value = "comment")
    val comment: kotlin.String? = null,

) {

    companion object
}
