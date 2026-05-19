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
package io.ton.walletkit.demo.presentation.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import io.ton.walletkit.demo.core.TonBiometric
import io.ton.walletkit.demo.designsystem.components.button.TonButton
import io.ton.walletkit.demo.designsystem.components.button.TonButtonConfig
import io.ton.walletkit.demo.designsystem.components.icon.TonCloseCircleIcon
import io.ton.walletkit.demo.designsystem.components.pin.TonPinScreen
import io.ton.walletkit.demo.designsystem.components.text.TonText
import io.ton.walletkit.demo.designsystem.theme.TonTheme
import io.ton.walletkit.demo.presentation.util.TestTags

private const val PIN_LENGTH = 4

// PIN unlock screen mirroring iOS [UnlockPinView] / [UnlockPinViewModel]:
//   • 4-digit input with shake-on-error
//   • "Forgot your PIN code?" opens a bottom sheet with reset
//   • Biometric auto-unlock TODO — needs androidx.biometric wiring
@Composable
fun UnlockPinScreen(
    onUnlock: (String) -> Boolean,
    onUnlockBiometric: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val activity = LocalContext.current as? FragmentActivity

    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var showForgotSheet by remember { mutableStateOf(false) }

    // Biometric auto-attempt on first composition. Mirrors iOS `.task { tryBiometry... }`.
    // Returns false (no-ops) when no biometric is enrolled — same as iOS Simulator.
    LaunchedEffect(Unit) {
        if (activity != null && TonBiometric.isAvailable(activity)) {
            val ok = TonBiometric.authenticate(
                activity = activity,
                title = "Unlock wallet",
                subtitle = "Use biometrics to unlock your wallet.",
            )
            if (ok) onUnlockBiometric()
        }
    }

    TonPinScreen(
        title = "Enter your PIN",
        pin = pin,
        onPinChange = { pin = it },
        error = error,
        onErrorChange = { error = it },
        length = PIN_LENGTH,
        onComplete = { entered ->
            val ok = onUnlock(entered)
            if (!ok) {
                error = "Incorrect PIN. Please try again."
                pin = ""
            }
        },
        modifier = modifier.testTag(TestTags.UNLOCK_PIN_FIELD),
        trailing = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showForgotSheet = true }
                    .padding(vertical = 8.dp)
                    .testTag(TestTags.UNLOCK_PIN_FORGOT_BUTTON),
                contentAlignment = Alignment.Center,
            ) {
                TonText(
                    text = "Forgot your PIN code?",
                    style = TonTheme.typography.callout,
                    color = TonTheme.colors.textTertiary,
                )
            }
        },
    )

    if (showForgotSheet) {
        ForgotPinSheet(
            onReset = {
                showForgotSheet = false
                onReset()
            },
            onClose = { showForgotSheet = false },
        )
    }
}

// Bottom sheet from figma `40001176:28842`:
//   • white bg, rounded-top-16
//   • px-16 py-24
//   • Title (Inter Bold 20, primary, left-aligned)
//   • Description (Inter Regular 17 / Body, secondary, left-aligned)
//   • Bezeled "Reset wallet" button (secondary style — light blue bg, brand text)
//   • Top-right close button (44x44 with 28x28 filled-X icon)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ForgotPinSheet(
    onReset: () -> Unit,
    onClose: () -> Unit,
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = state,
        containerColor = TonTheme.colors.bgPrimary,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        dragHandle = null,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TonText(
                        text = "Forgot your PIN code?",
                        style = TonTheme.typography.title3Bold,
                        color = TonTheme.colors.textPrimary,
                    )
                    TonText(
                        text = "There is no password recovery option. If you created a wallet " +
                            "backup, you can restore the wallet using your backup passphrase " +
                            "and then set a new PIN code.",
                        style = TonTheme.typography.body,
                        color = TonTheme.colors.textSecondary,
                    )
                }

                TonButton(
                    text = "Reset wallet",
                    onClick = onReset,
                    config = TonButtonConfig.Secondary,
                    modifier = Modifier.testTag(TestTags.FORGOT_PIN_RESET_BUTTON),
                )
            }

            // Top-right close — 44dp hit target around a 28dp filled circle-X glyph.
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 8.dp)
                    .size(44.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .clickable(onClick = onClose)
                    .testTag(TestTags.FORGOT_PIN_CLOSE_BUTTON),
                contentAlignment = Alignment.Center,
            ) {
                TonCloseCircleIcon(size = 28.dp)
            }
        }
    }
}
