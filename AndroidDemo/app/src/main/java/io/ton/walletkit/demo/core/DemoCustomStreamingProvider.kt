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

import android.util.Log
import io.ton.walletkit.api.generated.TONBalanceUpdate
import io.ton.walletkit.api.generated.TONJettonUpdate
import io.ton.walletkit.api.generated.TONNetwork
import io.ton.walletkit.api.generated.TONStreamingUpdateStatus
import io.ton.walletkit.api.generated.TONTransactionsUpdate
import io.ton.walletkit.model.TONUserFriendlyAddress
import io.ton.walletkit.streaming.ITONStreamingProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion

/**
 * Synthetic provider used by the demo app to exercise the custom Kotlin-provider path.
 * It emits local balance ticks and connection changes without depending on a live backend.
 */
class DemoCustomStreamingProvider(
    override val network: TONNetwork = TONNetwork(chainId = "-3"),
    override val id: String = "demo-custom-provider-${network.chainId}",
) : ITONStreamingProvider {

    private val connectionState = MutableStateFlow(false)

    override suspend fun connect() {
        Log.d(TAG, "connect(): providerId=$id network=${network.chainId}")
        connectionState.value = true
    }

    override suspend fun disconnect() {
        Log.d(TAG, "disconnect(): providerId=$id network=${network.chainId}")
        connectionState.value = false
    }

    override fun connectionChange(): Flow<Boolean> = connectionState

    override fun balance(address: String): Flow<TONBalanceUpdate> = flow {
        val parsedAddress = TONUserFriendlyAddress.parse(address)
        var rawBalance = INITIAL_RAW_BALANCE
        Log.d(TAG, "balance(): start providerId=$id address=$address")
        emit(balanceUpdate(parsedAddress, rawBalance))
        while (true) {
            delay(BALANCE_TICK_MS)
            rawBalance += BALANCE_STEP
            emit(balanceUpdate(parsedAddress, rawBalance))
        }
    }.onCompletion {
        Log.d(TAG, "balance(): stop providerId=$id address=$address")
    }

    override fun transactions(address: String): Flow<TONTransactionsUpdate> = emptyFlow()

    override fun jettons(address: String): Flow<TONJettonUpdate> = emptyFlow()

    private fun balanceUpdate(address: TONUserFriendlyAddress, rawBalance: Long): TONBalanceUpdate = TONBalanceUpdate(
        status = TONStreamingUpdateStatus.confirmed,
        address = address,
        rawBalance = rawBalance.toString(),
        balance = rawBalance.toString(),
    )

    private companion object {
        private const val TAG = "DemoCustomProvider"
        private const val INITIAL_RAW_BALANCE = 1_000_000_000L
        private const val BALANCE_STEP = 50_000_000L
        private const val BALANCE_TICK_MS = 3_000L
    }
}
