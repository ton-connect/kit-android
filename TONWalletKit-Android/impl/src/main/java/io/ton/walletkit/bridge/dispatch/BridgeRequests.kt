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
package io.ton.walletkit.bridge.dispatch

import io.ton.walletkit.engine.operations.responses.BridgeByteArraySerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray

@Serializable
internal data class SignWithCustomSignerRequest(
    val signerId: String,
    @Serializable(with = BridgeByteArraySerializer::class) val data: ByteArray,
)

/** JS → native invocation of a callback previously wrapped via [WrappedFunctionRegistry]. */
@Serializable
internal data class CallByReferenceRequest(
    val refId: String,
    val args: JsonArray,
)

@Serializable
internal data class AdapterByIdRequest(val adapterId: String)

@Serializable
internal data class AdapterSignTransactionRequest(
    val adapterId: String,
    val input: String,
    val fakeSignature: Boolean? = null,
)

@Serializable
internal data class AdapterSignDataRequest(
    val adapterId: String,
    val input: String,
    val fakeSignature: Boolean? = null,
)

@Serializable
internal data class AdapterSignTonProofRequest(
    val adapterId: String,
    val input: String,
    val fakeSignature: Boolean? = null,
)

@Serializable
internal data class KotlinProviderQuoteRequest(val providerId: String, val params: String)

@Serializable
internal data class KotlinProviderBuildRequest(val providerId: String, val params: String)

@Serializable
internal data class KotlinProviderIdRequest(val providerId: String)

@Serializable
internal data class KotlinStakingGetStakedBalanceRequest(
    val providerId: String,
    val userAddress: String,
    val networkChainId: String? = null,
)

@Serializable
internal data class KotlinStakingGetProviderInfoRequest(
    val providerId: String,
    val networkChainId: String? = null,
)

@Serializable
internal data class KotlinProviderWatchRequest(
    val providerId: String,
    val subscriptionId: String,
    val type: String,
    val address: String? = null,
)

@Serializable
internal data class KotlinProviderUnwatchRequest(val subscriptionId: String)
