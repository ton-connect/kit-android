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
import io.ton.walletkit.demo.e2e.infrastructure.ConnectTest
import io.ton.walletkit.demo.e2e.infrastructure.TestCaseDataProvider
import io.ton.walletkit.demo.presentation.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * E2E tests for TonConnect connection flow.
 *
 * These tests verify the wallet can successfully connect to a dApp
 * using TonConnect protocol.
 *
 * Flow:
 * 1. Setup wallet (generate new wallet)
 * 2. Open internal browser with demo dApp
 * 3. Click Connect button in dApp
 * 4. Click "Copy Link" to get TonConnect URL
 * 5. Close browser, open URL handler, paste URL
 * 6. Approve/reject connection request
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@Feature("TonConnect")
@Story("Connect")
class ConnectE2ETest : BaseE2ETest() {

    companion object {
        private const val TAG = "ConnectE2ETest"
    }

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    override fun setUp() {
        super.setUp()
        // Wait for the activity and Compose to be ready
        composeTestRule.waitForIdle()
        walletController.init(composeTestRule)
        dAppController.init(composeTestRule)

        // Ensure wallet is ready (handles setup/unlock automatically)
        ensureWalletReady()
    }

    @Test
    @ConnectTest
    @AllureId(AllureTestIds.CONNECT_IN_WALLET_BROWSER)
    @Severity(SeverityLevel.CRITICAL)
    @Description("Verify wallet can successfully connect to a dApp via HTTP bridge")
    fun testSuccessfulConnect() {
        // Fetch test case data - uses Allure API if available, falls back to hardcoded data
        val testData = TestCaseDataProvider.getTestCaseData(AllureTestIds.CONNECT_IN_WALLET_BROWSER, allureClient)
        val precondition = testData?.precondition ?: ""
        val expectedResult = testData?.expectedResult ?: ""
        android.util.Log.d("ConnectE2ETest", "Test data - precondition: $precondition, expectedResult: $expectedResult")

        step("Get TonConnect URL from dApp") {
            // Open browser
            dAppController.openBrowser()
            dAppController.waitForDAppPage()

            // Fill precondition and expected result BEFORE clicking connect
            if (precondition.isNotBlank()) {
                android.util.Log.d("ConnectE2ETest", "Filling precondition...")
                dAppController.fillConnectPrecondition(precondition)
            }
            if (expectedResult.isNotBlank()) {
                android.util.Log.d("ConnectE2ETest", "Filling expectedResult...")
                dAppController.fillConnectExpectedResult(expectedResult)
            }

            // Click Connect and get URL
            dAppController.clickConnectButton()
            val connectUrl = dAppController.clickCopyLinkInModal()

            // Close browser (just the modal + browser sheet)
            dAppController.closeBrowserFully()

            step("Paste TonConnect URL into wallet") {
                walletController.connectByUrl(connectUrl)
            }
        }

        step("Wait for connect request sheet") {
            // The wallet should show the connect approval dialog
            waitFor(timeoutMs = 10000) {
                walletController.isConnectRequestVisible()
            }
        }

        step("Approve connection") {
            walletController.approveConnect()
        }

        step("Verify connection established") {
            // Wait for the connect sheet to disappear (connection completed)
            waitFor(timeoutMs = 5000) {
                !walletController.isConnectRequestVisible()
            }
        }

        step("Verify dApp received valid connection") {
            // Open browser to check dApp validation result
            dAppController.openBrowser()

            // Wait for page to load
            dAppController.waitForDAppPage()

            // Close the TonConnect modal if it's still open from previous connection
            dAppController.closeTonConnectModal()

            // Scroll to the validation result in the e2e connect block
            dAppController.scrollToConnectValidation()

            // Wait for dApp to validate the connection response
            val validationPassed = dAppController.verifyConnectionValidation()

            // Get the actual validation text for debugging
            val validationText = dAppController.getConnectValidationResult()
            android.util.Log.d("ConnectE2ETest", "Validation text: $validationText")

            dAppController.closeBrowserFully()

            // Assert validation passed (either "Validation Passed" or connect event received)
            assert(validationPassed) { "dApp validation failed - got: $validationText" }
        }
    }

    @Test
    @ConnectTest
    @AllureId(AllureTestIds.CONNECT_USER_DECLINED)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify user can reject a connection request")
    fun testRejectConnect() {
        // Fetch test case data - uses Allure API if available, falls back to hardcoded data
        val testData = TestCaseDataProvider.getTestCaseData(AllureTestIds.CONNECT_USER_DECLINED, allureClient)
        val precondition = testData?.precondition ?: ""
        val expectedResult = testData?.expectedResult ?: ""

        step("Get TonConnect URL from dApp") {
            // Open browser
            dAppController.openBrowser()
            dAppController.waitForDAppPage()

            // Fill precondition and expected result BEFORE clicking connect
            if (precondition.isNotBlank()) {
                dAppController.fillConnectPrecondition(precondition)
            }
            if (expectedResult.isNotBlank()) {
                dAppController.fillConnectExpectedResult(expectedResult)
            }

            // Click Connect and get URL
            dAppController.clickConnectButton()
            val connectUrl = dAppController.clickCopyLinkInModal()

            // Close browser
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

        step("Reject connection") {
            walletController.rejectConnect()
        }

        step("Verify connection not established") {
            // Wait for the connect sheet to disappear (rejection processed)
            waitFor(timeoutMs = 5000) {
                !walletController.isConnectRequestVisible()
            }
        }

        step("Verify dApp received rejection error") {
            // Open browser to check dApp validation result
            dAppController.openBrowser()
            dAppController.waitForDAppPage()

            // Close the TonConnect modal if it's still open
            dAppController.closeTonConnectModal()

            // Scroll to the validation result
            dAppController.scrollToConnectValidation()

            // Verify the validation result
            val validationPassed = dAppController.verifyConnectionValidation()
            val validationText = dAppController.getConnectValidationResult()
            android.util.Log.d("ConnectE2ETest", "Rejection validation text: $validationText")

            dAppController.closeBrowserFully()

            // Assert validation passed (dApp validates rejection was handled correctly)
            assert(validationPassed) { "dApp rejection validation failed - got: $validationText" }
        }
    }

    /**
     * Test connection via injected WebView (JS bridge).
     *
     * This test verifies the in-wallet browser connection flow where:
     * 1. User opens a dApp in the wallet's built-in browser (with TonConnect injection)
     * 2. Fill expected JSON in the connect block
     * 3. Click Connect button - sheet appears immediately (no banner/link needed)
     * 4. User approves the connection
     * 5. Connection is validated in the dApp
     *
     * This is different from HTTP bridge flow - the connect request is instant
     * because the wallet injects the TonConnect provider into the WebView.
     */
    @Test
    @ConnectTest
    @AllureId(AllureTestIds.CONNECT_INJECTED_WEBVIEW)
    @Severity(SeverityLevel.CRITICAL)
    @Description("[In-Wallet browser][Mobile wallet] Connect wallet to dApp via injected WebView")
    fun testConnectViaInjectedWebView() {
        // Fetch test case data from Allure API
        val testData = TestCaseDataProvider.getTestCaseData(AllureTestIds.CONNECT_INJECTED_WEBVIEW, allureClient)
        val precondition = testData?.precondition ?: ""
        val expectedResult = testData?.expectedResult ?: ""
        android.util.Log.d(TAG, "Test data - precondition: $precondition, expectedResult: $expectedResult")

        step("Open dApp in in-wallet browser with TonConnect injection") {
            // Open the built-in browser WITH TonConnect injection enabled
            dAppController.openBrowser(injectTonConnect = true)
            dAppController.waitForDAppPage()
        }

        step("Fill expected result JSON") {
            // Fill expected result BEFORE clicking connect
            // This is required for the dApp to validate the response
            if (expectedResult.isNotBlank()) {
                android.util.Log.d(TAG, "Filling expectedResult...")
                dAppController.fillConnectExpectedResult(expectedResult)
            }
            if (precondition.isNotBlank()) {
                android.util.Log.d(TAG, "Filling precondition...")
                dAppController.fillConnectPrecondition(precondition)
            }
        }

        step("Click Connect button") {
            // For injected mode, clicking connect immediately triggers the approval sheet
            // No need to copy links or wait for banners
            dAppController.clickConnectButton()
        }

        step("Wait for connect request sheet") {
            // The wallet should show the connect approval dialog immediately
            waitFor(timeoutMs = 10000) {
                walletController.isConnectRequestVisible()
            }
        }

        step("Approve connection") {
            walletController.approveConnect()
        }

        step("Verify connection established") {
            // Wait for the connect sheet to disappear (connection completed)
            waitFor(timeoutMs = 5000) {
                !walletController.isConnectRequestVisible()
            }
        }

        step("Verify dApp received valid connection via JS bridge") {
            // Reopen browser to check validation result
            // (browser closes after clicking connect in injected mode)
            dAppController.openBrowser(injectTonConnect = true)
            dAppController.waitForDAppPage()

            // Scroll to validation and check result
            dAppController.scrollToConnectValidation()

            val validationPassed = dAppController.verifyConnectionValidation()
            val validationText = dAppController.getConnectValidationResult()
            android.util.Log.d(TAG, "Injected connection validation text: $validationText")

            dAppController.closeBrowserFully()

            // Assert validation passed
            assert(validationPassed) { "dApp validation failed for injected connection - got: $validationText" }
        }
    }

    // =========================================================================
    // Connect without ton_proof
    // =========================================================================

    /**
     * Test connection without ton_proof requirement.
     * Verifies wallet can connect when dApp doesn't require ton_proof.
     */
    @Test
    @ConnectTest
    @AllureId(AllureTestIds.CONNECT_WITHOUT_TON_PROOF)
    @Severity(SeverityLevel.NORMAL)
    @Description("Wallet supports connect to dApp with no ton_proof required")
    fun testConnectWithoutTonProof() {
        val testData = TestCaseDataProvider.getTestCaseData(AllureTestIds.CONNECT_WITHOUT_TON_PROOF, allureClient)
        val precondition = testData?.precondition ?: ""
        val expectedResult = testData?.expectedResult ?: ""
        android.util.Log.d(TAG, "Test data - precondition: $precondition, expectedResult: $expectedResult")

        step("Get TonConnect URL from dApp") {
            dAppController.openBrowser()
            dAppController.waitForDAppPage()

            if (precondition.isNotBlank()) {
                dAppController.fillConnectPrecondition(precondition)
            }
            if (expectedResult.isNotBlank()) {
                dAppController.fillConnectExpectedResult(expectedResult)
            }

            dAppController.clickConnectButton()
            val connectUrl = dAppController.clickCopyLinkInModal()
            dAppController.closeBrowserFully()

            step("Paste TonConnect URL into wallet") {
                walletController.connectByUrl(connectUrl)
            }
        }

        step("Wait for and approve connection") {
            waitFor(timeoutMs = 10000) {
                walletController.isConnectRequestVisible()
            }
            walletController.approveConnect()
        }

        step("Verify connection established") {
            waitFor(timeoutMs = 5000) {
                !walletController.isConnectRequestVisible()
            }
        }

        step("Verify dApp validation") {
            dAppController.openBrowser()
            dAppController.waitForDAppPage()
            dAppController.closeTonConnectModal()
            dAppController.scrollToConnectValidation()

            val validationPassed = dAppController.verifyConnectionValidation()
            val validationText = dAppController.getConnectValidationResult()
            android.util.Log.d(TAG, "Connect without ton_proof validation: $validationText")

            dAppController.closeBrowserFully()
            assert(validationPassed) { "Validation failed - got: $validationText" }
        }
    }

    // =========================================================================
    // Connect with ton_proof
    // =========================================================================

    /**
     * Test connection with ton_proof requirement.
     * Verifies wallet can connect and provide valid ton_proof when required.
     */
    @Test
    @ConnectTest
    @AllureId(AllureTestIds.CONNECT_IN_WALLET_BROWSER) // Same as regular connect - ton_proof is always provided
    @Severity(SeverityLevel.CRITICAL)
    @Description("Wallet supports connect to dApp with ton_proof required")
    fun testConnectWithTonProof() {
        val testData = TestCaseDataProvider.getTestCaseData(AllureTestIds.CONNECT_IN_WALLET_BROWSER, allureClient)
        val precondition = testData?.precondition ?: ""
        val expectedResult = testData?.expectedResult ?: ""
        android.util.Log.d(TAG, "Test data - precondition: $precondition, expectedResult: $expectedResult")

        step("Get TonConnect URL from dApp") {
            dAppController.openBrowser()
            dAppController.waitForDAppPage()

            if (precondition.isNotBlank()) {
                dAppController.fillConnectPrecondition(precondition)
            }
            if (expectedResult.isNotBlank()) {
                dAppController.fillConnectExpectedResult(expectedResult)
            }

            dAppController.clickConnectButton()
            val connectUrl = dAppController.clickCopyLinkInModal()
            dAppController.closeBrowserFully()

            step("Paste TonConnect URL into wallet") {
                walletController.connectByUrl(connectUrl)
            }
        }

        step("Wait for and approve connection") {
            waitFor(timeoutMs = 10000) {
                walletController.isConnectRequestVisible()
            }
            walletController.approveConnect()
        }

        step("Verify connection established") {
            waitFor(timeoutMs = 5000) {
                !walletController.isConnectRequestVisible()
            }
        }

        step("Verify dApp validation with ton_proof") {
            dAppController.openBrowser()
            dAppController.waitForDAppPage()
            dAppController.closeTonConnectModal()
            dAppController.scrollToConnectValidation()

            val validationPassed = dAppController.verifyConnectionValidation()
            val validationText = dAppController.getConnectValidationResult()
            android.util.Log.d(TAG, "Connect with ton_proof validation: $validationText")

            dAppController.closeBrowserFully()
            assert(validationPassed) { "Validation failed - got: $validationText" }
        }
    }

    // =========================================================================
    // Fake manifest URL tests (error cases)
    // =========================================================================

    /**
     * Test connection error when manifest URL is fake (via Wallet QR).
     * Verifies wallet properly handles malicious manifest URLs.
     */
    @Test
    @ConnectTest
    @AllureId(AllureTestIds.CONNECT_FAKE_MANIFEST_URL_CUSTOM_QR)
    @Severity(SeverityLevel.NORMAL)
    @Description("[Desktop browser][Mobile wallet] Connect wallet to dApp with fake manifest domain via Wallet QR Code")
    fun testConnectFakeManifestUrlWalletQR() {
        val testData = TestCaseDataProvider.getTestCaseData(AllureTestIds.CONNECT_FAKE_MANIFEST_URL_CUSTOM_QR, allureClient)
        val precondition = testData?.precondition ?: ""
        val expectedResult = testData?.expectedResult ?: ""
        android.util.Log.d(TAG, "Test data - precondition: $precondition, expectedResult: $expectedResult")

        step("Get TonConnect URL from dApp") {
            dAppController.openBrowser()
            dAppController.waitForDAppPage()

            if (precondition.isNotBlank()) {
                dAppController.fillConnectPrecondition(precondition)
            }
            if (expectedResult.isNotBlank()) {
                dAppController.fillConnectExpectedResult(expectedResult)
            }

            dAppController.clickConnectButton()
            val connectUrl = dAppController.clickCopyLinkInModal()
            dAppController.closeBrowserFully()

            step("Paste TonConnect URL into wallet") {
                walletController.connectByUrl(connectUrl)
            }
        }

        step("Verify error handling") {
            // For fake manifest tests, we expect either:
            // 1. Connection to fail/be rejected by wallet
            // 2. Error to be shown
            // Wait a bit to see if connect sheet appears
            Thread.sleep(3000)

            // Check validation in dApp
            dAppController.openBrowser()
            dAppController.waitForDAppPage()
            dAppController.closeTonConnectModal()
            dAppController.scrollToConnectValidation()

            val validationPassed = dAppController.verifyConnectionValidation()
            val validationText = dAppController.getConnectValidationResult()
            android.util.Log.d(TAG, "Fake manifest URL validation: $validationText")

            dAppController.closeBrowserFully()
            assert(validationPassed) { "Validation failed - got: $validationText" }
        }
    }

    /**
     * Test connection error when manifest URL is fake (via Universal QR).
     */
    @Test
    @ConnectTest
    @AllureId(AllureTestIds.CONNECT_FAKE_MANIFEST_URL_UNIVERSAL_QR)
    @Severity(SeverityLevel.NORMAL)
    @Description("[Desktop browser][Mobile wallet] Connect wallet to dApp with fake manifest domain via universal QR Code")
    fun testConnectFakeManifestUrlUniversalQR() {
        val testData = TestCaseDataProvider.getTestCaseData(AllureTestIds.CONNECT_FAKE_MANIFEST_URL_UNIVERSAL_QR, allureClient)
        val precondition = testData?.precondition ?: ""
        val expectedResult = testData?.expectedResult ?: ""
        android.util.Log.d(TAG, "Test data - precondition: $precondition, expectedResult: $expectedResult")

        step("Get TonConnect URL from dApp") {
            dAppController.openBrowser()
            dAppController.waitForDAppPage()

            if (precondition.isNotBlank()) {
                dAppController.fillConnectPrecondition(precondition)
            }
            if (expectedResult.isNotBlank()) {
                dAppController.fillConnectExpectedResult(expectedResult)
            }

            dAppController.clickConnectButton()
            val connectUrl = dAppController.clickCopyLinkInModal()
            dAppController.closeBrowserFully()

            step("Paste TonConnect URL into wallet") {
                walletController.connectByUrl(connectUrl)
            }
        }

        step("Verify error handling") {
            Thread.sleep(3000)

            dAppController.openBrowser()
            dAppController.waitForDAppPage()
            dAppController.closeTonConnectModal()
            dAppController.scrollToConnectValidation()

            val validationPassed = dAppController.verifyConnectionValidation()
            val validationText = dAppController.getConnectValidationResult()
            android.util.Log.d(TAG, "Fake manifest URL (universal QR) validation: $validationText")

            dAppController.closeBrowserFully()
            assert(validationPassed) { "Validation failed - got: $validationText" }
        }
    }

    // =========================================================================
    // Fake URL in manifest tests (error cases)
    // =========================================================================

    /**
     * Test connection error when URL in manifest is fake (via Wallet QR).
     */
    @Test
    @ConnectTest
    @AllureId(AllureTestIds.CONNECT_FAKE_URL_IN_MANIFEST_CUSTOM_QR)
    @Severity(SeverityLevel.NORMAL)
    @Description("[Desktop browser][Mobile wallet] Connect wallet to dApp with fake URL in manifest via Wallet QR Code")
    fun testConnectFakeUrlInManifestWalletQR() {
        val testData = TestCaseDataProvider.getTestCaseData(AllureTestIds.CONNECT_FAKE_URL_IN_MANIFEST_CUSTOM_QR, allureClient)
        val precondition = testData?.precondition ?: ""
        val expectedResult = testData?.expectedResult ?: ""
        android.util.Log.d(TAG, "Test data - precondition: $precondition, expectedResult: $expectedResult")

        step("Get TonConnect URL from dApp") {
            dAppController.openBrowser()
            dAppController.waitForDAppPage()

            if (precondition.isNotBlank()) {
                dAppController.fillConnectPrecondition(precondition)
            }
            if (expectedResult.isNotBlank()) {
                dAppController.fillConnectExpectedResult(expectedResult)
            }

            dAppController.clickConnectButton()
            val connectUrl = dAppController.clickCopyLinkInModal()
            dAppController.closeBrowserFully()

            step("Paste TonConnect URL into wallet") {
                walletController.connectByUrl(connectUrl)
            }
        }

        step("Verify error handling") {
            Thread.sleep(3000)

            dAppController.openBrowser()
            dAppController.waitForDAppPage()
            dAppController.closeTonConnectModal()
            dAppController.scrollToConnectValidation()

            val validationPassed = dAppController.verifyConnectionValidation()
            val validationText = dAppController.getConnectValidationResult()
            android.util.Log.d(TAG, "Fake URL in manifest (wallet QR) validation: $validationText")

            dAppController.closeBrowserFully()
            assert(validationPassed) { "Validation failed - got: $validationText" }
        }
    }

    /**
     * Test connection error when URL in manifest is fake (via Universal QR).
     */
    @Test
    @ConnectTest
    @AllureId(AllureTestIds.CONNECT_FAKE_URL_IN_MANIFEST_UNIVERSAL_QR)
    @Severity(SeverityLevel.NORMAL)
    @Description("[Desktop browser][Mobile wallet] Connect wallet to dApp with fake URL in manifest via universal QR Code")
    fun testConnectFakeUrlInManifestUniversalQR() {
        val testData = TestCaseDataProvider.getTestCaseData(AllureTestIds.CONNECT_FAKE_URL_IN_MANIFEST_UNIVERSAL_QR, allureClient)
        val precondition = testData?.precondition ?: ""
        val expectedResult = testData?.expectedResult ?: ""
        android.util.Log.d(TAG, "Test data - precondition: $precondition, expectedResult: $expectedResult")

        step("Get TonConnect URL from dApp") {
            dAppController.openBrowser()
            dAppController.waitForDAppPage()

            if (precondition.isNotBlank()) {
                dAppController.fillConnectPrecondition(precondition)
            }
            if (expectedResult.isNotBlank()) {
                dAppController.fillConnectExpectedResult(expectedResult)
            }

            dAppController.clickConnectButton()
            val connectUrl = dAppController.clickCopyLinkInModal()
            dAppController.closeBrowserFully()

            step("Paste TonConnect URL into wallet") {
                walletController.connectByUrl(connectUrl)
            }
        }

        step("Verify error handling") {
            Thread.sleep(3000)

            dAppController.openBrowser()
            dAppController.waitForDAppPage()
            dAppController.closeTonConnectModal()
            dAppController.scrollToConnectValidation()

            val validationPassed = dAppController.verifyConnectionValidation()
            val validationText = dAppController.getConnectValidationResult()
            android.util.Log.d(TAG, "Fake URL in manifest (universal QR) validation: $validationText")

            dAppController.closeBrowserFully()
            assert(validationPassed) { "Validation failed - got: $validationText" }
        }
    }
}
