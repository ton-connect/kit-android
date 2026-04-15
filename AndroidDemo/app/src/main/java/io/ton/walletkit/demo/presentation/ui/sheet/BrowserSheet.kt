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

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.ton.walletkit.ITONWalletKit
import io.ton.walletkit.demo.presentation.model.BrowserPageState
import io.ton.walletkit.extensions.cleanupTonConnect
import io.ton.walletkit.extensions.injectTonConnect

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserSheet(
    session: BrowserSession,
    onClose: () -> Unit,
    walletKit: ITONWalletKit,
) {
    val context = LocalContext.current
    var showAddTabDialog by remember { mutableStateOf(false) }

    if (showAddTabDialog) {
        AddTabDialog(
            onConfirm = { url ->
                session.openTab(url)
                showAddTabDialog = false
            },
            onDismiss = { showAddTabDialog = false },
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                val suffix = if (session.injectTonConnect) " · Injected" else " · Plain"
                Text("Browser$suffix", style = MaterialTheme.typography.titleMedium)
            },
            navigationIcon = {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            },
        )

        BrowserTabBar(
            tabs = session.tabs,
            activeTabId = session.activeTabId,
            pageStates = session.pageStates,
            onTabSelect = { session.activeTabId = it },
            onTabClose = { session.closeTab(it) { onClose() } },
            onAddTab = { showAddTabDialog = true },
        )

        val activeState = session.pageStates[session.activeTabId]
        Surface(modifier = Modifier.fillMaxWidth(), tonalElevation = 2.dp) {
            Text(
                text = activeState?.currentUrl ?: "",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (activeState?.isLoading == true) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            session.tabs.forEach { tab ->
                key(tab.id) {
                    val isActive = tab.id == session.activeTabId

                    DisposableEffect(tab.id) {
                        onDispose {
                            session.webViews.remove(tab.id)?.let { wv ->
                                wv.cleanupTonConnect()
                                wv.destroy()
                            }
                        }
                    }

                    AndroidView(
                        factory = {
                            session.webViews.getOrPut(tab.id) {
                                createTabWebView(
                                    context = context,
                                    url = tab.url,
                                    injectTonConnect = session.injectTonConnect,
                                    walletKit = walletKit,
                                ) { url, title, loading ->
                                    val cur = session.pageStates[tab.id] ?: BrowserPageState()
                                    session.pageStates[tab.id] = cur.copy(
                                        currentUrl = url ?: cur.currentUrl,
                                        title = title ?: cur.title,
                                        isLoading = loading,
                                    )
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                        update = { view ->
                            view.visibility = if (isActive) View.VISIBLE else View.GONE
                            view.setOnTouchListener { v, _ ->
                                v.parent?.requestDisallowInterceptTouchEvent(true)
                                false
                            }
                        },
                    )
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun createTabWebView(
    context: android.content.Context,
    url: String,
    injectTonConnect: Boolean,
    walletKit: ITONWalletKit,
    onUpdate: (url: String?, title: String?, isLoading: Boolean) -> Unit,
): WebView = WebView(context).apply {
    settings.javaScriptEnabled = true
    settings.domStorageEnabled = true
    layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT,
    )
    isNestedScrollingEnabled = false
    webViewClient = object : WebViewClient() {
        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
            onUpdate(url, null, true)
        }
        override fun onPageFinished(view: WebView, url: String?) {
            onUpdate(url, null, false)
        }
    }
    webChromeClient = object : WebChromeClient() {
        override fun onReceivedTitle(view: WebView, title: String?) {
            onUpdate(null, title, false)
        }
    }
    if (injectTonConnect) injectTonConnect(walletKit)
    loadUrl(url)
}

@Composable
private fun AddTabDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var url by remember { mutableStateOf("https://") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Tab") },
        text = {
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("URL") },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { if (url.isNotBlank()) onConfirm(url) }) { Text("Open") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
