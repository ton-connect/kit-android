package io.ton.walletkit.bridge.listener

import io.ton.walletkit.bridge.model.WalletKitEvent

fun interface WalletKitEngineListener {
    fun onEvent(event: WalletKitEvent)
}

typealias WalletKitBridgeListener = WalletKitEngineListener
