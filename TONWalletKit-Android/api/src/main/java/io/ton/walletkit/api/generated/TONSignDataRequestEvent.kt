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
 * Event containing a sign data request from a dApp via TON Connect.
 *
 * @param id Unique identifier for the bridge event
 * @param payload
 * @param preview
 * @param from
 * @param walletAddress
 * @param walletId Wallet identifier associated with the event
 * @param domain Domain of the dApp that initiated the event
 * @param isJsBridge Whether the event originated from JS Bridge (injected provider)
 * @param tabId Browser tab ID for JS Bridge events
 * @param sessionId Session identifier for the connection
 * @param isLocal
 * @param messageId
 * @param traceId
 * @param dAppInfo
 * @param returnStrategy Raw TonConnect return strategy string.
 */
@Serializable
data class TONSignDataRequestEvent(

    /* Unique identifier for the bridge event */
    @SerialName(value = "id")
    val id: kotlin.String,

    @SerialName(value = "payload")
    val payload: TONSignDataPayload,

    @SerialName(value = "preview")
    val preview: TONSignDataRequestEventPreview,

    @SerialName(value = "from")
    val from: kotlin.String? = null,

    @Contextual @SerialName(value = "walletAddress")
    val walletAddress: io.ton.walletkit.model.TONUserFriendlyAddress? = null,

    /* Wallet identifier associated with the event */
    @SerialName(value = "walletId")
    val walletId: kotlin.String? = null,

    /* Domain of the dApp that initiated the event */
    @SerialName(value = "domain")
    val domain: kotlin.String? = null,

    /* Whether the event originated from JS Bridge (injected provider) */
    @SerialName(value = "isJsBridge")
    val isJsBridge: kotlin.Boolean? = null,

    /* Browser tab ID for JS Bridge events */
    @SerialName(value = "tabId")
    val tabId: kotlin.String? = null,

    /* Session identifier for the connection */
    @SerialName(value = "sessionId")
    val sessionId: kotlin.String? = null,

    @SerialName(value = "isLocal")
    val isLocal: kotlin.Boolean? = null,

    @SerialName(value = "messageId")
    val messageId: kotlin.String? = null,

    @SerialName(value = "traceId")
    val traceId: kotlin.String? = null,

    @SerialName(value = "dAppInfo")
    val dAppInfo: TONDAppInfo? = null,

    /* Raw TonConnect return strategy string. */
    @SerialName(value = "returnStrategy")
    val returnStrategy: kotlin.String? = null,

) {

    companion object
}
