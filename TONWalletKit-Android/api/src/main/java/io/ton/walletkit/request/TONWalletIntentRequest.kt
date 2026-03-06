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

import io.ton.walletkit.api.generated.TONIntentRequestEvent
import io.ton.walletkit.api.generated.TONIntentResponseResult

/**
 * Represents an intent request from a deep link.
 *
 * Handle this request by calling [approve] with the wallet ID
 * or [reject] to deny it.
 *
 * The intent type can be inspected via [event] to display appropriate UI.
 *
 * @property event The underlying intent event with type-specific data
 */
class TONWalletIntentRequest(
    val event: TONIntentRequestEvent,
    private val handler: RequestHandler,
) {
    /**
     * Approve this intent request.
     *
     * Dispatches to the correct approval method based on the event type.
     *
     * @param walletId The wallet ID to approve with
     * @return The intent response result, or null for Action/Connect events
     * @throws io.ton.walletkit.WalletKitBridgeException if approval fails
     */
    suspend fun approve(walletId: String): TONIntentResponseResult? {
        return when (event) {
            is TONIntentRequestEvent.Transaction -> {
                val response = handler.approveTransactionIntent(event.value, walletId)
                TONIntentResponseResult.Transaction(response)
            }
            is TONIntentRequestEvent.SignData -> {
                val response = handler.approveSignDataIntent(event.value, walletId)
                TONIntentResponseResult.SignData(response)
            }
            is TONIntentRequestEvent.Action -> {
                handler.approveActionIntent(event.value, walletId)
                null
            }
            is TONIntentRequestEvent.Connect -> null
        }
    }

    /**
     * Reject this intent request.
     *
     * @param reason Optional reason for rejection
     * @param errorCode Optional error code
     * @throws io.ton.walletkit.WalletKitBridgeException if rejection fails
     */
    suspend fun reject(reason: String? = null, errorCode: Int? = null) {
        handler.rejectIntent(event, reason, errorCode)
    }
}
