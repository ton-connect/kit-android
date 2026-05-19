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
package io.ton.walletkit.bridge.transport

import android.net.Uri
import android.os.Handler
import android.webkit.WebView
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebMessagePortCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import io.ton.walletkit.WalletKitBridgeException
import io.ton.walletkit.internal.constants.LogConstants
import io.ton.walletkit.internal.util.Logger
import kotlinx.coroutines.CompletableDeferred
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicReference

internal class WebMessagePortBridgeTransport(
    private val webView: WebView,
    private val mainHandler: Handler,
    private val callbackHandler: Handler,
) : BridgeTransport {
    private val portRef = AtomicReference<WebMessagePortCompat?>(null)
    private val callbackRef = AtomicReference<((String) -> Unit)?>(null)
    private val pendingOutbound = ConcurrentLinkedQueue<String>()
    private val readyGate = CompletableDeferred<Unit>()

    override val isReady: Boolean
        get() = portRef.get() != null

    override suspend fun awaitReady() = readyGate.await()

    override fun setOnMessage(callback: (json: String) -> Unit) {
        callbackRef.set(callback)
    }

    override fun send(json: String) {
        val port = portRef.get()
        if (port != null) {
            post(port, json)
            return
        }
        pendingOutbound.add(json)
        // If portRef was set between our get and our add, handOffPortToJs's drain
        // may have already finished and our message would be stranded. Re-check
        // and drain ourselves so we don't lose it.
        portRef.get()?.let { drainPending(it) }
    }

    override fun fail(cause: Throwable) {
        if (!readyGate.isCompleted) readyGate.completeExceptionally(cause)
        portRef.getAndSet(null)?.close()
        pendingOutbound.clear()
    }

    override fun close() {
        portRef.getAndSet(null)?.close()
        pendingOutbound.clear()
    }

    /** Must be called on the main thread (WebView APIs are main-thread-only). */
    fun handOffPortToJs() {
        check(WebViewFeature.isFeatureSupported(WebViewFeature.CREATE_WEB_MESSAGE_CHANNEL)) {
            "WebView does not support CREATE_WEB_MESSAGE_CHANNEL — required for the WalletKit bridge."
        }
        check(WebViewFeature.isFeatureSupported(WebViewFeature.POST_WEB_MESSAGE)) {
            "WebView does not support POST_WEB_MESSAGE — required for the WalletKit bridge."
        }
        if (portRef.get() != null) {
            return
        }

        val ports = WebViewCompat.createWebMessageChannel(webView)
        check(ports.size >= 2) {
            "WebViewCompat.createWebMessageChannel returned ${ports.size} ports, expected 2."
        }
        val kotlinPort = ports[0]
        val jsPort = ports[1]

        kotlinPort.setWebMessageCallback(
            callbackHandler,
            object : WebMessagePortCompat.WebMessageCallbackCompat() {
                override fun onMessage(port: WebMessagePortCompat, message: WebMessageCompat?) {
                    val data = message?.data ?: return
                    val cb = callbackRef.get() ?: run {
                        Logger.w(TAG, "Bridge port message arrived before callback was installed")
                        return
                    }
                    cb(data)
                }
            },
        )

        WebViewCompat.postWebMessage(
            webView,
            WebMessageCompat(BRIDGE_HANDSHAKE_TAG, arrayOf(jsPort)),
            Uri.EMPTY,
        )

        portRef.set(kotlinPort)
        drainPending(kotlinPort)
        readyGate.complete(Unit)
    }

    private fun drainPending(port: WebMessagePortCompat) {
        while (true) {
            val next = pendingOutbound.poll() ?: return
            post(port, next)
        }
    }

    private fun post(port: WebMessagePortCompat, json: String) {
        try {
            mainHandler.post { port.postMessage(WebMessageCompat(json)) }
        } catch (e: Throwable) {
            throw WalletKitBridgeException("Failed to post bridge message: ${e.message}")
        }
    }

    private companion object {
        private const val TAG = LogConstants.TAG_WEBVIEW_ENGINE
        const val BRIDGE_HANDSHAKE_TAG = "__walletkit_bridge_init"
    }
}
