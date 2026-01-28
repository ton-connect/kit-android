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
package io.ton.walletkit.demo.e2e.infrastructure

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import io.qameta.allure.kotlin.Allure
import io.qameta.allure.kotlin.Epic
import io.qameta.allure.kotlin.Feature
import io.qameta.allure.kotlin.Step
import io.ton.walletkit.demo.core.RequestErrorTracker
import io.ton.walletkit.demo.e2e.dapp.JsDAppController
import io.ton.walletkit.demo.e2e.wallet.WalletController
import org.junit.Before
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Base class for all E2E tests.
 *
 * Provides common setup/teardown and helper methods.
 *
 * Test classes should:
 * 1. Create a @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()
 * 2. Call super.setUp() in their setUp() method
 * 3. Call walletController.init(composeTestRule) after super.setUp()
 */
@Epic("TonConnect E2E Tests")
abstract class BaseE2ETest {

    protected lateinit var context: Context
    protected lateinit var walletController: WalletController
    protected lateinit var dAppController: JsDAppController
    protected var allureClient: AllureApiClient? = null

    companion object {
        // Default password used for tests
        const val TEST_PASSWORD = "testpass123"

        // =====================================================================
        // E2E Test Timeouts
        // =====================================================================
        // CI emulators are significantly slower than local machines.
        // These timeouts are tuned for CI stability.

        /** Timeout for UI elements that depend on WebView/network (sheets, dialogs) */
        const val SHEET_APPEAR_TIMEOUT_MS = 60_000L

        /** Timeout for transaction/connect request sheets to appear */
        const val REQUEST_SHEET_TIMEOUT_MS = 50_000L

        /** Timeout for waiting for dApp responses after wallet action */
        const val DAPP_RESPONSE_TIMEOUT_MS = 50_000L

        /** Timeout for waiting for SDK error events to propagate */
        const val SDK_ERROR_EVENT_TIMEOUT_MS = 30_000L

        /** Timeout for UI elements to disappear after action */
        const val UI_DISMISS_TIMEOUT_MS = 20_000L

        /** General wait timeout for simple UI states */
        const val UI_STATE_TIMEOUT_MS = 20_000L

        /** Short sleep for auto-reject processing */
        const val AUTO_REJECT_PROCESS_MS = 3_000L

        /**
         * Test mnemonic for E2E tests.
         * This wallet should have sufficient balance for testing transactions.
         * The mnemonic MUST be provided via instrumentation arguments: -e testMnemonic "word1 word2 ..."
         * In CI, this is set via the WALLET_MNEMONIC secret.
         */
        val TEST_MNEMONIC: List<String> by lazy {
            val mnemonicArg = InstrumentationRegistry.getArguments().getString("testMnemonic")
            android.util.Log.d("BaseE2ETest", "testMnemonic from instrumentation args: ${mnemonicArg?.take(30)}...")
            if (mnemonicArg.isNullOrBlank()) {
                android.util.Log.e("BaseE2ETest", "Test mnemonic is missing! Available args: ${InstrumentationRegistry.getArguments()}")
                throw IllegalStateException(
                    "Test mnemonic is required. Set via instrumentation arg: -e testMnemonic \"word1 word2 ...\"\n" +
                        "In CI, ensure WALLET_MNEMONIC secret is configured.",
                )
            }
            val words = mnemonicArg.split(" ").filter { it.isNotBlank() }
            android.util.Log.d("BaseE2ETest", "Parsed ${words.size} mnemonic words")
            words
        }

        /**
         * Get Allure config from instrumentation arguments.
         * Token is passed via Android Studio run configuration or CI workflow.
         */
        fun getAllureConfig(): AllureConfig? {
            val token = InstrumentationRegistry.getArguments().getString("allureToken")?.takeIf { it.isNotBlank() }
            return token?.let { AllureConfig(apiToken = it) }
        }

        /**
         * Check if network send should be disabled.
         * Can be set via instrumentation arguments: -e disableNetworkSend true
         */
        fun shouldDisableNetworkSend(): Boolean = InstrumentationRegistry.getArguments().getString("disableNetworkSend")?.equals("true", ignoreCase = true) ?: true

        /**
         * Complete cleanup of all app data, WebView data, and SDK state.
         * Call this in @AfterClass to ensure the next test class starts fresh.
         */
        @JvmStatic
        fun cleanupAllData() {
            android.util.Log.d("BaseE2ETest", "cleanupAllData - clearing all app data for fresh start")
            val context = ApplicationProvider.getApplicationContext<Context>()

            // 1. Clear SharedPreferences
            val prefsToDelete = listOf(
                "walletkit_demo_wallets",
                "walletkit_demo_prefs",
            )
            for (prefName in prefsToDelete) {
                try {
                    context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
                        .edit()
                        .clear()
                        .commit()
                    val prefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
                    val prefsFile = File(prefsDir, "$prefName.xml")
                    if (prefsFile.exists()) {
                        prefsFile.delete()
                    }
                } catch (e: Exception) {
                    android.util.Log.w("BaseE2ETest", "Failed to clear prefs $prefName: ${e.message}")
                }
            }

            // 2. Clear files and cache
            context.filesDir?.deleteRecursively()
            context.cacheDir?.deleteRecursively()

            // 3. Clear datastore
            val datastoreDir = File(context.filesDir, "datastore")
            if (datastoreDir.exists()) {
                datastoreDir.deleteRecursively()
            }

            // 4. Clear WebView data (localStorage, sessionStorage, cookies, cache)
            try {
                android.webkit.CookieManager.getInstance().removeAllCookies(null)
                android.webkit.CookieManager.getInstance().flush()
                android.webkit.WebStorage.getInstance().deleteAllData()

                // Clear WebView cache directory
                val webViewCacheDir = File(context.cacheDir, "WebView")
                if (webViewCacheDir.exists()) {
                    webViewCacheDir.deleteRecursively()
                }
                val webViewDataDir = File(context.dataDir, "app_webview")
                if (webViewDataDir.exists()) {
                    webViewDataDir.deleteRecursively()
                }
            } catch (e: Exception) {
                android.util.Log.w("BaseE2ETest", "Failed to clear WebView data: ${e.message}")
            }

            android.util.Log.d("BaseE2ETest", "cleanupAllData completed")
        }
    }

    @Before
    open fun setUp() {
        context = ApplicationProvider.getApplicationContext()

        // Enable disableNetworkSend for testing (transactions simulated but not sent)
        // This must be set BEFORE the SDK is initialized
        io.ton.walletkit.demo.core.TONWalletKitHelper.disableNetworkSend = shouldDisableNetworkSend()

        // Initialize Allure client if token is available
        getAllureConfig()?.let { config ->
            allureClient = AllureApiClient(config)
        }

        // Initialize controllers
        walletController = WalletController()
        dAppController = JsDAppController()
    }

    /**
     * Ensure wallet is ready for testing.
     * This handles all authentication states: setup password, unlock, or already on home.
     * Uses the test mnemonic to import a wallet with known balance.
     * Call this after initializing controllers to ensure wallet is on home screen.
     */
    protected fun ensureWalletReady() {
        android.util.Log.d("BaseE2ETest", "ensureWalletReady called with mnemonic (${TEST_MNEMONIC.size} words)")
        android.util.Log.d("BaseE2ETest", "First few words: ${TEST_MNEMONIC.take(3).joinToString(" ")}...")
        walletController.setupWallet(TEST_MNEMONIC, TEST_PASSWORD)
        walletController.waitForWalletHome()
        android.util.Log.d("BaseE2ETest", "Wallet is ready on home screen")
    }

    /**
     * Wait for Activity to be fully launched and Compose hierarchy to be available.
     * This is critical for slow CI emulators where Activity launch can be delayed.
     * Call this in setUp() before initializing controllers.
     *
     * @param composeTestRule The compose test rule to wait on
     * @param initialDelayMs Initial delay after waitForIdle to let Activity stabilize
     */
    protected fun waitForActivityReady(
        composeTestRule: androidx.compose.ui.test.junit4.ComposeTestRule,
        initialDelayMs: Long = 3000,
    ) {
        android.util.Log.d("BaseE2ETest", "Waiting for Activity to launch...")
        composeTestRule.waitForIdle()

        // Give the Activity extra time to fully initialize on slow emulators
        Thread.sleep(initialDelayMs)

        // Wait for Compose hierarchy to be set up
        try {
            composeTestRule.waitUntil(UI_STATE_TIMEOUT_MS) {
                // Try to access the root node - this fails if Compose isn't ready
                try {
                    composeTestRule.onRoot().fetchSemanticsNode()
                    true
                } catch (e: Exception) {
                    false
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("BaseE2ETest", "Timeout waiting for Compose hierarchy: ${e.message}")
            // Don't throw - proceed and let the test fail with a more specific error if needed
        }

        composeTestRule.waitForIdle()
        android.util.Log.d("BaseE2ETest", "Activity is ready")
    }

    /**
     * Run a test step with Allure reporting.
     */
    @Step("{description}")
    protected fun step(description: String, block: () -> Unit) {
        block()
    }

    /**
     * Wait for a condition to be true.
     */
    protected fun waitFor(
        timeoutMs: Long = 10000,
        intervalMs: Long = 100,
        condition: () -> Boolean,
    ): Boolean {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (condition()) {
                return true
            }
            Thread.sleep(intervalMs)
        }
        return false
    }

    /**
     * Take a screenshot and attach to Allure report.
     * Note: This method requires passing the ComposeTestRule from the test class.
     * Consider using the Allure screenshot annotation instead.
     */
    protected fun takeScreenshot(name: String, activity: android.app.Activity) {
        val rootView = activity.window.decorView.rootView
        rootView.isDrawingCacheEnabled = true
        val bitmap = rootView.drawingCache
        if (bitmap != null) {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            val bytes = stream.toByteArray()
            Allure.attachment(name, ByteArrayInputStream(bytes), "image/png", "png")
        }
        rootView.isDrawingCacheEnabled = false
    }

    // =========================================================================
    // Shared Test Runners (reduces boilerplate in test subclasses)
    // =========================================================================

    /**
     * Shared runner for send transaction tests.
     *
     * @param allureId The Allure test case ID
     * @param expectWalletPrompt Whether the wallet should show a transaction prompt (false for validation errors)
     * @param approve Whether to approve or reject the transaction (when expectWalletPrompt is true)
     * @param expectSdkError Whether to expect SDK to emit RequestError event (true for SDK validation errors like invalid validUntil).
     *                       When false and expectWalletPrompt is also false, the wallet app auto-rejects (e.g., insufficient balance).
     * @param ensureConnected Callback to ensure wallet is connected before running the test
     */
    protected fun runSendTxTest(
        allureId: String,
        expectWalletPrompt: Boolean = true,
        approve: Boolean = true,
        expectSdkError: Boolean = !expectWalletPrompt,
        ensureConnected: () -> Unit,
    ) {
        val testData = TestCaseDataProvider.getTestCaseData(allureId, allureClient)
        val precondition = testData?.precondition ?: ""
        val expectedResult = testData?.expectedResult ?: ""

        android.util.Log.d("SendTxTest", "Test data for $allureId:")
        android.util.Log.d("SendTxTest", "  precondition length: ${precondition.length}")
        android.util.Log.d("SendTxTest", "  expectedResult length: ${expectedResult.length}")

        // Clear error tracker before test if expecting SDK to auto-reject
        if (!expectWalletPrompt) {
            RequestErrorTracker.clear()
        }

        step("Ensure wallet is connected to dApp") { ensureConnected() }

        step("Open browser and fill sendTx test data") {
            dAppController.openBrowser()
            dAppController.waitForDAppPage()
            if (precondition.isNotBlank()) {
                android.util.Log.d("SendTxTest", "Filling precondition...")
                dAppController.fillSendTxPrecondition(precondition)
            }
            if (expectedResult.isNotBlank()) {
                android.util.Log.d("SendTxTest", "Filling expectedResult...")
                dAppController.fillSendTxExpectedResult(expectedResult)
            }
        }

        step("Click Send Transaction button") {
            dAppController.clickSendTransaction()
        }

        if (expectWalletPrompt) {
            step("Wait for transaction request") {
                waitFor(REQUEST_SHEET_TIMEOUT_MS) { walletController.isTransactionRequestVisible() }
            }

            step(if (approve) "Approve transaction" else "Reject transaction") {
                if (approve) walletController.approveTransaction() else walletController.rejectTransaction()
            }

            step("Wait for wallet to close transaction sheet") {
                waitFor(UI_DISMISS_TIMEOUT_MS) { !walletController.isTransactionRequestVisible() }
            }

            step("Wait for dApp to receive transaction response") {
                dAppController.openBrowser()
                dAppController.waitForDAppPage()
                dAppController.waitForSendTxResponse(timeoutMs = DAPP_RESPONSE_TIMEOUT_MS)
            }
        } else {
            if (expectSdkError) {
                step("Verify SDK received RequestError event for sendTransaction") {
                    val error = RequestErrorTracker.waitForError(timeoutMs = SDK_ERROR_EVENT_TIMEOUT_MS, method = "sendTransaction")
                    android.util.Log.d("SendTxTest", "RequestError received: $error")
                    assert(error != null) { "Expected SDK to receive RequestError event for sendTransaction, but none received" }
                    assert(error!!.method == "sendTransaction") { "Expected method 'sendTransaction', got '${error.method}'" }
                }
            } else {
                step("Wait for wallet app to auto-reject transaction") {
                    // Wallet app receives the request but rejects it without showing UI (e.g., insufficient balance)
                    // Give it time to process and send rejection to dApp
                    Thread.sleep(AUTO_REJECT_PROCESS_MS)
                }
            }

            step("Wait for error response in dApp") {
                Thread.sleep(1000)
                dAppController.scrollToSendTxValidation()
            }
        }

        step("Verify result in browser") {
            dAppController.scrollToSendTxValidation()
            val isValid = dAppController.verifySendTxValidation()
            val validationText = dAppController.getSendTxValidationResult()
            dAppController.closeBrowserFully()
            assert(isValid) { "Validation failed - got: $validationText" }
        }
    }

    /**
     * Shared runner for connect tests via HTTP bridge (copy link flow).
     *
     * @param allureId The Allure test case ID
     * @param approve Whether to approve or reject the connection
     * @param expectConnectSheet Whether the connect sheet should appear (false for error cases)
     */
    protected fun runHttpBridgeConnectTest(
        allureId: String,
        approve: Boolean = true,
        expectConnectSheet: Boolean = true,
    ) {
        val testData = TestCaseDataProvider.getTestCaseData(allureId, allureClient)
        val precondition = testData?.precondition ?: ""
        val expectedResult = testData?.expectedResult ?: ""
        android.util.Log.d("ConnectTest", "Test $allureId - precondition: ${precondition.take(100)}")

        // Clear error tracker before test if expecting SDK to auto-reject
        if (!expectConnectSheet) {
            RequestErrorTracker.clear()
        }

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

            walletController.connectByUrl(connectUrl)
        }

        if (expectConnectSheet) {
            step("Wait for connect request sheet") {
                waitFor(SHEET_APPEAR_TIMEOUT_MS) { walletController.isConnectRequestVisible() }
            }

            step(if (approve) "Approve connection" else "Reject connection") {
                if (approve) walletController.approveConnect() else walletController.rejectConnect()
            }

            step("Wait for connect sheet to close") {
                waitFor(UI_DISMISS_TIMEOUT_MS) { !walletController.isConnectRequestVisible() }
            }
        } else {
            step("Verify no connect sheet shown (wallet app auto-rejected)") {
                val connectSheetVisible = walletController.isConnectRequestVisible()
                android.util.Log.d("ConnectTest", "Connect sheet visible: $connectSheetVisible")
                assert(!connectSheetVisible) { "Connect sheet should NOT be visible - wallet should auto-reject manifest error" }
            }
        }

        step("Verify dApp validation") {
            dAppController.openBrowser()
            dAppController.waitForDAppPage()
            dAppController.closeTonConnectModal()
            dAppController.scrollToConnectValidation()

            val validationPassed = dAppController.verifyConnectionValidation()
            val validationText = dAppController.getConnectValidationResult()
            android.util.Log.d("ConnectTest", "Validation result: $validationText")

            dAppController.closeBrowserFully()
            assert(validationPassed) { "Validation failed - got: $validationText" }
        }
    }

    /**
     * Shared runner for connect tests via injected WebView (in-wallet browser).
     *
     * @param allureId The Allure test case ID
     */
    protected fun runInjectedConnectTest(allureId: String) {
        val testData = TestCaseDataProvider.getTestCaseData(allureId, allureClient)
        val precondition = testData?.precondition ?: ""
        val expectedResult = testData?.expectedResult ?: ""
        android.util.Log.d("ConnectTest", "Test $allureId - precondition: ${precondition.take(100)}")

        step("Open dApp with TonConnect injection") {
            dAppController.openBrowser(injectTonConnect = true)
            dAppController.waitForDAppPage()
        }

        step("Fill test data and click Connect") {
            if (expectedResult.isNotBlank()) {
                dAppController.fillConnectExpectedResult(expectedResult)
            }
            if (precondition.isNotBlank()) {
                dAppController.fillConnectPrecondition(precondition)
            }
            dAppController.clickConnectButton()
        }

        step("Wait for and approve connection") {
            waitFor(SHEET_APPEAR_TIMEOUT_MS) { walletController.isConnectRequestVisible() }
            walletController.approveConnect()
        }

        step("Wait for connect sheet to close") {
            waitFor(UI_DISMISS_TIMEOUT_MS) { !walletController.isConnectRequestVisible() }
        }

        step("Verify dApp validation") {
            dAppController.openBrowser(injectTonConnect = true)
            dAppController.waitForDAppPage()
            dAppController.scrollToConnectValidation()

            val validationPassed = dAppController.verifyConnectionValidation()
            val validationText = dAppController.getConnectValidationResult()
            android.util.Log.d("ConnectTest", "Injected validation result: $validationText")

            dAppController.closeBrowserFully()
            assert(validationPassed) { "Validation failed - got: $validationText" }
        }
    }

    /**
     * Shared runner for sign data tests.
     *
     * @param allureId The Allure test case ID
     * @param approve Whether to approve or reject sign data
     * @param ensureConnected Callback to ensure wallet is connected before running the test
     */
    protected fun runSignDataTest(
        allureId: String,
        approve: Boolean = true,
        ensureConnected: () -> Unit,
    ) {
        val testData = TestCaseDataProvider.getTestCaseData(allureId, allureClient)
        val precondition = testData?.precondition ?: ""
        val expectedResult = testData?.expectedResult ?: ""

        step("Ensure wallet is connected to dApp") { ensureConnected() }

        step("Open browser and fill signData test data") {
            dAppController.openBrowser()
            dAppController.waitForDAppPage()
            if (precondition.isNotBlank()) dAppController.fillSignDataPrecondition(precondition)
            if (expectedResult.isNotBlank()) dAppController.fillSignDataExpectedResult(expectedResult)
        }

        step("Click Sign Data button") {
            dAppController.clickSignData()
        }

        step("Wait for sign data request") {
            waitFor(REQUEST_SHEET_TIMEOUT_MS) { walletController.isSignDataRequestVisible() }
        }

        step(if (approve) "Approve sign data" else "Reject sign data") {
            if (approve) walletController.approveSignData() else walletController.rejectSignData()
        }

        step("Wait for wallet to close sign data sheet") {
            waitFor(UI_DISMISS_TIMEOUT_MS) { !walletController.isSignDataRequestVisible() }
        }

        step("Wait for dApp to receive sign data response") {
            dAppController.openBrowser()
            dAppController.waitForDAppPage()
            dAppController.waitForSignDataResponse(timeoutMs = DAPP_RESPONSE_TIMEOUT_MS)
        }

        step("Verify result in browser") {
            dAppController.scrollToSignDataValidation()
            val isValid = dAppController.verifySignDataValidation()
            val validationText = dAppController.getSignDataValidationResult()
            dAppController.closeBrowserFully()
            assert(isValid) { "Sign Data validation failed - got: $validationText" }
        }
    }
}

/**
 * Test category annotation for connect tests.
 */
@Feature("Connect")
annotation class ConnectTest

/**
 * Test category annotation for send transaction tests.
 */
@Feature("Send Transaction")
annotation class SendTransactionTest

/**
 * Test category annotation for sign data tests.
 */
@Feature("Sign Data")
annotation class SignDataTest
