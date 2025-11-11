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
package io.ton.walletkit.browser

import android.content.Context
import android.webkit.WebView
import io.ton.walletkit.bridge.BuildConfig
import io.ton.walletkit.internal.constants.BrowserConstants
import io.ton.walletkit.internal.util.Logger

/**
 * Handles injection of the TonConnect bridge script into WebView frames.
 */
internal object BridgeInjector {
    private const val TAG = "BridgeInjector"

    /**
     * Inject the TonConnect bridge into the current frame.
     *
     * NOTE: Android WebView automatically makes JavaScript accessible to ALL frames
     * (main frame + all iframes) when using addJavascriptInterface() and evaluateJavascript().
     * We don't need to manually inject into each iframe - WebView handles this automatically!
     *
     * The script will be re-injected on every page navigation via WebViewClient.onPageFinished.
     */
    fun injectIntoAllFrames(
        context: Context,
        webView: WebView?,
        onError: (String) -> Unit,
    ) {
        webView ?: return

        // Load inject.mjs from assets
        val injectScript = try {
            context.assets.open(BrowserConstants.INJECT_SCRIPT_PATH)
                .bufferedReader()
                .use { it.readText() }
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to load inject.mjs", e)
            onError("Failed to load bridge script: ${e.message}")
            return
        }

        // Set debug flag before injecting - enables console logs only in debug builds
        val debugFlag = if (BuildConfig.ENABLE_LOGGING) "true" else "false"
        webView.evaluateJavascript("window.__TONCONNECT_DEBUG__ = $debugFlag;", null)

        // Inject once - WebView automatically makes this available to all frames!
        // The script includes a guard to prevent double-injection.
        webView.evaluateJavascript(
            """
            (function() {
                try {
                    $injectScript
                } catch (e) {
                    console.error('${BrowserConstants.CONSOLE_PREFIX_NATIVE} ${BrowserConstants.CONSOLE_MSG_FAILED_TO_INJECT}:', e);
                }
            })();
            """.trimIndent(),
            null,
        )

        Logger.d(TAG, "Bridge injected - automatically available to all frames (main + iframes)")
    }
}
