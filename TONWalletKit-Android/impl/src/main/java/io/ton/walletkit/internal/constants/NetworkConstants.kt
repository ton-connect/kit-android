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
package io.ton.walletkit.internal.constants

/**
 * Constants for TON network configuration and endpoints.
 *
 * These constants define the available networks and their default API endpoints
 * used throughout the SDK.
 *
 * @suppress Internal implementation constants. Not part of public API.
 */
internal object NetworkConstants {
    /**
     * Mainnet network identifier.
     */
    const val NETWORK_MAINNET = "mainnet"

    /**
     * Testnet network identifier (default for development).
     */
    const val NETWORK_TESTNET = "testnet"

    /**
     * Default network used by the SDK.
     */
    const val DEFAULT_NETWORK = NETWORK_TESTNET

    /**
     * Default TON API URL for testnet.
     */
    const val DEFAULT_TESTNET_API_URL = "https://testnet.tonapi.io"

    /**
     * Default TON API URL for mainnet.
     */
    const val DEFAULT_MAINNET_API_URL = "https://tonapi.io"

    /**
     * Default wallet image URL.
     */
    const val DEFAULT_WALLET_IMAGE_URL = "https://wallet.ton.org/assets/ui/qr-logo.png"

    /**
     * Default wallet about URL.
     */
    const val DEFAULT_WALLET_ABOUT_URL = "https://wallet.ton.org"

    /**
     * Default app version when unable to retrieve from package manager.
     */
    const val DEFAULT_APP_VERSION = "1.0.0"

    /**
     * Maximum protocol version supported by the SDK.
     */
    const val MAX_PROTOCOL_VERSION = 2
}
