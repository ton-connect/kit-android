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

import io.ton.walletkit.model.DAppInfo
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Represents a connection request event from the bridge.
 * Provides the typed representation of the event data for consumers.
 */
@Serializable
data class ConnectRequestEvent(
    val id: String? = null,
    val from: String? = null,
    val preview: Preview? = null,
    val request: List<Request>? = null,
    val dAppInfo: DAppInfo? = null,
    var walletAddress: String? = null,
    var walletId: String? = null,

    // JS Bridge fields for internal browser
    val isJsBridge: Boolean? = null,
    val domain: String? = null,
    val tabId: String? = null,
    val sessionId: String? = null,
    val isLocal: Boolean? = null,
    val messageId: String? = null,
    val traceId: String? = null,
    val method: String? = null,
    // params can be either an array or an object depending on the method
    // For 'connect' method: params is an object with manifest info
    // For 'send' method: params is an array
    val params: JsonElement? = null,
) {
    @Serializable
    data class Preview(
        val manifestURL: String? = null,
        val manifest: Manifest? = null,
        val permissions: List<ConnectPermission>,
        val requestedItems: List<Request>? = null,
        val manifestFetchErrorCode: Int? = null,
    )

    @Serializable
    data class Manifest(
        val name: String? = null,
        val description: String? = null,
        val url: String? = null,
        val iconUrl: String? = null,
    )

    @Serializable
    data class ConnectPermission(
        val name: String? = null,
        val title: String? = null,
        val description: String? = null,
    )

    @Serializable
    data class Request(
        val name: String? = null,
        val payload: String? = null,
    )
}
