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
@file:Suppress(
    "ArrayInDataClass",
    "EnumEntryName",
    "RemoveRedundantQualifierName",
    "UnusedImport",
)

package io.ton.walletkit.api.generated

import io.ton.walletkit.model.TONUserFriendlyAddress
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 *
 *
 * @param sessionId
 * @param walletId Wallet ID - unique identifier for a wallet, should be different for similar wallets on different networks
 * @param walletAddress
 * @param createdAt
 * @param lastActivityAt
 * @param privateKey
 * @param publicKey
 * @param domain
 * @param schemaVersion
 * @param dAppName Display name of the dApp
 * @param dAppDescription Brief description of the dApp's purpose
 * @param dAppUrl Main website URL of the dApp
 * @param dAppIconUrl Icon/logo URL of the dApp
 * @param isJsBridge
 */
@Serializable
data class TONConnectSession(

    @SerialName(value = "sessionId")
    val sessionId: kotlin.String,

    /* Wallet ID - unique identifier for a wallet, should be different for similar wallets on different networks */
    @SerialName(value = "walletId")
    val walletId: kotlin.String,

    @Contextual @SerialName(value = "walletAddress")
    val walletAddress: io.ton.walletkit.model.TONUserFriendlyAddress,

    @SerialName(value = "createdAt")
    val createdAt: kotlin.String,

    @SerialName(value = "lastActivityAt")
    val lastActivityAt: kotlin.String,

    @SerialName(value = "privateKey")
    val privateKey: kotlin.String,

    @SerialName(value = "publicKey")
    val publicKey: kotlin.String,

    @SerialName(value = "domain")
    val domain: kotlin.String,

    @SerialName(value = "schemaVersion")
    val schemaVersion: kotlin.Int,

    /* Display name of the dApp */
    @SerialName(value = "dAppName")
    val dAppName: kotlin.String? = null,

    /* Brief description of the dApp's purpose */
    @SerialName(value = "dAppDescription")
    val dAppDescription: kotlin.String? = null,

    /* Main website URL of the dApp */
    @SerialName(value = "dAppUrl")
    val dAppUrl: kotlin.String? = null,

    /* Icon/logo URL of the dApp */
    @SerialName(value = "dAppIconUrl")
    val dAppIconUrl: kotlin.String? = null,

    @SerialName(value = "isJsBridge")
    val isJsBridge: kotlin.Boolean? = null,

) {

    companion object
}
