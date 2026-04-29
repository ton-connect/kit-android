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
package io.ton.walletkit.core.streaming

import io.ton.walletkit.api.generated.TONNetwork
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Internal bridge request models for streaming operations. These DTOs replace the
 * hand-rolled `JSONObject().apply { put(...) }` payloads that previously served the
 * register / connect / watch / unwatch RPCs. Encode through `json.toJSONObject(...)`
 * at the bridge boundary; the wire format is identical to what the manual builders
 * produced.
 *
 * @suppress Internal bridge communication only.
 */

@Serializable
internal data class StreamingNetworkRequest(val network: TONNetwork)

@Serializable
internal data class StreamingProviderRequest(val providerId: String)

@Serializable
internal data class StreamingProviderNetworkRequest(
    val providerId: String,
    val network: TONNetwork,
)

@Serializable
internal data class StreamingWatchRequest(
    val network: TONNetwork,
    val address: String,
)

@Serializable
internal data class StreamingWatchTypesRequest(
    val network: TONNetwork,
    val address: String,
    val types: List<String>,
)

@Serializable
internal data class StreamingSubscriptionRequest(val subscriptionId: String)

/**
 * Wraps a generated streaming-provider config (TonCenter / TonApi / etc.) for the
 * `startStreamingProvider` / `startWebViewStreamingProvider` core entry points.
 * The config is passed pre-encoded as [JsonElement] so this DTO doesn't need to
 * know every concrete config subclass.
 */
@Serializable
internal data class StartStreamingProviderRequest(val config: JsonElement)
