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
import io.ton.walletkit.demo.e2e.infrastructure.TestCaseDataProvider
import io.ton.walletkit.demo.presentation.MainActivity
import org.junit.AfterClass
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

/**
 * E2E tests for Send Transaction functionality via HTTP bridge (normal WebView).
 *
 * Flow for first test:
 * 1. Open browser, fill sendTx precondition and expected result fields
 * 2. Click "Connect and Send Transaction" button - opens TonConnect modal
 * 3. Copy the TonConnect URL from the modal
 * 4. Close browser, paste URL into wallet, approve connect request
 * 5. Re-open browser, click "Send Transaction" button (now connected)
 * 6. Approve/reject transaction in wallet
 * 7. Re-open browser and verify the validation result
 *
 * Flow for subsequent tests (already connected):
 * 1. Open browser, fill test data
 * 2. Click "Send Transaction" button
 * 3. Approve/reject transaction in wallet
 * 4. Re-open browser and verify validation result
 *
 * Tests are ordered to run approve first (which establishes connection), then reject.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@Feature("TonConnect")
@Story("Send Transaction")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class SendTransactionE2ETest : BaseE2ETest() {

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

        // Ensure wallet is ready (handles setup/unlock automatically)
        ensureWalletReady()
    }

    /**
     * Connect to the dApp if not already connected within this test class.
     * Each test class starts fresh thanks to @AfterClass cleanup.
     */
    private fun ensureConnected() {
        if (isConnected) return

        dAppController.openBrowser()
        dAppController.waitForDAppPage()

        // Click Send Transaction to initiate connection
        dAppController.clickSendTransaction()

        val connectUrl = dAppController.clickCopyLinkInModal()

        dAppController.closeBrowserFully()
        walletController.connectByUrl(connectUrl)

        waitFor(timeoutMs = 10000) { walletController.isConnectRequestVisible() }
        walletController.approveConnect()
        waitFor(timeoutMs = 5000) { !walletController.isConnectRequestVisible() }

        isConnected = true
    }

    @Test
    @SendTransactionTest
    @AllureId(AllureTestIds.TX_IN_WALLET_BROWSER)
    @Severity(SeverityLevel.CRITICAL)
    @Description("Test that approving a send transaction request returns success")
    fun test1_SendTransactionApprove() {
        val testData = TestCaseDataProvider.getTestCaseData(AllureTestIds.TX_IN_WALLET_BROWSER, allureClient)
        val precondition = testData?.precondition ?: ""
        val expectedResult = testData?.expectedResult ?: ""

        step("Ensure wallet is connected to dApp") {
            ensureConnected()
        }

        step("Open browser and fill sendTx test data") {
            dAppController.openBrowser()
            dAppController.waitForDAppPage()
            if (precondition.isNotBlank()) dAppController.fillSendTxPrecondition(precondition)
            if (expectedResult.isNotBlank()) dAppController.fillSendTxExpectedResult(expectedResult)
        }

        step("Click Send Transaction button") {
            dAppController.clickSendTransaction()
        }

        step("Wait for transaction request") {
            waitFor(timeoutMs = 15000) { walletController.isTransactionRequestVisible() }
        }

        step("Approve transaction") {
            walletController.approveTransaction()
        }

        step("Verify transaction result in browser") {
            waitFor(timeoutMs = 5000) { !walletController.isTransactionRequestVisible() }
            Thread.sleep(2000)

            dAppController.openBrowser()
            dAppController.waitForDAppPage()
            dAppController.scrollToSendTxValidation()
            val isValid = dAppController.verifySendTxValidation()
            val validationText = dAppController.getSendTxValidationResult()

            dAppController.closeBrowserFully()
            assert(isValid) { "Send Transaction validation failed - got: $validationText" }
        }
    }

    @Test
    @SendTransactionTest
    @AllureId(AllureTestIds.TX_USER_DECLINED)
    @Severity(SeverityLevel.CRITICAL)
    @Description("Test that rejecting a send transaction request returns appropriate error")
    fun test2_SendTransactionReject() {
        val testData = TestCaseDataProvider.getTestCaseData(AllureTestIds.TX_USER_DECLINED, allureClient)
        val precondition = testData?.precondition ?: ""
        val expectedResult = testData?.expectedResult ?: ""

        step("Ensure wallet is connected to dApp") {
            ensureConnected()
        }

        step("Open browser and fill sendTx test data") {
            dAppController.openBrowser()
            dAppController.waitForDAppPage()
            if (precondition.isNotBlank()) dAppController.fillSendTxPrecondition(precondition)
            if (expectedResult.isNotBlank()) dAppController.fillSendTxExpectedResult(expectedResult)
        }

        step("Click Send Transaction button") {
            dAppController.clickSendTransaction()
        }

        step("Wait for transaction request") {
            waitFor(timeoutMs = 15000) { walletController.isTransactionRequestVisible() }
        }

        step("Reject transaction") {
            walletController.rejectTransaction()
        }

        step("Verify transaction reject result in browser") {
            waitFor(timeoutMs = 5000) { !walletController.isTransactionRequestVisible() }
            Thread.sleep(2000)

            dAppController.openBrowser()
            dAppController.waitForDAppPage()
            dAppController.scrollToSendTxValidation()
            val isValid = dAppController.verifySendTxValidation()
            val validationText = dAppController.getSendTxValidationResult()

            dAppController.closeBrowserFully()
            assert(isValid) { "Send Transaction reject validation failed - got: $validationText" }
        }
    }
}
