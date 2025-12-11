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
import io.ton.walletkit.event.SignDataRequestEvent
import io.ton.walletkit.event.TransactionRequestEvent
import io.ton.walletkit.model.TONNetwork

/**
 * Internal interface for handling request approvals/rejections.
 * Implementation provided by the bridge module.
 * @suppress
 */
interface RequestHandler {
    suspend fun approveConnect(event: ConnectRequestEvent, network: TONNetwork)
    suspend fun rejectConnect(event: ConnectRequestEvent, reason: String?, errorCode: Int? = null)

    suspend fun approveTransaction(event: TransactionRequestEvent, network: TONNetwork)
    suspend fun rejectTransaction(event: TransactionRequestEvent, reason: String?, errorCode: Int? = null)

    suspend fun approveSignData(event: SignDataRequestEvent, network: TONNetwork)
    suspend fun rejectSignData(event: SignDataRequestEvent, reason: String?)
}
