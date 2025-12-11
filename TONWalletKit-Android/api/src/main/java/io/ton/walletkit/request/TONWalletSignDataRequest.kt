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

import io.ton.walletkit.event.SignDataRequestEvent
import io.ton.walletkit.event.SignDataType
import io.ton.walletkit.model.DAppInfo
import io.ton.walletkit.model.TONNetwork

/**
 * Represents a data signing request from a dApp.
 *
 * Aligns with the shared TON Wallet Kit API contract for cross-platform consistency.
 *
 * Handle this request by calling [approve] to sign the data
 * or [reject] to deny it.
 *
 * @property dAppInfo Information about the requesting dApp
 * @property walletAddress Address of the wallet to use for signing
 * @property payloadType Type of data to sign (TEXT, BINARY, or CELL)
 * @property payloadContent The actual content to sign (text, base64 bytes, or cell BOC)
 * @property preview Human-readable preview of the data if available
 */
class TONWalletSignDataRequest(
    val dAppInfo: DAppInfo?,
    val walletAddress: String?,
    val tonNetwork: TONNetwork,
    private val event: SignDataRequestEvent,
    private val handler: RequestHandler,
) {
    /**
     * Type of data to sign
     */
    val payloadType: SignDataType
        get() = event.request?.type ?: SignDataType.BINARY

    /**
     * The content to be signed (text, base64 bytes, or cell BOC depending on type)
     */
    val payloadContent: String
        get() = when (event.request?.type) {
            SignDataType.TEXT -> event.request.text ?: ""
            SignDataType.BINARY -> event.request.bytes ?: ""
            SignDataType.CELL -> event.request.cell ?: ""
            null -> ""
        }

    /**
     * Human-readable preview of the data if available
     */
    val preview: String?
        get() = event.preview?.content

    /**
     * Approve and sign this data signing request.
     *
     * Note: This method does not return the signature. The signature is sent to the dApp
     * automatically through the bridge.
     *
     * @throws io.ton.walletkit.WalletKitBridgeException if approval or signing fails
     */
    suspend fun approve() {
        handler.approveSignData(event, tonNetwork)
    }

    /**
     * Reject this data signing request.
     *
     * @param reason Optional reason for rejection
     * @throws io.ton.walletkit.WalletKitBridgeException if rejection fails
     */
    suspend fun reject(reason: String? = null) {
        handler.rejectSignData(event, reason)
    }
}
