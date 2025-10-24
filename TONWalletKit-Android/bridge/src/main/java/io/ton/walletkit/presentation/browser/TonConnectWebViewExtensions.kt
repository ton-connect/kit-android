package io.ton.walletkit.presentation.browser

import android.webkit.WebView
import io.ton.walletkit.presentation.TONWalletKit
import io.ton.walletkit.presentation.browser.internal.TonConnectInjector

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
private const val TAG_TON_CONNECT_INJECTOR = "tonconnect_injector"

fun WebView.injectTonConnect(
    walletKit: TONWalletKit,
) {
    // Check if injector already exists - don't recreate if it's already set up
    val existingInjector = getTonConnectInjector()
    if (existingInjector != null) {
        // Injector already exists, no need to recreate
        return
    }
    
    val injector = TonConnectInjector(
        webView = this,
        walletKit = walletKit,
    )
    injector.setup()
    
    // Store injector in WebView tag for later access
    setTag(TAG_TON_CONNECT_INJECTOR.hashCode(), injector)
}

/**
 * Clean up TonConnect resources for this WebView.
 * Should be called when the WebView is no longer needed.
 */
fun WebView.cleanupTonConnect() {
    val injector = getTonConnectInjector()
    injector?.cleanup()
    setTag(TAG_TON_CONNECT_INJECTOR.hashCode(), null)
}

/**
 * Get the TonConnectInjector attached to this WebView, if any.
 *
 * @return The injector, or null if TonConnect has not been injected into this WebView
 */
internal fun WebView.getTonConnectInjector(): TonConnectInjector? {
    return getTag(TAG_TON_CONNECT_INJECTOR.hashCode()) as? TonConnectInjector
}
