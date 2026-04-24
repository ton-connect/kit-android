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
package io.ton.walletkit.api

import io.ton.walletkit.api.generated.TONTonStakersChainConfig

/**
 * Per-chain configuration for the TonStakers staking provider.
 *
 * The underlying JS API uses an index-signature type `{ [chainId: string]: TonStakersChainConfig }`
 * where chainIds are "-239" (mainnet) and "-3" (testnet). This class provides a named-field
 * alternative that converts to the required map via [toChainConfigMap].
 */
data class TONTonStakersProviderConfig(
    val mainnet: TONTonStakersChainConfig? = null,
    val testnet: TONTonStakersChainConfig? = null,
) {
    fun toChainConfigMap(): Map<String, TONTonStakersChainConfig> = buildMap {
        mainnet?.let { put("-239", it) }
        testnet?.let { put("-3", it) }
    }
}
