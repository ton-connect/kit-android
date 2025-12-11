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
package io.ton.walletkit.event

import io.ton.walletkit.internal.constants.JsonConsts
import io.ton.walletkit.model.DAppInfo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Represents a transaction request event from the bridge.
 * Provides the typed representation of the event data for consumers.
 */
@Serializable
data class TransactionRequestEvent(
    val id: String? = null,
    val from: String? = null,
    val walletAddress: String? = null,
    var walletId: String? = null,
    val domain: String? = null,
    val sessionId: String? = null,
    val messageId: String? = null,
    val request: Request? = null,
    val dAppInfo: DAppInfo? = null,
    val preview: Preview? = null,
    val error: String? = null,

    // JS Bridge fields for internal browser
    val isJsBridge: Boolean? = null,
    val tabId: String? = null,
    val isLocal: Boolean? = null,
    val traceId: String? = null,
    val method: String? = null,
    // params can be either an array or an object depending on the protocol
    val params: JsonElement? = null,
) {
    @Serializable
    data class Request(
        val messages: List<Message>? = null,
        val network: String? = null,
        @SerialName(JsonConsts.KEY_VALID_UNTIL)
        val validUntil: Long? = null,
        val from: String? = null,
    )

    @Serializable
    data class Message(
        val address: String? = null,
        val amount: String? = null,
        val payload: String? = null,
        val stateInit: String? = null,
        val mode: Int? = null,
    )

    @Serializable
    data class Preview(
        val kind: String? = null,
        val content: String? = null,
        val manifest: Manifest? = null,
    )

    @Serializable
    data class Manifest(
        val name: String? = null,
        val url: String? = null,
        val iconUrl: String? = null,
    )
}
