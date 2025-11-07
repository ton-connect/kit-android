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

import io.ton.walletkit.event.TransactionRequestEvent
import io.ton.walletkit.model.DAppInfo

/**
 * Represents a transaction request from a dApp.
 *
 * Aligns with the shared TON Wallet Kit API contract for cross-platform consistency.
 *
 * Handle this request by calling [approve] to execute the transaction
 * or [reject] to deny it.
 *
 * @property dAppInfo Information about the requesting dApp
 * @property messages List of messages in this transaction
 * @property validUntil Unix timestamp when this transaction expires (optional)
 * @property network Network to use for this transaction (optional)
 */
class TONWalletTransactionRequest(
    val dAppInfo: DAppInfo?,
    private val event: TransactionRequestEvent,
    private val handler: RequestHandler,
) {
    /**
     * List of messages to be sent in this transaction
     */
    val messages: List<TransactionMessage>
        get() = event.request?.messages?.map { msg ->
            TransactionMessage(
                address = msg.address ?: "",
                amount = msg.amount ?: "0",
                payload = msg.payload,
                stateInit = msg.stateInit,
            )
        } ?: emptyList()

    /**
     * Unix timestamp when this transaction request expires (optional)
     */
    val validUntil: Long?
        get() = event.request?.validUntil

    /**
     * Network identifier for this transaction (optional)
     */
    val network: String?
        get() = event.request?.network

    /**
     * Approve this transaction request.
     *
     * @throws io.ton.walletkit.WalletKitBridgeException if approval fails
     */
    suspend fun approve() {
        handler.approveTransaction(event)
    }

    /**
     * Reject this transaction request.
     *
     * @param reason Optional reason for rejection
     * @throws io.ton.walletkit.WalletKitBridgeException if rejection fails
     */
    suspend fun reject(reason: String? = null) {
        handler.rejectTransaction(event, reason)
    }
}

/**
 * Represents a single message in a transaction request.
 *
 * @property address Destination address for this message
 * @property amount Amount in nanotons (as string to preserve precision)
 * @property payload Optional message payload (base64 encoded BOC)
 * @property stateInit Optional state init (base64 encoded BOC)
 */
data class TransactionMessage(
    val address: String,
    val amount: String,
    val payload: String? = null,
    val stateInit: String? = null,
)
