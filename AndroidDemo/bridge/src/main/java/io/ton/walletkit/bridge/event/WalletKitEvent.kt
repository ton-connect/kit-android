package io.ton.walletkit.bridge.event

import io.ton.walletkit.bridge.request.ConnectRequest
import io.ton.walletkit.bridge.request.SignDataRequest
import io.ton.walletkit.bridge.request.TransactionRequest

/**
 * Represents events from WalletKit bridge using a type-safe sealed hierarchy for all possible events.
 *
 * This is an alternative to the [io.ton.walletkit.bridge.listener.WalletKitEngineListener] interface
 * that provides sealed classes and exhaustive when() expressions.
 */
sealed class WalletKitEvent {
    /**
     * A dApp is requesting to connect to a wallet.
     *
     * @property request Connection request with approve/reject methods
     */
    data class ConnectRequestEvent(val request: ConnectRequest) : WalletKitEvent()

    /**
     * A dApp is requesting to execute a transaction.
     *
     * @property request Transaction request with approve/reject methods
     */
    data class TransactionRequestEvent(val request: TransactionRequest) : WalletKitEvent()

    /**
     * A dApp is requesting to sign arbitrary data.
     *
     * @property request Sign data request with approve/reject methods
     */
    data class SignDataRequestEvent(val request: SignDataRequest) : WalletKitEvent()

    /**
     * A session has been disconnected.
     *
     * @property sessionId ID of the disconnected session
     */
    data class DisconnectEvent(val sessionId: String) : WalletKitEvent()

    /**
     * Wallet state has changed (balance, transactions, etc.).
     *
     * @property address Address of the wallet that changed
     */
    data class StateChangedEvent(val address: String) : WalletKitEvent()

    /**
     * Active sessions list has changed.
     */
    data object SessionsChangedEvent : WalletKitEvent()
}
