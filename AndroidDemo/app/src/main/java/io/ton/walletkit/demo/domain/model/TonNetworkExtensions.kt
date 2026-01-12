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
package io.ton.walletkit.demo.domain.model

import io.ton.walletkit.api.MAINNET
import io.ton.walletkit.api.TESTNET
import io.ton.walletkit.api.generated.TONNetwork

private const val BRIDGE_MAINNET = "-239"
private const val BRIDGE_TESTNET = "-3"

/**
 * Convert SDK network to the string value we persist in demo storage.
 */
fun TONNetwork.toBridgeValue(): String = this.chainId

/**
 * Parse a persisted network string (bridge manifest style or chain id) into SDK network.
 */
fun String?.toTonNetwork(fallback: TONNetwork = TONNetwork.MAINNET): TONNetwork {
    val normalized = this?.trim()
    return when (normalized) {
        BRIDGE_MAINNET, "mainnet" -> TONNetwork.MAINNET
        BRIDGE_TESTNET, "testnet" -> TONNetwork.TESTNET
        null, "" -> fallback
        else -> fallback
    }
}
