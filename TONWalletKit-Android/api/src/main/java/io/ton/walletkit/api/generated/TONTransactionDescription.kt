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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Detailed description of transaction execution phases.
 *
 * @param type The transaction type (e.g., tick-tock, ord, split-prepare)
 * @param isAborted The flag indicating if the transaction was aborted
 * @param isDestroyed The flag indicating if the account was destroyed
 * @param isCreditFirst The flag indicating if the credit phase was executed first
 * @param isTock The flag indicating if this was a tock transaction
 * @param isInstalled The flag indicating if the contract was installed
 * @param storagePhase
 * @param creditPhase
 * @param computePhase
 * @param action
 */
@Serializable
data class TONTransactionDescription(

    /* The transaction type (e.g., tick-tock, ord, split-prepare) */
    @SerialName(value = "type")
    val type: kotlin.String,

    /* The flag indicating if the transaction was aborted */
    @SerialName(value = "isAborted")
    val isAborted: kotlin.Boolean,

    /* The flag indicating if the account was destroyed */
    @SerialName(value = "isDestroyed")
    val isDestroyed: kotlin.Boolean,

    /* The flag indicating if the credit phase was executed first */
    @SerialName(value = "isCreditFirst")
    val isCreditFirst: kotlin.Boolean,

    /* The flag indicating if this was a tock transaction */
    @SerialName(value = "isTock")
    val isTock: kotlin.Boolean,

    /* The flag indicating if the contract was installed */
    @SerialName(value = "isInstalled")
    val isInstalled: kotlin.Boolean,

    @SerialName(value = "storagePhase")
    val storagePhase: TONTransactionStoragePhase? = null,

    @SerialName(value = "creditPhase")
    val creditPhase: TONTransactionCreditPhase? = null,

    @SerialName(value = "computePhase")
    val computePhase: TONTransactionComputePhase? = null,

    @SerialName(value = "action")
    val action: TONTransactionAction? = null,

) {

    companion object
}
