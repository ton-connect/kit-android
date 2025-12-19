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
package io.ton.walletkit.request

import io.ton.walletkit.api.generated.TONConnectionRequestEvent
import io.ton.walletkit.api.generated.TONNetwork

/**
 * Represents a connection request from a dApp.
 *
 * Mirrors iOS TONWalletConnectionRequest for cross-platform consistency.
 *
 * Handle this request by calling [approve] with a wallet
 * or [reject] to deny the connection.
 *
 * @property event The underlying connection request event with all details
 */
class TONWalletConnectionRequest(
    val event: TONConnectionRequestEvent,
    private val handler: RequestHandler,
) {
    /**
     * Approve this connection request.
     *
     * The wallet address is taken from event.walletAddress.
     *
     * @param network Network to connect on
     * @throws io.ton.walletkit.WalletKitBridgeException if approval fails
     */
    suspend fun approve(network: TONNetwork) {
        handler.approveConnect(event, network)
    }

    /**
     * Reject this connection request.
     *
     * @param reason Optional reason for rejection
     * @param errorCode Optional error code for the TON Connect protocol
     * @throws io.ton.walletkit.WalletKitBridgeException if rejection fails
     */
    suspend fun reject(reason: String? = null, errorCode: Int? = null) {
        handler.rejectConnect(event, reason, errorCode)
    }
}
