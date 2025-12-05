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
 * Parameters for creating a TON transfer transaction.
 *
 * Matches the TypeScript TonTransferParams interface from the JS WalletKit API:
 * ```typescript
 * type TonTransferParams = {
 *   toAddress: string;
 *   amount: string;
 *   body?: string;     // base64 BOC
 *   comment?: string;
 *   stateInit?: string; // base64 BOC
 *   extraCurrency?: ConnectExtraCurrency;
 *   mode?: SendMode;
 * }
 * ```
 *
 * @property toAddress Recipient address
 * @property amount Amount in nanoTON as a string
 * @property comment Optional comment/memo text (mutually exclusive with body)
 * @property body Optional raw cell payload as base64 BOC (mutually exclusive with comment)
 * @property stateInit Optional state init as base64 BOC
 */
@Serializable
data class TONTransferParams(
    val toAddress: TONUserFriendlyAddress,
    val amount: String,
    val comment: String? = null,
    val body: String? = null,
    val stateInit: String? = null,
) {
    init {
        // Ensure only one of comment or body is set
        require(comment == null || body == null) {
            "Only one of 'comment' or 'body' can be specified, not both"
        }
    }
}
