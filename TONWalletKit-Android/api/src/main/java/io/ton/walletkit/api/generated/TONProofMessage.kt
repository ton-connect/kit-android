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

import io.ton.walletkit.model.TONBase64
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Message structure used for TON Connect proof of ownership.
 *
 * @param workchain Workchain ID of the wallet address
 * @param addressHash
 * @param timestamp Unix timestamp when the proof was created
 * @param domain
 * @param payload Payload string to be signed
 * @param stateInit
 * @param signature
 */
@Serializable
data class TONProofMessage(

    /* Workchain ID of the wallet address */
    @SerialName(value = "workchain")
    val workchain: kotlin.Int,

    @Contextual @SerialName(value = "addressHash")
    val addressHash: io.ton.walletkit.model.TONHex,

    /* Unix timestamp when the proof was created */
    @SerialName(value = "timestamp")
    val timestamp: kotlin.Int,

    @SerialName(value = "domain")
    val domain: TONProofMessageDomain,

    /* Payload string to be signed */
    @SerialName(value = "payload")
    val payload: kotlin.String,

    @Contextual @SerialName(value = "stateInit")
    val stateInit: io.ton.walletkit.model.TONBase64,

    @Contextual @SerialName(value = "signature")
    val signature: io.ton.walletkit.model.TONHex? = null,

) {

    companion object
}
