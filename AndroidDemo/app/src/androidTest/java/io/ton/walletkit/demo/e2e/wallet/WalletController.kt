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
package io.ton.walletkit.demo.e2e.wallet

import android.util.Log
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.rules.ActivityScenarioRule
import io.qameta.allure.kotlin.Step
import io.ton.walletkit.demo.presentation.MainActivity
import io.ton.walletkit.demo.presentation.util.TestTags

/**
 * Controller for interacting with the wallet demo app via Espresso/Compose testing.
 *
 * This mirrors the wallet controller concept from the web demo-wallet.
 */
class WalletController(composeTestRule: ComposeTestRule? = null) {

    companion object {
        // Timeouts - longer for CI emulators which are slower than local machines
        const val SHEET_APPEAR_TIMEOUT = 30_000L // Wait for sheets to appear (depends on WebView/network)
        const val BUTTON_ENABLE_TIMEOUT = 15_000L // Wait for buttons to become enabled
        const val UI_IDLE_TIMEOUT = 10_000L // General UI idle/animation timeout

        const val PIN_LENGTH = 4
        const val DEFAULT_PIN = "1234"

        // Sign-data approve uses TonHoldToSignButton (DEFAULT_HOLD_DURATION_MS = 700ms). Press
        // for noticeably longer so the test isn't racy with the fill animation completing.
        const val SIGN_DATA_HOLD_DURATION_MS = 1_200L
    }

    private lateinit var composeTestRule: ComposeTestRule
    private var androidComposeTestRule: AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>? = null

    init {
        composeTestRule?.let { this.composeTestRule = it }
    }

    /**
     * Initialize the controller with a ComposeTestRule.
     */
    fun init(rule: ComposeTestRule) {
        this.composeTestRule = rule
        if (rule is AndroidComposeTestRule<*, *>) {
            @Suppress("UNCHECKED_CAST")
            androidComposeTestRule = rule as? AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>
        }
    }

    // ===========================================
    // Wallet Setup Actions
    // ===========================================

    /**
     * Check if we're on the legacy wallet home screen (the only screen the e2e suite knows how
     * to drive). The legacy toolbar exposes [TestTags.BROWSER_NO_INJECT_BUTTON]; the modern
     * [WalletScreen] does not. Returns true only when the legacy home is visible AND the
     * [AddWalletSheet] is not in front of it.
     */
    fun isOnHomeScreen(): Boolean {
        if (!isOnLegacyHome()) return false
        return !isAddWalletSheetShowing()
    }

    private fun isOnLegacyHome(): Boolean = try {
        composeTestRule.onNodeWithTag(TestTags.BROWSER_NO_INJECT_BUTTON).assertExists()
        true
    } catch (e: AssertionError) {
        false
    }

    /**
     * The modern [WalletScreen] is the default for new installs. It carries no legacy testTags
     * but exposes [TestTags.WALLET_BALANCE] on the balance area, which is also the 5-tap secret
     * gesture target that toggles back to the legacy screen.
     */
    private fun isOnModernHome(): Boolean = try {
        composeTestRule.onNodeWithTag(TestTags.WALLET_BALANCE).assertExists()
        true
    } catch (e: AssertionError) {
        false
    }

    /**
     * Handle the authentication flow - either setup, unlock, or skip if already on home.
     * Automatically detects which screen we're on.
     */
    @Step("Authenticate")
    fun authenticate(pin: String = DEFAULT_PIN) {
        composeTestRule.waitForIdle()
        // Give app time to settle after activity start

        // Wait for one of the expected screens to appear (up to 15 seconds)
        // This handles the case when the app is starting up
        var screenDetected = false
        var attempts = 0
        val maxAttempts = 30 // 30 * 500ms = 15 seconds

        while (!screenDetected && attempts < maxAttempts) {
            composeTestRule.waitForIdle()

            // Check if already on home screen (no auth needed)
            if (isOnHomeScreen()) {
                Log.d("WalletController", "Detected home screen - no authentication needed")
                return
            }

            // Check if on unlock screen FIRST (more likely if wallet exists from previous test)
            val onUnlock = try {
                composeTestRule.onNodeWithTag(TestTags.UNLOCK_PIN_FIELD)
                    .assertExists()
                true
            } catch (e: AssertionError) {
                false
            }

            if (onUnlock) {
                Log.d("WalletController", "Detected unlock screen")
                unlockPinSafe(pin)
                screenDetected = true
                return
            }

            // Check if on create PIN screen
            val onSetup = try {
                composeTestRule.onNodeWithTag(TestTags.CREATE_PIN_FIELD)
                    .assertExists()
                true
            } catch (e: AssertionError) {
                false
            }

            if (onSetup) {
                Log.d("WalletController", "Detected create PIN screen")
                createPinSafe(pin)
                screenDetected = true
                return
            }

            // None detected yet, wait and try again
            Log.d("WalletController", "No auth screen detected yet (attempt ${attempts + 1}/$maxAttempts)")
            attempts++
        }

        // If we get here, check one more time for home screen (maybe transitions happened)
        if (isOnHomeScreen()) {
            Log.d("WalletController", "Detected home screen after waiting")
            return
        }

        // Last resort - try unlock as fallback
        Log.w("WalletController", "No auth screen detected after $maxAttempts attempts, trying unlock")
        unlockPinSafe(pin)
    }

    /**
     * Safe version of createPin — types the PIN twice (entering → confirming) and taps Save.
     *
     * The PIN field has an 800ms grace before [onComplete] fires (so the user can see all dots
     * fill before the screen transitions), so we don't fire the second [performTextInput] until
     * the Save button appears — that's the observable signal the screen is in the Confirming
     * phase and the field has been reset to empty.
     */
    private fun createPinSafe(pin: String) {
        try {
            require(pin.length == PIN_LENGTH) {
                "PIN must be $PIN_LENGTH digits, got '${pin.length}'"
            }

            // First entry. We let the screen's own grace + state change advance us to the
            // Confirming phase rather than firing both inputs back-to-back (the second input
            // would otherwise append to a still-populated field and get capped at 4 digits).
            composeTestRule.onNodeWithTag(TestTags.CREATE_PIN_FIELD)
                .performTextInput(pin)

            composeTestRule.waitUntil(UI_IDLE_TIMEOUT) {
                composeTestRule.onAllNodesWithTag(TestTags.CREATE_PIN_SAVE_BUTTON)
                    .fetchSemanticsNodes().isNotEmpty()
            }

            // Second entry — Confirming phase has an empty field, so performTextInput types the
            // full 4 digits cleanly.
            composeTestRule.onNodeWithTag(TestTags.CREATE_PIN_FIELD)
                .performTextInput(pin)

            // Save becomes enabled once the confirming field has 4 digits.
            composeTestRule.waitUntil(UI_IDLE_TIMEOUT) {
                try {
                    composeTestRule.onNodeWithTag(TestTags.CREATE_PIN_SAVE_BUTTON)
                        .assertIsEnabled()
                    true
                } catch (e: AssertionError) {
                    false
                }
            }

            composeTestRule.onNodeWithTag(TestTags.CREATE_PIN_SAVE_BUTTON)
                .performClick()

            composeTestRule.waitForIdle()
        } catch (e: Exception) {
            Log.w("WalletController", "createPinSafe failed: ${e.message}")
        }
    }

    /**
     * Safe version of unlockPin — types the PIN; the screen fires onComplete after an 800ms
     * grace once 4 digits are typed. We wait for the unlock screen to disappear before
     * returning so the caller doesn't race the transition.
     */
    private fun unlockPinSafe(pin: String) {
        try {
            require(pin.length == PIN_LENGTH) {
                "PIN must be $PIN_LENGTH digits, got '${pin.length}'"
            }
            composeTestRule.onNodeWithTag(TestTags.UNLOCK_PIN_FIELD)
                .performTextInput(pin)

            // Wait for the unlock field to disappear — confirms the grace fired and the wallet
            // has navigated away from UnlockPinScreen.
            try {
                composeTestRule.waitUntil(UI_IDLE_TIMEOUT) {
                    composeTestRule.onAllNodesWithTag(TestTags.UNLOCK_PIN_FIELD)
                        .fetchSemanticsNodes().isEmpty()
                }
            } catch (e: Exception) {
                Log.w("WalletController", "Unlock screen did not dismiss within timeout: ${e.message}")
            }
            composeTestRule.waitForIdle()
            Log.d("WalletController", "Unlock completed")
        } catch (e: Exception) {
            Log.w("WalletController", "unlockPinSafe failed: ${e.message}")
        }
    }

    /**
     * Import a wallet using a 24-word mnemonic.
     */
    @Step("Import wallet with mnemonic")
    fun importWallet(mnemonic: List<String>) {
        Log.d("WalletController", "importWallet called with ${mnemonic.size} words")

        composeTestRule.waitForIdle()

        val landed = waitForOne(UI_IDLE_TIMEOUT) {
            isOnOnboardingScreen() || isOnImportScreen() || isAddWalletSheetShowing()
        }
        if (!landed) {
            Log.w("WalletController", "importWallet: no import surface detected within timeout")
        }

        when {
            isOnImportScreen() -> {
                Log.d("WalletController", "On ImportWalletScreen directly - entering mnemonic")
                enterMnemonicOnImportScreen(mnemonic)
            }
            isOnOnboardingScreen() -> {
                Log.d("WalletController", "On CreateWalletOnboardingScreen - tapping 'Add existing'")
                composeTestRule.onNodeWithTag(TestTags.ONBOARDING_IMPORT_WALLET_BUTTON).performClick()
                composeTestRule.waitForIdle()
                composeTestRule.waitUntil(UI_IDLE_TIMEOUT) { isOnImportScreen() }
                enterMnemonicOnImportScreen(mnemonic)
            }
            isAddWalletSheetShowing() -> {
                Log.d("WalletController", "Legacy AddWalletSheet visible - using paste-all field")
                importWalletViaLegacySheet(mnemonic)
            }
            else -> {
                Log.w("WalletController", "importWallet: no recognised import UI - aborting")
                return
            }
        }

        // Wait for wallet home (either modern or legacy) to appear after import succeeds.
        composeTestRule.waitForIdle()
        try {
            composeTestRule.waitUntil(15_000L) { isOnLegacyHome() || isOnModernHome() }
            Log.d("WalletController", "Wallet loaded successfully in UI")
        } catch (e: Exception) {
            Log.e("WalletController", "Wallet home still not visible after timeout: ${e.message}")
        }
        Log.d("WalletController", "After import - legacy: ${isOnLegacyHome()}, modern: ${isOnModernHome()}")
    }

    /** Modern import flow: paste the full phrase into the first tagged word field, tap Continue. */
    private fun enterMnemonicOnImportScreen(mnemonic: List<String>) {
        val mnemonicString = mnemonic.joinToString(" ")

        composeTestRule.waitUntil(UI_IDLE_TIMEOUT) {
            composeTestRule.onAllNodesWithTag(TestTags.IMPORT_WALLET_WORD_FIELD)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag(TestTags.IMPORT_WALLET_WORD_FIELD).performTextClearance()
        composeTestRule.onNodeWithTag(TestTags.IMPORT_WALLET_WORD_FIELD).performTextInput(mnemonicString)
        composeTestRule.waitForIdle()
        composeTestRule.waitUntil(UI_IDLE_TIMEOUT) {
            try {
                composeTestRule.onNodeWithTag(TestTags.IMPORT_WALLET_CONTINUE_BUTTON).assertIsEnabled()
                true
            } catch (e: AssertionError) {
                false
            }
        }
        composeTestRule.onNodeWithTag(TestTags.IMPORT_WALLET_CONTINUE_BUTTON).performClick()
        composeTestRule.waitForIdle()
    }

    /** Legacy AddWalletSheet flow (only reachable when the dev legacy-screen toggle is on). */
    private fun importWalletViaLegacySheet(mnemonic: List<String>) {
        val mnemonicString = mnemonic.joinToString(" ")

        composeTestRule.waitUntil(5_000L) {
            composeTestRule.onAllNodesWithTag(TestTags.MNEMONIC_FIELD)
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag(TestTags.MNEMONIC_FIELD).performTextClearance()
        composeTestRule.onNodeWithTag(TestTags.MNEMONIC_FIELD).performTextInput(mnemonicString)
        composeTestRule.waitForIdle()
        Thread.sleep(1_000L) // Mnemonic parsing inside the sheet is async; let it settle.

        try {
            composeTestRule.onNodeWithTag(TestTags.IMPORT_WALLET_PROCESS_BUTTON).performScrollTo()
        } catch (e: Exception) {
            Log.w("WalletController", "Could not scroll to legacy import button: ${e.message}")
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(TestTags.IMPORT_WALLET_PROCESS_BUTTON).performClick()
    }

    private fun isOnOnboardingScreen(): Boolean = try {
        composeTestRule.onNodeWithTag(TestTags.ONBOARDING_IMPORT_WALLET_BUTTON).assertExists()
        true
    } catch (e: AssertionError) {
        false
    }

    private fun isOnImportScreen(): Boolean = try {
        composeTestRule.onNodeWithTag(TestTags.IMPORT_WALLET_WORD_FIELD).assertExists()
        true
    } catch (e: AssertionError) {
        false
    }

    private fun waitForOne(timeoutMs: Long, predicate: () -> Boolean): Boolean = try {
        composeTestRule.waitUntil(timeoutMs) { predicate() }
        true
    } catch (e: Exception) {
        false
    }

    /**
     * Check if the AddWalletSheet is currently showing.
     */
    private fun isAddWalletSheetShowing(): Boolean = try {
        composeTestRule.onNodeWithTag(TestTags.ADD_WALLET_TAB_GENERATE)
            .assertExists()
        true
    } catch (e: AssertionError) {
        false
    }

    /**
     * Complete the full wallet setup flow.
     * Handles both fresh setup (SetupPasswordScreen) and existing setup (UnlockWalletScreen).
     *
     * For e2e tests that need a specific wallet:
     * - If no wallet exists: sets up password and imports wallet
     * - If wallet already exists: unlocks and checks if AddWalletSheet is shown
     *   - If AddWalletSheet: imports wallet
     *   - If home screen: wallet already set up, skip import (assume same wallet)
     */
    @Step("Complete wallet setup")
    fun setupWallet(mnemonic: List<String>, pin: String = DEFAULT_PIN) {
        Log.d("WalletController", "=== setupWallet called with ${mnemonic.size} word mnemonic ===")
        composeTestRule.waitForIdle()

        // Check if we're already on the legacy home screen (wallet exists from a previous run
        // and the dev toggle is already flipped).
        if (isOnHomeScreen()) {
            Log.d("WalletController", "Already on legacy home screen - wallet exists, skipping setup")
            return
        }

        // Check if AddWalletSheet is already showing (password was set previously, dev toggle on)
        if (isAddWalletSheetShowing()) {
            Log.d("WalletController", "AddWalletSheet already showing - importing wallet directly")
            importWallet(mnemonic)
            ensureLegacyScreen()
            return
        }

        // Modern first-run lands on the onboarding screen (or the import screen if the user
        // already tapped through). Both are valid pre-wallet states.
        if (isOnOnboardingScreen() || isOnImportScreen()) {
            Log.d("WalletController", "Modern onboarding visible - importing wallet")
            importWallet(mnemonic)
            ensureLegacyScreen()
            return
        }

        // The post-import / post-unlock app lands on the modern WalletScreen by default.
        // Tests target legacy UI, so toggle if needed.
        if (isOnModernHome()) {
            Log.d("WalletController", "On modern WalletScreen - wallet exists, toggling to legacy")
            ensureLegacyScreen()
            return
        }

        // First authenticate (handles both setup and unlock)
        Log.d("WalletController", "Neither home nor AddWalletSheet detected, calling authenticate()...")
        authenticate(pin)

        // After authentication, wait for UI to stabilize
        composeTestRule.waitForIdle()
        Log.d("WalletController", "authenticate() completed, checking state...")

        // Check state after authentication
        for (i in 1..10) {
            composeTestRule.waitForIdle()

            // If on legacy home screen, wallet already exists and toggle was on
            if (isOnHomeScreen()) {
                Log.d("WalletController", "On legacy home after auth (check $i) - wallet exists, skipping import")
                return
            }

            // If AddWalletSheet is showing, import wallet
            if (isAddWalletSheetShowing()) {
                Log.d("WalletController", "AddWalletSheet showing after auth (check $i) - importing wallet")
                importWallet(mnemonic)
                ensureLegacyScreen()
                return
            }

            // Modern first-run onboarding (or already on the import screen).
            if (isOnOnboardingScreen() || isOnImportScreen()) {
                Log.d("WalletController", "Onboarding visible after auth (check $i) - importing wallet")
                importWallet(mnemonic)
                ensureLegacyScreen()
                return
            }

            // Default landing for an existing-wallet app: the modern WalletScreen with the balance
            // tile visible. Switch to legacy so the rest of the suite can drive it.
            if (isOnModernHome()) {
                Log.d("WalletController", "Modern home after auth (check $i) - toggling to legacy")
                ensureLegacyScreen()
                return
            }

            Log.d("WalletController", "Waiting for home/addWallet screen (check $i)...")
            Thread.sleep(200) // Small delay between checks
        }

        // Fallback: try to import wallet anyway (this may fail)
        Log.w("WalletController", "No expected screen detected after 10 checks, attempting import as fallback")
        importWallet(mnemonic)
        ensureLegacyScreen()
    }

    /**
     * Make sure we end up on the legacy wallet home. The new auth flow drops the user on
     * [WalletScreen] by default; e2e tests were written against [LegacyWalletScreen], which is
     * reached by the dev-only 5-tap secret on the balance tile. No-op if already on legacy.
     */
    @Step("Switch to legacy wallet screen")
    fun ensureLegacyScreen() {
        composeTestRule.waitForIdle()
        if (isOnLegacyHome()) {
            Log.d("WalletController", "ensureLegacyScreen: already on legacy home")
            return
        }

        // Wait for the modern home (with the WALLET_BALANCE tile) to be ready before tapping.
        try {
            composeTestRule.waitUntil(UI_IDLE_TIMEOUT) { isOnModernHome() }
        } catch (e: Exception) {
            Log.w("WalletController", "ensureLegacyScreen: timed out waiting for modern home: ${e.message}")
        }

        if (!isOnModernHome()) {
            Log.w("WalletController", "ensureLegacyScreen: modern home not visible, skipping toggle")
            return
        }

        // DevToggleTaps requires 5 taps inside a 3-second window. performClick runs on the test
        // thread back-to-back so they always land inside that window.
        Log.d("WalletController", "ensureLegacyScreen: performing 5 quick taps on WALLET_BALANCE")
        repeat(5) {
            composeTestRule.onNodeWithTag(TestTags.WALLET_BALANCE).performClick()
        }
        composeTestRule.waitForIdle()

        try {
            composeTestRule.waitUntil(UI_IDLE_TIMEOUT) { isOnLegacyHome() }
            Log.d("WalletController", "ensureLegacyScreen: legacy home is now visible")
        } catch (e: Exception) {
            Log.w("WalletController", "ensureLegacyScreen: legacy home did not appear: ${e.message}")
        }
        composeTestRule.waitForIdle()
    }

    // ===========================================
    // TonConnect URL Handling
    // ===========================================

    /**
     * Connect to a dApp by pasting a TonConnect URL.
     * This opens the "Handle URL" dialog, pastes the URL, and processes it.
     *
     * @param url The TonConnect URL (tc://... or https://...)
     */
    @Step("Connect by TonConnect URL")
    fun connectByUrl(url: String) {
        // Wait for wallet home screen with Handle URL button
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithTag(TestTags.HANDLE_URL_BUTTON)
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Click "Handle URL" button to open dialog
        composeTestRule.onNodeWithTag(TestTags.HANDLE_URL_BUTTON)
            .performClick()

        composeTestRule.waitForIdle()

        // Wait for the URL input field in the dialog
        composeTestRule.waitUntil(3000) {
            composeTestRule.onAllNodesWithTag(TestTags.TONCONNECT_URL_FIELD)
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Enter the TonConnect URL
        composeTestRule.onNodeWithTag(TestTags.TONCONNECT_URL_FIELD)
            .performTextInput(url)

        // Click the process button
        composeTestRule.onNodeWithTag(TestTags.TONCONNECT_PROCESS_BUTTON)
            .performClick()

        composeTestRule.waitForIdle()
    }

    /**
     * Wait for the (legacy) wallet home screen to be visible. The e2e suite runs against the
     * legacy screen, so [setupWallet] ensures we are toggled there before this is called.
     */
    @Step("Wait for wallet home screen")
    fun waitForWalletHome() {
        composeTestRule.waitUntil(SHEET_APPEAR_TIMEOUT) {
            composeTestRule.onAllNodesWithTag(TestTags.WALLET_ADDRESS)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    // ===========================================
    // TonConnect Actions
    // ===========================================

    /**
     * Approve a TonConnect connection request.
     */
    @Step("Approve connection request")
    fun approveConnect() {
        // Wait for connect request sheet
        composeTestRule.waitUntil(SHEET_APPEAR_TIMEOUT) {
            composeTestRule.onAllNodesWithTag(TestTags.CONNECT_REQUEST_SHEET)
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Wait a bit for the sheet to fully load and button to become enabled
        Thread.sleep(500)
        composeTestRule.waitForIdle()

        // Wait for button to be enabled (may take time to verify manifest)
        composeTestRule.waitUntil(BUTTON_ENABLE_TIMEOUT) {
            try {
                composeTestRule.onNodeWithTag(TestTags.CONNECT_APPROVE_BUTTON)
                    .assertIsEnabled()
                true
            } catch (e: AssertionError) {
                false
            }
        }

        // Click approve button
        composeTestRule.onNodeWithTag(TestTags.CONNECT_APPROVE_BUTTON)
            .performClick()

        composeTestRule.waitForIdle()
    }

    /**
     * Reject a TonConnect connection request.
     */
    @Step("Reject connection request")
    fun rejectConnect() {
        // Wait for connect request sheet
        composeTestRule.waitUntil(SHEET_APPEAR_TIMEOUT) {
            composeTestRule.onAllNodesWithTag(TestTags.CONNECT_REQUEST_SHEET)
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Wait a bit for the sheet to fully load
        Thread.sleep(500)
        composeTestRule.waitForIdle()

        // Click reject button
        composeTestRule.onNodeWithTag(TestTags.CONNECT_REJECT_BUTTON)
            .performClick()

        composeTestRule.waitForIdle()
    }

    /**
     * Approve a transaction request.
     */
    @Step("Approve transaction request")
    fun approveTransaction() {
        Log.d("WalletController", "approveTransaction: waiting for sheet")
        // Wait for transaction request sheet
        composeTestRule.waitUntil(SHEET_APPEAR_TIMEOUT) {
            composeTestRule.onAllNodesWithTag(TestTags.TRANSACTION_REQUEST_SHEET)
                .fetchSemanticsNodes().isNotEmpty()
        }

        Log.d("WalletController", "approveTransaction: clicking approve button")
        // Click approve button
        composeTestRule.onNodeWithTag(TestTags.TRANSACTION_APPROVE_BUTTON)
            .performClick()

        Log.d("WalletController", "approveTransaction: waiting for idle")
        composeTestRule.waitForIdle()
        Log.d("WalletController", "approveTransaction: done")
    }

    /**
     * Reject a transaction request.
     */
    @Step("Reject transaction request")
    fun rejectTransaction() {
        // Wait for transaction request sheet
        composeTestRule.waitUntil(SHEET_APPEAR_TIMEOUT) {
            composeTestRule.onAllNodesWithTag(TestTags.TRANSACTION_REQUEST_SHEET)
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Click reject button
        composeTestRule.onNodeWithTag(TestTags.TRANSACTION_REJECT_BUTTON)
            .performClick()

        composeTestRule.waitForIdle()
    }

    /**
     * Approve a sign data request.
     *
     * The redesigned sheet uses a hold-to-sign button — `onComplete` fires only after a
     * sustained ~700ms press (DEFAULT_HOLD_DURATION_MS in TonHoldToSignButton). On
     * instrumented tests `advanceEventTime` only stamps the next input event; it does
     * not actually advance the device's frame clock, so the underlying animation never
     * gets time to reach 1.0. We split the touch into two `performTouchInput` calls
     * with a real `Thread.sleep` between them so the device's frame loop drives the
     * animation to completion before we release.
     */
    @Step("Approve sign data request")
    fun approveSignData() {
        composeTestRule.waitUntil(SHEET_APPEAR_TIMEOUT) {
            composeTestRule.onAllNodesWithTag(TestTags.SIGN_DATA_SHEET)
                .fetchSemanticsNodes().isNotEmpty()
        }

        val node = composeTestRule.onNodeWithTag(TestTags.SIGN_DATA_APPROVE_BUTTON)
        node.performTouchInput { down(center) }
        Thread.sleep(SIGN_DATA_HOLD_DURATION_MS)
        node.performTouchInput { up() }
        composeTestRule.waitForIdle()
    }

    /**
     * Reject a sign data request.
     */
    @Step("Reject sign data request")
    fun rejectSignData() {
        // Wait for sign data sheet
        composeTestRule.waitUntil(SHEET_APPEAR_TIMEOUT) {
            composeTestRule.onAllNodesWithTag(TestTags.SIGN_DATA_SHEET)
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Click reject button
        composeTestRule.onNodeWithTag(TestTags.SIGN_DATA_REJECT_BUTTON)
            .performClick()

        composeTestRule.waitForIdle()
    }

    // ===========================================
    // Verification
    // ===========================================

    /**
     * Check if the connect request sheet is visible.
     */
    fun isConnectRequestVisible(): Boolean = try {
        composeTestRule.onNodeWithTag(TestTags.CONNECT_REQUEST_SHEET)
            .assertIsDisplayed()
        true
    } catch (e: AssertionError) {
        false
    }

    /**
     * Check if the transaction request sheet is visible.
     */
    fun isTransactionRequestVisible(): Boolean = try {
        composeTestRule.onNodeWithTag(TestTags.TRANSACTION_REQUEST_SHEET)
            .assertIsDisplayed()
        true
    } catch (e: AssertionError) {
        false
    }

    /**
     * Check if the sign data sheet is visible.
     */
    fun isSignDataRequestVisible(): Boolean = try {
        composeTestRule.onNodeWithTag(TestTags.SIGN_DATA_SHEET)
            .assertIsDisplayed()
        true
    } catch (e: AssertionError) {
        false
    }
}
