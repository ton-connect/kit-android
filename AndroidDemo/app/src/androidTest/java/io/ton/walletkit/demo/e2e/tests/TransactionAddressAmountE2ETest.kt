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
package io.ton.walletkit.demo.e2e.tests

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.Description
import io.qameta.allure.kotlin.Feature
import io.qameta.allure.kotlin.Severity
import io.qameta.allure.kotlin.SeverityLevel
import io.qameta.allure.kotlin.Story
import io.ton.walletkit.demo.e2e.infrastructure.AllureTestIds
import io.ton.walletkit.demo.e2e.infrastructure.BaseE2ETest
import io.ton.walletkit.demo.e2e.infrastructure.SendTransactionTest
import io.ton.walletkit.demo.presentation.MainActivity
import org.junit.AfterClass
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

/**
 * E2E tests for Send Transaction - Address & Amount validation.
 *
 * Tests cover:
 * - Address validation (absent, invalid, HEX format, bounceable, non-bounceable)
 * - Amount validation (absent, as number, insufficient balance, zero, as string)
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@Feature("TonConnect")
@Story("Transaction Address & Amount")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class TransactionAddressAmountE2ETest : BaseE2ETest() {

    companion object {
        @Volatile
        private var isConnected = false

        @JvmStatic
        @AfterClass
        fun tearDownClass() {
            cleanupAllData()
        }
    }

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    override fun setUp() {
        super.setUp()
        waitForActivityReady(composeTestRule)
        walletController.init(composeTestRule)
        dAppController.init(composeTestRule)
        ensureWalletReady()
    }

    private fun ensureConnected() {
        if (isConnected) return

        dAppController.openBrowser()
        dAppController.waitForDAppPage()
        dAppController.clickSendTransaction()
        val connectUrl = dAppController.clickCopyLinkInModal()
        dAppController.closeBrowserFully()
        walletController.connectByUrl(connectUrl)
        waitFor(timeoutMs = SHEET_APPEAR_TIMEOUT_MS) { walletController.isConnectRequestVisible() }
        walletController.approveConnect()
        waitFor(timeoutMs = UI_DISMISS_TIMEOUT_MS) { !walletController.isConnectRequestVisible() }
        isConnected = true
    }

    // =====================================================================
    // ADDRESS TESTS
    // =====================================================================

    @Test
    @SendTransactionTest
    @AllureId(AllureTestIds.TX_ADDRESS_ERROR_ABSENT)
    @Severity(SeverityLevel.CRITICAL)
    @Description("[address] Error if absent")
    fun test01_AddressErrorIfAbsent() {
        runSendTxTest(AllureTestIds.TX_ADDRESS_ERROR_ABSENT, expectWalletPrompt = false, ensureConnected = ::ensureConnected)
    }

    @Test
    @SendTransactionTest
    @AllureId(AllureTestIds.TX_ADDRESS_ERROR_HEX_FORMAT)
    @Severity(SeverityLevel.NORMAL)
    @Description("[address] Error if in HEX format")
    fun test02_AddressErrorIfHexFormat() {
        runSendTxTest(AllureTestIds.TX_ADDRESS_ERROR_HEX_FORMAT, expectWalletPrompt = false, ensureConnected = ::ensureConnected)
    }

    @Test
    @SendTransactionTest
    @AllureId(AllureTestIds.TX_ADDRESS_ERROR_INVALID)
    @Severity(SeverityLevel.NORMAL)
    @Description("[address] Error if invalid value")
    fun test03_AddressErrorIfInvalid() {
        runSendTxTest(AllureTestIds.TX_ADDRESS_ERROR_INVALID, expectWalletPrompt = false, ensureConnected = ::ensureConnected)
    }

    @Test
    @SendTransactionTest
    @AllureId(AllureTestIds.TX_ADDRESS_SUCCESS_BOUNCEABLE)
    @Severity(SeverityLevel.CRITICAL)
    @Description("[address] Success if in bounceable format")
    fun test04_AddressSuccessBounceable() {
        runSendTxTest(AllureTestIds.TX_ADDRESS_SUCCESS_BOUNCEABLE, ensureConnected = ::ensureConnected)
    }

    @Test
    @SendTransactionTest
    @AllureId(AllureTestIds.TX_ADDRESS_SUCCESS_NON_BOUNCEABLE)
    @Severity(SeverityLevel.CRITICAL)
    @Description("[address] Success if in non-bounceable format")
    fun test05_AddressSuccessNonBounceable() {
        runSendTxTest(AllureTestIds.TX_ADDRESS_SUCCESS_NON_BOUNCEABLE, ensureConnected = ::ensureConnected)
    }

    // =====================================================================
    // AMOUNT TESTS
    // =====================================================================

    @Test
    @SendTransactionTest
    @AllureId(AllureTestIds.TX_AMOUNT_ERROR_ABSENT)
    @Severity(SeverityLevel.CRITICAL)
    @Description("[amount] Error if absent")
    fun test06_AmountErrorIfAbsent() {
        runSendTxTest(AllureTestIds.TX_AMOUNT_ERROR_ABSENT, expectWalletPrompt = false, ensureConnected = ::ensureConnected)
    }

    @Test
    @SendTransactionTest
    @AllureId(AllureTestIds.TX_AMOUNT_ERROR_AS_NUMBER)
    @Severity(SeverityLevel.NORMAL)
    @Description("[amount] Error if as a number")
    fun test07_AmountErrorAsNumber() {
        runSendTxTest(AllureTestIds.TX_AMOUNT_ERROR_AS_NUMBER, expectWalletPrompt = false, ensureConnected = ::ensureConnected)
    }

    @Test
    @SendTransactionTest
    @AllureId(AllureTestIds.TX_AMOUNT_ERROR_INSUFFICIENT_BALANCE)
    @Severity(SeverityLevel.NORMAL)
    @Description("[amount] Error if insufficient balance")
    fun test08_AmountErrorInsufficientBalance() {
        // Insufficient balance is auto-rejected by demo app (wallet UI should NOT appear)
        // expectSdkError=false because this is a wallet-app rejection, not SDK validation error
        runSendTxTest(AllureTestIds.TX_AMOUNT_ERROR_INSUFFICIENT_BALANCE, expectWalletPrompt = false, expectSdkError = false, ensureConnected = ::ensureConnected)
    }

    @Test
    @SendTransactionTest
    @AllureId(AllureTestIds.TX_AMOUNT_SUCCESS_ZERO)
    @Severity(SeverityLevel.NORMAL)
    @Description("[amount] Success if '0'")
    fun test09_AmountSuccessZero() {
        runSendTxTest(AllureTestIds.TX_AMOUNT_SUCCESS_ZERO, ensureConnected = ::ensureConnected)
    }

    @Test
    @SendTransactionTest
    @AllureId(AllureTestIds.TX_AMOUNT_SUCCESS_AS_STRING)
    @Severity(SeverityLevel.CRITICAL)
    @Description("[amount] Success if as a string")
    fun test10_AmountSuccessAsString() {
        runSendTxTest(AllureTestIds.TX_AMOUNT_SUCCESS_AS_STRING, ensureConnected = ::ensureConnected)
    }
}
