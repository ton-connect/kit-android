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

import io.ton.walletkit.api.generated.TONTransactionEmulatedPreview
import io.ton.walletkit.api.generated.TONTransferRequest
import io.ton.walletkit.engine.infrastructure.BridgeRpcClient
import io.ton.walletkit.engine.infrastructure.toJSONObject
import io.ton.walletkit.engine.model.TONTransactionWithPreview
import io.ton.walletkit.engine.operations.requests.CreateTransferMultiTonRequest
import io.ton.walletkit.engine.operations.requests.CreateTransferTonRequest
import io.ton.walletkit.engine.operations.requests.GetTransactionPreviewRequest
import io.ton.walletkit.engine.operations.requests.HandleNewTransactionRequest
import io.ton.walletkit.engine.operations.requests.SendTransactionRequest
import io.ton.walletkit.exceptions.JSValueConversionException
import io.ton.walletkit.internal.constants.BridgeMethodConstants
import io.ton.walletkit.internal.constants.ResponseConstants
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Groups TON transaction related bridge operations including creation, preview,
 * submission, and acknowledgement of newly created transactions.
 *
 * @property ensureInitialized Suspended callback that ensures the bridge is ready.
 * @property rpcClient Client used to invoke bridge methods.
 * @property json Kotlin serialization instance used for decoding bridge payloads.
 *
 * @suppress Internal component used by [WebViewWalletKitEngine].
 */
internal class TransactionOperations(
    private val ensureInitialized: suspend () -> Unit,
    private val rpcClient: BridgeRpcClient,
    private val json: Json,
) {

    suspend fun createTransferTonTransaction(
        walletId: String,
        params: TONTransferRequest,
    ): TONTransactionWithPreview {
        ensureInitialized()

        val request = CreateTransferTonRequest(
            walletId = walletId,
            recipientAddress = params.recipientAddress.value,
            transferAmount = params.transferAmount,
            comment = params.comment,
            body = params.payload?.value,
            stateInit = params.stateInit?.value,
        )
        val result = rpcClient.call(BridgeMethodConstants.METHOD_CREATE_TRANSFER_TON_TRANSACTION, json.toJSONObject(request))

        // JS returns { transaction, preview } or { transaction }
        return if (result.has("transaction")) {
            val transactionContent = result.get("transaction").toString()
            val preview = if (result.has("preview") && !result.isNull("preview")) {
                val previewJson = result.getJSONObject("preview")
                try {
                    json.decodeFromString<TONTransactionEmulatedPreview>(previewJson.toString())
                } catch (e: SerializationException) {
                    throw JSValueConversionException.DecodingError(
                        message = "Failed to decode TONTransactionEmulatedPreview in createTransferTonTransaction: ${e.message}",
                        cause = e,
                    )
                }
            } else {
                null
            }
            TONTransactionWithPreview(transactionContent, preview)
        } else {
            // Fallback for legacy response format
            TONTransactionWithPreview(result.toString(), null)
        }
    }

    suspend fun createTransferMultiTonTransaction(
        walletId: String,
        messages: List<TONTransferRequest>,
    ): TONTransactionWithPreview {
        ensureInitialized()

        val request = CreateTransferMultiTonRequest(walletId = walletId, messages = messages)
        val result = rpcClient.call(BridgeMethodConstants.METHOD_CREATE_TRANSFER_MULTI_TON_TRANSACTION, json.toJSONObject(request))

        // JS returns { transaction, preview } or { transaction }
        return if (result.has("transaction")) {
            val transactionContent = result.get("transaction").toString()
            val preview = if (result.has("preview") && !result.isNull("preview")) {
                val previewJson = result.getJSONObject("preview")
                try {
                    json.decodeFromString<TONTransactionEmulatedPreview>(previewJson.toString())
                } catch (e: SerializationException) {
                    throw JSValueConversionException.DecodingError(
                        message = "Failed to decode TONTransactionEmulatedPreview in createTransferMultiTonTransaction: ${e.message}",
                        cause = e,
                    )
                }
            } else {
                null
            }
            TONTransactionWithPreview(transactionContent, preview)
        } else {
            // Fallback for legacy response format
            TONTransactionWithPreview(result.toString(), null)
        }
    }

    suspend fun handleNewTransaction(walletId: String, transactionContent: String) {
        ensureInitialized()

        val request = HandleNewTransactionRequest(walletId = walletId, transactionContent = transactionContent)
        rpcClient.call(BridgeMethodConstants.METHOD_HANDLE_NEW_TRANSACTION, json.toJSONObject(request))
    }

    suspend fun sendTransaction(walletId: String, transactionContent: String): String {
        ensureInitialized()

        val request = SendTransactionRequest(walletId = walletId, transactionContent = transactionContent)
        val result = rpcClient.call(BridgeMethodConstants.METHOD_SEND_TRANSACTION, json.toJSONObject(request))
        return result.getString(ResponseConstants.KEY_SIGNED_BOC)
    }

    suspend fun getTransactionPreview(walletId: String, transactionContent: String): TONTransactionEmulatedPreview {
        ensureInitialized()

        val request = GetTransactionPreviewRequest(walletId = walletId, transactionContent = transactionContent)
        val result = rpcClient.call(BridgeMethodConstants.METHOD_GET_TRANSACTION_PREVIEW, json.toJSONObject(request))
        return try {
            json.decodeFromString(TONTransactionEmulatedPreview.serializer(), result.toString())
        } catch (e: SerializationException) {
            throw JSValueConversionException.DecodingError(
                message = "Failed to decode TONTransactionEmulatedPreview: ${e.message}",
                cause = e,
            )
        }
    }
}
