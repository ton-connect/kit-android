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
 * Constants for bridge event type strings.
 *
 * These constants define the event types that can be received from the JavaScript
 * bridge layer. They are used for event routing and handling in the presentation layer.
 *
 * @suppress Internal implementation constants. Not part of public API.
 */
internal object EventTypeConstants {
    /**
     * Event type for TON Connect connection requests.
     */
    const val EVENT_CONNECT_REQUEST = "connectRequest"

    /**
     * Event type for transaction approval requests.
     */
    const val EVENT_TRANSACTION_REQUEST = "transactionRequest"

    /**
     * Event type for sign data requests (text, binary, cell).
     */
    const val EVENT_SIGN_DATA_REQUEST = "signDataRequest"

    /**
     * Event type for session disconnection.
     */
    const val EVENT_DISCONNECT = "disconnect"

    /**
     * Event type for wallet state changes.
     */
    const val EVENT_STATE_CHANGED = "stateChanged"

    /**
     * Alternative event type for wallet state changes.
     */
    const val EVENT_WALLET_STATE_CHANGED = "walletStateChanged"

    /**
     * Event type for sessions list changes.
     */
    const val EVENT_SESSIONS_CHANGED = "sessionsChanged"

    /**
     * Event type for browser page started loading.
     */
    const val EVENT_BROWSER_PAGE_STARTED = "browserPageStarted"

    /**
     * Event type for browser page finished loading.
     */
    const val EVENT_BROWSER_PAGE_FINISHED = "browserPageFinished"

    /**
     * Event type for browser errors.
     */
    const val EVENT_BROWSER_ERROR = "browserError"

    /**
     * Event type for browser bridge requests.
     */
    const val EVENT_BROWSER_BRIDGE_REQUEST = "browserBridgeRequest"

    /**
     * Event type for external signer wallet sign requests.
     */
    const val EVENT_SIGNER_SIGN_REQUEST = "signerSignRequest"

    /**
     * Default value for unknown event types.
     */
    const val EVENT_TYPE_UNKNOWN = "unknown"
}
