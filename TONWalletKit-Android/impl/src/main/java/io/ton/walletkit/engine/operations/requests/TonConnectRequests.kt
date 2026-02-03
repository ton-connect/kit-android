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
package io.ton.walletkit.engine.operations.requests

import io.ton.walletkit.api.generated.TONConnectionApprovalResponse
import io.ton.walletkit.api.generated.TONConnectionRequestEvent
import io.ton.walletkit.api.generated.TONSendTransactionApprovalResponse
import io.ton.walletkit.api.generated.TONSendTransactionRequestEvent
import io.ton.walletkit.api.generated.TONSignDataApprovalResponse
import io.ton.walletkit.api.generated.TONSignDataRequestEvent
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Internal bridge request models for TonConnect operations.
 * These DTOs represent the exact JSON structure sent to the JavaScript bridge.
 *
 * @suppress Internal bridge communication only.
 */

@Serializable
internal data class HandleTonConnectUrlRequest(
    val url: String,
)

@Serializable
internal data class ProcessInternalBrowserRequest(
    val messageId: String,
    val method: String,
    // Raw JSON (can be array or object)
    val params: JsonElement? = null,
    val url: String? = null,
)

@Serializable
internal data class ApproveConnectRequest(
    @Contextual
    val event: TONConnectionRequestEvent,
    val response: TONConnectionApprovalResponse? = null,
)

@Serializable
internal data class RejectConnectRequest(
    @Contextual
    val event: TONConnectionRequestEvent,
    val reason: String? = null,
    val errorCode: Int? = null,
)

@Serializable
internal data class ApproveTransactionRequest(
    @Contextual
    val event: TONSendTransactionRequestEvent,
    val response: TONSendTransactionApprovalResponse? = null,
)

@Serializable
internal data class RejectTransactionRequest(
    @Contextual
    val event: TONSendTransactionRequestEvent,
    val reason: kotlinx.serialization.json.JsonElement? = null,
)

@Serializable
internal data class ApproveSignDataRequest(
    @Contextual
    val event: TONSignDataRequestEvent,
    val response: TONSignDataApprovalResponse? = null,
)

@Serializable
internal data class RejectSignDataRequest(
    @Contextual
    val event: TONSignDataRequestEvent,
    val reason: String? = null,
)

@Serializable
internal data class DisconnectSessionRequest(
    val sessionId: String? = null,
)

@Serializable
internal data class HandleIntentUrlRequest(
    val url: String,
)

@Serializable
internal data class IsIntentUrlRequest(
    val url: String,
)

@Serializable
internal data class IntentItemsToTransactionRequestRequest(
    val event: IntentEventBridgeDto,
    val walletId: String,
)

/** Request for approving a transaction intent (txIntent or signMsg) */
@Serializable
internal data class ApproveTransactionIntentRequest(
    val event: TransactionIntentEventBridgeDto,
    val walletId: String,
)

/** Request for approving a sign data intent (signIntent) */
@Serializable
internal data class ApproveSignDataIntentRequest(
    val event: SignDataIntentEventBridgeDto,
    val walletId: String,
)

/** Request for rejecting any intent */
@Serializable
internal data class RejectIntentRequest(
    val event: IntentEventRefBridgeDto,
    val reason: String? = null,
    val errorCode: Int? = null,
)

/** Full transaction intent event for approval */
@Serializable
internal data class TransactionIntentEventBridgeDto(
    val id: String,
    val clientId: String,
    val hasConnectRequest: Boolean,
    val type: String,
    val network: String? = null,
    val validUntil: Long? = null,
    val items: List<IntentItemBridgeDto>,
)

/** Full sign data intent event for approval */
@Serializable
internal data class SignDataIntentEventBridgeDto(
    val id: String,
    val clientId: String,
    val hasConnectRequest: Boolean,
    val type: String,
    val network: String? = null,
    val manifestUrl: String,
    val payload: SignDataIntentPayloadBridgeDto,
)

/** Sign data payload DTO */
@Serializable
internal data class SignDataIntentPayloadBridgeDto(
    val type: String,
    val text: String? = null,
    val bytes: String? = null,
    val schema: String? = null,
    val cell: String? = null,
)

/** Minimal intent event reference for rejection */
@Serializable
internal data class IntentEventRefBridgeDto(
    val id: String,
    val clientId: String,
    val type: String,
)

/**
 * Intent event DTO for bridge communication.
 * @property id Unique event ID
 * @property type Intent type - "txIntent" or "signMsg"
 * @property network Network chain ID
 * @property validUntil Validity timestamp
 * @property items List of intent items
 */
@Serializable
internal data class IntentEventBridgeDto(
    val id: String,
    val type: String,
    val network: String? = null,
    val validUntil: Long? = null,
    val items: List<IntentItemBridgeDto>,
)

/**
 * Intent item DTO for bridge communication.
 * @property t Item type - "ton", "jetton", "nft"
 * @property a TON address
 * @property am TON amount
 * @property p Payload (base64)
 * @property si StateInit (base64)
 * @property ec Extra currencies
 * @property ma Jetton master address
 * @property qi Query ID
 * @property ja Jetton amount
 * @property d Destination address
 * @property rd Response destination
 * @property cp Custom payload
 * @property fta Forward TON amount
 * @property fp Forward payload
 * @property na NFT address
 * @property no New owner address
 */
@Serializable
internal data class IntentItemBridgeDto(
    val t: String,
    val a: String? = null,
    val am: String? = null,
    val p: String? = null,
    val si: String? = null,
    val ec: Map<String, String>? = null,
    val ma: String? = null,
    val qi: Long? = null,
    val ja: String? = null,
    val d: String? = null,
    val rd: String? = null,
    val cp: String? = null,
    val fta: String? = null,
    val fp: String? = null,
    val na: String? = null,
    val no: String? = null,
)

/** Request for approving an action intent (actionIntent) */
@Serializable
internal data class ApproveActionIntentRequest(
    val event: ActionIntentEventBridgeDto,
    val walletId: String,
)

/** Full action intent event for approval */
@Serializable
internal data class ActionIntentEventBridgeDto(
    val id: String,
    val clientId: String,
    val hasConnectRequest: Boolean,
    val type: String = "actionIntent",
    val network: String? = null,
    val actionUrl: String,
    val manifestUrl: String? = null,
)

/** Request for processing connect request after intent approval */
@Serializable
internal data class ProcessConnectAfterIntentRequest(
    val event: IntentEventWithConnectBridgeDto,
    val walletId: String,
    val proof: ConnectionApprovalProofBridgeDto? = null,
)

/** Intent event with optional connect request */
@Serializable
internal data class IntentEventWithConnectBridgeDto(
    val id: String,
    val clientId: String,
    val hasConnectRequest: Boolean,
    val type: String,
    val connectRequest: ConnectRequestBridgeDto? = null,
)

/** Connect request DTO */
@Serializable
internal data class ConnectRequestBridgeDto(
    val manifestUrl: String,
    val items: List<ConnectItemBridgeDto>? = null,
)

/** Connect item DTO */
@Serializable
internal data class ConnectItemBridgeDto(
    val name: String,
    val payload: String? = null,
)

/** Connection approval proof DTO */
@Serializable
internal data class ConnectionApprovalProofBridgeDto(
    val signature: String,
    val timestamp: Long,
    val domain: ConnectionApprovalProofDomainBridgeDto,
    val payload: String,
)

/** Connection approval proof domain DTO */
@Serializable
internal data class ConnectionApprovalProofDomainBridgeDto(
    val lengthBytes: Int,
    val value: String,
)
