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
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import io.qameta.allure.kotlin.Allure
import io.qameta.allure.kotlin.Epic
import io.qameta.allure.kotlin.Feature
import io.qameta.allure.kotlin.Step
import io.ton.walletkit.demo.e2e.dapp.JsDAppController
import io.ton.walletkit.demo.e2e.wallet.WalletController
import org.junit.After
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

    @After
    open fun tearDown() {
        // Override in subclasses if needed
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
     * Ensure wallet is ready with a forced clean state.
     * This clears app data first to ensure the test mnemonic wallet is imported fresh.
     * Use this for tests that require a specific wallet (SendTransaction, SignData).
     */
    protected fun ensureWalletReadyClean() {
        android.util.Log.d("BaseE2ETest", "ensureWalletReadyClean - clearing app data first")
        clearAppData()
        // Note: After clearing data, the app needs to restart to pick up the changes
        // The walletController.setupWallet will handle the fresh setup
        android.util.Log.d("BaseE2ETest", "App data cleared, setting up wallet...")
        walletController.setupWallet(TEST_MNEMONIC, TEST_PASSWORD)
        walletController.waitForWalletHome()
        android.util.Log.d("BaseE2ETest", "Wallet is ready on home screen (clean state)")
    }

    /**
     * Clear app data to ensure clean state.
     * This clears the EncryptedSharedPreferences files used by the demo app.
     */
    protected fun clearAppData() {
        // Clear the encrypted shared preferences files
        // These are the actual names used by SecureDemoAppStorage
        val prefsToDelete = listOf(
            "walletkit_demo_wallets",
            "walletkit_demo_prefs",
        )

        for (prefName in prefsToDelete) {
            try {
                // Clear the SharedPreferences
                context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
                    .edit()
                    .clear()
                    .commit()

                // Also delete the file itself (for EncryptedSharedPreferences)
                val prefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
                val prefsFile = File(prefsDir, "$prefName.xml")
                if (prefsFile.exists()) {
                    prefsFile.delete()
                }
            } catch (e: Exception) {
                // Ignore errors - file might not exist or be inaccessible
            }
        }

        // Also clear any files/cache
        context.filesDir?.deleteRecursively()
        context.cacheDir?.deleteRecursively()

        // Clear datastore if present
        val datastoreDir = File(context.filesDir, "datastore")
        if (datastoreDir.exists()) {
            datastoreDir.deleteRecursively()
        }
    }

    /**
     * Get test case data from Allure TestOps.
     */
    protected fun getTestCaseData(allureId: String): TestCaseData? = allureClient?.getTestCaseData(allureId)

    /**
     * Setup wallet and connect to dApp.
     * This is a common flow used by SendTransaction and SignData tests.
     *
     * @return true if connection was established successfully
     */
    protected fun setupWalletAndConnect(): Boolean {
        step("Set up wallet") {
            walletController.setupWallet(TEST_MNEMONIC, TEST_PASSWORD)
        }

        step("Wait for wallet home screen") {
            walletController.waitForWalletHome()
        }

        step("Get TonConnect URL from dApp") {
            // Open browser
            dAppController.openBrowser()
            dAppController.waitForDAppPage()

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

        step("Approve connection") {
            walletController.approveConnect()
        }

        step("Verify connection established") {
            waitFor(timeoutMs = 5000) {
                !walletController.isConnectRequestVisible()
            }
        }

        return true
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
