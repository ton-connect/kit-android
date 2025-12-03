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
import io.ton.walletkit.demo.e2e.infrastructure.SignDataTest
import io.ton.walletkit.demo.e2e.infrastructure.TestCaseDataProvider
import io.ton.walletkit.demo.presentation.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * E2E tests for Sign Data functionality.
 *
 * These tests verify the sign data flow works correctly by:
 * 1. Connecting to the test dApp
 * 2. Filling in precondition and expected result from test data
 * 3. Triggering sign data request
 * 4. Approving or rejecting in the wallet
 * 5. Validating the result matches expected outcome
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@Feature("TonConnect")
@Story("Sign Data")
class SignDataE2ETest : BaseE2ETest() {

    companion object {
        private const val TAG = "SignDataE2ETest"
        private const val SIGN_DATA_APPROVE_TEST_ID = "2300"
        private const val SIGN_DATA_REJECT_TEST_ID = "2301"
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
    @SignDataTest
    @AllureId("2300")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Test that approving a sign data request returns valid signature")
    fun testSignDataApprove() {
        Log.d(TAG, "Starting Sign Data Approve test")

        // Fetch test case data
        val testData = TestCaseDataProvider.getTestCaseData(SIGN_DATA_APPROVE_TEST_ID, allureClient)
        val precondition = testData?.precondition ?: ""
        val expectedResult = testData?.expectedResult ?: ""
        Log.d(TAG, "Test data - precondition: ${precondition.take(100)}..., expectedResult: ${expectedResult.take(100)}...")

        // Setup wallet and connect to dApp
        connectToDApp(precondition, expectedResult, "signData")

        // Scroll to sign data section
        step("Scroll to Sign Data section") {
            dAppController.scrollToSignDataValidation()
            Thread.sleep(1000)
        }

        // Click sign data button
        step("Click Sign Data button") {
            dAppController.clickSignData()
            Thread.sleep(2000)
        }

        // Approve in wallet
        step("Approve sign data in wallet") {
            walletController.approveSignData()
            Thread.sleep(3000)
        }

        // Verify result
        step("Verify sign data result") {
            val isValid = dAppController.verifySignDataValidation()
            assert(isValid) { "Sign Data validation failed - expected 'Validation Passed'" }
            Log.d(TAG, "✅ Sign Data Approve test passed!")
        }
    }

    @Test
    @SignDataTest
    @AllureId("2301")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Test that rejecting a sign data request returns appropriate error")
    fun testSignDataReject() {
        Log.d(TAG, "Starting Sign Data Reject test")

        // Fetch test case data
        val testData = TestCaseDataProvider.getTestCaseData(SIGN_DATA_REJECT_TEST_ID, allureClient)
        val precondition = testData?.precondition ?: ""
        val expectedResult = testData?.expectedResult ?: ""
        Log.d(TAG, "Test data - precondition: ${precondition.take(100)}..., expectedResult: ${expectedResult.take(100)}...")

        // Setup wallet and connect to dApp
        connectToDApp(precondition, expectedResult, "signData")

        // Scroll to sign data section
        step("Scroll to Sign Data section") {
            dAppController.scrollToSignDataValidation()
            Thread.sleep(1000)
        }

        // Click sign data button
        step("Click Sign Data button") {
            dAppController.clickSignData()
            Thread.sleep(2000)
        }

        // Reject in wallet
        step("Reject sign data in wallet") {
            walletController.rejectSignData()
            Thread.sleep(3000)
        }

        // Verify result
        step("Verify sign data reject result") {
            val isValid = dAppController.verifySignDataValidation()
            assert(isValid) { "Sign Data reject validation failed - expected 'Validation Passed'" }
            Log.d(TAG, "✅ Sign Data Reject test passed!")
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
                "signData" -> {
                    if (precondition.isNotBlank()) {
                        Log.d(TAG, "Filling sign data precondition...")
                        dAppController.fillSignDataPrecondition(precondition)
                    }
                    if (expectedResult.isNotBlank()) {
                        Log.d(TAG, "Filling sign data expectedResult...")
                        dAppController.fillSignDataExpectedResult(expectedResult)
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
