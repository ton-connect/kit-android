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

    override fun setDefaultProvider(providerId: TONStakingProviderIdentifier) {
        // Delegates to engine on the next coroutine opportunity.
        // Stored locally so the manager can be used before the first suspending call.
        _pendingDefaultProviderId = providerId
    }

    /** Buffered default provider identifier applied lazily on the next bridge call. */
    @Suppress("PropertyName")
    @Volatile
    private var _pendingDefaultProviderId: TONStakingProviderIdentifier? = null

    private suspend fun applyPendingDefault() {
        _pendingDefaultProviderId?.let { identifier ->
            engine.setDefaultStakingProvider(identifier.name)
            _pendingDefaultProviderId = null
        }
    }

    override suspend fun getQuote(
        params: TONStakingQuoteParams<JsonElement>,
        providerId: TONStakingProviderIdentifier?,
    ): TONStakingQuote {
        applyPendingDefault()
        return engine.getStakingQuote(params, providerId?.name)
    }

    override suspend fun buildStakeTransaction(
        params: TONStakeParams<JsonElement>,
        providerId: TONStakingProviderIdentifier?,
    ): TONTransactionRequest {
        applyPendingDefault()
        val content = engine.buildStakeTransaction(params, providerId?.name)
        return json.decodeFromString(content)
    }

    override suspend fun getStakedBalance(
        userAddress: TONUserFriendlyAddress,
        network: TONNetwork?,
        providerId: TONStakingProviderIdentifier?,
    ): TONStakingBalance {
        applyPendingDefault()
        return engine.getStakedBalance(userAddress.value, network, providerId?.name)
    }

    override suspend fun getStakingProviderInfo(
        network: TONNetwork?,
        providerId: TONStakingProviderIdentifier?,
    ): TONStakingProviderInfo {
        applyPendingDefault()
        return engine.getStakingProviderInfo(network, providerId?.name)
    }

    override suspend fun getSupportedUnstakeModes(
        providerId: TONStakingProviderIdentifier?,
    ): List<TONUnstakeMode> {
        applyPendingDefault()
        return engine.getSupportedUnstakeModes(providerId?.name)
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
