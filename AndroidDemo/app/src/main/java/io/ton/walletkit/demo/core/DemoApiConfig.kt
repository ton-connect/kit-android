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
package io.ton.walletkit.demo.core

import io.ton.walletkit.api.TONTonStakersProviderConfig
import io.ton.walletkit.api.generated.TONTonStakersChainConfig
import io.ton.walletkit.demo.BuildConfig

object DemoApiConfig {
    private fun normalizeApiKey(value: String): String {
        val normalized = value.trim()
        if (normalized.isEmpty()) {
            return ""
        }
        if (normalized.startsWith("YOUR_") && normalized.endsWith("_KEY")) {
            return ""
        }
        return normalized
    }

    val toncenterApiKey: String
        get() = normalizeApiKey(BuildConfig.TONCENTER_API_KEY)

    val tonApiKey: String
        get() = normalizeApiKey(BuildConfig.TONAPI_API_KEY)

    fun tonStakersProviderConfig(): TONTonStakersProviderConfig? {
        val token = tonApiKey.takeIf { it.isNotEmpty() } ?: return null
        val chainConfig = TONTonStakersChainConfig(tonApiToken = token)
        return TONTonStakersProviderConfig(
            mainnet = chainConfig,
            testnet = chainConfig,
        )
    }
}
