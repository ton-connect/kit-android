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
 * Detailed description of all execution phases in an emulated transaction.
 *
 * @param type Transaction type (e.g. \"ord\", \"ticktock\", \"storage\")
 * @param isAborted Whether the transaction was aborted
 * @param isDestroyed Whether the account was destroyed by this transaction
 * @param isCreditFirst Whether the credit phase was executed before the storage phase
 * @param isTock Whether this was a tock transaction
 * @param isInstalled Whether a contract was installed in this transaction
 * @param storagePhase
 * @param computePhase
 * @param creditPhase
 * @param actionPhase
 */
@Serializable
data class TONEmulationTransactionDescription(

    /* Transaction type (e.g. \"ord\", \"ticktock\", \"storage\") */
    @SerialName(value = "type")
    val type: kotlin.String,

    /* Whether the transaction was aborted */
    @SerialName(value = "isAborted")
    val isAborted: kotlin.Boolean,

    /* Whether the account was destroyed by this transaction */
    @SerialName(value = "isDestroyed")
    val isDestroyed: kotlin.Boolean,

    /* Whether the credit phase was executed before the storage phase */
    @SerialName(value = "isCreditFirst")
    val isCreditFirst: kotlin.Boolean,

    /* Whether this was a tock transaction */
    @SerialName(value = "isTock")
    val isTock: kotlin.Boolean,

    /* Whether a contract was installed in this transaction */
    @SerialName(value = "isInstalled")
    val isInstalled: kotlin.Boolean,

    @SerialName(value = "storagePhase")
    val storagePhase: TONEmulationStoragePhase,

    @SerialName(value = "computePhase")
    val computePhase: TONEmulationComputePhase,

    @SerialName(value = "creditPhase")
    val creditPhase: TONEmulationCreditPhase? = null,

    @SerialName(value = "actionPhase")
    val actionPhase: TONEmulationActionPhase? = null,

) {

    companion object
}
