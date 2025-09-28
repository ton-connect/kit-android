package io.ton.walletkit.bridge.listener

import io.ton.walletkit.bridge.model.WalletKitEvent

fun interface WalletKitBridgeListener {
    fun onEvent(event: WalletKitEvent)
}
