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
 * @param isSkipped Whether the compute phase was skipped
 * @param isSuccess Whether the TVM execution succeeded
 * @param isMsgStateUsed Whether the message state was used during compute
 * @param isAccountActivated Whether the account was activated during compute
 * @param gasFees
 * @param gasUsed Total gas consumed
 * @param gasLimit Gas limit for this execution
 * @param mode Compute execution mode
 * @param exitCode TVM exit code
 * @param vmSteps Number of TVM steps executed
 * @param gasCredit Gas credit, if any
 * @param vmInitStateHash
 * @param vmFinalStateHash
 */
@Serializable
data class TONEmulationComputePhase(

    /* Whether the compute phase was skipped */
    @SerialName(value = "isSkipped")
    val isSkipped: kotlin.Boolean,

    /* Whether the TVM execution succeeded */
    @SerialName(value = "isSuccess")
    val isSuccess: kotlin.Boolean,

    /* Whether the message state was used during compute */
    @SerialName(value = "isMsgStateUsed")
    val isMsgStateUsed: kotlin.Boolean,

    /* Whether the account was activated during compute */
    @SerialName(value = "isAccountActivated")
    val isAccountActivated: kotlin.Boolean,

    @SerialName(value = "gasFees")
    val gasFees: kotlin.String,

    /* Total gas consumed */
    @SerialName(value = "gasUsed")
    val gasUsed: kotlin.String,

    /* Gas limit for this execution */
    @SerialName(value = "gasLimit")
    val gasLimit: kotlin.String,

    /* Compute execution mode */
    @SerialName(value = "mode")
    val mode: kotlin.Int,

    /* TVM exit code */
    @SerialName(value = "exitCode")
    val exitCode: kotlin.Int,

    /* Number of TVM steps executed */
    @SerialName(value = "vmSteps")
    val vmSteps: kotlin.Int,

    /* Gas credit, if any */
    @SerialName(value = "gasCredit")
    val gasCredit: kotlin.String? = null,

    @Contextual @SerialName(value = "vmInitStateHash")
    val vmInitStateHash: io.ton.walletkit.model.TONHex? = null,

    @Contextual @SerialName(value = "vmFinalStateHash")
    val vmFinalStateHash: io.ton.walletkit.model.TONHex? = null,

) {

    companion object
}
