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

import io.ton.walletkit.api.generated.TONSignMessageApprovalResponse
import io.ton.walletkit.api.walletkit.TONSignMessageRequestEvent

/**
 * Represents a signMessage request from a dApp for gasless transactions.
 *
 * Mirrors iOS TONWalletSignMessageRequest for cross-platform consistency.
 *
 * This is similar to a transaction request, but instead of sending the transaction
 * to the network, it returns the signed internal BOC that can be sent to a
 * gasless provider for execution.
 *
 * Handle this request by calling [approve] to sign and return the internal BOC
 * or [reject] to deny it.
 *
 * @property event The underlying signMessage request event with all details
 */
class TONWalletSignMessageRequest(
    val event: TONSignMessageRequestEvent,
    private val handler: RequestHandler,
) {
    /**
     * Approve this signMessage request.
     *
     * This will sign the internal message and return the signed BOC.
     * The BOC can then be sent to a gasless provider for execution.
     *
     * @return The signed internal message BOC (Base64 encoded)
     * @throws io.ton.walletkit.WalletKitBridgeException if approval fails
     */
    suspend fun approve(): TONSignMessageApprovalResponse {
        return handler.approveSignMessage(event)
    }

    /**
     * Reject this signMessage request.
     *
     * @param reason Optional reason for rejection
     * @param errorCode Optional error code for the TON Connect protocol
     * @throws io.ton.walletkit.WalletKitBridgeException if rejection fails
     */
    suspend fun reject(reason: String? = null, errorCode: Int? = null) {
        handler.rejectSignMessage(event, reason, errorCode)
    }
}
