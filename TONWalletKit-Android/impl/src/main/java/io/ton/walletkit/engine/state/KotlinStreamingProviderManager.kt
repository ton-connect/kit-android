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
package io.ton.walletkit.engine.state

import io.ton.walletkit.engine.infrastructure.BridgeRpcClient
import io.ton.walletkit.internal.constants.BridgeMethodConstants
import io.ton.walletkit.internal.util.Logger
import io.ton.walletkit.streaming.ITONStreamingProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

/** Manages custom Kotlin [ITONStreamingProvider] instances registered into the JS bridge. */
internal class KotlinStreamingProviderManager(
    private val rpcClient: BridgeRpcClient,
    private val json: Json,
) {
    private data class SubscriptionEntry(
        val providerId: String,
        val job: Job,
    )

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val providers = ConcurrentHashMap<String, ITONStreamingProvider>()
    private val subscriptionJobs = ConcurrentHashMap<String, SubscriptionEntry>()

    fun register(providerId: String, provider: ITONStreamingProvider) {
        unregister(providerId)
        providers[providerId] = provider
    }

    fun getProvider(providerId: String): ITONStreamingProvider? = providers[providerId]

    fun unregister(providerId: String) {
        providers.remove(providerId)?.let { provider ->
            scope.launch {
                try {
                    provider.disconnect()
                } catch (e: Exception) {
                    Logger.w(TAG, "Failed to disconnect provider during unregister: id=$providerId", e)
                }
            }
        }
        val subIdsToRemove = subscriptionJobs
            .filterValues { it.providerId == providerId }
            .keys
            .toList()
        subIdsToRemove.forEach { subId ->
            subscriptionJobs.remove(subId)?.job?.cancel()
        }
    }

    fun watch(providerId: String, subId: String, type: String, address: String?) {
        val provider = providers[providerId] ?: run {
            Logger.w(TAG, "kotlinProviderWatch: no provider for id=$providerId")
            return
        }
        val job = scope.launch {
            try {
                when (type) {
                    TYPE_BALANCE -> provider.balance(address ?: return@launch).collect { dispatch(subId, it) }
                    TYPE_TRANSACTIONS -> provider.transactions(address ?: return@launch).collect { dispatch(subId, it) }
                    TYPE_JETTONS -> provider.jettons(address ?: return@launch).collect { dispatch(subId, it) }
                    TYPE_CONNECTION_CHANGE -> provider.connectionChange().collect { dispatch(subId, it) }
                    else -> Logger.w(TAG, "kotlinProviderWatch: unknown type=$type")
                }
            } catch (e: Exception) {
                Logger.w(TAG, "kotlinProviderWatch collection ended: subId=$subId", e)
            }
        }
        subscriptionJobs.put(subId, SubscriptionEntry(providerId, job))?.job?.cancel()
    }

    fun unwatch(subId: String) {
        subscriptionJobs.remove(subId)?.job?.cancel()
    }

    fun clear() {
        providers.keys.toList().forEach(::unregister)
        subscriptionJobs.values.forEach { it.job.cancel() }
        subscriptionJobs.clear()
    }

    private suspend inline fun <reified T> dispatch(subId: String, value: T) {
        try {
            val updateJson = json.encodeToString(value)
            rpcClient.call(
                BridgeMethodConstants.METHOD_KOTLIN_PROVIDER_DISPATCH,
                JSONObject().apply {
                    put("subId", subId)
                    put("updateJson", updateJson)
                },
            )
        } catch (_: Exception) {
        }
    }

    private companion object {
        private const val TAG = "KotlinStreamingProviderManager"
        const val TYPE_BALANCE = "balance"
        const val TYPE_TRANSACTIONS = "transactions"
        const val TYPE_JETTONS = "jettons"
        const val TYPE_CONNECTION_CHANGE = "connectionChange"
    }
}
