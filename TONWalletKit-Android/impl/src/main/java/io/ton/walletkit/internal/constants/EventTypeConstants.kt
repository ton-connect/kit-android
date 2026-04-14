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

/** Internal bridge event type names. */
internal object EventTypeConstants {
    const val EVENT_CONNECT_REQUEST = "connectRequest"
    const val EVENT_TRANSACTION_REQUEST = "transactionRequest"
    const val EVENT_SIGN_DATA_REQUEST = "signDataRequest"
    const val EVENT_DISCONNECT = "disconnect"
    const val EVENT_STATE_CHANGED = "stateChanged"
    const val EVENT_WALLET_STATE_CHANGED = "walletStateChanged"
    const val EVENT_SESSIONS_CHANGED = "sessionsChanged"
    const val EVENT_BROWSER_PAGE_STARTED = "browserPageStarted"
    const val EVENT_BROWSER_PAGE_FINISHED = "browserPageFinished"
    const val EVENT_BROWSER_ERROR = "browserError"
    const val EVENT_BROWSER_BRIDGE_REQUEST = "browserBridgeRequest"

    const val EVENT_STREAMING_UPDATE = "streamingUpdate"
    const val EVENT_STREAMING_CONNECTION_CHANGE = "streamingConnectionChange"
    const val EVENT_STREAMING_BALANCE_UPDATE = "streamingBalanceUpdate"
    const val EVENT_STREAMING_TRANSACTIONS_UPDATE = "streamingTransactionsUpdate"
    const val EVENT_STREAMING_JETTONS_UPDATE = "streamingJettonsUpdate"
    const val EVENT_REQUEST_ERROR = "requestError"
    const val EVENT_TYPE_UNKNOWN = "unknown"
}
