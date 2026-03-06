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
package io.ton.walletkit.listener

import io.ton.walletkit.event.TONWalletKitEvent

/**
 * Handler for TON Wallet Kit events.
 *
 * Mirrors the canonical TON Wallet Kit protocol for cross-platform consistency.
 *
 * Implement this interface to receive events from the wallet kit
 * such as connection requests, transaction requests, and sign data requests.
 *
 * Example:
 * ```kotlin
 * class MyEventsHandler : TONBridgeEventsHandler {
 *     override fun handle(event: TONWalletKitEvent) {
 *         when (event) {
 *             is TONWalletKitEvent.ConnectRequest -> {
 *                 // Handle connection request
 *                 event.request.approve(walletAddress)
 *             }
 *             is TONWalletKitEvent.SendTransactionRequest -> {
 *                 // Handle transaction request
 *                 event.request.approve()
 *             }
 *             is TONWalletKitEvent.SignDataRequest -> {
 *                 // Handle sign data request
 *                 event.request.approve()
 *             }
 *             is TONWalletKitEvent.Disconnect -> {
 *                 // Handle disconnect
 *             }
 *         }
 *     }
 * }
 * ```
 */
interface TONBridgeEventsHandler {
    /**
     * Handle a wallet kit event.
     *
     * Use when() expression for exhaustive handling of all event types.
     *
     * @param event The event to handle
     */
    fun handle(event: TONWalletKitEvent)
}
