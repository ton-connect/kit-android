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

import io.ton.walletkit.api.generated.TONSignDataApprovalResponse
import io.ton.walletkit.api.generated.TONSignDataRequestEvent

/**
 * A data signing request from a dApp.
 *
 * When this request is the embedded follow-up of a connect-with-intent flow, [event] is the
 * embedded variant (a subclass of [TONSignDataRequestEvent]); the bridge picks up the
 * `connectionResult` field at serialization time so the JS side can finalise the session.
 */
class TONWalletSignDataRequest(
    val event: TONSignDataRequestEvent,
    private val handler: RequestHandler,
) {
    suspend fun approve(response: TONSignDataApprovalResponse? = null) {
        handler.approveSignData(event, response)
    }

    suspend fun reject(reason: String? = null, errorCode: Int? = null) {
        handler.rejectSignData(event, reason, errorCode)
    }
}
