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
 * State of an account at a specific point in time.
 *
 * @param hash The state hash of the account
 * @param balance
 * @param extraCurrencies Map of extra currency IDs to their amounts. Extra currencies are additional tokens that can be attached to TON messages.
 * @param accountStatus
 * @param frozenHash The hash of the frozen account state, if the account is frozen
 * @param dataHash The hash of the contract's data section
 * @param codeHash The hash of the smart contract code
 */
@Serializable
data class TONAccountState(

    /* The state hash of the account */
    @SerialName(value = "hash")
    val hash: kotlin.String,

    @SerialName(value = "balance")
    val balance: kotlin.String,

    /* Map of extra currency IDs to their amounts. Extra currencies are additional tokens that can be attached to TON messages. */
    @SerialName(value = "extraCurrencies")
    val extraCurrencies: kotlin.collections.Map<kotlin.String, kotlin.String>? = null,

    @SerialName(value = "accountStatus")
    val accountStatus: TONAccountStatus? = null,

    /* The hash of the frozen account state, if the account is frozen */
    @SerialName(value = "frozenHash")
    val frozenHash: kotlin.String? = null,

    /* The hash of the contract's data section */
    @SerialName(value = "dataHash")
    val dataHash: kotlin.String? = null,

    /* The hash of the smart contract code */
    @SerialName(value = "codeHash")
    val codeHash: kotlin.String? = null,

) {

    companion object
}
