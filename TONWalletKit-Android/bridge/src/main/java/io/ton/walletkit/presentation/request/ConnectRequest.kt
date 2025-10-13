package io.ton.walletkit.presentation.request

import io.ton.walletkit.presentation.WalletKitEngine
import io.ton.walletkit.presentation.event.ConnectRequestEvent
import io.ton.walletkit.presentation.model.DAppInfo

/**
 * Represents a connection request from a dApp.
 * Encapsulates both request data and approval/rejection actions.
 *
 * @property requestId Unique identifier for this request
 * @property dAppInfo Information about the requesting dApp
 * @property permissions List of requested permissions
 * @property event Typed event data from the bridge
 */
class ConnectRequest internal constructor(
    val requestId: String,
    val dAppInfo: DAppInfo?,
    val permissions: List<ConnectRequestEvent.ConnectPermission>,
    private val event: ConnectRequestEvent,
    private val engine: WalletKitEngine,
) {
    /**
     * Approve this connection request with the specified wallet.
     *
     * @param walletAddress Address of the wallet to connect with
     * @throws io.ton.walletkit.bridge.WalletKitBridgeException if approval fails
     */
    suspend fun approve(walletAddress: String) {
        val eventWithWallet = event.copy(walletAddress = walletAddress)
        engine.approveConnect(eventWithWallet)
    }

    /**
     * Reject this connection request.
     *
     * @param reason Optional reason for rejection
     * @throws io.ton.walletkit.bridge.WalletKitBridgeException if rejection fails
     */
    suspend fun reject(reason: String? = null) {
        engine.rejectConnect(event, reason)
    }
}
