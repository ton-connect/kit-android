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
package io.ton.walletkit.demo.presentation.dev

import android.os.SystemClock
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

// Detects a burst of [requiredTaps] taps within [windowMs] and fires [onTrigger].
// Used as the gesture target for hidden dev toggles ("tap the balance 5 times").
// Tap count resets on idle gap > windowMs so accidental long-spaced taps don't
// accumulate forever.
fun Modifier.devToggleTaps(
    requiredTaps: Int = 5,
    windowMs: Long = 3_000L,
    onTrigger: () -> Unit,
): Modifier = this.then(
    Modifier.pointerInput(requiredTaps, windowMs) {
        var count = 0
        var firstAt = 0L
        detectTapGestures(onTap = {
            val now = SystemClock.elapsedRealtime()
            if (count == 0 || now - firstAt > windowMs) {
                count = 1
                firstAt = now
            } else {
                count++
            }
            if (count >= requiredTaps) {
                count = 0
                onTrigger()
            }
        })
    },
)
