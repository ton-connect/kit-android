package io.ton.walletkit.data.model

/**
 * Complete session data including private keys for TonConnect sessions.
 * Based on TypeScript SessionData interface.
 *
 * @property sessionId Unique session identifier
 * @property walletAddress Associated wallet address
 * @property createdAt ISO 8601 timestamp of session creation
 * @property lastActivityAt ISO 8601 timestamp of last activity
 * @property privateKey Session private key (hex string) - CRITICAL security data
 * @property publicKey Session public key (hex string)
 * @property dAppName Display name of the dApp
 * @property dAppDescription Description of the dApp
 * @property domain Domain of the dApp
 * @property dAppIconUrl Icon URL of the dApp
 * @suppress Internal storage model. Not part of public API.
 */
internal data class StoredSessionData(
    val sessionId: String,
    val walletAddress: String,
    val createdAt: String,
    val lastActivityAt: String,
    // CRITICAL - must be encrypted
    val privateKey: String,
    val publicKey: String,
    val dAppName: String,
    val dAppDescription: String,
    val domain: String,
    val dAppIconUrl: String,
)
