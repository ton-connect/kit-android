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
package io.ton.walletkit.core.streaming

import io.ton.walletkit.api.generated.TONBalanceUpdate
import io.ton.walletkit.api.generated.TONJettonUpdate
import io.ton.walletkit.api.generated.TONStreamingUpdate
import io.ton.walletkit.api.generated.TONTransactionsUpdate

/**
 * Internal streaming events dispatched through the dedicated streaming channel,
 * separate from the public [io.ton.walletkit.event.TONWalletKitEvent] hierarchy.
 *
 * @suppress Internal component used by [TONStreamingManager].
 */
internal sealed class StreamingEvent {
    abstract val subscriptionId: String

    /** Generic multi-type update (from [streamingWatch]). */
    data class Update(
        override val subscriptionId: String,
        val update: TONStreamingUpdate,
    ) : StreamingEvent()

    data class ConnectionChange(
        override val subscriptionId: String,
        val connected: Boolean,
    ) : StreamingEvent()

    data class BalanceUpdate(
        override val subscriptionId: String,
        val update: TONBalanceUpdate,
    ) : StreamingEvent()

    data class TransactionsUpdate(
        override val subscriptionId: String,
        val update: TONTransactionsUpdate,
    ) : StreamingEvent()

    data class JettonsUpdate(
        override val subscriptionId: String,
        val update: TONJettonUpdate,
    ) : StreamingEvent()
}
