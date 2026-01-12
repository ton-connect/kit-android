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
package io.ton.walletkit.demo.presentation.model

import io.ton.walletkit.api.generated.TONNetwork
import io.ton.walletkit.api.generated.TONTransaction
import io.ton.walletkit.demo.domain.model.WalletInterfaceType

data class WalletSummary(
    val address: String,
    val name: String,
    val network: TONNetwork,
    val version: String,
    val publicKey: String?,
    val balanceNano: String?,
    val balance: String?,
    val transactions: List<TONTransaction>?,
    val lastUpdated: Long?,
    val connectedSessions: List<SessionSummary> = emptyList(), // Sessions connected to this wallet
    val createdAt: Long? = null, // Unix timestamp in milliseconds when wallet was created/imported
    val interfaceType: WalletInterfaceType = WalletInterfaceType.MNEMONIC, // Wallet interface type
)
