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
package io.ton.walletkit.demo.designsystem.components.button

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.ton.walletkit.demo.designsystem.components.text.TonText
import io.ton.walletkit.demo.designsystem.icons.TonIcon
import io.ton.walletkit.demo.designsystem.icons.TonIconImage
import io.ton.walletkit.demo.designsystem.theme.SmoothCornerShape
import io.ton.walletkit.demo.designsystem.theme.TonTheme

private val Height = 50.dp
private val CornerRadius = 12.dp
private val IconSize = 20.dp
private val IconLabelSpacing = 8.dp
private const val DEFAULT_HOLD_DURATION_MS = 700
private const val RELEASE_DURATION_MS = 150

/**
 * Primary action button that requires a sustained press to fire `onComplete`.
 *
 * Press-and-hold fills a lighter overlay across the button width over `holdDurationMs`.
 * Releasing early animates the fill back to zero. When the fill reaches 100%,
 * `onComplete` is invoked once and a confirm haptic plays.
 */
@Composable
fun TonHoldToSignButton(
    text: String,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: TonIcon? = TonIcon.Fingerprint,
    holdDurationMs: Int = DEFAULT_HOLD_DURATION_MS,
) {
    val colors = TonTheme.colors
    val haptics = LocalHapticFeedback.current
    val currentOnComplete by rememberUpdatedState(onComplete)

    var pressing by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(pressing, enabled) {
        if (!enabled) {
            progress.snapTo(0f)
            return@LaunchedEffect
        }
        if (pressing) {
            val remainingMs = ((1f - progress.value) * holdDurationMs).toInt().coerceAtLeast(0)
            progress.animateTo(1f, tween(durationMillis = remainingMs))
            if (progress.value >= 1f) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                progress.snapTo(0f)
                pressing = false
                currentOnComplete()
            }
        } else if (progress.value > 0f) {
            val backMs = (progress.value * RELEASE_DURATION_MS).toInt().coerceAtLeast(0)
            progress.animateTo(0f, tween(durationMillis = backMs))
        }
    }

    val backgroundColor = if (enabled) colors.bgBrand else colors.bgDisabled
    val fillColor = colors.bgBrandActive
    val labelColor = if (enabled) colors.textOnBrand else colors.textTertiary

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(Height)
            .clip(SmoothCornerShape(CornerRadius))
            .background(backgroundColor)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    pressing = true
                    try {
                        waitForUpOrCancellation()
                    } finally {
                        pressing = false
                    }
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.value.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(fillColor),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(IconLabelSpacing),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                icon?.let {
                    TonIconImage(icon = it, size = IconSize, tint = labelColor)
                }
                TonText(
                    text = text,
                    style = TonTheme.typography.bodySemibold,
                    color = labelColor,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TonHoldToSignButtonPreview() {
    TonTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            TonHoldToSignButton(text = "Hold to sign", onComplete = {})
        }
    }
}
