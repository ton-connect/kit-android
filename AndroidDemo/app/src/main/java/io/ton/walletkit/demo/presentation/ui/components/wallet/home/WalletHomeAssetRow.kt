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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import io.ton.walletkit.demo.designsystem.components.text.TonText
import io.ton.walletkit.demo.designsystem.icons.TonIcon
import io.ton.walletkit.demo.designsystem.icons.TonIconImage
import io.ton.walletkit.demo.designsystem.theme.TonTheme

// Mirrors iOS WalletHomeAssetItem.Icon — keeps the icon shape behind a sealed type
// so we can swap between the bundled TON brand mark, a network image URL, and a
// last-resort coloured initials placeholder without leaking the resolution to call
// sites.
@Immutable
sealed interface WalletHomeAssetIcon {
    data object Ton : WalletHomeAssetIcon
    data class Url(val url: String) : WalletHomeAssetIcon
    data class Placeholder(val symbol: String) : WalletHomeAssetIcon
}

@Immutable
data class WalletHomeAssetItem(
    val id: String,
    val name: String,
    val symbol: String,
    val formattedAmount: String,
    val icon: WalletHomeAssetIcon,
)

private val IconSize = 48.dp

@Composable
fun WalletHomeAssetRow(
    item: WalletHomeAssetItem,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.defaultMinSize(minHeight = IconSize),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AssetIcon(item = item)

        TonText(
            text = item.name,
            style = TonTheme.typography.bodySemibold,
            color = TonTheme.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Box(modifier = Modifier.weight(1f))

        TonText(
            text = item.formattedAmount,
            style = TonTheme.typography.bodySemibold,
            color = TonTheme.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AssetIcon(item: WalletHomeAssetItem) {
    Box(
        modifier = Modifier
            .size(IconSize)
            .clip(CircleShape)
            .background(TonTheme.colors.bgFillTertiary),
        contentAlignment = Alignment.Center,
    ) {
        when (val icon = item.icon) {
            WalletHomeAssetIcon.Ton -> TonIconImage(
                icon = TonIcon.Ton,
                size = IconSize,
                // No tint — TON brand mark ships its blue + white fills baked into the SVG.
            )
            is WalletHomeAssetIcon.Url -> AsyncImage(
                model = ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                    .data(icon.url)
                    .crossfade(true)
                    .build(),
                contentDescription = item.name,
                modifier = Modifier.size(IconSize),
                contentScale = ContentScale.Crop,
            )
            is WalletHomeAssetIcon.Placeholder -> TonText(
                text = symbolInitials(icon.symbol),
                style = TonTheme.typography.subheadline2Semibold,
                color = TonTheme.colors.textSecondary,
            )
        }
    }
}

private fun symbolInitials(symbol: String): String {
    val trimmed = symbol.trim()
    if (trimmed.isEmpty()) return "?"
    return trimmed.take(3).uppercase()
}
