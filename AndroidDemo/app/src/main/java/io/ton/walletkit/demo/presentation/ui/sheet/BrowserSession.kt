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

import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.ton.walletkit.demo.core.TONWalletKitHelper
import io.ton.walletkit.demo.presentation.model.BrowserPageState
import io.ton.walletkit.demo.presentation.model.BrowserTab
import io.ton.walletkit.extensions.cleanupTonConnect
import io.ton.walletkit.session.TONConnectSession
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.UUID
import kotlin.coroutines.resume

/**
 * Holds the live state for a browser session (injected or plain).
 * Lives at WalletScreen level so tabs survive Connect/Transaction overlay sheets.
 * Each session has independent tabs, WebViews, and page state.
 *
 * On API 35+ cookie isolation is handled via WebView Profiles (see BrowserSheet).
 * On API 21–34 this falls back to a single active shared profile:
 * cookies and JS-bridge sessions are swapped, WebViews are recreated on close,
 * and shared DOM storage is cleared when switching between injected/plain modes.
 */
class BrowserSession(val injectTonConnect: Boolean) {
    val tabs = mutableStateListOf<BrowserTab>()
    var activeTabId by mutableStateOf<String?>(null)
    val pageStates = mutableStateMapOf<String, BrowserPageState>()

    /** WebViews keyed by tab ID. Not observable — managed explicitly. */
    val webViews = mutableMapOf<String, WebView>()

    /** Persisted cookie state for pre-API-35 isolation. Maps URL → raw cookie header. */
    private val savedCookies = mutableMapOf<String, String>()
    private val savedJsBridgeSessions = mutableListOf<TONConnectSession>()
    private val sessionKey = if (injectTonConnect) "injected" else "plain"

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
        webViews.remove(tabId)?.let { wv ->
            wv.cleanupTonConnect()
            wv.destroy()
        }
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

    /**
     * Prepare this session to become the active pre-API-35 browser mode.
     *
     * This swaps the demo JS-bridge session store and clears shared DOM storage when
     * moving between injected/plain modes before any new WebViews are created.
     */
    suspend fun activatePreApi35Isolation() {
        restoreJsBridgeSessions()
        if (activePreApi35SessionKey != sessionKey) {
            WebStorage.getInstance().deleteAllData()
        }
        restoreCookies()
        activePreApi35SessionKey = sessionKey
    }

    /**
     * Snapshot this session's visible state before the browser sheet closes.
     *
     * WebViews must be destroyed here; otherwise the hidden sheet keeps the same JS heap,
     * DOM, and TonConnect bridge alive and the two browser modes continue to leak state.
     */
    fun deactivatePreApi35Isolation() {
        saveCookies()
        saveJsBridgeSessions()
        destroyAllWebViews()
    }

    private fun saveCookies() {
        val cm = CookieManager.getInstance()
        val urls = buildSet<String> {
            tabs.forEach { add(it.url) }
            pageStates.values.mapNotNull { it.currentUrl }.forEach { add(it) }
        }
        savedCookies.clear()
        for (url in urls) {
            val cookies = cm.getCookie(url)
            if (!cookies.isNullOrEmpty()) savedCookies[url] = cookies
        }
    }

    /**
     * Clear the global CookieManager and restore this session's snapshot.
     */
    private suspend fun restoreCookies() {
        suspendCancellableCoroutine { continuation ->
            val cm = CookieManager.getInstance()
            cm.removeAllCookies {
                for ((url, cookieStr) in savedCookies) {
                    cookieStr.split(";").map { it.trim() }.filter { it.isNotEmpty() }.forEach { single ->
                        cm.setCookie(url, single)
                    }
                }
                cm.flush()
                if (continuation.isActive) {
                    continuation.resume(Unit)
                }
            }
        }
    }

    private fun saveJsBridgeSessions() {
        val manager = TONWalletKitHelper.sessionManager ?: return
        savedJsBridgeSessions.clear()
        savedJsBridgeSessions.addAll(manager.snapshotJsBridgeSessions())
    }

    private fun restoreJsBridgeSessions() {
        TONWalletKitHelper.sessionManager?.replaceJsBridgeSessions(savedJsBridgeSessions)
    }

    companion object {
        private var activePreApi35SessionKey: String? = null
    }
}
