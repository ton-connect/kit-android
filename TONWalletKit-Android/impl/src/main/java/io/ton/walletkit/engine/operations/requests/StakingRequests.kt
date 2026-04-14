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
package io.ton.walletkit.engine.operations.requests

import io.ton.walletkit.api.generated.TONNetwork
import io.ton.walletkit.api.generated.TONStakingQuote
import io.ton.walletkit.api.generated.TONStakingQuoteDirection
import io.ton.walletkit.api.generated.TONTonStakersChainConfig
import io.ton.walletkit.api.generated.TONUnstakeMode
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * Internal bridge request models for staking operations.
 * These DTOs represent the exact JSON structure sent to the JavaScript bridge.
 *
 * @suppress Internal bridge communication only.
 */

@Serializable
internal data class CreateTonStakersStakingProviderRequest(
    /** Chain-ID keyed config map, e.g. { "-239": { ... } }. */
    val config: Map<String, TONTonStakersChainConfig>? = null,
)

@Serializable
internal data class RegisterStakingProviderRequest(
    val providerId: String,
)

@Serializable
internal data class SetDefaultStakingProviderRequest(
    val providerId: String,
)

@Serializable
internal data class GetStakingQuoteRequest(
    val direction: TONStakingQuoteDirection,
    val amount: String,
    val userAddress: String? = null,
    val network: TONNetwork? = null,
    val unstakeMode: TONUnstakeMode? = null,
    @Contextual val providerOptions: kotlinx.serialization.json.JsonElement? = null,
    val providerId: String? = null,
)

@Serializable
internal data class BuildStakeTransactionRequest(
    val quote: TONStakingQuote,
    val userAddress: String,
    @Contextual val providerOptions: kotlinx.serialization.json.JsonElement? = null,
    val providerId: String? = null,
)

@Serializable
internal data class GetStakedBalanceRequest(
    val userAddress: String,
    val network: TONNetwork? = null,
    val providerId: String? = null,
)

@Serializable
internal data class GetStakingProviderInfoRequest(
    val network: TONNetwork? = null,
    val providerId: String? = null,
)

@Serializable
internal data class GetSupportedUnstakeModesRequest(
    val providerId: String? = null,
)
