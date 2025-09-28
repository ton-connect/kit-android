package io.ton.walletkit.bridge

import io.ton.walletkit.bridge.config.WalletKitBridgeConfig
import io.ton.walletkit.bridge.listener.WalletKitEngineListener
import io.ton.walletkit.bridge.model.WalletAccount
import io.ton.walletkit.bridge.model.WalletSession
import io.ton.walletkit.bridge.model.WalletState
import org.json.JSONObject
import java.io.Closeable

/**
 * Abstraction over a runtime that can execute the WalletKit JavaScript bundle and expose
 * the wallet APIs to Android callers. Implementations may back the runtime with a WebView or
 * an embedded JavaScript engine such as QuickJS. Every implementation must provide the same
 * JSON-RPC surface as the historical [WalletKitBridge] class.
 */
interface WalletKitEngine {
    val kind: WalletKitEngineKind

    fun addListener(listener: WalletKitEngineListener): Closeable

    suspend fun init(config: WalletKitBridgeConfig = WalletKitBridgeConfig()): JSONObject

    suspend fun addWalletFromMnemonic(
        words: List<String>,
        version: String,
        network: String? = null,
    ): JSONObject

    suspend fun getWallets(): List<WalletAccount>

    suspend fun getWalletState(address: String): WalletState

    suspend fun handleTonConnectUrl(url: String): JSONObject

    suspend fun approveConnect(
        requestId: Any,
        walletAddress: String,
    ): JSONObject

    suspend fun rejectConnect(
        requestId: Any,
        reason: String? = null,
    ): JSONObject

    suspend fun approveTransaction(requestId: Any): JSONObject

    suspend fun rejectTransaction(
        requestId: Any,
        reason: String? = null,
    ): JSONObject

    suspend fun approveSignData(requestId: Any): JSONObject

    suspend fun rejectSignData(
        requestId: Any,
        reason: String? = null,
    ): JSONObject

    suspend fun listSessions(): List<WalletSession>

    suspend fun disconnectSession(sessionId: String? = null): JSONObject

    suspend fun destroy()
}

enum class WalletKitEngineKind {
    WEBVIEW,
    QUICKJS,
}
