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

import android.util.Log
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.Description
import io.qameta.allure.kotlin.Feature
import io.qameta.allure.kotlin.Severity
import io.qameta.allure.kotlin.SeverityLevel
import io.qameta.allure.kotlin.Story
import io.ton.walletkit.demo.e2e.infrastructure.BaseE2ETest
import io.ton.walletkit.demo.e2e.infrastructure.SendTransactionTest
import io.ton.walletkit.demo.e2e.infrastructure.TestCaseDataProvider
import io.ton.walletkit.demo.presentation.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * E2E tests for Send Transaction functionality.
 *
 * These tests verify the send transaction flow works correctly by:
 * 1. Connecting to the test dApp
 * 2. Filling in precondition and expected result from test data
 * 3. Triggering send transaction
 * 4. Approving or rejecting in the wallet
 * 5. Validating the result matches expected outcome
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@Feature("TonConnect")
@Story("Send Transaction")
class SendTransactionE2ETest : BaseE2ETest() {

    companion object {
        private const val TAG = "SendTransactionE2ETest"
        private const val SEND_TX_APPROVE_TEST_ID = "2297"
        private const val SEND_TX_REJECT_TEST_ID = "2298"
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

    @Test
    @SendTransactionTest
    @AllureId("2297")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Test that approving a send transaction request returns success")
    fun testSendTransactionApprove() {
        Log.d(TAG, "Starting Send Transaction Approve test")

        // Fetch test case data
        val testData = TestCaseDataProvider.getTestCaseData(SEND_TX_APPROVE_TEST_ID, allureClient)
        val precondition = testData?.precondition ?: ""
        val expectedResult = testData?.expectedResult ?: ""
        Log.d(TAG, "Test data - precondition: ${precondition.take(100)}..., expectedResult: ${expectedResult.take(100)}...")

        // Setup wallet and connect to dApp
        connectToDApp(precondition, expectedResult, "sendTx")

        // Scroll to send transaction section
        step("Scroll to Send Transaction section") {
            dAppController.scrollToSendTxValidation()
            Thread.sleep(1000)
        }

        // Click send transaction button
        step("Click Send Transaction button") {
            dAppController.clickSendTransaction()
            Thread.sleep(2000)
        }

        // Approve in wallet
        step("Approve transaction in wallet") {
            walletController.approveTransaction()
            Thread.sleep(3000)
        }

        // Verify result
        step("Verify send transaction result") {
            val isValid = dAppController.verifySendTxValidation()
            assert(isValid) { "Send Transaction validation failed - expected 'Validation Passed'" }
            Log.d(TAG, "✅ Send Transaction Approve test passed!")
        }
    }

    @Test
    @SendTransactionTest
    @AllureId("2298")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Test that rejecting a send transaction request returns appropriate error")
    fun testSendTransactionReject() {
        Log.d(TAG, "Starting Send Transaction Reject test")

        // Fetch test case data
        val testData = TestCaseDataProvider.getTestCaseData(SEND_TX_REJECT_TEST_ID, allureClient)
        val precondition = testData?.precondition ?: ""
        val expectedResult = testData?.expectedResult ?: ""
        Log.d(TAG, "Test data - precondition: ${precondition.take(100)}..., expectedResult: ${expectedResult.take(100)}...")

        // Setup wallet and connect to dApp
        connectToDApp(precondition, expectedResult, "sendTx")

        // Scroll to send transaction section
        step("Scroll to Send Transaction section") {
            dAppController.scrollToSendTxValidation()
            Thread.sleep(1000)
        }

        // Click send transaction button
        step("Click Send Transaction button") {
            dAppController.clickSendTransaction()
            Thread.sleep(2000)
        }

        // Reject in wallet
        step("Reject transaction in wallet") {
            walletController.rejectTransaction()
            Thread.sleep(3000)
        }

        // Verify result
        step("Verify send transaction reject result") {
            val isValid = dAppController.verifySendTxValidation()
            assert(isValid) { "Send Transaction reject validation failed - expected 'Validation Passed'" }
            Log.d(TAG, "✅ Send Transaction Reject test passed!")
        }
    }

    /**
     * Connect to dApp and fill in test data fields.
     * Wallet is already set up in setUp(), so we just need to connect.
     */
    private fun connectToDApp(precondition: String, expectedResult: String, testType: String) {
        step("Get TonConnect URL from dApp") {
            dAppController.openBrowser()
            dAppController.waitForDAppPage()

            // Fill precondition and expected result for the specific test type
            when (testType) {
                "sendTx" -> {
                    if (precondition.isNotBlank()) {
                        Log.d(TAG, "Filling send tx precondition...")
                        dAppController.fillSendTxPrecondition(precondition)
                    }
                    if (expectedResult.isNotBlank()) {
                        Log.d(TAG, "Filling send tx expectedResult...")
                        dAppController.fillSendTxExpectedResult(expectedResult)
                    }
                }
            }

            // Click Connect and get URL
            dAppController.clickConnectButton()
            val connectUrl = dAppController.clickCopyLinkInModal()
            dAppController.closeBrowserFully()

            step("Paste TonConnect URL into wallet") {
                walletController.connectByUrl(connectUrl)
            }
        }

        step("Wait for connect request sheet") {
            waitFor(timeoutMs = 10000) {
                walletController.isConnectRequestVisible()
            }
        }

        step("Approve connection") {
            walletController.approveConnect()
        }

        step("Verify connection established") {
            waitFor(timeoutMs = 5000) {
                !walletController.isConnectRequestVisible()
            }
        }

        step("Re-open dApp browser to continue test") {
            dAppController.openBrowser()
            dAppController.waitForDAppPage()

            // Close TonConnect modal if still open
            dAppController.closeTonConnectModal()
        }
    }
}
