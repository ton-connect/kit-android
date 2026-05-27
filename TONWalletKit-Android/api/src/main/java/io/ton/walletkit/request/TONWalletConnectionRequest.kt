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

import io.ton.walletkit.ITONWallet
import io.ton.walletkit.api.generated.TONConnectionApprovalResponse
import io.ton.walletkit.api.generated.TONConnectionRequestEvent
import io.ton.walletkit.api.generated.TONEmbeddedRequestEvent
import io.ton.walletkit.event.TONWalletKitEvent

/**
 * A connection request from a dApp. Mirrors iOS `TONWalletConnectionRequest`.
 */
class TONWalletConnectionRequest(
    val event: TONConnectionRequestEvent,
    private val handler: RequestHandler,
) {
    /**
     * Approves the connection with [wallet]. Returns the wrapped follow-up event when the
     * dApp's request carried an `embeddedRequest` intent (sendTransaction / signMessage /
     * signData), so the host can immediately process it. Returns null for a plain connect.
     */
    suspend fun approve(
        wallet: ITONWallet,
        response: TONConnectionApprovalResponse? = null,
    ): TONWalletKitEvent? {
        val updatedEvent = event.copy(
            walletId = wallet.id,
            walletAddress = wallet.address,
        )
        val embedded = handler.approveConnect(updatedEvent, response) ?: return null
        return when (embedded) {
            is TONEmbeddedRequestEvent.SendTransaction ->
                TONWalletKitEvent.SendTransactionRequest(
                    TONWalletTransactionRequest(event = embedded.value, handler = handler),
                )
            is TONEmbeddedRequestEvent.SignMessage ->
                TONWalletKitEvent.SignMessageRequest(
                    TONWalletSignMessageRequest(event = embedded.value, handler = handler),
                )
            is TONEmbeddedRequestEvent.SignData ->
                TONWalletKitEvent.SignDataRequest(
                    TONWalletSignDataRequest(event = embedded.value, handler = handler),
                )
        }
    }

    suspend fun reject(reason: String? = null, errorCode: Int? = null) {
        handler.rejectConnect(event, reason, errorCode)
    }
}
