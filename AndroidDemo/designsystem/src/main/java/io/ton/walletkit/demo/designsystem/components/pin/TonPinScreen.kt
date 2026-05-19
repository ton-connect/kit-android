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
package io.ton.walletkit.demo.designsystem.components.pin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.ton.walletkit.demo.designsystem.components.text.TonText
import io.ton.walletkit.demo.designsystem.theme.TonTheme

// Shared layout for both PIN-creation and PIN-unlock screens.
// Matches figma `40001176:24957` / `40001176:28800` / `40001176:25021` / `40001176:25043`:
//   • white background
//   • centered title (Inter Bold 20) + optional description (Inter Regular 16, gray)
//   • 32dp gap → dots row
//   • below dots: error text (red) — shown whenever [error] != null, sits between dots and bottom slot
//   • bottom slot above keyboard: trailing slot (e.g. forgot link or primary Save button)
//
// The error string auto-clears the moment the user starts typing again, mirroring
// iOS — so the UX isn't "stuck red" once the user starts a fresh attempt.
@Composable
fun TonPinScreen(
    title: String,
    pin: String,
    onPinChange: (String) -> Unit,
    error: String?,
    onErrorChange: (String?) -> Unit,
    onComplete: (String) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    length: Int = 4,
    trailing: @Composable () -> Unit = {},
) {
    LaunchedEffect(pin) {
        if (pin.isNotEmpty() && error != null) onErrorChange(null)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TonTheme.colors.bgPrimary)
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Greedy spacer capped at 160dp — mirrors iOS `Spacer().frame(maxHeight: 160)`.
        // [Modifier.weight] makes it claim available space; [heightIn(max=160)] caps it
        // so on a tall screen the title block sits exactly 160dp from the top edge and
        // the remainder falls through to the bottom [Spacer], which pushes everything
        // toward the lower-middle of the screen (the Figma layout).
        Spacer(modifier = Modifier.weight(1f).heightIn(max = 160.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            TonText(
                text = title,
                style = TonTheme.typography.title3Bold,
                color = TonTheme.colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            if (!description.isNullOrEmpty()) {
                TonText(
                    text = description,
                    style = TonTheme.typography.callout,
                    color = TonTheme.colors.textTertiary,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        TonPinField(
            pin = pin,
            onPinChange = onPinChange,
            isError = error != null,
            onComplete = onComplete,
            length = length,
        )

        // Reserve the error row's vertical space at all times so the layout doesn't jump.
        TonText(
            text = error ?: " ",
            style = TonTheme.typography.callout,
            color = TonTheme.colors.textError,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(top = 24.dp)
                .alpha(if (error == null) 0f else 1f),
        )

        Spacer(modifier = Modifier.weight(1f))

        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            trailing()
        }
    }
}
