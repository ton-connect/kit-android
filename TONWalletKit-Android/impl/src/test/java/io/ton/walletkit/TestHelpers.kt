/*
 * Copyright (c) 2025 TonTech
 */
package io.ton.walletkit

import io.ton.walletkit.config.TONWalletKitConfiguration
import io.ton.walletkit.event.TONWalletKitEvent
import io.ton.walletkit.listener.TONBridgeEventsHandler
import io.ton.walletkit.model.TONNetwork
import java.util.concurrent.CopyOnWriteArrayList

val NoopEventsHandler: TONBridgeEventsHandler =
    object : TONBridgeEventsHandler {
        override fun handle(event: TONWalletKitEvent) = Unit
    }

class RecordingEventsHandler : TONBridgeEventsHandler {
    private val _events = CopyOnWriteArrayList<TONWalletKitEvent>()
    val events: List<TONWalletKitEvent>
        get() = _events

    override fun handle(event: TONWalletKitEvent) {
        _events += event
    }
}
