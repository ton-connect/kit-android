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
package io.ton.walletkit.event

import org.json.JSONObject

/**
 * Intent event from a dApp deep link (tc://intent_inline or tc://intent).
 *
 * Discriminated by [intentType]: transaction, signData, or action.
 */
sealed class TONIntentEvent {
    abstract val id: String
    abstract val origin: String
    abstract val clientId: String?
    abstract val intentType: String

    /** Raw JSON for bridge passthrough. */
    abstract val rawJson: JSONObject

    data class TransactionIntent(
        override val id: String,
        override val origin: String,
        override val clientId: String?,
        val deliveryMode: String,
        val network: String?,
        val validUntil: Long?,
        val items: List<TONIntentItem>,
        override val rawJson: JSONObject,
    ) : TONIntentEvent() {
        override val intentType: String = "transaction"
    }

    data class SignDataIntent(
        override val id: String,
        override val origin: String,
        override val clientId: String?,
        val network: String?,
        val manifestUrl: String,
        val payload: TONSignDataPayload,
        override val rawJson: JSONObject,
    ) : TONIntentEvent() {
        override val intentType: String = "signData"
    }

    data class ActionIntent(
        override val id: String,
        override val origin: String,
        override val clientId: String?,
        val actionUrl: String,
        override val rawJson: JSONObject,
    ) : TONIntentEvent() {
        override val intentType: String = "action"
    }

    /**
     * Connect intent — appears as the first item in a [TONBatchedIntentEvent]
     * when the intent URL carries a connect request.
     */
    data class ConnectIntent(
        override val id: String,
        override val origin: String,
        override val clientId: String?,
        val manifestUrl: String?,
        val dAppName: String?,
        val dAppUrl: String?,
        val dAppIconUrl: String?,
        override val rawJson: JSONObject,
    ) : TONIntentEvent() {
        override val intentType: String = "connect"
    }
}

/** Individual intent item (transaction payload). */
sealed class TONIntentItem {
    abstract val type: String

    data class SendTon(
        val address: String,
        val amount: String,
        val payload: String?,
        val stateInit: String?,
        val extraCurrency: Map<String, String>?,
    ) : TONIntentItem() {
        override val type: String = "sendTon"
    }

    data class SendJetton(
        val jettonMasterAddress: String,
        val jettonAmount: String,
        val destination: String,
        val responseDestination: String?,
        val customPayload: String?,
        val forwardTonAmount: String?,
        val forwardPayload: String?,
        val queryId: Long?,
    ) : TONIntentItem() {
        override val type: String = "sendJetton"
    }

    data class SendNft(
        val nftAddress: String,
        val newOwnerAddress: String,
        val responseDestination: String?,
        val customPayload: String?,
        val forwardTonAmount: String?,
        val forwardPayload: String?,
        val queryId: Long?,
    ) : TONIntentItem() {
        override val type: String = "sendNft"
    }
}

/** Sign data payload variants. */
sealed class TONSignDataPayload {
    abstract val type: String

    data class Text(val text: String) : TONSignDataPayload() {
        override val type: String = "text"
    }

    data class Binary(val bytes: String) : TONSignDataPayload() {
        override val type: String = "binary"
    }

    data class Cell(val cell: String, val schema: String) : TONSignDataPayload() {
        override val type: String = "cell"
    }
}
