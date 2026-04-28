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
package io.ton.walletkit.demo.presentation.ui.components.wallet.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.ton.walletkit.demo.designsystem.components.badge.TonBadge
import io.ton.walletkit.demo.designsystem.components.badge.TonBadgeStyle
import io.ton.walletkit.demo.designsystem.components.text.TonText
import io.ton.walletkit.demo.designsystem.icons.TonIcon
import io.ton.walletkit.demo.designsystem.icons.TonIconImage
import io.ton.walletkit.demo.designsystem.theme.TonTheme

// Toolbar pill from iOS WalletHomeHeaderView: 36dp wallet-glyph circle on a tertiary
// fill background, then a column with [name + network badge] over [truncated address].
// Tapping anywhere triggers [onClick] (opens the wallets bottom sheet upstream).
@Composable
fun WalletHomeHeader(
    title: String,
    networkLabel: String,
    truncatedAddress: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(TonTheme.colors.bgFillTertiary),
            contentAlignment = Alignment.Center,
        ) {
            TonIconImage(
                icon = TonIcon.Wallet,
                size = 20.dp,
                tint = TonTheme.colors.textSecondary,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TonText(
                    text = title,
                    style = TonTheme.typography.bodySemibold,
                    color = TonTheme.colors.textPrimary,
                )
                TonBadge(title = networkLabel, style = TonBadgeStyle.Gray)
            }
            TonText(
                text = truncatedAddress,
                style = TonTheme.typography.subheadline2,
                color = TonTheme.colors.textSecondary,
            )
        }
    }
}
