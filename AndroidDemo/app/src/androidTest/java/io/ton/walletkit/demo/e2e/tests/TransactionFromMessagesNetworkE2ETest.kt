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
 * E2E tests for Send Transaction - From, Messages & Network validation.
 *
 * Tests cover:
 * - From validation (address mismatch, invalid, bounceable, HEX, non-bounceable)
 * - Messages validation (empty array, invalid message, maximum messages)
 * - Network validation (testnet, as number, mainnet)
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@Feature("TonConnect")
@Story("Transaction From & Messages & Network")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class TransactionFromMessagesNetworkE2ETest : BaseE2ETest() {

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
    // FROM TESTS
    // =====================================================================

    @Test
    @SendTransactionTest
    @AllureId(AllureTestIds.TX_FROM_ERROR_ADDRESS_MISMATCH)
    @Severity(SeverityLevel.CRITICAL)
    @Description("[from] Error if address doesn't match the user's wallet address")
    fun test01_FromErrorAddressMismatch() {
        runSendTxTest(AllureTestIds.TX_FROM_ERROR_ADDRESS_MISMATCH, expectWalletPrompt = false, ensureConnected = ::ensureConnected)
    }

    @Test
    @SendTransactionTest
    @AllureId(AllureTestIds.TX_FROM_ERROR_INVALID)
    @Severity(SeverityLevel.NORMAL)
    @Description("[from] Error if invalid value")
    fun test02_FromErrorInvalid() {
        runSendTxTest(AllureTestIds.TX_FROM_ERROR_INVALID, expectWalletPrompt = false, ensureConnected = ::ensureConnected)
    }

    @Test
    @SendTransactionTest
    @AllureId(AllureTestIds.TX_FROM_SUCCESS_BOUNCEABLE)
    @Severity(SeverityLevel.CRITICAL)
    @Description("[from] Success if in bounceable format")
    fun test03_FromSuccessBounceable() {
        runSendTxTest(AllureTestIds.TX_FROM_SUCCESS_BOUNCEABLE, ensureConnected = ::ensureConnected)
    }

    @Test
    @SendTransactionTest
    @AllureId(AllureTestIds.TX_FROM_SUCCESS_HEX)
    @Severity(SeverityLevel.NORMAL)
    @Description("[from] Success if in HEX format")
    fun test04_FromSuccessHex() {
        runSendTxTest(AllureTestIds.TX_FROM_SUCCESS_HEX, ensureConnected = ::ensureConnected)
    }

    @Test
    @SendTransactionTest
    @AllureId(AllureTestIds.TX_FROM_SUCCESS_NON_BOUNCEABLE)
    @Severity(SeverityLevel.NORMAL)
    @Description("[from] Success if in non-bounceable format")
    fun test05_FromSuccessNonBounceable() {
        runSendTxTest(AllureTestIds.TX_FROM_SUCCESS_NON_BOUNCEABLE, ensureConnected = ::ensureConnected)
    }

    // =====================================================================
    // MESSAGES TESTS
    // =====================================================================

    @Test
    @SendTransactionTest
    @AllureId(AllureTestIds.TX_MESSAGES_ERROR_EMPTY_ARRAY)
    @Severity(SeverityLevel.CRITICAL)
    @Description("[messages] Error if array is empty")
    fun test06_MessagesErrorEmptyArray() {
        runSendTxTest(AllureTestIds.TX_MESSAGES_ERROR_EMPTY_ARRAY, expectWalletPrompt = false, ensureConnected = ::ensureConnected)
    }

    @Test
    @SendTransactionTest
    @AllureId(AllureTestIds.TX_MESSAGES_ERROR_INVALID_MESSAGE)
    @Severity(SeverityLevel.NORMAL)
    @Description("[messages] Error if contains invalid message")
    fun test07_MessagesErrorInvalidMessage() {
        runSendTxTest(AllureTestIds.TX_MESSAGES_ERROR_INVALID_MESSAGE, expectWalletPrompt = false, ensureConnected = ::ensureConnected)
    }

    @Test
    @SendTransactionTest
    @AllureId(AllureTestIds.TX_MESSAGES_SUCCESS_MAX_MESSAGES)
    @Severity(SeverityLevel.NORMAL)
    @Description("[messages] Success if contains maximum messages")
    fun test08_MessagesSuccessMaxMessages() {
        runSendTxTest(AllureTestIds.TX_MESSAGES_SUCCESS_MAX_MESSAGES, ensureConnected = ::ensureConnected)
    }

    // =====================================================================
    // NETWORK TESTS
    // =====================================================================

    @Test
    @SendTransactionTest
    @AllureId(AllureTestIds.TX_NETWORK_ERROR_TESTNET)
    @Severity(SeverityLevel.CRITICAL)
    @Description("[network] Error if '-3' (testnet)")
    fun test09_NetworkErrorTestnet() {
        runSendTxTest(AllureTestIds.TX_NETWORK_ERROR_TESTNET, expectWalletPrompt = false, ensureConnected = ::ensureConnected)
    }

    @Test
    @SendTransactionTest
    @AllureId(AllureTestIds.TX_NETWORK_ERROR_AS_NUMBER)
    @Severity(SeverityLevel.NORMAL)
    @Description("[network] Error if as a number")
    fun test10_NetworkErrorAsNumber() {
        runSendTxTest(AllureTestIds.TX_NETWORK_ERROR_AS_NUMBER, expectWalletPrompt = false, ensureConnected = ::ensureConnected)
    }

    @Test
    @SendTransactionTest
    @AllureId(AllureTestIds.TX_NETWORK_SUCCESS_MAINNET)
    @Severity(SeverityLevel.CRITICAL)
    @Description("[network] Success if '-239' (mainnet)")
    fun test11_NetworkSuccessMainnet() {
        runSendTxTest(AllureTestIds.TX_NETWORK_SUCCESS_MAINNET, ensureConnected = ::ensureConnected)
    }
}
