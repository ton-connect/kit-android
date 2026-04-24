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
package io.ton.walletkit.core.streaming

import io.ton.walletkit.api.generated.TONBalanceUpdate
import io.ton.walletkit.api.generated.TONJettonUpdate
import io.ton.walletkit.api.generated.TONNetwork
import io.ton.walletkit.api.generated.TONTransactionsUpdate
import io.ton.walletkit.engine.WalletKitEngine
import io.ton.walletkit.internal.constants.BridgeMethodConstants
import io.ton.walletkit.internal.util.Logger
import io.ton.walletkit.streaming.ITONStreamingProvider
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

internal class TONStreamingProviderImpl(
    private val engine: WalletKitEngine,
    override val network: TONNetwork,
    override val id: String,
) : ITONStreamingProvider {

    override suspend fun connect() {
        engine.callBridgeMethod(BridgeMethodConstants.METHOD_STREAMING_CONNECT)
    }

    override suspend fun disconnect() {
        engine.callBridgeMethod(BridgeMethodConstants.METHOD_STREAMING_DISCONNECT)
    }

    override fun connectionChange(): Flow<Boolean> = callbackFlow {
        var subId: String? = null
        val params = JSONObject().apply { put("network", networkJson()) }

        val collectJob = launch {
            engine.streamingEvents.collect { event ->
                val id = subId ?: return@collect
                if (event is StreamingEvent.ConnectionChange && event.subscriptionId == id) {
                    trySend(event.connected)
                }
            }
        }

        try {
            val response = engine.callBridgeMethod(BridgeMethodConstants.METHOD_STREAMING_WATCH_CONNECTION_CHANGE, params)
            subId = response.optString("subscriptionId").takeUnless { it.isBlank() }
        } catch (e: Exception) {
            collectJob.cancel()
            close(e)
            return@callbackFlow
        }

        awaitClose {
            collectJob.cancel()
            unwatchSubscription(subId)
        }
    }

    override fun balance(address: String): Flow<TONBalanceUpdate> =
        watchFlow(BridgeMethodConstants.METHOD_STREAMING_WATCH_BALANCE, address) { event ->
            (event as? StreamingEvent.BalanceUpdate)?.update
        }

    override fun transactions(address: String): Flow<TONTransactionsUpdate> =
        watchFlow(BridgeMethodConstants.METHOD_STREAMING_WATCH_TRANSACTIONS, address) { event ->
            (event as? StreamingEvent.TransactionsUpdate)?.update
        }

    override fun jettons(address: String): Flow<TONJettonUpdate> =
        watchFlow(BridgeMethodConstants.METHOD_STREAMING_WATCH_JETTONS, address) { event ->
            (event as? StreamingEvent.JettonsUpdate)?.update
        }

    private fun <T> watchFlow(method: String, address: String, transform: (StreamingEvent) -> T?): Flow<T> = callbackFlow {
        var subId: String? = null
        val params = JSONObject().apply {
            put("network", JSONObject().apply { put("chainId", network.chainId) })
            put("address", address)
        }

        val collectJob = launch {
            engine.streamingEvents.collect { event ->
                val id = subId ?: return@collect
                if (event.subscriptionId == id) {
                    transform(event)?.let { trySend(it) }
                }
            }
        }

        try {
            val response = engine.callBridgeMethod(method, params)
            subId = response.optString("subscriptionId").takeUnless { it.isBlank() }
        } catch (e: Exception) {
            collectJob.cancel()
            close(e)
            return@callbackFlow
        }

        awaitClose {
            collectJob.cancel()
            unwatchSubscription(subId)
        }
    }

    /**
     * Fires METHOD_STREAMING_UNWATCH so the JS-side subscription stops and its entry is
     * removed from the bridge registry. Called from [callbackFlow]'s [awaitClose], which
     * runs once when the collector goes away — cleanup is tied to Flow lifecycle, not to
     * any JS-ref releaseOnce machinery.
     */
    private fun unwatchSubscription(id: String?) {
        if (id == null) return
        @OptIn(DelicateCoroutinesApi::class)
        GlobalScope.launch {
            try {
                engine.callBridgeMethod(
                    BridgeMethodConstants.METHOD_STREAMING_UNWATCH,
                    JSONObject().apply { put("subscriptionId", id) },
                )
            } catch (_: Exception) {
                Logger.w(TAG, "Failed to unwatch streaming subscription: $id")
            }
        }
    }

    private fun networkJson(): JSONObject =
        JSONObject().apply { put("chainId", network.chainId) }

    private companion object {
        const val TAG = "TONStreamingProviderImpl"
    }
}
