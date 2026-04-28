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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

// PIN entry: visible dot row + invisible TextField that opens the system numeric
// keypad. Tapping the dots refocuses the field. Non-digit input is filtered.
// [onComplete] fires after a short grace period once the entered value reaches
// [length] characters — long enough that the user sees all slots fill before the
// screen transitions away.
private const val COMPLETE_GRACE_MS = 800L

@Composable
fun TonPinField(
    pin: String,
    onPinChange: (String) -> Unit,
    isError: Boolean,
    onComplete: (String) -> Unit,
    modifier: Modifier = Modifier,
    length: Int = 4,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val currentOnComplete by rememberUpdatedState(onComplete)
    LaunchedEffect(pin) {
        if (pin.length == length) {
            delay(COMPLETE_GRACE_MS)
            currentOnComplete(pin)
        }
    }

    Box(
        modifier = modifier
            .clickable { focusRequester.requestFocus() },
        contentAlignment = Alignment.Center,
    ) {
        BasicTextField(
            value = pin,
            onValueChange = { newValue ->
                val digits = newValue.filter { it.isDigit() }.take(length)
                onPinChange(digits)
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            modifier = Modifier
                .size(1.dp)
                .alpha(0f)
                .focusRequester(focusRequester),
        )

        TonPinDots(pin = pin, length = length, isError = isError)
    }
}
