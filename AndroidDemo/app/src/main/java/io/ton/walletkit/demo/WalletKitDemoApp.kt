package io.ton.walletkit.demo

import android.app.Application
import io.ton.walletkit.bridge.WalletKitEngine
import io.ton.walletkit.bridge.WalletKitEngineKind
import io.ton.walletkit.bridge.impl.QuickJsWalletKitEngine
import io.ton.walletkit.bridge.impl.WebViewWalletKitEngine
import io.ton.walletkit.demo.storage.DemoAppStorage
import io.ton.walletkit.demo.storage.SecureDemoAppStorage

class WalletKitDemoApp : Application() {
    val defaultEngineKind: WalletKitEngineKind = WalletKitEngineKind.WEBVIEW

    @Suppress("DEPRECATION") // QuickJS kept for PerformanceActivity benchmarking only
    fun obtainEngine(kind: WalletKitEngineKind = defaultEngineKind): WalletKitEngine = when (kind) {
        WalletKitEngineKind.WEBVIEW -> WebViewWalletKitEngine(this)
        WalletKitEngineKind.QUICKJS -> QuickJsWalletKitEngine(this) // Deprecated but kept for comparison
    }

    /**
     * Demo app storage for wallet mnemonics, metadata, and user preferences.
     * This is separate from the SDK's internal bridge storage.
     */
    val storage: DemoAppStorage by lazy {
        SecureDemoAppStorage(this)
    }
}
