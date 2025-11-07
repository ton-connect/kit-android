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

/**
 * Represents a transaction request initiated by a dApp.
 *
 * This model contains all the necessary information to construct and send
 * a blockchain transaction. It's typically received from a dApp through
 * the TON Connect protocol.
 *
 * @property recipient The destination wallet address on the TON blockchain
 * @property amount Transaction amount in nanoTON (1 TON = 1,000,000,000 nanoTON)
 * @property comment Optional text comment/memo attached to the transaction
 * @property payload Optional raw cell payload for complex contract interactions
 */
data class TransactionRequest(
    val recipient: String,
    val amount: String,
    val comment: String? = null,
    val payload: String? = null,
)
