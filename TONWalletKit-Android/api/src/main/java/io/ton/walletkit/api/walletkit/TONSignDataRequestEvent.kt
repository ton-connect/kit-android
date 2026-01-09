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
package io.ton.walletkit.api.walletkit

import io.ton.walletkit.api.generated.TONDAppInfo
import io.ton.walletkit.api.generated.TONSignDataPayload
import io.ton.walletkit.api.generated.TONSignDataRequestEventPreview
import io.ton.walletkit.model.TONUserFriendlyAddress
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Event containing a sign data request from a dApp via TON Connect.
 *
 * This model extends the generated SignDataRequestEvent with all BridgeEvent fields.
 */
@Serializable
data class TONSignDataRequestEvent(
    /** Unique identifier for the bridge event */
    @SerialName(value = "id")
    val id: String? = null,

    @SerialName(value = "from")
    val from: String? = null,

    @Contextual
    @SerialName(value = "walletAddress")
    val walletAddress: TONUserFriendlyAddress? = null,

    /** Wallet identifier associated with the event */
    @SerialName(value = "walletId")
    val walletId: String? = null,

    /** Domain of the dApp that initiated the event */
    @SerialName(value = "domain")
    val domain: String? = null,

    /** Whether the event originated from JS Bridge (injected provider) */
    @SerialName(value = "isJsBridge")
    val isJsBridge: Boolean? = null,

    /** Browser tab ID for JS Bridge events */
    @SerialName(value = "tabId")
    val tabId: String? = null,

    /** Session identifier for the connection */
    @SerialName(value = "sessionId")
    val sessionId: String? = null,

    @SerialName(value = "isLocal")
    val isLocal: Boolean? = null,

    @SerialName(value = "messageId")
    val messageId: String? = null,

    @SerialName(value = "traceId")
    val traceId: String? = null,

    /** Information about the requesting dApp */
    @SerialName(value = "dAppInfo")
    val dAppInfo: TONDAppInfo? = null,

    @SerialName(value = "payload")
    val payload: TONSignDataPayload,

    @SerialName(value = "preview")
    val preview: TONSignDataRequestEventPreview,
)
