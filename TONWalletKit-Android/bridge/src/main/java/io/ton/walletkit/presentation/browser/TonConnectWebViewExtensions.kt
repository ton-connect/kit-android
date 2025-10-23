package io.ton.walletkit.presentation.browser

import android.webkit.WebView
import io.ton.walletkit.presentation.TONWalletKit

/**
 * Extension functions for adding TonConnect support to any WebView.
 *
 * These extensions make it easy to integrate TonConnect into existing WebView-based applications.
 */

/**
 * Inject TonConnect bridge into this WebView.
 *
 * This method adds TonConnect support to the WebView, enabling dApps to connect
 * with the wallet and request transactions/signatures. All requests are automatically
 * routed to the provided TONWalletKit instance which handles them internally.
 *
 * **Automatic Cleanup**: Resources are automatically cleaned up when the WebView is
 * detached from the window hierarchy. No manual cleanup is required.
 *
 * Example usage:
 * ```kotlin
 * val webView = WebView(context)
 * webView.settings.javaScriptEnabled = true
 *
 * // Inject TonConnect - that's it!
 * webView.injectTonConnect(TONWalletKit)
 *
 * // Load your dApp
 * webView.loadUrl("https://demo.tonconnect.dev")
 *
 * // Cleanup happens automatically when WebView is destroyed
 * ```
 *
 * @param walletKit TONWalletKit instance to route requests to
 */
fun WebView.injectTonConnect(
    walletKit: TONWalletKit,
) {
    val injector = TonConnectInjector(
        webView = this,
        walletKit = walletKit,
    )
    injector.setup()
}
