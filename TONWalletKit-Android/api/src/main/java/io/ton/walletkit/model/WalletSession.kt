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
 * Represents an active TON Connect session between a wallet and a dApp.
 *
 * A session is established when a user connects their wallet to a decentralized application.
 * It persists across app restarts and allows the dApp to request transactions and signatures
 * without requiring the user to reconnect.
 *
 * @property sessionId Unique identifier for this session
 * @property dAppName Display name of the connected dApp
 * @property walletAddress The wallet address used for this session
 * @property dAppUrl Optional URL of the dApp's website
 * @property manifestUrl Optional URL to the dApp's TON Connect manifest
 * @property iconUrl Optional URL to the dApp's icon/logo
 * @property createdAtIso ISO 8601 timestamp when the session was created (nullable if unknown)
 * @property lastActivityIso ISO 8601 timestamp of the last activity on this session (nullable if unknown)
 */
data class WalletSession(
    val sessionId: String,
    val dAppName: String,
    val walletAddress: String,
    val dAppUrl: String?,
    val manifestUrl: String?,
    val iconUrl: String?,
    val createdAtIso: String?,
    val lastActivityIso: String?,
)
