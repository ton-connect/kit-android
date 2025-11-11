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
 * Constants for WebView configuration and JavaScript interface.
 *
 * These constants define the WebView setup, asset paths, and JavaScript
 * interface names used for communication between Kotlin and JS.
 *
 * @suppress Internal implementation constants. Not part of public API.
 */
internal object WebViewConstants {
    /**
     * Name of the JavaScript interface exposed to WebView.
     *
     * JavaScript code can call methods on this interface via:
     * `window.WalletKitNative.methodName()`
     */
    const val JS_INTERFACE_NAME = "WalletKitNative"

    /**
     * Default asset path for the WebView HTML entry point.
     */
    const val DEFAULT_ASSET_PATH = "walletkit/index.html"

    /**
     * Default asset directory for QuickJS engine.
     */
    const val DEFAULT_QUICKJS_ASSET_DIR = "walletkit"

    /**
     * Asset loader domain for WebView.
     *
     * Used to create secure URLs like: https://appassets.androidplatform.net/assets/
     */
    const val ASSET_LOADER_DOMAIN = "appassets.androidplatform.net"

    /**
     * Asset loader path prefix.
     */
    const val ASSET_LOADER_PATH = "/assets/"

    /**
     * Platform identifier for device info.
     */
    const val PLATFORM_ANDROID = "android"

    /**
     * Log tag for WebView engine.
     */
    const val LOG_TAG_WEBVIEW = "WebViewWalletKitEngine"

    /**
     * Error message for bundle loading failure.
     */
    const val ERROR_BUNDLE_LOAD_FAILED = "Failed to load WalletKit bundle"

    /**
     * Error message prefix for WebView load failures.
     */
    const val ERROR_WEBVIEW_LOAD_PREFIX = "WebView load failed: "

    /**
     * Instruction message shown when bundle fails to load.
     */
    const val BUILD_INSTRUCTION = "Run `pnpm -w --filter androidkit build` and recompile."

    /**
     * JavaScript function call template for WalletKit bridge.
     */
    const val JS_FUNCTION_WALLETKIT_CALL = "window.__walletkitCall"

    /**
     * JavaScript function name for base64 decoding.
     */
    const val JS_FUNCTION_ATOB = "atob"

    /**
     * Null literal for JavaScript.
     */
    const val JS_NULL = "null"

    /**
     * URL prefix for loading assets.
     */
    const val URL_PREFIX_HTTPS = "https://"

    /**
     * JavaScript snippet that checks whether the WalletKit bridge call function is installed.
     */
    const val JS_BRIDGE_READY_CHECK = "(typeof window.__walletkitCall === 'function')"

    /**
     * Serialized boolean true returned by evaluateJavascript.
     */
    const val JS_BOOLEAN_TRUE = "true"

    /**
     * Delay between bridge readiness polling attempts (in ms).
     */
    const val JS_BRIDGE_POLL_DELAY_MS = 32L
}
