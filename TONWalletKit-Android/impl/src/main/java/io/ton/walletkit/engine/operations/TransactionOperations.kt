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
package io.ton.walletkit.engine.operations

import io.ton.walletkit.api.generated.TONSendTransactionResponse
import io.ton.walletkit.api.generated.TONTransactionEmulatedPreview
import io.ton.walletkit.api.generated.TONTransactionPreviewOptions
import io.ton.walletkit.api.generated.TONTransactionRequest
import io.ton.walletkit.api.generated.TONTransferRequest
import io.ton.walletkit.engine.infrastructure.BridgeRpcClient
import io.ton.walletkit.engine.infrastructure.callTyped
import io.ton.walletkit.engine.operations.requests.CreateTransferMultiTonRequest
import io.ton.walletkit.engine.operations.requests.CreateTransferTonRequest
import io.ton.walletkit.engine.operations.requests.GetTransactionPreviewRequest
import io.ton.walletkit.engine.operations.requests.HandleNewTransactionRequest
import io.ton.walletkit.engine.operations.requests.SendTransactionRequest
import io.ton.walletkit.internal.constants.BridgeMethodConstants

internal suspend fun BridgeRpcClient.createTransferTonTransaction(
    walletId: String,
    params: TONTransferRequest,
): TONTransactionRequest = callTyped(
    BridgeMethodConstants.METHOD_CREATE_TRANSFER_TON_TRANSACTION,
    CreateTransferTonRequest(
        walletId = walletId,
        recipientAddress = params.recipientAddress.value,
        transferAmount = params.transferAmount,
        comment = params.comment,
        body = params.payload?.value,
        stateInit = params.stateInit?.value,
    ),
)

internal suspend fun BridgeRpcClient.createTransferMultiTonTransaction(
    walletId: String,
    messages: List<TONTransferRequest>,
): TONTransactionRequest = callTyped(
    BridgeMethodConstants.METHOD_CREATE_TRANSFER_MULTI_TON_TRANSACTION,
    CreateTransferMultiTonRequest(walletId = walletId, messages = messages),
)

internal suspend fun BridgeRpcClient.handleNewTransaction(walletId: String, transactionContent: TONTransactionRequest) {
    send(
        BridgeMethodConstants.METHOD_HANDLE_NEW_TRANSACTION,
        HandleNewTransactionRequest(walletId = walletId, transactionContent = transactionContent),
    )
}

internal suspend fun BridgeRpcClient.sendTransaction(
    walletId: String,
    transactionContent: TONTransactionRequest,
): TONSendTransactionResponse = callTyped(
    BridgeMethodConstants.METHOD_SEND_TRANSACTION,
    SendTransactionRequest(walletId = walletId, transactionContent = transactionContent),
)

internal suspend fun BridgeRpcClient.getTransactionPreview(
    walletId: String,
    transactionContent: TONTransactionRequest,
    options: TONTransactionPreviewOptions? = null,
): TONTransactionEmulatedPreview = callTyped(
    BridgeMethodConstants.METHOD_GET_TRANSACTION_PREVIEW,
    GetTransactionPreviewRequest(
        walletId = walletId,
        transactionContent = transactionContent,
        options = options,
    ),
)
