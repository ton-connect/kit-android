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
import io.ton.walletkit.event.TONWalletKitEvent
import io.ton.walletkit.internal.constants.BridgeMethodConstants
import io.ton.walletkit.listener.TONBridgeEventsHandler
import io.ton.walletkit.streaming.ITONStreamingManager
import io.ton.walletkit.streaming.ITONStreamingProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject

internal class TONStreamingManager(
    private val engine: WalletKitEngine,
) : ITONStreamingManager {

    override fun hasProvider(network: TONNetwork): Boolean = runBlocking {
        try {
            val params = JSONObject().apply {
                put("network", createNetworkJson(network))
            }
            val response = engine.callBridgeMethod(BridgeMethodConstants.METHOD_STREAMING_HAS_PROVIDER, params)
            response.optBoolean("hasProvider", false)
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun register(provider: ITONStreamingProvider) {
        val bridgeProvider = provider as? TONStreamingProviderImpl
            ?: throw IllegalArgumentException("Streaming provider must be created by TONWalletKit")

        engine.callBridgeMethod(
            BridgeMethodConstants.METHOD_REGISTER_STREAMING_PROVIDER,
            JSONObject().apply {
                put("providerId", bridgeProvider.providerId)
            },
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
        val params = JSONObject().apply {
            put("network", createNetworkJson(network))
        }

        launch {
            try {
                val response = engine.callBridgeMethod(BridgeMethodConstants.METHOD_STREAMING_WATCH_CONNECTION_CHANGE, params)
                subscriptionId = response.optString("subscriptionId").takeUnless { it.isBlank() }
            } catch (e: Exception) {
                close(e)
            }
        }

        val handler = object : TONBridgeEventsHandler {
            override fun handle(event: TONWalletKitEvent) {
                val currentSubId = subscriptionId ?: return
                if (event is TONWalletKitEvent.StreamingConnectionChange && event.subscriptionId == currentSubId) {
                    trySend(event.connected)
                }
            }
        }
        launch { engine.addEventsHandler(handler) }

        awaitClose {
            launch {
                engine.removeEventsHandler(handler)
                subscriptionId?.let { subId ->
                    try {
                        engine.callBridgeMethod(
                            BridgeMethodConstants.METHOD_STREAMING_UNWATCH,
                            JSONObject().apply { put("subscriptionId", subId) },
                        )
                    } catch (e: Exception) { }
                }
            }
        }
    }

    private fun createWatchFlow(network: TONNetwork, address: String, types: List<TONStreamingWatchType>, onUpdate: (TONStreamingUpdate) -> Unit): Flow<Any> = callbackFlow {
        var subscriptionId: String? = null
        val params = JSONObject().apply {
            put("network", createNetworkJson(network))
            put("address", address)
            val typesArray = JSONArray()
            types.forEach { typesArray.put(it.value) }
            put("types", typesArray)
        }

        launch {
            try {
                val response = engine.callBridgeMethod(BridgeMethodConstants.METHOD_STREAMING_WATCH, params)
                subscriptionId = response.optString("subscriptionId").takeUnless { it.isBlank() }
            } catch (e: Exception) {
                close(e)
            }
        }

        val handler = object : TONBridgeEventsHandler {
            override fun handle(event: TONWalletKitEvent) {
                val currentSubId = subscriptionId ?: return
                if (event is TONWalletKitEvent.StreamingUpdate && event.subscriptionId == currentSubId) {
                    onUpdate(event.update)
                }
            }
        }
        launch { engine.addEventsHandler(handler) }

        awaitClose {
            launch {
                engine.removeEventsHandler(handler)
                subscriptionId?.let { subId ->
                    try {
                        engine.callBridgeMethod(
                            BridgeMethodConstants.METHOD_STREAMING_UNWATCH,
                            JSONObject().apply { put("subscriptionId", subId) },
                        )
                    } catch (e: Exception) { }
                }
            }
        }
    }

    override fun updates(network: TONNetwork, address: String, types: List<TONStreamingWatchType>): Flow<TONStreamingUpdate> = callbackFlow {
        var subscriptionId: String? = null
        val params = JSONObject().apply {
            put("network", createNetworkJson(network))
            put("address", address)
            val typesArray = JSONArray()
            types.forEach { typesArray.put(it.value) }
            put("types", typesArray)
        }

        launch {
            try {
                val response = engine.callBridgeMethod(BridgeMethodConstants.METHOD_STREAMING_WATCH, params)
                subscriptionId = response.optString("subscriptionId").takeUnless { it.isBlank() }
            } catch (e: Exception) {
                close(e)
            }
        }

        val handler = object : TONBridgeEventsHandler {
            override fun handle(event: TONWalletKitEvent) {
                val currentSubId = subscriptionId ?: return
                if (event is TONWalletKitEvent.StreamingUpdate && event.subscriptionId == currentSubId) {
                    trySend(event.update)
                }
            }
        }
        launch { engine.addEventsHandler(handler) }

        awaitClose {
            launch {
                engine.removeEventsHandler(handler)
                subscriptionId?.let { subId ->
                    try {
                        engine.callBridgeMethod(
                            BridgeMethodConstants.METHOD_STREAMING_UNWATCH,
                            JSONObject().apply { put("subscriptionId", subId) },
                        )
                    } catch (e: Exception) { }
                }
            }
        }
    }

    override fun balance(network: TONNetwork, address: String): Flow<TONBalanceUpdate> = callbackFlow {
        var subscriptionId: String? = null
        val params = JSONObject().apply {
            put("network", createNetworkJson(network))
            put("address", address)
            put("types", JSONArray().apply { put(TONStreamingWatchType.balance.value) })
        }

        launch {
            try {
                val response = engine.callBridgeMethod(BridgeMethodConstants.METHOD_STREAMING_WATCH, params)
                subscriptionId = response.optString("subscriptionId").takeUnless { it.isBlank() }
            } catch (e: Exception) {
                close(e)
            }
        }

        val handler = object : TONBridgeEventsHandler {
            override fun handle(event: TONWalletKitEvent) {
                val currentSubId = subscriptionId ?: return
                if (event is TONWalletKitEvent.StreamingUpdate && event.subscriptionId == currentSubId) {
                    val update = event.update
                    if (update is TONStreamingUpdate.Balance) {
                        trySend(update.value)
                    }
                }
            }
        }
        launch { engine.addEventsHandler(handler) }

        awaitClose {
            launch {
                engine.removeEventsHandler(handler)
                subscriptionId?.let { subId ->
                    try {
                        engine.callBridgeMethod(
                            BridgeMethodConstants.METHOD_STREAMING_UNWATCH,
                            JSONObject().apply { put("subscriptionId", subId) },
                        )
                    } catch (e: Exception) { }
                }
            }
        }
    }

    override fun transactions(network: TONNetwork, address: String): Flow<TONTransactionsUpdate> = callbackFlow {
        var subscriptionId: String? = null
        val params = JSONObject().apply {
            put("network", createNetworkJson(network))
            put("address", address)
            put("types", JSONArray().apply { put(TONStreamingWatchType.transactions.value) })
        }

        launch {
            try {
                val response = engine.callBridgeMethod(BridgeMethodConstants.METHOD_STREAMING_WATCH, params)
                subscriptionId = response.optString("subscriptionId").takeUnless { it.isBlank() }
            } catch (e: Exception) {
                close(e)
            }
        }

        val handler = object : TONBridgeEventsHandler {
            override fun handle(event: TONWalletKitEvent) {
                val currentSubId = subscriptionId ?: return
                if (event is TONWalletKitEvent.StreamingUpdate && event.subscriptionId == currentSubId) {
                    val update = event.update
                    if (update is TONStreamingUpdate.Transactions) {
                        trySend(update.value)
                    }
                }
            }
        }
        launch { engine.addEventsHandler(handler) }

        awaitClose {
            launch {
                engine.removeEventsHandler(handler)
                subscriptionId?.let { subId ->
                    try {
                        engine.callBridgeMethod(
                            BridgeMethodConstants.METHOD_STREAMING_UNWATCH,
                            JSONObject().apply { put("subscriptionId", subId) },
                        )
                    } catch (e: Exception) { }
                }
            }
        }
    }

    override fun jettons(network: TONNetwork, address: String): Flow<TONJettonUpdate> = callbackFlow {
        var subscriptionId: String? = null
        val params = JSONObject().apply {
            put("network", createNetworkJson(network))
            put("address", address)
            put("types", JSONArray().apply { put(TONStreamingWatchType.jettons.value) })
        }

        launch {
            try {
                val response = engine.callBridgeMethod(BridgeMethodConstants.METHOD_STREAMING_WATCH, params)
                subscriptionId = response.optString("subscriptionId").takeUnless { it.isBlank() }
            } catch (e: Exception) {
                close(e)
            }
        }

        val handler = object : TONBridgeEventsHandler {
            override fun handle(event: TONWalletKitEvent) {
                val currentSubId = subscriptionId ?: return
                if (event is TONWalletKitEvent.StreamingUpdate && event.subscriptionId == currentSubId) {
                    val update = event.update
                    if (update is TONStreamingUpdate.Jettons) {
                        trySend(update.value)
                    }
                }
            }
        }
        launch { engine.addEventsHandler(handler) }

        awaitClose {
            launch {
                engine.removeEventsHandler(handler)
                subscriptionId?.let { subId ->
                    try {
                        engine.callBridgeMethod(
                            BridgeMethodConstants.METHOD_STREAMING_UNWATCH,
                            JSONObject().apply { put("subscriptionId", subId) },
                        )
                    } catch (e: Exception) { }
                }
            }
        }
    }

    private fun createNetworkJson(network: TONNetwork): JSONObject =
        JSONObject().apply {
            put("chainId", network.chainId)
        }
}
