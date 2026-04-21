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
 * Configuration options for the TonAPI streaming provider (v2 WebSocket protocol).
 *
 * @param network
 * @param endpoint Full WebSocket URL for the streaming API. When set, it is used as-is (after http→wss normalization). When omitted, the default TonAPI host for the network is used with `/api/streaming/v2/ws`.
 * @param apiKey Optional bearer token for TonAPI (`token` query parameter on the WebSocket URL).
 */
@Serializable
data class TONTonApiStreamingProviderConfig(

    @SerialName(value = "network")
    val network: TONNetwork,

    /* Full WebSocket URL for the streaming API. When set, it is used as-is (after http→wss normalization). When omitted, the default TonAPI host for the network is used with `/api/streaming/v2/ws`. */
    @SerialName(value = "endpoint")
    val endpoint: kotlin.String? = null,

    /* Optional bearer token for TonAPI (`token` query parameter on the WebSocket URL). */
    @SerialName(value = "apiKey")
    val apiKey: kotlin.String? = null,

) {

    companion object
}
