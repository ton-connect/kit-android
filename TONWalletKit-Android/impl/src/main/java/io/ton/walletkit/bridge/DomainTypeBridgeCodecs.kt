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
package io.ton.walletkit.bridge

import io.ton.walletkit.model.TONBase64
import io.ton.walletkit.model.TONHex
import io.ton.walletkit.model.TONRawAddress
import io.ton.walletkit.model.TONUserFriendlyAddress

fun TONHex.encodeForBridge(): Any = value

object TONHexBridgeDecoder : BridgeDecodable<TONHex> {
    override fun decodeFromBridge(raw: Any?): TONHex? = (raw as? String)?.let(::TONHex)
}

fun TONBase64.encodeForBridge(): Any = value

object TONBase64BridgeDecoder : BridgeDecodable<TONBase64> {
    override fun decodeFromBridge(raw: Any?): TONBase64? = (raw as? String)?.let(::TONBase64)
}

fun TONUserFriendlyAddress.encodeForBridge(): Any = value

object TONUserFriendlyAddressBridgeDecoder : BridgeDecodable<TONUserFriendlyAddress> {
    override fun decodeFromBridge(raw: Any?): TONUserFriendlyAddress? =
        (raw as? String)?.let { runCatching { TONUserFriendlyAddress.parse(it) }.getOrNull() }
}

fun TONRawAddress.encodeForBridge(): Any = string

object TONRawAddressBridgeDecoder : BridgeDecodable<TONRawAddress> {
    override fun decodeFromBridge(raw: Any?): TONRawAddress? =
        (raw as? String)?.let { runCatching { TONRawAddress.parse(it) }.getOrNull() }
}
