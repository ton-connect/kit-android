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
 * E2E tests for Send Transaction - Payload, StateInit, ValidUntil validation.
 *
 * Tests cover:
 * - Payload validation (invalid, absent, valid)
 * - StateInit validation (invalid, absent, valid)
 * - ValidUntil validation (as string, expired, expired during confirm, NaN, NULL, absent, less/more than 5 min)
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@Feature("TonConnect")
@Story("Transaction Payload & StateInit & ValidUntil")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class TransactionPayloadStateInitValidUntilE2ETest : BaseE2ETest() {

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
        composeTestRule.waitForIdle()
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
        waitFor(timeoutMs = 10000) { walletController.isConnectRequestVisible() }
        walletController.approveConnect()
        waitFor(timeoutMs = 5000) { !walletController.isConnectRequestVisible() }
        isConnected = true
    }

    // =====================================================================
    // PAYLOAD TESTS
    // =====================================================================

    @Test
    @SendTransactionTest
    @AllureId(AllureTestIds.TX_PAYLOAD_ERROR_INVALID)
    @Severity(SeverityLevel.NORMAL)
    @Description("[payload] Error if invalid value")
    fun test01_PayloadErrorInvalid() {
        runSendTxTest(AllureTestIds.TX_PAYLOAD_ERROR_INVALID, expectWalletPrompt = false, ensureConnected = ::ensureConnected)
    }

    @Test
    @SendTransactionTest
    @AllureId(AllureTestIds.TX_PAYLOAD_SUCCESS_ABSENT)
    @Severity(SeverityLevel.CRITICAL)
    @Description("[payload] Success if absent")
    fun test02_PayloadSuccessAbsent() {
        runSendTxTest(AllureTestIds.TX_PAYLOAD_SUCCESS_ABSENT, ensureConnected = ::ensureConnected)
    }

    @Test
    @SendTransactionTest
    @AllureId(AllureTestIds.TX_PAYLOAD_SUCCESS_VALID)
    @Severity(SeverityLevel.CRITICAL)
    @Description("[payload] Success if valid value")
    fun test03_PayloadSuccessValid() {
        runSendTxTest(AllureTestIds.TX_PAYLOAD_SUCCESS_VALID, ensureConnected = ::ensureConnected)
    }

    // =====================================================================
    // STATEINIT TESTS
    // =====================================================================

    @Test
    @SendTransactionTest
    @AllureId(AllureTestIds.TX_STATEINIT_ERROR_INVALID)
    @Severity(SeverityLevel.NORMAL)
    @Description("[stateInit] Error if invalid value")
    fun test04_StateInitErrorInvalid() {
        runSendTxTest(AllureTestIds.TX_STATEINIT_ERROR_INVALID, expectWalletPrompt = false, ensureConnected = ::ensureConnected)
    }

    @Test
    @SendTransactionTest
    @AllureId(AllureTestIds.TX_STATEINIT_SUCCESS_ABSENT)
    @Severity(SeverityLevel.CRITICAL)
    @Description("[stateInit] Success if absent")
    fun test05_StateInitSuccessAbsent() {
        runSendTxTest(AllureTestIds.TX_STATEINIT_SUCCESS_ABSENT, ensureConnected = ::ensureConnected)
    }

    @Test
    @SendTransactionTest
    @AllureId(AllureTestIds.TX_STATEINIT_SUCCESS_VALID)
    @Severity(SeverityLevel.CRITICAL)
    @Description("[stateInit] Success if valid value")
    fun test06_StateInitSuccessValid() {
        runSendTxTest(AllureTestIds.TX_STATEINIT_SUCCESS_VALID, ensureConnected = ::ensureConnected)
    }

    // =====================================================================
    // VALIDUNTIL TESTS
    // =====================================================================

    @Test
    @SendTransactionTest
    @AllureId(AllureTestIds.TX_VALIDUNTIL_ERROR_AS_STRING)
    @Severity(SeverityLevel.NORMAL)
    @Description("[validUntil] Error if as a string")
    fun test07_ValidUntilErrorAsString() {
        runSendTxTest(AllureTestIds.TX_VALIDUNTIL_ERROR_AS_STRING, expectWalletPrompt = false, ensureConnected = ::ensureConnected)
    }

    @Test
    @SendTransactionTest
    @AllureId(AllureTestIds.TX_VALIDUNTIL_ERROR_EXPIRED)
    @Severity(SeverityLevel.CRITICAL)
    @Description("[validUntil] Error if expired")
    fun test08_ValidUntilErrorExpired() {
        runSendTxTest(AllureTestIds.TX_VALIDUNTIL_ERROR_EXPIRED, expectWalletPrompt = false, ensureConnected = ::ensureConnected)
    }

    @Test
    @SendTransactionTest
    @AllureId(AllureTestIds.TX_VALIDUNTIL_ERROR_EXPIRED_DURING_CONFIRM)
    @Severity(SeverityLevel.NORMAL)
    @Description("[validUntil] Error if has expired during confirmation")
    fun test09_ValidUntilErrorExpiredDuringConfirm() {
        // This test requires a delay before approving to let it expire
        runSendTxTest(AllureTestIds.TX_VALIDUNTIL_ERROR_EXPIRED_DURING_CONFIRM, expectWalletPrompt = true, ensureConnected = ::ensureConnected)
    }

    @Test
    @SendTransactionTest
    @AllureId(AllureTestIds.TX_VALIDUNTIL_NAN)
    @Severity(SeverityLevel.NORMAL)
    @Description("[validUntil] if NaN")
    fun test10_ValidUntilNaN() {
        runSendTxTest(AllureTestIds.TX_VALIDUNTIL_NAN, expectWalletPrompt = false, ensureConnected = ::ensureConnected)
    }

    @Test
    @SendTransactionTest
    @AllureId(AllureTestIds.TX_VALIDUNTIL_NULL)
    @Severity(SeverityLevel.NORMAL)
    @Description("[validUntil] if NULL")
    fun test11_ValidUntilNull() {
        runSendTxTest(AllureTestIds.TX_VALIDUNTIL_NULL, ensureConnected = ::ensureConnected)
    }

    @Test
    @SendTransactionTest
    @AllureId(AllureTestIds.TX_VALIDUNTIL_SUCCESS_ABSENT)
    @Severity(SeverityLevel.CRITICAL)
    @Description("[validUntil] Success if absent")
    fun test12_ValidUntilSuccessAbsent() {
        runSendTxTest(AllureTestIds.TX_VALIDUNTIL_SUCCESS_ABSENT, ensureConnected = ::ensureConnected)
    }

    @Test
    @SendTransactionTest
    @AllureId(AllureTestIds.TX_VALIDUNTIL_SUCCESS_LESS_THAN_5_MIN)
    @Severity(SeverityLevel.NORMAL)
    @Description("[validUntil] Success if less than in 5 minutes")
    fun test13_ValidUntilSuccessLessThan5Min() {
        runSendTxTest(AllureTestIds.TX_VALIDUNTIL_SUCCESS_LESS_THAN_5_MIN, ensureConnected = ::ensureConnected)
    }

    @Test
    @SendTransactionTest
    @AllureId(AllureTestIds.TX_VALIDUNTIL_SUCCESS_MORE_THAN_5_MIN)
    @Severity(SeverityLevel.NORMAL)
    @Description("[validUntil] Success if more then in 5 minutes")
    fun test14_ValidUntilSuccessMoreThan5Min() {
        runSendTxTest(AllureTestIds.TX_VALIDUNTIL_SUCCESS_MORE_THAN_5_MIN, ensureConnected = ::ensureConnected)
    }
}
