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
package io.ton.walletkit.demo.core

import android.util.Log
import io.ton.walletkit.api.generated.TONConnectSession
import io.ton.walletkit.api.generated.TONDAppInfo
import io.ton.walletkit.model.TONUserFriendlyAddress
import io.ton.walletkit.session.TONConnectSessionManager
import java.net.URL
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Test implementation of TONConnectSessionManager that stores sessions in memory.
 * This demonstrates how Tonkeeper or other wallet apps can provide their own session management.
 *
 * In a real implementation, this would persist sessions to the app's database.
 */
class TestSessionManager : TONConnectSessionManager {

    private val sessions = ConcurrentHashMap<String, TONConnectSession>()

    override suspend fun createSession(
        sessionId: String,
        dAppInfo: TONDAppInfo,
        walletId: String,
        walletAddress: String,
        isJsBridge: Boolean,
    ): TONConnectSession {
        Log.d(TAG, "🔵 createSession called:")
        Log.d(TAG, "   sessionId: $sessionId")
        Log.d(TAG, "   dAppInfo: name=${dAppInfo.name}, url=${dAppInfo.url}")
        Log.d(TAG, "   walletId: $walletId")
        Log.d(TAG, "   walletAddress: $walletAddress")
        Log.d(TAG, "   isJsBridge: $isJsBridge")

        val now = Instant.now().toString()

        // Generate keypair for the session (in real implementation, use proper crypto)
        val keyPairId = UUID.randomUUID().toString()
        val privateKey = "test-private-key-$keyPairId"
        val publicKey = "test-public-key-$keyPairId"

        // Extract domain from dApp URL
        val domain = try {
            dAppInfo.url?.let { URL(it).host } ?: ""
        } catch (e: Exception) {
            ""
        }

        val session = TONConnectSession(
            sessionId = sessionId,
            walletId = walletId,
            walletAddress = TONUserFriendlyAddress(walletAddress),
            createdAt = now,
            lastActivityAt = now,
            privateKey = privateKey,
            publicKey = publicKey,
            domain = domain,
            dAppInfo = dAppInfo,
            isJsBridge = isJsBridge,
        )

        sessions[sessionId] = session
        Log.d(TAG, "✅ Session stored. Total sessions: ${sessions.size}")

        return session
    }

    override suspend fun getSession(sessionId: String): TONConnectSession? {
        val session = sessions[sessionId]
        Log.d(TAG, "🔍 getSession($sessionId): ${if (session != null) "found" else "not found"}")
        return session
    }

    override suspend fun getSessionByDomain(domain: String): TONConnectSession? {
        Log.d(TAG, "🔍 getSessionByDomain($domain)")

        // Extract host from domain URL
        val host = try {
            URL(domain).host
        } catch (e: Exception) {
            domain
        }

        for (session in sessions.values) {
            if (session.domain == host) {
                Log.d(TAG, "✅ Found session by domain: ${session.sessionId}")
                return session
            }
        }

        Log.d(TAG, "❌ No session found for domain: $domain")
        return null
    }

    override suspend fun getSessions(): List<TONConnectSession> {
        val sessionList = sessions.values.toList()
        Log.d(TAG, "📋 getSessions(): returning ${sessionList.size} sessions")
        return sessionList
    }

    override suspend fun getSessionsForWallet(walletId: String): List<TONConnectSession> {
        val walletSessions = sessions.values.filter { it.walletId == walletId }
        Log.d(TAG, "📋 getSessionsForWallet($walletId): returning ${walletSessions.size} sessions")
        return walletSessions
    }

    override suspend fun removeSession(sessionId: String) {
        val removed = sessions.remove(sessionId) != null
        Log.d(TAG, "🗑️ removeSession($sessionId): ${if (removed) "removed" else "not found"}")
    }

    override suspend fun removeSessionsForWallet(walletId: String) {
        val toRemove = sessions.entries.filter { it.value.walletId == walletId }.map { it.key }
        toRemove.forEach { sessions.remove(it) }
        Log.d(TAG, "🗑️ removeSessionsForWallet($walletId): removed ${toRemove.size} sessions")
    }

    override suspend fun clearSessions() {
        val count = sessions.size
        sessions.clear()
        Log.d(TAG, "🗑️ clearSessions(): cleared $count sessions")
    }

    companion object {
        private const val TAG = "TestSessionManager"
    }
}
