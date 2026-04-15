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
package io.ton.walletkit.demo.presentation.ui.sheet

import android.webkit.WebView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.ton.walletkit.demo.presentation.model.BrowserPageState
import io.ton.walletkit.demo.presentation.model.BrowserTab
import io.ton.walletkit.extensions.cleanupTonConnect
import java.util.UUID

/**
 * Holds the live state for a browser session (injected or plain).
 * Lives at WalletScreen level so tabs survive Connect/Transaction overlay sheets.
 * Each session has independent tabs, WebViews, and page state.
 *
 * Note: WebView instances isolate the JS heap per tab. Cookie and localStorage
 * isolation beyond that requires Android API 35+ WebView profiles.
 */
class BrowserSession(val injectTonConnect: Boolean) {
    val tabs = mutableStateListOf<BrowserTab>()
    var activeTabId by mutableStateOf<String?>(null)
    val pageStates = mutableStateMapOf<String, BrowserPageState>()

    /** WebViews keyed by tab ID. Not observable — managed explicitly. */
    val webViews = mutableMapOf<String, WebView>()

    fun openTab(url: String) {
        val tab = BrowserTab(id = UUID.randomUUID().toString(), url = url)
        tabs.add(tab)
        pageStates[tab.id] = BrowserPageState(currentUrl = url)
        activeTabId = tab.id
    }

    fun closeTab(tabId: String, onEmpty: () -> Unit) {
        val idx = tabs.indexOfFirst { it.id == tabId }
        tabs.removeAll { it.id == tabId }
        pageStates.remove(tabId)
        if (activeTabId == tabId) {
            activeTabId = tabs.getOrNull(minOf(idx, tabs.size - 1))?.id
        }
        if (tabs.isEmpty()) onEmpty()
    }

    fun destroyAllWebViews() {
        webViews.values.forEach { wv ->
            wv.cleanupTonConnect()
            wv.destroy()
        }
        webViews.clear()
    }
}
