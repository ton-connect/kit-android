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
package io.ton.walletkit.client

import io.ton.walletkit.api.generated.TONGetMethodResult
import io.ton.walletkit.api.generated.TONNetwork
import io.ton.walletkit.api.generated.TONRawStackItem
import io.ton.walletkit.model.TONBase64
import io.ton.walletkit.model.TONUserFriendlyAddress

/**
 * Interface for custom API client implementations.
 *
 * Implement this interface to provide custom TON blockchain API access.
 * This allows wallet applications to use their own API infrastructure
 * instead of the default TONCenter API.
 *
 * The API client is responsible for:
 * - Sending signed BOC (Bag of Cells) to the network
 * - Running get methods on smart contracts
 *
 * @see TONGetMethodResult for the get method result structure
 * @see TONRawStackItem for the TVM stack item types
 */
interface TONAPIClient {
    /**
     * The network this API client is configured for.
     */
    val network: TONNetwork

    /**
     * Send a signed BOC (Bag of Cells) to the network.
     *
     * @param boc The base64-encoded BOC to send
     * @return The transaction hash or message ID
     * @throws Exception if the send fails
     */
    suspend fun sendBoc(boc: TONBase64): String

    /**
     * Run a get method on a smart contract.
     *
     * @param address The contract address
     * @param method The method name to call
     * @param stack Optional stack items to pass as arguments
     * @param seqno Optional seqno for historical state queries
     * @return The get method result including gas used, exit code, and stack
     * @throws Exception if the call fails
     */
    suspend fun runGetMethod(
        address: TONUserFriendlyAddress,
        method: String,
        stack: List<TONRawStackItem>? = null,
        seqno: Int? = null,
    ): TONGetMethodResult
}
