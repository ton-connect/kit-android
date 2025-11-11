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
package io.ton.walletkit.bridge

import io.ton.walletkit.config.TONWalletKitConfiguration
import io.ton.walletkit.event.TONWalletKitEvent
import io.ton.walletkit.listener.TONBridgeEventsHandler
import io.ton.walletkit.model.TONNetwork
import java.util.concurrent.CopyOnWriteArrayList

val NoopEventsHandler: TONBridgeEventsHandler =
    object : TONBridgeEventsHandler {
        override fun handle(event: TONWalletKitEvent) = Unit
    }

fun testWalletKitConfiguration(
    network: TONNetwork = TONNetwork.TESTNET,
    persistent: Boolean = true,
    bridgeUrl: String = "https://bridge.tonapi.io/bridge",
    apiUrl: String? = null,
    features: List<TONWalletKitConfiguration.Feature> = emptyList(),
): TONWalletKitConfiguration {
    return TONWalletKitConfiguration(
        network = network,
        walletManifest =
        TONWalletKitConfiguration.Manifest(
            name = "Test Wallet",
            appName = "Wallet",
            imageUrl = "https://example.com/icon.png",
            aboutUrl = "https://example.com",
            universalLink = "https://example.com/app",
            bridgeUrl = bridgeUrl,
        ),
        bridge = TONWalletKitConfiguration.Bridge(bridgeUrl = bridgeUrl),
        apiClient = apiUrl?.let { TONWalletKitConfiguration.APIClient(url = it, key = "test-key") },
        features = features,
        storage = TONWalletKitConfiguration.Storage(persistent = persistent),
    )
}

class RecordingEventsHandler : TONBridgeEventsHandler {
    private val _events = CopyOnWriteArrayList<TONWalletKitEvent>()
    val events: List<TONWalletKitEvent>
        get() = _events

    override fun handle(event: TONWalletKitEvent) {
        _events += event
    }
}

fun compositeEventsHandler(vararg handlers: TONBridgeEventsHandler): TONBridgeEventsHandler =
    object : TONBridgeEventsHandler {
        override fun handle(event: TONWalletKitEvent) {
            handlers.forEach { it.handle(event) }
        }
    }
