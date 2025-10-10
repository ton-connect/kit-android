package io.ton.walletkit.bridge.request

import io.ton.walletkit.bridge.WalletKitEngine
import io.ton.walletkit.bridge.model.DAppInfo

/**
 * Represents a connection request from a dApp.
 * Encapsulates both request data and approval/rejection actions.
 *
 * @property requestId Unique identifier for this request
 * @property dAppInfo Information about the requesting dApp
 * @property permissions List of requested permissions
 */
class ConnectRequest internal constructor(
    val requestId: Any,
    val dAppInfo: DAppInfo?,
    val permissions: List<String>,
    private val engine: WalletKitEngine,
) {
    /**
     * Approve this connection request with the specified wallet.
     *
     * @param walletAddress Address of the wallet to connect with
     * @throws io.ton.walletkit.bridge.WalletKitBridgeException if approval fails
     */
    suspend fun approve(walletAddress: String) {
        engine.approveConnect(requestId, walletAddress)
    }

    /**
     * Reject this connection request.
     *
     * @param reason Optional reason for rejection
     * @throws io.ton.walletkit.bridge.WalletKitBridgeException if rejection fails
     */
    suspend fun reject(reason: String? = null) {
        engine.rejectConnect(requestId, reason)
    }
}
