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
package io.ton.walletkit.demo.presentation.ui.sheet.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.ton.walletkit.demo.designsystem.components.text.TonText
import io.ton.walletkit.demo.designsystem.icons.TonIcon
import io.ton.walletkit.demo.designsystem.icons.TonIconImage
import io.ton.walletkit.demo.designsystem.theme.SmoothCornerShape
import io.ton.walletkit.demo.designsystem.theme.TonTheme
import io.ton.walletkit.demo.presentation.model.WalletSummary
import io.ton.walletkit.demo.presentation.util.abbreviated

/**
 * Top section of every TonConnect sheet: paired dApp+wallet circles, title with the
 * domain highlighted in the brand colour, optional subtitle, and a close (X) button
 * in the top-right that fires `onClose` (which the sheet wires to `onReject`).
 */
@Composable
internal fun TonConnectSheetHeader(
    titleLeading: String,
    titleAccent: String,
    titleTrailing: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    dAppIconUrl: String? = null,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(28.dp)
                .clip(CircleShape)
                .background(TonTheme.colors.bgSecondary)
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            TonIconImage(icon = TonIcon.Close, size = 14.dp, tint = TonTheme.colors.textSecondary)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            PairedAvatars(dAppIconUrl = dAppIconUrl)
            Spacer(modifier = Modifier.height(16.dp))
            TonText(
                text = titleLeading.trimEnd(),
                style = TonTheme.typography.title2,
                color = TonTheme.colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            TonText(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = TonTheme.colors.textBrand)) {
                        append(titleAccent)
                        append(titleTrailing)
                    }
                },
                style = TonTheme.typography.title2,
                color = TonTheme.colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            subtitle?.let {
                Spacer(modifier = Modifier.height(8.dp))
                TonText(
                    text = it,
                    style = TonTheme.typography.subheadline2,
                    color = TonTheme.colors.textSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

private val AvatarShape = SmoothCornerShape(12.dp)
private val AvatarSize = 56.dp

@Composable
private fun PairedAvatars(dAppIconUrl: String?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(AvatarSize)
                .clip(AvatarShape)
                .background(TonTheme.colors.bgSecondary),
            contentAlignment = Alignment.Center,
        ) {
            if (dAppIconUrl != null) {
                AsyncImage(
                    model = dAppIconUrl,
                    contentDescription = null,
                    modifier = Modifier.size(AvatarSize).clip(AvatarShape),
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(AvatarSize)
                .clip(AvatarShape)
                .background(Color(0xFF31AA00)),
            contentAlignment = Alignment.Center,
        ) {
            TonIconImage(icon = TonIcon.Wallet, size = 28.dp, tint = TonTheme.colors.white)
        }
    }
}

/**
 * Section: small uppercase grey label + content rendered on a tinted card.
 * Mirrors the Figma section pattern (REQUESTED PERMISSIONS / DATA TO SIGN / etc.).
 */
@Composable
internal fun TonConnectSheetSection(
    label: String,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        TonText(
            text = label,
            style = TonTheme.typography.footnoteCaps,
            color = TonTheme.colors.textSecondary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(SmoothCornerShape(12.dp))
                .background(if (accent) TonTheme.colors.bgBrandSubtle else TonTheme.colors.bgSecondary)
                .padding(16.dp),
        ) {
            content()
        }
    }
}

/**
 * Wallet picker row used inside Connect/Sign sheets. Renders the currently selected
 * wallet with a chevron; if more than one wallet is provided, tapping the row toggles
 * an inline expansion that lists the alternatives. Single wallet → static, no chevron.
 */
@Composable
internal fun TonConnectWalletPicker(
    wallets: List<WalletSummary>,
    selected: WalletSummary?,
    onSelect: (WalletSummary) -> Unit,
    modifier: Modifier = Modifier,
) {
    val multi = wallets.size > 1
    var expanded by remember { mutableStateOf(false) }
    val current = selected ?: wallets.firstOrNull() ?: return

    val shape = SmoothCornerShape(12.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(TonTheme.colors.bgPrimary)
            .border(1.dp, TonTheme.colors.bgFillTertiary, shape),
    ) {
        WalletPickerRow(
            wallet = current,
            showPicker = true,
            onClick = if (multi) ({ expanded = !expanded }) else null,
        )
        if (expanded) {
            wallets.filter { it.address != current.address }.forEach { other ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(TonTheme.colors.bgFillTertiary),
                )
                WalletPickerRow(
                    wallet = other,
                    showPicker = false,
                    onClick = {
                        onSelect(other)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun WalletPickerRow(
    wallet: WalletSummary,
    showPicker: Boolean,
    onClick: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(SmoothCornerShape(8.dp))
                .background(TonTheme.colors.bgFillTertiary),
            contentAlignment = Alignment.Center,
        ) {
            TonIconImage(icon = TonIcon.Wallet, size = 20.dp, tint = TonTheme.colors.textSecondary)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            TonText(
                text = wallet.name,
                style = TonTheme.typography.bodySemibold,
                color = TonTheme.colors.textPrimary,
            )
            TonText(
                text = wallet.address.abbreviated(),
                style = TonTheme.typography.subheadline2,
                color = TonTheme.colors.textSecondary,
            )
        }
        if (showPicker) {
            UpDownChevron()
        }
    }
}

@Composable
private fun UpDownChevron() {
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TonIconImage(icon = TonIcon.ChevronTopSmall, size = 12.dp, tint = TonTheme.colors.textTertiary)
        TonIconImage(icon = TonIcon.ChevronDownSmall, size = 12.dp, tint = TonTheme.colors.textTertiary)
    }
}

/** Footer disclaimer text shown beneath the primary action. */
@Composable
internal fun TonConnectSheetDisclaimer(
    text: String,
    modifier: Modifier = Modifier,
) {
    TonText(
        text = text,
        style = TonTheme.typography.caption2Medium,
        color = TonTheme.colors.textTertiary,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
    )
}

/**
 * Single-line key/value row used for permissions inside the section card.
 */
@Composable
internal fun TonConnectPermissionRow(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        TonText(
            text = title,
            style = TonTheme.typography.bodySemibold,
            color = TonTheme.colors.textPrimary,
        )
        TonText(
            text = description,
            style = TonTheme.typography.subheadline2,
            color = TonTheme.colors.textSecondary,
        )
    }
}
