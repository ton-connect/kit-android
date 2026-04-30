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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.ton.walletkit.demo.designsystem.components.text.TonText
import io.ton.walletkit.demo.designsystem.icons.TonIcon
import io.ton.walletkit.demo.designsystem.icons.TonIconImage
import io.ton.walletkit.demo.designsystem.theme.SmoothCornerShape
import io.ton.walletkit.demo.designsystem.theme.TonTheme

// Wallet "action" button — vertical icon (24px) over short caption.
// Spec: padding (top 7, bottom 8, h 24), gap 2, rounded-12, label SF Pro Medium 11 (Caption 2 Medium).
// Primary: bg = accent blue, text/icon = white.
// Secondary: bg = 10% accent blue, text/icon = brand.
enum class TonActionButtonStyle { Primary, Secondary }

@Composable
fun TonActionButton(
    icon: TonIcon,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: TonActionButtonStyle = TonActionButtonStyle.Primary,
) {
    val (background: Color, foreground: Color) = when (style) {
        TonActionButtonStyle.Primary -> TonTheme.colors.bgBrand to TonTheme.colors.textOnBrand
        TonActionButtonStyle.Secondary -> TonTheme.colors.bgBrandFillSubtle to TonTheme.colors.textBrand
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(SmoothCornerShape(12.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp)
            .padding(top = 7.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        TonIconImage(icon = icon, size = 24.dp, tint = foreground)
        TonText(
            text = title,
            style = TonTheme.typography.caption2Medium,
            color = foreground,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TonActionButtonPreview() {
    TonTheme {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            TonActionButton(
                icon = TonIcon.ArrowUpCircle,
                title = "Send",
                style = TonActionButtonStyle.Primary,
                onClick = {},
                modifier = Modifier.weight(1f),
            )
            TonActionButton(
                icon = TonIcon.ArrowDownCircle,
                title = "Receive",
                style = TonActionButtonStyle.Secondary,
                onClick = {},
                modifier = Modifier.weight(1f),
            )
            TonActionButton(
                icon = TonIcon.SwitchVertical24,
                title = "Swap",
                style = TonActionButtonStyle.Secondary,
                onClick = {},
                modifier = Modifier.weight(1f),
            )
        }
    }
}
