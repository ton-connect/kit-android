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
package io.ton.walletkit.demo.designsystem.components.badge

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.ton.walletkit.demo.designsystem.components.text.TonText
import io.ton.walletkit.demo.designsystem.theme.SmoothCornerShape
import io.ton.walletkit.demo.designsystem.theme.TonTheme

// Spec: rounded-6, padding (top=2, bottom=1, h=4), font Footnote/CAPS.
// Filled: bg accent blue + text white. Gray: bg light gray + text dark gray.
enum class TonBadgeStyle { Filled, Gray }

@Composable
fun TonBadge(
    title: String,
    modifier: Modifier = Modifier,
    style: TonBadgeStyle = TonBadgeStyle.Filled,
) {
    val (background: Color, foreground: Color) = when (style) {
        TonBadgeStyle.Filled -> TonTheme.colors.bgBrand to TonTheme.colors.textOnBrand
        TonBadgeStyle.Gray -> TonTheme.colors.bgLightGray to TonTheme.colors.textSecondary
    }

    TonText(
        text = title,
        style = TonTheme.typography.footnoteCaps,
        color = foreground,
        modifier = modifier
            .clip(SmoothCornerShape(6.dp))
            .background(background)
            .padding(horizontal = 4.dp)
            .padding(top = 2.dp, bottom = 1.dp),
    )
}

@Preview(showBackground = true)
@Composable
private fun TonBadgePreview() {
    TonTheme {
        androidx.compose.foundation.layout.Row(
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            TonBadge("NEW", style = TonBadgeStyle.Filled)
            TonBadge("PRO", style = TonBadgeStyle.Gray)
        }
    }
}
