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

import io.ton.walletkit.event.ConnectRequestEvent
import io.ton.walletkit.model.DAppInfo

/**
 * Represents a connection request from a dApp.
 *
 * Aligns with the shared TON Wallet Kit API contract for cross-platform consistency.
 *
 * Handle this request by calling [approve] with a wallet address
 * or [reject] to deny the connection.
 *
 * @property dAppInfo Information about the requesting dApp
 * @property permissions List of requested permissions
 */
class TONWalletConnectionRequest(
    val dAppInfo: DAppInfo?,
    val permissions: List<ConnectRequestEvent.ConnectPermission>,
    private val event: ConnectRequestEvent,
    private val handler: RequestHandler,
) {
    /**
     * Approve this connection request with the specified wallet.
     *
     * @param walletAddress Address of the wallet to connect with
     * @throws io.ton.walletkit.WalletKitBridgeException if approval fails
     */
    suspend fun approve(walletAddress: String) {
        val eventWithWallet = event.copy(walletAddress = walletAddress)
        handler.approveConnect(eventWithWallet)
    }

    /**
     * Reject this connection request.
     *
     * @param reason Optional reason for rejection
     * @throws io.ton.walletkit.WalletKitBridgeException if rejection fails
     */
    suspend fun reject(reason: String? = null) {
        handler.rejectConnect(event, reason)
    }
}
