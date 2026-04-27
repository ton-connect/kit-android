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

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import io.ton.walletkit.demo.core.TonBiometric
import io.ton.walletkit.demo.designsystem.components.button.TonButton
import io.ton.walletkit.demo.designsystem.components.button.TonButtonConfig
import io.ton.walletkit.demo.designsystem.components.pin.TonPinScreen
import io.ton.walletkit.demo.presentation.util.TestTags
import kotlinx.coroutines.launch

private const val PIN_LENGTH = 4

// Two-step PIN setup mirroring iOS [CreatePinView] / [CreatePinViewModel]:
//   1. Entering — user types 4 digits, screen auto-advances on completion.
//   2. Confirming — user re-types and taps Save; mismatch resets to step 1
//      and surfaces an error.
//
// Local state only; the actual persistence is handled by the upstream callback
// (matches the existing `viewModel.setupPassword(...)` wiring). No biometry yet —
// follow-up to add `androidx.biometric` prompt after Save succeeds.
@Composable
fun CreatePinScreen(
    onPinSet: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val activity = LocalContext.current as? FragmentActivity
    val scope = rememberCoroutineScope()

    var phase by remember { mutableStateOf<CreatePinPhase>(CreatePinPhase.Entering) }
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val title = when (phase) {
        CreatePinPhase.Entering -> "Create PIN"
        is CreatePinPhase.Confirming -> "Enter your PIN again"
    }
    val description = "Create a 4-digit PIN to protect your wallet.\n" +
        "You can back up your wallet later in Settings."

    TonPinScreen(
        title = title,
        description = description,
        pin = pin,
        onPinChange = { pin = it },
        error = error,
        onErrorChange = { error = it },
        length = PIN_LENGTH,
        onComplete = { entered ->
            when (val current = phase) {
                CreatePinPhase.Entering -> {
                    phase = CreatePinPhase.Confirming(firstPin = entered)
                    pin = ""
                    error = null
                }
                is CreatePinPhase.Confirming -> {
                    // Save button drives the actual confirmation; auto-advance is
                    // entering-only to match iOS behaviour.
                    if (entered != current.firstPin) {
                        error = "The PIN codes do not match"
                        phase = CreatePinPhase.Entering
                        pin = ""
                    }
                }
            }
        },
        modifier = modifier.testTag(TestTags.CREATE_PIN_FIELD),
        trailing = {
            if (phase is CreatePinPhase.Confirming) {
                TonButton(
                    text = "Save",
                    onClick = {
                        val confirming = phase as? CreatePinPhase.Confirming ?: return@TonButton
                        if (pin.length == PIN_LENGTH && pin == confirming.firstPin) {
                            scope.launch {
                                // Mirrors iOS `requestBiometry()` after save — prompts the
                                // user to authenticate so the OS records biometric enrollment
                                // for this app. We don't gate save on the result.
                                if (activity != null && TonBiometric.isAvailable(activity)) {
                                    TonBiometric.authenticate(
                                        activity = activity,
                                        title = "Enable biometric unlock",
                                        subtitle = "Use biometrics to unlock the wallet next time.",
                                    )
                                }
                                onPinSet(pin)
                            }
                        } else {
                            error = "The PIN codes do not match"
                            phase = CreatePinPhase.Entering
                            pin = ""
                        }
                    },
                    enabled = pin.length == PIN_LENGTH,
                    config = TonButtonConfig.Primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag(TestTags.CREATE_PIN_SAVE_BUTTON),
                )
            }
        },
    )
}

private sealed interface CreatePinPhase {
    data object Entering : CreatePinPhase
    data class Confirming(val firstPin: String) : CreatePinPhase
}
