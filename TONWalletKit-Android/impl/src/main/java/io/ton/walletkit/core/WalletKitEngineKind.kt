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
package io.ton.walletkit.core

/**
 * Identifies which JavaScript runtime engine implementation is being used by the SDK.
 *
 * Internal implementation detail. SDK always uses WebView engine.
 *
 * @suppress
 */
internal enum class WalletKitEngineKind {
    /**
     * WebView-based JavaScript engine (recommended for all use cases).
     *
     * This engine uses Android's WebView component to execute the WalletKit
     * JavaScript bundle. It provides:
     * - 2x faster performance compared to QuickJS
     * - Active maintenance and updates
     * - Production-ready stability
     * - Available in all SDK variants (webview-only and full)
     */
    WEBVIEW,

    /**
     * QuickJS-based JavaScript engine (deprecated).
     *
     * This engine uses a native QuickJS runtime compiled for Android.
     * **Note:** This option is deprecated and should not be used for new projects.
     *
     * Limitations:
     * - 2x slower than WebView
     * - No longer actively maintained
     * - Only available in the 'full' SDK variant
     * - Larger AAR size due to native libraries
     *
     * @deprecated QuickJS is deprecated due to performance and maintenance concerns.
     *     Use [WEBVIEW] instead. See QUICKJS_DEPRECATION.md for migration guide.
     */
    @Deprecated(
        message = "QuickJS is deprecated. Use WEBVIEW instead for 2x better performance.",
        replaceWith = ReplaceWith("WalletKitEngineKind.WEBVIEW"),
        level = DeprecationLevel.WARNING,
    )
    QUICKJS,
}
