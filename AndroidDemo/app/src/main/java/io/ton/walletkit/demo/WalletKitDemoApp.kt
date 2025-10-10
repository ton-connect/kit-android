package io.ton.walletkit.demo

import android.app.Application
import io.ton.walletkit.presentation.WalletKitEngine
import io.ton.walletkit.presentation.WalletKitEngineFactory
import io.ton.walletkit.presentation.WalletKitEngineKind
import io.ton.walletkit.demo.storage.DemoAppStorage
import io.ton.walletkit.demo.storage.SecureDemoAppStorage

class WalletKitDemoApp : Application() {
    val defaultEngineKind: WalletKitEngineKind = WalletKitEngineKind.WEBVIEW

    fun obtainEngine(kind: WalletKitEngineKind = defaultEngineKind): WalletKitEngine {
        // Use factory to avoid compile-time dependency on engine implementations
        // This works with both webview-only and full SDK variants
        return WalletKitEngineFactory.create(this, kind)
    }
    
    /**
     * Check if a specific engine kind is available in the current SDK variant.
     */
    fun isEngineAvailable(kind: WalletKitEngineKind): Boolean {
        return WalletKitEngineFactory.isAvailable(kind)
    }

    /**
     * Demo app storage for wallet mnemonics, metadata, and user preferences.
     * This is separate from the SDK's internal bridge storage.
     */
    val storage: DemoAppStorage by lazy {
        SecureDemoAppStorage(this)
    }
}
