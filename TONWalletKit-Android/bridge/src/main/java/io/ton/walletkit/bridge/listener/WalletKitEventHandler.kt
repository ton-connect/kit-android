package io.ton.walletkit.bridge.listener

import io.ton.walletkit.bridge.event.WalletKitEvent

/**
 * Typed event handler for WalletKit bridge events.
 * Provides a single method with sealed event types for exhaustive handling.
 *
 * This is an alternative to [WalletKitEngineListener] that offers sealed classes and exhaustive when() expressions.
 *
 * Example usage:
 * ```kotlin
 * class MyHandler : WalletKitEventHandler {
 *     override fun handleEvent(event: WalletKitEvent) {
 *         when (event) {
 *             is WalletKitEvent.ConnectRequestEvent -> {
 *                 // Handle connection
 *                 event.request.approve(walletAddress)
 *             }
 *             is WalletKitEvent.TransactionRequestEvent -> {
 *                 // Handle transaction
 *                 event.request.approve()
 *             }
 *             is WalletKitEvent.SignDataRequestEvent -> {
 *                 // Handle signing
 *                 val result = event.request.approve()
 *             }
 *             is WalletKitEvent.DisconnectEvent -> {
 *                 // Handle disconnect
 *             }
 *             is WalletKitEvent.StateChangedEvent -> {
 *                 // Refresh UI
 *             }
 *             is WalletKitEvent.SessionsChangedEvent -> {
 *                 // Refresh sessions
 *             }
 *         }
 *     }
 * }
 * ```
 */
interface WalletKitEventHandler {
    /**
     * Handle a WalletKit event.
     * Use when() expression for exhaustive handling of all event types.
     *
     * @param event The event to handle
     */
    fun handleEvent(event: WalletKitEvent)
}
