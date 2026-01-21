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
package io.ton.walletkit.session

import io.ton.walletkit.api.generated.TONDAppInfo

/**
 * Abstraction for session management in TONConnect protocol.
 * Provides interface for session CRUD operations and lifecycle management.
 *
 * Implement this interface to provide custom session storage and management,
 * for example when integrating WalletKit into an existing wallet that already
 * has its own session management.
 */
interface TONConnectSessionManager {

    /**
     * Create a new session.
     *
     * @param sessionId Unique session identifier
     * @param dAppInfo Information about the dApp (name, url, iconUrl, description)
     * @param walletId The wallet ID to associate with this session
     * @param walletAddress The wallet address to associate with this session
     * @param isJsBridge If true, indicates this session was created via JS Bridge (internal browser)
     * @return The created session
     */
    suspend fun createSession(
        sessionId: String,
        dAppInfo: TONDAppInfo,
        walletId: String,
        walletAddress: String,
        isJsBridge: Boolean,
    ): TONConnectSession

    /**
     * Get session by ID.
     *
     * @param sessionId The session ID to retrieve
     * @return The session, or null if not found
     */
    suspend fun getSession(sessionId: String): TONConnectSession?

    /**
     * Get session by domain.
     *
     * @param domain The domain to search for
     * @return The session for that domain, or null if not found
     */
    suspend fun getSessionByDomain(domain: String): TONConnectSession?

    /**
     * Get all sessions as a list.
     *
     * @return List of all active sessions
     */
    suspend fun getSessions(): List<TONConnectSession>

    /**
     * Get sessions for a specific wallet by wallet ID.
     *
     * @param walletId The wallet ID to filter by
     * @return List of sessions for that wallet
     */
    suspend fun getSessionsForWallet(walletId: String): List<TONConnectSession>

    /**
     * Remove session by ID.
     *
     * @param sessionId The session ID to remove
     */
    suspend fun removeSession(sessionId: String)

    /**
     * Remove all sessions for a wallet by wallet ID.
     *
     * @param walletId Wallet ID string
     */
    suspend fun removeSessionsForWallet(walletId: String)

    /**
     * Clear all sessions.
     */
    suspend fun clearSessions()
}
