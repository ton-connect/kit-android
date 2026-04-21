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
 * Configuration options for the TonCenter streaming provider.
 *
 * @param network
 * @param endpoint Optional custom WebSocket endpoint URL for the TonCenter v2 streaming API. If omitted, it defaults to the official mainnet or testnet URL based on the network context.
 * @param apiKey Optional API key for authenticating requests to TonCenter. Highly recommended to avoid rate limiting on the streaming endpoint.
 */
@Serializable
data class TONTonCenterStreamingProviderConfig(

    @SerialName(value = "network")
    val network: TONNetwork,

    /* Optional custom WebSocket endpoint URL for the TonCenter v2 streaming API. If omitted, it defaults to the official mainnet or testnet URL based on the network context. */
    @SerialName(value = "endpoint")
    val endpoint: kotlin.String? = null,

    /* Optional API key for authenticating requests to TonCenter. Highly recommended to avoid rate limiting on the streaming endpoint. */
    @SerialName(value = "apiKey")
    val apiKey: kotlin.String? = null,

) {

    companion object
}
