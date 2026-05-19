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
package io.ton.walletkit.demo.designsystem.components.icon

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.ton.walletkit.demo.designsystem.theme.TonTheme

// Single-glyph circle-X. Mirrors iOS SF Symbol `xmark.circle.fill` with two color
// slots: filled circle background + darker stroke X over it. Drawn on Canvas so
// callers don't have to compose a Box + drawable + ColorFilter chain themselves.
//
// Defaults match the iOS pattern of `.foregroundStyle(.tonTextTertiary, .tonBgLightGray)`
// (foreground = X stroke, background = circle fill).
@Composable
fun TonCloseCircleIcon(
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    backgroundColor: Color = TonTheme.colors.bgLightGray,
    foregroundColor: Color = TonTheme.colors.textTertiary,
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val r = minOf(w, h) / 2f

        // Filled circle background.
        drawCircle(color = backgroundColor, radius = r, center = Offset(w / 2f, h / 2f))

        // X mark — inset by ~32% of the radius on each side, matching SF Symbol metrics.
        val inset = r * 0.45f
        val strokeWidth = r * 0.13f
        drawLine(
            color = foregroundColor,
            start = Offset(w / 2f - inset, h / 2f - inset),
            end = Offset(w / 2f + inset, h / 2f + inset),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = foregroundColor,
            start = Offset(w / 2f + inset, h / 2f - inset),
            end = Offset(w / 2f - inset, h / 2f + inset),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
    }
}
