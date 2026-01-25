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

import io.ton.walletkit.model.TONUserFriendlyAddress
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a TONConnect session between the wallet and a dApp.
 *
 * This model is used internally for session management and matches
 * the structure expected by the JavaScript bridge.
 */
@Serializable
data class TONConnectSession(
    val sessionId: String,
    /**
     * Wallet ID format: "network:address"
     * Examples:
     * - "-239:EQDtFpEwcFAEcRe5mLVh2N6C0x-_hJEM7W61_JLnSF74p4q2" (mainnet)
     * - "-3:EQDtFpEwcFAEcRe5mLVh2N6C0x-_hJEM7W61_JLnSF74p4q2" (testnet)
     */
    val walletId: String,
    val walletAddress: TONUserFriendlyAddress,
    val createdAt: String,
    val lastActivityAt: String,
    val privateKey: String,
    val publicKey: String,
    val domain: String,
    val schemaVersion: Int,
    /** Display name of the dApp */
    val dAppName: String? = null,
    /** Brief description of the dApp's purpose */
    val dAppDescription: String? = null,
    /** Main website URL of the dApp */
    val dAppUrl: String? = null,
    /** Icon/logo URL of the dApp */
    val dAppIconUrl: String? = null,
    @SerialName("isJsBridge")
    val isJsBridge: Boolean? = null,
)
