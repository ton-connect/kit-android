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
import com.google.crypto.tink.subtle.X25519
import io.ton.walletkit.api.generated.TONDAppInfo
import io.ton.walletkit.model.TONUserFriendlyAddress
import io.ton.walletkit.session.SessionFilter
import io.ton.walletkit.session.TONConnectSession
import io.ton.walletkit.session.TONConnectSessionManager
import java.net.URL
import java.time.Instant
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
        Log.d(TAG, "createSession called:")
        Log.d(TAG, "   sessionId: $sessionId")
        Log.d(TAG, "   dAppInfo: name=${dAppInfo.name}, url=${dAppInfo.url}")
        Log.d(TAG, "   walletId: $walletId")
        Log.d(TAG, "   walletAddress: $walletAddress")
        Log.d(TAG, "   isJsBridge: $isJsBridge")

        val now = Instant.now().toString()

        // Generate real X25519 keypair for session encryption using Google Tink
        // X25519.generatePrivateKey() returns a 32-byte private key
        val privateKeyBytes = X25519.generatePrivateKey()
        // X25519.publicFromPrivate() derives the 32-byte public key
        val publicKeyBytes = X25519.publicFromPrivate(privateKeyBytes)

        // Encode as hex strings (matching @tonconnect/protocol format)
        val privateKey = privateKeyBytes.toHexString()
        val publicKey = publicKeyBytes.toHexString()

        Log.d(TAG, "   Generated X25519 keypair: publicKey=${publicKey.take(16)}...")

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
            schemaVersion = SCHEMA_VERSION,
            dAppName = dAppInfo.name,
            dAppDescription = dAppInfo.description,
            dAppUrl = dAppInfo.url,
            dAppIconUrl = dAppInfo.iconUrl,
            isJsBridge = isJsBridge,
        )

        sessions[sessionId] = session
        Log.d(TAG, "Session stored. Total sessions: ${sessions.size}")

        return session
    }

    override suspend fun getSession(sessionId: String): TONConnectSession? {
        val session = sessions[sessionId]
        Log.d(TAG, "🔍 getSession($sessionId): ${if (session != null) "found" else "not found"}")
        return session
    }

    override suspend fun getSessions(filter: SessionFilter?): List<TONConnectSession> {
        var result = sessions.values.toList()

        if (filter != null) {
            filter.walletId?.let { wId ->
                result = result.filter { it.walletId == wId }
            }
            filter.domain?.let { d ->
                val host = try {
                    URL(d).host
                } catch (e: Exception) {
                    d
                }
                result = result.filter { it.domain == host }
            }
            filter.isJsBridge?.let { jsb ->
                result = result.filter { it.isJsBridge == jsb }
            }
        }

        Log.d(TAG, "getSessions(filter=$filter): returning ${result.size} sessions")
        return result
    }

    override suspend fun removeSession(sessionId: String): TONConnectSession? {
        val removed = sessions.remove(sessionId)
        Log.d(TAG, "removeSession($sessionId): ${if (removed != null) "removed" else "not found"}")
        return removed
    }

    override suspend fun removeSessions(filter: SessionFilter?): List<TONConnectSession> {
        val toRemove = getSessions(filter)
        toRemove.forEach { sessions.remove(it.sessionId) }
        Log.d(TAG, "removeSessions(filter=$filter): removed ${toRemove.size} sessions")
        return toRemove
    }

    override suspend fun clearSessions() {
        val count = sessions.size
        sessions.clear()
        Log.d(TAG, "clearSessions(): cleared $count sessions")
    }

    private fun ByteArray.toHexString(): String = joinToString("") { "%02x".format(it) }

    companion object {
        private const val TAG = "TestSessionManager"
        private const val SCHEMA_VERSION = 1
    }
}
