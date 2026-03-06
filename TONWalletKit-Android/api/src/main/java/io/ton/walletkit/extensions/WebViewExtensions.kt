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
package io.ton.walletkit.extensions

import android.webkit.WebView
import io.ton.walletkit.ITONWalletKit
import io.ton.walletkit.WebViewTonConnectInjector

/**
 * Extension functions for adding TonConnect support to WebViews.
 */

private const val TAG_TON_CONNECT_INJECTOR = "tonconnect_injector"

fun WebView.injectTonConnect(walletKit: ITONWalletKit, walletId: String? = null) {
    val existingInjector = getTag(TAG_TON_CONNECT_INJECTOR.hashCode()) as? WebViewTonConnectInjector
    if (existingInjector != null) return

    val injector = walletKit.createWebViewInjector(this, walletId)
    injector.setup()
    setTag(TAG_TON_CONNECT_INJECTOR.hashCode(), injector)
}

fun WebView.cleanupTonConnect() {
    val injector = getTag(TAG_TON_CONNECT_INJECTOR.hashCode()) as? WebViewTonConnectInjector
    injector?.cleanup()
    setTag(TAG_TON_CONNECT_INJECTOR.hashCode(), null)
}
