package io.ton.walletkit.bridge.request

import io.ton.walletkit.bridge.WalletKitEngine
import io.ton.walletkit.bridge.model.DAppInfo
import io.ton.walletkit.bridge.model.TransactionRequest as TransactionRequestData

/**
 * Represents a transaction request from a dApp.
 * Encapsulates both request data and approval/rejection actions.
 *
 * @property requestId Unique identifier for this request
 * @property dAppInfo Information about the requesting dApp
 * @property request Transaction request details (recipient, amount, etc.)
 */
class TransactionRequest internal constructor(
    val requestId: Any,
    val dAppInfo: DAppInfo?,
    val request: TransactionRequestData,
    private val engine: WalletKitEngine,
) {
    /**
     * Approve and execute this transaction request.
     *
     * @throws io.ton.walletkit.bridge.WalletKitBridgeException if approval or signing fails
     */
    suspend fun approve() {
        engine.approveTransaction(requestId)
    }

    /**
     * Reject this transaction request.
     *
     * @param reason Optional reason for rejection
     * @throws io.ton.walletkit.bridge.WalletKitBridgeException if rejection fails
     */
    suspend fun reject(reason: String? = null) {
        engine.rejectTransaction(requestId, reason)
    }
}
