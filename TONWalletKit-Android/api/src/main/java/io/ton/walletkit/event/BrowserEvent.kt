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
package io.ton.walletkit.event

/**
 * Events emitted by the internal browser.
 */
sealed class BrowserEvent {
    /**
     * Page started loading.
     */
    data class PageStarted(val url: String) : BrowserEvent()

    /**
     * Page finished loading.
     */
    data class PageFinished(val url: String) : BrowserEvent()

    /**
     * Error occurred.
     */
    data class Error(val message: String) : BrowserEvent()

    /**
     * Bridge request received from dApp (connect, send transaction, etc.).
     * The SDK automatically forwards this to WalletKit engine for processing.
     * This event is for UI updates (showing notifications, etc.).
     */
    data class BridgeRequest(
        val messageId: String,
        val method: String,
        val request: String,
    ) : BrowserEvent()
}
