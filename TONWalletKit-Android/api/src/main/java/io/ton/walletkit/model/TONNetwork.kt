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
package io.ton.walletkit.model

import kotlinx.serialization.Serializable

/**
 * TON blockchain network.
 *
 * Mirrors the shared TON Wallet Kit model for cross-platform consistency.
 *
 * @property value Network chain ID
 */
@Serializable
enum class TONNetwork(val value: String) {
    /** Mainnet (chain ID: -239) */
    MAINNET("-239"),

    /** Testnet (chain ID: -3) */
    TESTNET("-3"),
    ;

    companion object {
        private const val CHAIN_ID_MAINNET = "-239"
        private const val CHAIN_ID_TESTNET = "-3"
        private const val NAME_MAINNET = "mainnet"
        private const val NAME_TESTNET = "testnet"

        /**
         * Parse network from string value.
         * Accepts both chain IDs ("-239", "-3") and names ("mainnet", "testnet").
         *
         * @param value String representation of network
         * @return TONNetwork enum or null if invalid
         */
        fun fromString(value: String): TONNetwork? = when (value.lowercase()) {
            CHAIN_ID_MAINNET, NAME_MAINNET -> MAINNET
            CHAIN_ID_TESTNET, NAME_TESTNET -> TESTNET
            else -> null
        }
    }
}
