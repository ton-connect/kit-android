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
package io.ton.walletkit.core

import io.ton.walletkit.api.generated.TONGetMethodResult
import io.ton.walletkit.api.generated.TONMasterchainInfo
import io.ton.walletkit.api.generated.TONRawStackItem
import io.ton.walletkit.client.TONAPIClient
import io.ton.walletkit.engine.WalletKitEngine
import io.ton.walletkit.model.TONBase64
import io.ton.walletkit.model.TONUserFriendlyAddress

/**
 * [TONAPIClient] backed by the JS-side `ApiClient` bound to a specific wallet.
 *
 * Every method delegates to the engine, which routes through the WebView bridge to
 * the wallet's underlying JS client (custom or built-in).
 */
internal class BridgedJSAPIClient(
    private val walletId: String,
    private val engine: WalletKitEngine,
) : TONAPIClient {

    override suspend fun sendBoc(boc: TONBase64): String =
        engine.walletClientSendBoc(walletId, boc.value)

    override suspend fun runGetMethod(
        address: TONUserFriendlyAddress,
        method: String,
        stack: List<TONRawStackItem>?,
        seqno: Int?,
    ): TONGetMethodResult = engine.walletClientRunGetMethod(walletId, address.value, method, stack, seqno)

    override suspend fun getMasterchainInfo(): TONMasterchainInfo =
        engine.walletClientGetMasterchainInfo(walletId)
}
