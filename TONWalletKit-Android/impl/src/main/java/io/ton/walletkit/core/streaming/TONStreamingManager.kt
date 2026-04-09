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
import io.ton.walletkit.api.generated.TONStreamingUpdate
import io.ton.walletkit.api.generated.TONStreamingWatchType
import io.ton.walletkit.api.generated.TONTransactionsUpdate
import io.ton.walletkit.engine.WalletKitEngine
import io.ton.walletkit.internal.constants.BridgeMethodConstants
import io.ton.walletkit.streaming.ITONStreamingManager
import io.ton.walletkit.streaming.ITONStreamingProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

internal class TONStreamingManager(
    private val engine: WalletKitEngine,
) : ITONStreamingManager {

    override suspend fun hasProvider(network: TONNetwork): Boolean {
        return try {
            val params = JSONObject().apply {
                put("network", networkJson(network))
            }
            val response = engine.callBridgeMethod(BridgeMethodConstants.METHOD_STREAMING_HAS_PROVIDER, params)
            response.optBoolean("hasProvider", false)
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun register(provider: ITONStreamingProvider) {
        engine.callBridgeMethod(
            BridgeMethodConstants.METHOD_REGISTER_STREAMING_PROVIDER,
            JSONObject().apply { put("providerId", provider.id) },
        )
    }

    override suspend fun connect() {
        engine.callBridgeMethod(BridgeMethodConstants.METHOD_STREAMING_CONNECT)
    }

    override suspend fun disconnect() {
        engine.callBridgeMethod(BridgeMethodConstants.METHOD_STREAMING_DISCONNECT)
    }

    override fun connectionChange(network: TONNetwork): Flow<Boolean> = callbackFlow {
        var subscriptionId: String? = null
        val params = JSONObject().apply { put("network", networkJson(network)) }

        val collectJob = launch {
            engine.streamingEvents.collect { event ->
                val sub = subscriptionId ?: return@collect
                if (event is StreamingEvent.ConnectionChange && event.subscriptionId == sub) {
                    trySend(event.connected)
                }
            }
        }

        try {
            val response = engine.callBridgeMethod(BridgeMethodConstants.METHOD_STREAMING_WATCH_CONNECTION_CHANGE, params)
            subscriptionId = response.optString("subscriptionId").takeUnless { it.isBlank() }
        } catch (e: Exception) {
            collectJob.cancel()
            close(e)
            return@callbackFlow
        }

        awaitClose {
            collectJob.cancel()
            launch { subscriptionId?.let { subId -> unwatch(subId) } }
        }
    }

    override fun balance(network: TONNetwork, address: String): Flow<TONBalanceUpdate> =
        watchFlow(watchParams(network, address, TONStreamingWatchType.balance)) {
            (it as? TONStreamingUpdate.Balance)?.value
        }

    override fun transactions(network: TONNetwork, address: String): Flow<TONTransactionsUpdate> =
        watchFlow(watchParams(network, address, TONStreamingWatchType.transactions)) {
            (it as? TONStreamingUpdate.Transactions)?.value
        }

    override fun jettons(network: TONNetwork, address: String): Flow<TONJettonUpdate> =
        watchFlow(watchParams(network, address, TONStreamingWatchType.jettons)) {
            (it as? TONStreamingUpdate.Jettons)?.value
        }

    override fun updates(network: TONNetwork, address: String, types: List<TONStreamingWatchType>): Flow<TONStreamingUpdate> =
        watchFlow(watchParams(network, address, *types.toTypedArray())) { it }

    private fun <T> watchFlow(params: JSONObject, transform: (TONStreamingUpdate) -> T?): Flow<T> = callbackFlow {
        var subscriptionId: String? = null

        val collectJob = launch {
            engine.streamingEvents.collect { event ->
                val sub = subscriptionId ?: return@collect
                if (event is StreamingEvent.Update && event.subscriptionId == sub) {
                    transform(event.update)?.let { trySend(it) }
                }
            }
        }

        try {
            val response = engine.callBridgeMethod(BridgeMethodConstants.METHOD_STREAMING_WATCH, params)
            subscriptionId = response.optString("subscriptionId").takeUnless { it.isBlank() }
        } catch (e: Exception) {
            collectJob.cancel()
            close(e)
            return@callbackFlow
        }

        awaitClose {
            collectJob.cancel()
            launch { subscriptionId?.let { subId -> unwatch(subId) } }
        }
    }

    private suspend fun unwatch(subscriptionId: String) {
        try {
            engine.callBridgeMethod(
                BridgeMethodConstants.METHOD_STREAMING_UNWATCH,
                JSONObject().apply { put("subscriptionId", subscriptionId) },
            )
        } catch (_: Exception) { }
    }

    private fun watchParams(network: TONNetwork, address: String, vararg types: TONStreamingWatchType): JSONObject =
        JSONObject().apply {
            put("network", networkJson(network))
            put("address", address)
            put("types", JSONArray().apply { types.forEach { put(it.value) } })
        }

    private fun networkJson(network: TONNetwork): JSONObject =
        JSONObject().apply { put("chainId", network.chainId) }
}
