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

import io.ton.walletkit.api.generated.TONConnectionApprovalResponse
import io.ton.walletkit.api.generated.TONConnectionRequestEvent
import io.ton.walletkit.api.generated.TONNetwork
import io.ton.walletkit.api.generated.TONSendTransactionApprovalResponse
import io.ton.walletkit.api.generated.TONSendTransactionRequestEvent
import io.ton.walletkit.api.generated.TONSignDataApprovalResponse
import io.ton.walletkit.api.generated.TONSignDataRequestEvent

/**
 * Internal interface for handling request approvals/rejections.
 * Implementation provided by the bridge module.
 * @suppress
 */
interface RequestHandler {
    /**
     * Approve a connection request. The event should have walletId and walletAddress set.
     * @param event The connection request event
     * @param response Optional pre-computed approval response. If provided, the SDK will use this
     *                 response directly instead of computing it internally.
     */
    suspend fun approveConnect(
        event: TONConnectionRequestEvent,
        response: TONConnectionApprovalResponse? = null,
    )

    suspend fun rejectConnect(event: TONConnectionRequestEvent, reason: String?, errorCode: Int?)

    /**
     * Approve a transaction request.
     * @param event The transaction request event
     * @param network The network to execute the transaction on
     * @param response Optional pre-computed approval response. If provided, the SDK will use this
     *                 response directly instead of signing the transaction internally.
     */
    suspend fun approveTransaction(
        event: TONSendTransactionRequestEvent,
        network: TONNetwork,
        response: TONSendTransactionApprovalResponse? = null,
    )

    suspend fun rejectTransaction(event: TONSendTransactionRequestEvent, reason: String?, errorCode: Int?)

    /**
     * Approve a sign data request.
     * @param event The sign data request event
     * @param network The network to sign on
     * @param response Optional pre-computed approval response. If provided, the SDK will use this
     *                 response directly instead of signing the data internally.
     */
    suspend fun approveSignData(
        event: TONSignDataRequestEvent,
        network: TONNetwork,
        response: TONSignDataApprovalResponse? = null,
    )

    suspend fun rejectSignData(event: TONSignDataRequestEvent, reason: String?, errorCode: Int?)
}
