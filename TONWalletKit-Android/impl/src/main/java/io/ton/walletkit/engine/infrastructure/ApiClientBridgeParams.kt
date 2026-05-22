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

import io.ton.walletkit.api.generated.TONNFTsRequest
import io.ton.walletkit.api.generated.TONNetwork
import io.ton.walletkit.api.generated.TONRawStackItem
import io.ton.walletkit.api.generated.TONUserNFTsRequest
import kotlinx.serialization.Serializable

internal sealed interface ApiParamsWithNetwork {
    val network: TONNetwork
}

@Serializable
internal data class ApiNetworkOnlyParams(override val network: TONNetwork) : ApiParamsWithNetwork

@Serializable
internal data class ApiSendBocParams(
    override val network: TONNetwork,
    val boc: String,
) : ApiParamsWithNetwork

@Serializable
internal data class ApiRunGetMethodParams(
    override val network: TONNetwork,
    val address: String,
    val method: String,
    val stack: List<TONRawStackItem>? = null,
    val seqno: Int? = null,
) : ApiParamsWithNetwork

@Serializable
internal data class ApiAddressSeqnoParams(
    override val network: TONNetwork,
    val address: String,
    val seqno: Int? = null,
) : ApiParamsWithNetwork

@Serializable
internal data class ApiNftItemsByAddressParams(
    override val network: TONNetwork,
    val request: TONNFTsRequest,
) : ApiParamsWithNetwork

@Serializable
internal data class ApiNftItemsByOwnerParams(
    override val network: TONNetwork,
    val request: TONUserNFTsRequest,
) : ApiParamsWithNetwork

@Serializable
internal data class ApiFetchEmulationParams(
    override val network: TONNetwork,
    val messageBoc: String,
    val ignoreSignature: Boolean? = null,
) : ApiParamsWithNetwork

@Serializable
internal data class ApiAccountStatesParams(
    override val network: TONNetwork,
    val addresses: List<String>,
) : ApiParamsWithNetwork

@Serializable
internal data class ApiResolveDnsParams(
    override val network: TONNetwork,
    val domain: String,
) : ApiParamsWithNetwork

@Serializable
internal data class ApiBackResolveDnsParams(
    override val network: TONNetwork,
    val address: String,
) : ApiParamsWithNetwork
