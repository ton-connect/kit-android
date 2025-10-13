package io.ton.walletkit.presentation.request

import io.ton.walletkit.presentation.WalletKitEngine
import io.ton.walletkit.presentation.event.TransactionRequestEvent
import io.ton.walletkit.presentation.model.DAppInfo
import io.ton.walletkit.presentation.model.TransactionRequest as TransactionRequestData

/**
 * Represents a transaction request from a dApp.
 * Encapsulates both request data and approval/rejection actions.
 *
 * @property requestId Unique identifier for this request
 * @property dAppInfo Information about the requesting dApp
 * @property request Transaction request details (recipient, amount, etc.)
 * @property preview Optional preview data as JSON string
 * @property event Typed event data from the bridge
 */
class TransactionRequest internal constructor(
    val requestId: String,
    val dAppInfo: DAppInfo?,
    val request: TransactionRequestData,
    val preview: String? = null,
    private val event: TransactionRequestEvent,
    private val engine: WalletKitEngine,
) {
    /**
     * Approve this transaction request.
     *
     * @throws io.ton.walletkit.bridge.WalletKitBridgeException if approval fails
     */
    suspend fun approve() {
        engine.approveTransaction(event)
    }

    /**
     * Reject this transaction request.
     *
     * @param reason Optional reason for rejection
     * @throws io.ton.walletkit.bridge.WalletKitBridgeException if rejection fails
     */
    suspend fun reject(reason: String? = null) {
        engine.rejectTransaction(event, reason)
    }
}
