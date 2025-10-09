package io.ton.walletkit.demo

import android.app.Application
import io.ton.walletkit.bridge.WalletKitEngine
import io.ton.walletkit.bridge.WalletKitEngineKind
import io.ton.walletkit.bridge.impl.QuickJsWalletKitEngine
import io.ton.walletkit.bridge.impl.WebViewWalletKitEngine
import io.ton.walletkit.storage.WalletKitStorage
import io.ton.walletkit.storage.impl.SecureWalletKitStorage

class WalletKitDemoApp : Application() {
    val defaultEngineKind: WalletKitEngineKind = WalletKitEngineKind.WEBVIEW

    @Suppress("DEPRECATION") // QuickJS kept for PerformanceActivity benchmarking only
    fun obtainEngine(kind: WalletKitEngineKind = defaultEngineKind): WalletKitEngine = when (kind) {
        WalletKitEngineKind.WEBVIEW -> WebViewWalletKitEngine(this)
        WalletKitEngineKind.QUICKJS -> QuickJsWalletKitEngine(this) // Deprecated but kept for comparison
    }

    val storage: WalletKitStorage by lazy {
        // Production-ready secure storage using Android Keystore + EncryptedSharedPreferences
        SecureWalletKitStorage(this)
    }
}
