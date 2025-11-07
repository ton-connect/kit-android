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
 * Represents a single money transfer row in a transaction.
 *
 * Mirrors the shared TON Wallet Kit model for cross-platform consistency.
 *
 * @property type Asset type (TON or Jetton)
 * @property jetton Jetton address if type is JETTON
 * @property from Sender address
 * @property to Recipient address
 * @property amount Transfer amount
 */
@Serializable
data class TONMoneyFlowRow(
    val type: TONAssetType,
    val jetton: String? = null,
    val from: String? = null,
    val to: String? = null,
    val amount: String? = null,
)

/**
 * Represents a transfer involving the wallet owner.
 *
 * Mirrors the shared TON Wallet Kit model for cross-platform consistency.
 *
 * @property type Asset type (TON or Jetton)
 * @property jetton Jetton address if type is JETTON
 * @property amount Transfer amount
 */
@Serializable
data class TONMoneyFlowSelf(
    val type: TONAssetType,
    val jetton: String? = null,
    val amount: String,
)

/**
 * Analysis of money flow in a transaction.
 *
 * Mirrors the shared TON Wallet Kit model for cross-platform consistency.
 *
 * @property outputs Total outgoing amount in nanoTON
 * @property inputs Total incoming amount in nanoTON
 * @property allJettonTransfers All jetton transfers in transaction
 * @property ourTransfers Transfers involving our wallet
 * @property ourAddress Our wallet address
 */
@Serializable
data class TONMoneyFlow(
    val outputs: String? = null,
    val inputs: String? = null,
    val allJettonTransfers: List<TONMoneyFlowRow>? = null,
    val ourTransfers: List<TONMoneyFlowSelf>? = null,
    val ourAddress: String? = null,
)
