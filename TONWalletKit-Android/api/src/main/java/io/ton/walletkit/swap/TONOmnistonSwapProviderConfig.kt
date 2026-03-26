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
package io.ton.walletkit.swap

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Configuration for the Omniston (STON.fi) swap provider.
 *
 * All fields are optional; omitting them uses provider defaults.
 */
@Serializable
data class TONOmnistonSwapProviderConfig(
    /** Custom provider identifier. Defaults to "omniston". */
    @SerialName("providerId") val providerId: String? = null,
    /** Omniston WebSocket API URL. Default: wss://omni-ws.ston.fi */
    @SerialName("apiUrl") val apiUrl: String? = null,
    /** Default slippage tolerance in basis points (1 bp = 0.01%). */
    @SerialName("defaultSlippageBps") val defaultSlippageBps: Int? = null,
    /** Timeout for quote requests in milliseconds. */
    @SerialName("quoteTimeoutMs") val quoteTimeoutMs: Int? = null,
    /** Referrer address. */
    @SerialName("referrerAddress") val referrerAddress: String? = null,
    /** Referrer fee in basis points. */
    @SerialName("referrerFeeBps") val referrerFeeBps: Int? = null,
    /** Whether a flexible referrer fee is allowed. */
    @SerialName("flexibleReferrerFee") val flexibleReferrerFee: Boolean? = null,
)
