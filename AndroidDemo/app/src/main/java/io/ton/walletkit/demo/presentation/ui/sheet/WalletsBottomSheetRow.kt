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
package io.ton.walletkit.demo.presentation.ui.sheet

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.ton.walletkit.demo.designsystem.components.text.TonText
import io.ton.walletkit.demo.designsystem.icons.TonIcon
import io.ton.walletkit.demo.designsystem.icons.TonIconImage
import io.ton.walletkit.demo.designsystem.theme.TonTheme

private val IconSize = 48.dp

// Single wallet row inside [WalletsBottomSheet]. Tapping the leading area selects the
// wallet; the trailing copy / tick / more icons each have their own hit targets.
@Composable
fun WalletsBottomSheetRow(
    title: String,
    truncatedAddress: String,
    isActive: Boolean,
    onSelect: () -> Unit,
    onCopyAddress: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onSelect),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(IconSize)
                    .clip(CircleShape)
                    .background(TonTheme.colors.bgFillTertiary),
                contentAlignment = Alignment.Center,
            ) {
                TonIconImage(
                    icon = TonIcon.Wallet,
                    size = 24.dp,
                    tint = TonTheme.colors.textSecondary,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                TonText(
                    text = title,
                    style = TonTheme.typography.bodySemibold,
                    color = TonTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TonText(
                        text = truncatedAddress,
                        style = TonTheme.typography.subheadline2,
                        color = TonTheme.colors.textSecondary,
                    )
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clickable(onClick = onCopyAddress),
                        contentAlignment = Alignment.Center,
                    ) {
                        TonIconImage(
                            icon = TonIcon.Copy,
                            size = 16.dp,
                            tint = TonTheme.colors.textSecondary,
                        )
                    }
                }
            }
        }

        if (isActive) {
            TonIconImage(
                icon = TonIcon.Tick,
                size = 20.dp,
                tint = TonTheme.colors.textBrand,
            )
        }

        Box(
            modifier = Modifier
                .size(24.dp)
                .clickable(onClick = onMore),
            contentAlignment = Alignment.Center,
        ) {
            TonIconImage(
                icon = TonIcon.More,
                size = 24.dp,
                tint = TonTheme.colors.textSecondary,
            )
        }
    }
}
