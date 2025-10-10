package io.ton.walletkit.presentation

import android.content.Context

/**
 * Factory for creating WalletKitEngine instances without directly referencing implementation classes.
 * This allows demo apps and partners to work with any SDK variant (webview-only or full)
 * without compile-time dependencies on unavailable engine implementations.
 */
object WalletKitEngineFactory {
    /**
     * Create a WalletKitEngine instance for the specified kind.
     * 
     * @param context Android application context
     * @param kind The engine kind to create (WEBVIEW or QUICKJS)
     * @return WalletKitEngine instance
     * @throws IllegalStateException if the requested engine is not available in this SDK variant
     * 
     * @sample
     * ```kotlin
     * // Works with both webview-only and full SDK variants
     * val engine = WalletKitEngineFactory.create(context, WalletKitEngineKind.WEBVIEW)
     * ```
     */
    @Suppress("DEPRECATION") // QuickJS still supported in full variant
    fun create(context: Context, kind: WalletKitEngineKind = WalletKitEngineKind.WEBVIEW): WalletKitEngine {
        return when (kind) {
            WalletKitEngineKind.WEBVIEW -> {
                // WebViewWalletKitEngine is always available in all variants
                createWebViewEngine(context)
            }
            WalletKitEngineKind.QUICKJS -> {
                // QuickJsWalletKitEngine is only available in full variant
                createQuickJsEngine(context)
            }
        }
    }
    
    /**
     * Check if a specific engine kind is available in the current SDK variant.
     * 
     * @param kind The engine kind to check
     * @return true if the engine is available, false otherwise
     */
    fun isAvailable(kind: WalletKitEngineKind): Boolean {
        return when (kind) {
            WalletKitEngineKind.WEBVIEW -> true // Always available
            WalletKitEngineKind.QUICKJS -> {
                try {
                    // Check if QuickJS class exists (only in full variant)
                    Class.forName("io.ton.walletkit.presentation.impl.QuickJsWalletKitEngine")
                    true
                } catch (e: ClassNotFoundException) {
                    false
                }
            }
        }
    }
    
    private fun createWebViewEngine(context: Context): WalletKitEngine {
        // Direct instantiation - WebViewWalletKitEngine is in the same module
        return io.ton.walletkit.presentation.impl.WebViewWalletKitEngine(context)
    }
    
    private fun createQuickJsEngine(context: Context): WalletKitEngine {
        try {
            // Use reflection only for QuickJS to avoid compile-time dependency in webview variant
            val clazz = Class.forName("io.ton.walletkit.presentation.impl.QuickJsWalletKitEngine")
            val constructor = clazz.getConstructor(Context::class.java)
            return constructor.newInstance(context) as WalletKitEngine
        } catch (e: ClassNotFoundException) {
            throw IllegalStateException(
                "QuickJS engine is not available in this SDK variant. " +
                "Use the 'full' variant AAR to access QuickJS, or use WalletKitEngineKind.WEBVIEW instead.",
                e
            )
        }
    }
}
