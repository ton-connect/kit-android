package io.ton.walletkit.demo

import android.app.Application
import io.ton.walletkit.bridge.WalletKitBridge
import io.ton.walletkit.storage.WalletKitStorage
import io.ton.walletkit.storage.impl.DebugSharedPrefsStorage

class WalletKitDemoApp : Application() {
    val bridge: WalletKitBridge by lazy {
        WalletKitBridge(this)
    }

    val storage: WalletKitStorage by lazy {
        DebugSharedPrefsStorage(this)
    }
}
