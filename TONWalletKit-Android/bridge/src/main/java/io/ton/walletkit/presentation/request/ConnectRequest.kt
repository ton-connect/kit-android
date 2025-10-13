package io.ton.walletkit.presentation.request

import io.ton.walletkit.presentation.WalletKitEngine
import io.ton.walletkit.presentation.model.DAppInfo
import org.json.JSONObject

/**
 * Represents a connection request from a dApp.
 * Encapsulates both request data and approval/rejection actions.
 *
 * @property requestId Unique identifier for this request
 * @property dAppInfo Information about the requesting dApp
 * @property permissions List of requested permissions
 * @property eventJson Full event JSON from the bridge (required for approval/rejection)
 */
class ConnectRequest internal constructor(
    val requestId: String,
    val dAppInfo: DAppInfo?,
    val permissions: List<String>,
    private val eventJson: JSONObject,
    private val engine: WalletKitEngine,
) {
    /**
     * Approve this connection request with the specified wallet.
     *
     * @param walletAddress Address of the wallet to connect with
     * @throws io.ton.walletkit.bridge.WalletKitBridgeException if approval fails
     */
    suspend fun approve(walletAddress: String) {
        engine.approveConnect(eventJson, walletAddress)
    }

    /**
     * Reject this connection request.
     *
     * @param reason Optional reason for rejection
     * @throws io.ton.walletkit.bridge.WalletKitBridgeException if rejection fails
     */
    suspend fun reject(reason: String? = null) {
        engine.rejectConnect(eventJson, reason)
    }
}
