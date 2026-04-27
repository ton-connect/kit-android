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
package io.ton.walletkit.demo.designsystem.theme

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

// Continuous-curvature ("squircle") rounded rectangle — the iOS / Figma corner shape.
//
// SwiftUI's `RoundedRectangle(cornerRadius:, style: .continuous)` extends each
// corner curve onto the adjacent edge so the radius derivative stays continuous,
// giving the softer look you see on Apple hardware. Compose's stock
// [androidx.compose.foundation.shape.RoundedCornerShape] is a circular arc — visibly
// different at radii ≥ 16dp.
//
// We approximate the iOS curve with one cubic per corner, smoothing factor 0.6 (the
// value Apple's renderer uses, per the Figma corner-smoothing reference). For
// fully-rounded shapes (capsule / circle) keep using [CircleShape] — both renderers
// degenerate to the same circle there.
@Stable
class SmoothCornerShape(
    val radius: Dp,
    val smoothing: Float = 0.6f,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val rPx = with(density) { radius.toPx() }
        val r = rPx.coerceAtMost(size.minDimension / 2f)
        val s = r * smoothing
        val w = size.width
        val h = size.height

        val path = Path().apply {
            // Start at top-left, just past the smoothed corner extension.
            moveTo(r + s, 0f)

            // Top edge, then top-right corner.
            lineTo(w - r - s, 0f)
            cubicTo(
                w - r + s / 2f,
                0f,
                w,
                r - s / 2f,
                w,
                r + s,
            )

            // Right edge, then bottom-right corner.
            lineTo(w, h - r - s)
            cubicTo(
                w,
                h - r + s / 2f,
                w - r + s / 2f,
                h,
                w - r - s,
                h,
            )

            // Bottom edge, then bottom-left corner.
            lineTo(r + s, h)
            cubicTo(
                r - s / 2f,
                h,
                0f,
                h - r + s / 2f,
                0f,
                h - r - s,
            )

            // Left edge, then top-left corner.
            lineTo(0f, r + s)
            cubicTo(
                0f,
                r - s / 2f,
                r - s / 2f,
                0f,
                r + s,
                0f,
            )

            close()
        }
        return Outline.Generic(path)
    }
}
