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
 * Union of all intent request events, discriminated by `intentType`.
 *
 * @param id Unique identifier for the bridge event
 * @param origin
 * @param hasConnectRequest Whether a connect flow should follow after intent approval
 * @param intentType Event type discriminator
 * @param deliveryMode
 * @param items Original intent action items (for display / re-conversion)
 * @param manifestUrl Manifest URL (for domain binding)
 * @param payload
 * @param actionUrl Action URL to fetch
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
 * @param clientId Client public key (for response encryption)
 * @param network Network chain ID
 * @param validUntil Transaction validity deadline (unix timestamp)
 * @param resolvedTransaction
 * @param preview
 */
@Serializable
data class TONIntentRequestEvent(

    /* Unique identifier for the bridge event */
    @SerialName(value = "id")
    val id: kotlin.String,

    @Contextual @SerialName(value = "origin")
    val origin: TONIntentOrigin,

    /* Whether a connect flow should follow after intent approval */
    @SerialName(value = "hasConnectRequest")
    val hasConnectRequest: kotlin.Boolean,

    /* Event type discriminator */
    @SerialName(value = "intentType")
    val intentType: TONIntentRequestEvent.IntentType,

    @Contextual @SerialName(value = "deliveryMode")
    val deliveryMode: TONIntentDeliveryMode,

    /* Original intent action items (for display / re-conversion) */
    @SerialName(value = "items")
    val items: kotlin.collections.List<TONIntentActionItem>,

    /* Manifest URL (for domain binding) */
    @SerialName(value = "manifestUrl")
    val manifestUrl: kotlin.String,

    @SerialName(value = "payload")
    val payload: TONSignDataPayload,

    /* Action URL to fetch */
    @SerialName(value = "actionUrl")
    val actionUrl: kotlin.String,

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

    /* Client public key (for response encryption) */
    @SerialName(value = "clientId")
    val clientId: kotlin.String? = null,

    /* Network chain ID */
    @SerialName(value = "network")
    val network: kotlin.String? = null,

    /* Transaction validity deadline (unix timestamp) */
    @SerialName(value = "validUntil")
    val validUntil: kotlin.Int? = null,

    @SerialName(value = "resolvedTransaction")
    val resolvedTransaction: TONTransactionRequest? = null,

    @SerialName(value = "preview")
    val preview: TONTransactionIntentPreview? = null,

) {

    companion object

    /**
     * Event type discriminator
     *
     * Values: action
     */
    @Serializable
    enum class IntentType(val value: kotlin.String) {
        @SerialName(value = "action")
        action("action"),
    }
}
