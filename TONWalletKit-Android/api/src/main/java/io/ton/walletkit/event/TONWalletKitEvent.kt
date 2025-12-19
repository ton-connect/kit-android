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

import io.ton.walletkit.api.generated.TONDisconnectionEvent
import io.ton.walletkit.request.TONWalletConnectionRequest
import io.ton.walletkit.request.TONWalletSignDataRequest
import io.ton.walletkit.request.TONWalletTransactionRequest

/**
 * Events from TON Wallet Kit using a type-safe sealed hierarchy.
 *
 * Mirrors iOS TONWalletKitEvent for cross-platform consistency.
 *
 * Use this with exhaustive when() expressions to handle all possible events.
 */
sealed class TONWalletKitEvent {
    /**
     * A dApp is requesting to connect to a wallet.
     *
     * Handle by calling [TONWalletConnectionRequest.approve] with wallet info
     * or [TONWalletConnectionRequest.reject] to deny.
     *
     * @property request Connection request with approve/reject methods
     */
    data class ConnectRequest(
        val request: TONWalletConnectionRequest,
    ) : TONWalletKitEvent()

    /**
     * A dApp is requesting to execute a transaction.
     *
     * Handle by calling [TONWalletTransactionRequest.approve] to execute
     * or [TONWalletTransactionRequest.reject] to deny.
     *
     * @property request Transaction request with approve/reject methods
     */
    data class TransactionRequest(
        val request: TONWalletTransactionRequest,
    ) : TONWalletKitEvent()

    /**
     * A dApp is requesting to sign arbitrary data.
     *
     * Handle by calling [TONWalletSignDataRequest.approve] to sign
     * or [TONWalletSignDataRequest.reject] to deny.
     *
     * @property request Sign data request with approve/reject methods
     */
    data class SignDataRequest(
        val request: TONWalletSignDataRequest,
    ) : TONWalletKitEvent()

    /**
     * A session has been disconnected.
     *
     * This is informational - no action required.
     *
     * @property event Disconnect event details (generated type)
     */
    data class Disconnect(
        val event: TONDisconnectionEvent,
    ) : TONWalletKitEvent()
}
