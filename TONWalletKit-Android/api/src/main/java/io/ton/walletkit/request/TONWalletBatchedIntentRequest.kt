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

import io.ton.walletkit.api.generated.TONBatchedIntentEvent
import io.ton.walletkit.api.generated.TONIntentResponseResult

/**
 * Represents a batched intent request containing multiple intent items
 * that should be processed as a group.
 *
 * Handle this request by calling [approve] with the wallet ID
 * to process all intents, or [reject] to deny the entire batch.
 *
 * @property event The underlying batched intent event with all details
 */
class TONWalletBatchedIntentRequest(
    val event: TONBatchedIntentEvent,
    private val handler: RequestHandler,
) {
    /**
     * Approve this batched intent request.
     *
     * @param walletId The wallet ID to approve with
     * @param response Optional pre-computed response. If provided, the SDK will use this
     *                 directly instead of signing internally.
     * @return The intent response result
     * @throws io.ton.walletkit.WalletKitBridgeException if approval fails
     */
    suspend fun approve(walletId: String, response: TONIntentResponseResult? = null): TONIntentResponseResult {
        return handler.approveBatchedIntent(event, walletId, response)
    }

    /**
     * Reject this batched intent request.
     *
     * @param reason Optional reason for rejection
     * @param errorCode Optional error code
     * @throws io.ton.walletkit.WalletKitBridgeException if rejection fails
     */
    suspend fun reject(reason: String? = null, errorCode: Int? = null) {
        handler.rejectBatchedIntent(event, reason, errorCode)
    }
}
