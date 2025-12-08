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
import io.ton.walletkit.demo.e2e.infrastructure.SignDataTest
import io.ton.walletkit.demo.e2e.infrastructure.TestCaseDataProvider
import io.ton.walletkit.demo.presentation.MainActivity
import org.junit.AfterClass
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

/**
 * E2E tests for Sign Data functionality via HTTP bridge (normal WebView).
 *
 * Flow for first test:
 * 1. Open browser, click "Connect and Sign Data" button - opens TonConnect modal
 * 2. Copy the TonConnect URL from the modal
 * 3. Close browser, paste URL into wallet, approve connect request
 * 4. Re-open browser, fill test data, click "Sign Data" button (now connected)
 * 5. Approve/reject sign data in wallet
 * 6. Re-open browser and verify the validation result
 *
 * Flow for subsequent tests (already connected):
 * 1. Open browser, fill test data
 * 2. Click "Sign Data" button
 * 3. Approve/reject sign data in wallet
 * 4. Re-open browser and verify validation result
 *
 * Tests are ordered to run approve first (which establishes connection), then reject.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@Feature("TonConnect")
@Story("Sign Data")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class SignDataE2ETest : BaseE2ETest() {

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

        // Click Sign Data to initiate connection
        dAppController.clickSignData()

        val connectUrl = dAppController.clickCopyLinkInModal()

        dAppController.closeBrowserFully()
        walletController.connectByUrl(connectUrl)

        waitFor(timeoutMs = 10000) { walletController.isConnectRequestVisible() }
        walletController.approveConnect()
        waitFor(timeoutMs = 5000) { !walletController.isConnectRequestVisible() }

        isConnected = true
    }

    @Test
    @SignDataTest
    @AllureId(AllureTestIds.SIGN_DATA_TEXT)
    @Severity(SeverityLevel.CRITICAL)
    @Description("Test that approving a sign data request returns valid signature")
    fun test1_SignDataApprove() {
        val testData = TestCaseDataProvider.getTestCaseData(AllureTestIds.SIGN_DATA_TEXT, allureClient)
        val precondition = testData?.precondition ?: ""
        val expectedResult = testData?.expectedResult ?: ""

        step("Ensure wallet is connected to dApp") {
            ensureConnected()
        }

        step("Open browser and fill sign data test data") {
            dAppController.openBrowser()
            dAppController.waitForDAppPage()
            if (precondition.isNotBlank()) dAppController.fillSignDataPrecondition(precondition)
            if (expectedResult.isNotBlank()) dAppController.fillSignDataExpectedResult(expectedResult)
        }

        step("Click Sign Data button") {
            dAppController.clickSignData()
        }

        step("Wait for sign data request") {
            waitFor(timeoutMs = 15000) { walletController.isSignDataRequestVisible() }
        }

        step("Approve sign data in wallet") {
            walletController.approveSignData()
        }

        step("Verify sign data result in browser") {
            waitFor(timeoutMs = 5000) { !walletController.isSignDataRequestVisible() }
            Thread.sleep(2000)

            dAppController.openBrowser()
            dAppController.waitForDAppPage()
            val isValid = dAppController.verifySignDataValidation()

            dAppController.closeBrowserFully()
            assert(isValid) { "Sign Data validation failed - expected 'Validation Passed'" }
        }
    }

    @Test
    @SignDataTest
    @AllureId(AllureTestIds.SIGN_DATA_BINARY)
    @Severity(SeverityLevel.CRITICAL)
    @Description("Test that rejecting a sign data request returns appropriate error")
    fun test2_SignDataReject() {
        val testData = TestCaseDataProvider.getTestCaseData(AllureTestIds.SIGN_DATA_BINARY, allureClient)
        val precondition = testData?.precondition ?: ""
        val expectedResult = testData?.expectedResult ?: ""

        step("Ensure wallet is connected to dApp") {
            ensureConnected()
        }

        step("Open browser and fill sign data test data") {
            dAppController.openBrowser()
            dAppController.waitForDAppPage()
            if (precondition.isNotBlank()) dAppController.fillSignDataPrecondition(precondition)
            if (expectedResult.isNotBlank()) dAppController.fillSignDataExpectedResult(expectedResult)
        }

        step("Click Sign Data button") {
            dAppController.clickSignData()
        }

        step("Wait for sign data request") {
            waitFor(timeoutMs = 15000) { walletController.isSignDataRequestVisible() }
        }

        step("Reject sign data in wallet") {
            walletController.rejectSignData()
        }

        step("Verify sign data reject result in browser") {
            waitFor(timeoutMs = 5000) { !walletController.isSignDataRequestVisible() }
            Thread.sleep(2000)

            dAppController.openBrowser()
            dAppController.waitForDAppPage()
            val isValid = dAppController.verifySignDataValidation()

            dAppController.closeBrowserFully()
            assert(isValid) { "Sign Data reject validation failed - expected 'Validation Passed'" }
        }
    }
}
