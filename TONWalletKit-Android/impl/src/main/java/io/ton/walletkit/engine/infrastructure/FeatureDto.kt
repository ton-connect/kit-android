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
package io.ton.walletkit.engine.infrastructure

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

/**
 * Shared TonConnect feature DTOs. Used by both [io.ton.walletkit.browser.TonConnectInjector]
 * (when building the `injectWalletKit(...)` device-info payload) and by
 * [InitializationManager] (when building the `init` RPC's device-info features list).
 *
 * Both producers must emit the same wire shape:
 *   * `SendTransaction`: `{ name: "SendTransaction", maxMessages?: Int, extraCurrencySupported?: Bool }`
 *   * `SignData`:        `{ name: "SignData", types: ["text", "binary", ...] }`
 *
 * @suppress Internal bridge plumbing only.
 */

@OptIn(ExperimentalSerializationApi::class)
@Serializable
internal data class SendTransactionFeatureDto(
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val name: String = "SendTransaction",
    val maxMessages: Int? = null,
    val extraCurrencySupported: Boolean? = null,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
internal data class SignDataFeatureDto(
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val name: String = "SignData",
    val types: List<String>,
)
