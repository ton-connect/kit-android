/*
 * Copyright (c) 2025 TonTech
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.ton.walletkit.internal.constants

/**
 * Constants for reflection-based class loading.
 *
 * These constants define fully qualified class names used for reflective
 * instantiation of engine implementations, particularly for QuickJS support
 * in the full variant.
 *
 * @suppress Internal implementation constants. Not part of public API.
 */
internal object ReflectionConstants {
    /**
     * Fully qualified class name for QuickJsWalletKitEngine.
     *
     * Used for reflection-based loading in full variant AAR.
     */
    const val CLASS_QUICKJS_ENGINE = "io.ton.walletkit.presentation.impl.QuickJsWalletKitEngine"

    /**
     * Fully qualified class name for OkHttpClient.
     *
     * Used for reflection-based constructor parameter type matching.
     */
    const val CLASS_OKHTTP_CLIENT = "okhttp3.OkHttpClient"

    /**
     * Fully qualified class name for TONWalletKitConfiguration.
     *
     * Used for reflection-based constructor parameter type matching.
     */
    const val CLASS_TON_WALLET_KIT_CONFIGURATION = "io.ton.walletkit.config.TONWalletKitConfiguration"

    /**
     * Fully qualified class name for TONBridgeEventsHandler.
     *
     * Used for reflection-based constructor parameter type matching.
     */
    const val CLASS_TON_BRIDGE_EVENTS_HANDLER = "io.ton.walletkit.presentation.listener.TONBridgeEventsHandler"

    /**
     * Error message when QuickJS engine is not available.
     */
    const val ERROR_QUICKJS_NOT_AVAILABLE =
        "QuickJS engine is not available in this SDK variant. " +
            "Use the 'full' variant AAR to access QuickJS, or use WalletKitEngineKind.WEBVIEW instead."
}
