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
package io.ton.walletkit.demo.presentation.model

import io.ton.walletkit.event.TONIntentEvent
import io.ton.walletkit.event.TONIntentItem
import io.ton.walletkit.event.TONSignDataPayload

/**
 * UI model for intent requests from deep links.
 */
sealed interface IntentRequestUi {
    val id: String
    val clientId: String
    val hasConnectRequest: Boolean
    val walletId: String

    /**
     * Transaction intent - TON, jetton, or NFT transfers.
     */
    data class Transaction(
        override val id: String,
        override val clientId: String,
        override val hasConnectRequest: Boolean,
        override val walletId: String,
        val type: String,
        val network: String?,
        val validUntil: Long?,
        val items: List<IntentItemUi>,
        val event: TONIntentEvent.TransactionIntent,
    ) : IntentRequestUi

    /**
     * Sign data intent - sign arbitrary data without sending a transaction.
     */
    data class SignData(
        override val id: String,
        override val clientId: String,
        override val hasConnectRequest: Boolean,
        override val walletId: String,
        val manifestUrl: String,
        val payload: SignDataPayloadUi,
        val event: TONIntentEvent.SignDataIntent,
    ) : IntentRequestUi

    /**
     * Action intent - fetch action details from URL.
     */
    data class Action(
        override val id: String,
        override val clientId: String,
        override val hasConnectRequest: Boolean,
        override val walletId: String,
        val actionUrl: String,
        val event: TONIntentEvent.ActionIntent,
    ) : IntentRequestUi
}

/**
 * UI model for intent items (individual transfers in a transaction intent).
 */
sealed interface IntentItemUi {
    val type: String

    data class SendTon(
        override val type: String = "ton",
        val address: String,
        val amount: String,
        val amountInTon: String,
        val hasPayload: Boolean,
        val hasStateInit: Boolean,
    ) : IntentItemUi

    data class SendJetton(
        override val type: String = "jetton",
        val masterAddress: String,
        val jettonAmount: String,
        val destination: String,
        val hasForwardPayload: Boolean,
    ) : IntentItemUi

    data class SendNft(
        override val type: String = "nft",
        val nftAddress: String,
        val newOwner: String,
        val hasForwardPayload: Boolean,
    ) : IntentItemUi
}

/**
 * UI model for sign data payload.
 */
sealed interface SignDataPayloadUi {
    val type: String

    data class Text(
        override val type: String = "text",
        val text: String,
    ) : SignDataPayloadUi

    data class Binary(
        override val type: String = "binary",
        val bytes: String,
        val bytesPreview: String,
    ) : SignDataPayloadUi

    data class Cell(
        override val type: String = "cell",
        val schema: String,
        val cell: String,
        val cellPreview: String,
    ) : SignDataPayloadUi
}

/**
 * Mapping functions to convert from SDK types to UI types.
 */
object IntentMapper {

    fun mapTransactionIntent(
        event: TONIntentEvent.TransactionIntent,
        walletId: String,
    ): IntentRequestUi.Transaction = IntentRequestUi.Transaction(
        id = event.id,
        clientId = event.clientId,
        hasConnectRequest = event.hasConnectRequest,
        walletId = walletId,
        type = event.type,
        network = event.network,
        validUntil = event.validUntil,
        items = event.items.map { mapIntentItem(it) },
        event = event,
    )

    fun mapSignDataIntent(
        event: TONIntentEvent.SignDataIntent,
        walletId: String,
    ): IntentRequestUi.SignData = IntentRequestUi.SignData(
        id = event.id,
        clientId = event.clientId,
        hasConnectRequest = event.hasConnectRequest,
        walletId = walletId,
        manifestUrl = event.manifestUrl,
        payload = mapSignDataPayload(event.payload),
        event = event,
    )

    fun mapActionIntent(
        event: TONIntentEvent.ActionIntent,
        walletId: String,
    ): IntentRequestUi.Action = IntentRequestUi.Action(
        id = event.id,
        clientId = event.clientId,
        hasConnectRequest = event.hasConnectRequest,
        walletId = walletId,
        actionUrl = event.actionUrl,
        event = event,
    )

    private fun mapIntentItem(item: TONIntentItem): IntentItemUi = when (item) {
        is TONIntentItem.SendTon -> IntentItemUi.SendTon(
            address = item.address,
            amount = item.amount,
            amountInTon = formatNanoTon(item.amount),
            hasPayload = item.payload != null,
            hasStateInit = item.stateInit != null,
        )
        is TONIntentItem.SendJetton -> IntentItemUi.SendJetton(
            masterAddress = item.masterAddress,
            jettonAmount = item.jettonAmount,
            destination = item.destination,
            hasForwardPayload = item.forwardPayload != null,
        )
        is TONIntentItem.SendNft -> IntentItemUi.SendNft(
            nftAddress = item.nftAddress,
            newOwner = item.newOwner,
            hasForwardPayload = item.forwardPayload != null,
        )
    }

    private fun mapSignDataPayload(payload: TONSignDataPayload): SignDataPayloadUi = when (payload) {
        is TONSignDataPayload.Text -> SignDataPayloadUi.Text(text = payload.text)
        is TONSignDataPayload.Binary -> SignDataPayloadUi.Binary(
            bytes = payload.bytes,
            bytesPreview = payload.bytes.take(40) + if (payload.bytes.length > 40) "..." else "",
        )
        is TONSignDataPayload.Cell -> SignDataPayloadUi.Cell(
            schema = payload.schema,
            cell = payload.cell,
            cellPreview = payload.cell.take(40) + if (payload.cell.length > 40) "..." else "",
        )
    }

    private fun formatNanoTon(nanoTon: String): String = try {
        val nano = nanoTon.toBigDecimal()
        val ton = nano.divide(java.math.BigDecimal("1000000000"), 9, java.math.RoundingMode.HALF_UP)
        ton.stripTrailingZeros().toPlainString()
    } catch (e: Exception) {
        nanoTon
    }
}
