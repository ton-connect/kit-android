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

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Compute phase of transaction execution (TVM execution).
 *
 * @param isSkipped The flag indicating if the compute phase was skipped
 * @param isSuccess The success state of the compute phase
 * @param isMessageStateUsed The flag indicating if message state was used
 * @param isAccountActivated The flag indicating if the account was activated during compute
 * @param gasFees
 * @param gasUsed
 * @param gasLimit
 * @param gasCredit
 * @param mode The compute execution mode
 * @param exitCode The exit code returned from the VM
 * @param vmStepsNumber The number of steps executed by the VM
 * @param vmInitStateHash
 * @param vmFinalStateHash
 */
@Serializable
data class TONTransactionComputePhase(

    /* The flag indicating if the compute phase was skipped */
    @SerialName(value = "isSkipped")
    val isSkipped: kotlin.Boolean? = null,

    /* The success state of the compute phase */
    @SerialName(value = "isSuccess")
    val isSuccess: kotlin.Boolean? = null,

    /* The flag indicating if message state was used */
    @SerialName(value = "isMessageStateUsed")
    val isMessageStateUsed: kotlin.Boolean? = null,

    /* The flag indicating if the account was activated during compute */
    @SerialName(value = "isAccountActivated")
    val isAccountActivated: kotlin.Boolean? = null,

    @SerialName(value = "gasFees")
    val gasFees: kotlin.String? = null,

    @SerialName(value = "gasUsed")
    val gasUsed: kotlin.String? = null,

    @SerialName(value = "gasLimit")
    val gasLimit: kotlin.String? = null,

    @SerialName(value = "gasCredit")
    val gasCredit: kotlin.String? = null,

    /* The compute execution mode */
    @SerialName(value = "mode")
    val mode: kotlin.Int? = null,

    /* The exit code returned from the VM */
    @SerialName(value = "exitCode")
    val exitCode: kotlin.Int? = null,

    /* The number of steps executed by the VM */
    @SerialName(value = "vmStepsNumber")
    val vmStepsNumber: kotlin.Int? = null,

    @Contextual @SerialName(value = "vmInitStateHash")
    val vmInitStateHash: io.ton.walletkit.model.TONHex? = null,

    @Contextual @SerialName(value = "vmFinalStateHash")
    val vmFinalStateHash: io.ton.walletkit.model.TONHex? = null,

) {

    companion object
}
