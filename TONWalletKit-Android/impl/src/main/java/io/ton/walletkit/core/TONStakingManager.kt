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
package io.ton.walletkit.core

import io.ton.walletkit.AnyTONProviderIdentifier
import io.ton.walletkit.ITONStakingManager
import io.ton.walletkit.ITONTonStakersStakingProvider
import io.ton.walletkit.TONStakingProviderIdentifier
import io.ton.walletkit.TONTonStakersStakingProviderIdentifier
import io.ton.walletkit.api.generated.TONNetwork
import io.ton.walletkit.api.generated.TONStakeParams
import io.ton.walletkit.api.generated.TONStakingBalance
import io.ton.walletkit.api.generated.TONStakingProviderInfo
import io.ton.walletkit.api.generated.TONStakingQuote
import io.ton.walletkit.api.generated.TONStakingQuoteParams
import io.ton.walletkit.api.generated.TONTransactionRequest
import io.ton.walletkit.api.generated.TONUnstakeMode
import io.ton.walletkit.engine.WalletKitEngine
import io.ton.walletkit.model.TONUserFriendlyAddress
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/**
 * Internal implementation of [ITONStakingManager] backed by the JS bridge engine.
 *
 * @suppress Internal. Use [ITONWalletKit.staking] to obtain an instance.
 */
internal class TONStakingManager(
    private val engine: WalletKitEngine,
) : ITONStakingManager {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun register(provider: ITONTonStakersStakingProvider) {
        val internalProvider = provider as? TONTonStakersStakingProvider
            ?: throw IllegalArgumentException("Unsupported staking provider implementation: ${provider::class.java.name}")
        engine.registerStakingProvider(internalProvider.internalRef)
    }

    override suspend fun setDefaultProvider(identifier: TONStakingProviderIdentifier<*, *>) {
        engine.setDefaultStakingProvider(identifier.name)
    }

    override suspend fun registeredProviders(): List<AnyTONProviderIdentifier> {
        return engine.getRegisteredStakingProviders().map { AnyTONProviderIdentifier(it) }
    }

    override suspend fun hasProvider(identifier: TONStakingProviderIdentifier<*, *>): Boolean {
        return engine.hasStakingProvider(identifier.name)
    }

    override suspend fun getQuote(
        params: TONStakingQuoteParams<JsonElement>,
        identifier: TONStakingProviderIdentifier<*, *>?,
    ): TONStakingQuote {
        return engine.getStakingQuote(params, identifier?.name)
    }

    override suspend fun buildStakeTransaction(
        params: TONStakeParams<JsonElement>,
        identifier: TONStakingProviderIdentifier<*, *>?,
    ): TONTransactionRequest {
        val content = engine.buildStakeTransaction(params, identifier?.name)
        return json.decodeFromString(content)
    }

    override suspend fun getStakedBalance(
        userAddress: TONUserFriendlyAddress,
        network: TONNetwork?,
        identifier: TONStakingProviderIdentifier<*, *>?,
    ): TONStakingBalance {
        return engine.getStakedBalance(userAddress.value, network, identifier?.name)
    }

    override suspend fun getStakingProviderInfo(
        network: TONNetwork?,
        identifier: TONStakingProviderIdentifier<*, *>?,
    ): TONStakingProviderInfo {
        return engine.getStakingProviderInfo(network, identifier?.name)
    }

    override suspend fun getSupportedUnstakeModes(
        identifier: TONStakingProviderIdentifier<*, *>?,
    ): List<TONUnstakeMode> {
        return engine.getSupportedUnstakeModes(identifier?.name)
    }
}

/**
 * Internal [ITONTonStakersStakingProvider] implementation.
 *
 * @property internalRef JS bridge registry reference ID returned by `createTonStakersStakingProvider`.
 * @property identifier The typed provider identifier understood by the staking manager.
 */
internal data class TONTonStakersStakingProvider(
    internal val internalRef: String,
    override val identifier: TONTonStakersStakingProviderIdentifier = TONTonStakersStakingProviderIdentifier(),
) : ITONTonStakersStakingProvider
