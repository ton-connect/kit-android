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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.ton.walletkit.demo.designsystem.components.text.TonText
import io.ton.walletkit.demo.designsystem.theme.TonTheme

// "Balance" label + price-style total ("123.456 TON"). Integer part renders in price64,
// fraction (".456") and the trailing " TON" suffix render in price40 — same baseline
// as iOS `lastTextBaseline` HStack.
@Composable
fun WalletHomeBalance(
    totalBalanceInteger: String,
    totalBalanceFraction: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        TonText(
            text = "Balance",
            style = TonTheme.typography.title3RoundedRegular,
            color = TonTheme.colors.textPrimary.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
        )
        // [Modifier.alignByBaseline] is the Compose equivalent of iOS's
        // `HStack(alignment: .lastTextBaseline)` — each Text's first baseline sits on
        // the row's common baseline. Without it the 40sp glyphs (.456 / TON) appear
        // visually lower than the 64sp integer because Row's default vertical
        // alignment aligns bounding-box bottoms, not baselines.
        Row {
            TonText(
                text = totalBalanceInteger,
                style = TonTheme.typography.price64,
                color = TonTheme.colors.textPrimary,
                maxLines = 1,
                modifier = Modifier.alignByBaseline(),
            )
            if (totalBalanceFraction.isNotEmpty()) {
                TonText(
                    text = totalBalanceFraction,
                    style = TonTheme.typography.price40,
                    color = TonTheme.colors.textPrimary,
                    maxLines = 1,
                    modifier = Modifier.alignByBaseline(),
                )
            }
            TonText(
                text = " TON",
                style = TonTheme.typography.price40,
                color = TonTheme.colors.textPrimary,
                maxLines = 1,
                modifier = Modifier.alignByBaseline(),
            )
        }
    }
}
