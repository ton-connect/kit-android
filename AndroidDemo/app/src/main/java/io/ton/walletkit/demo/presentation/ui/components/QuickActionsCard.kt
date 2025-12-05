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
package io.ton.walletkit.demo.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.ton.walletkit.demo.R
import io.ton.walletkit.demo.presentation.util.TestTags

@Composable
fun QuickActionsCard(
    onHandleUrl: () -> Unit,
    onAddWallet: () -> Unit,
    onRefresh: () -> Unit,
) {
    ElevatedCard {
        Column(
            modifier = Modifier.padding(ACTION_CARD_PADDING),
            verticalArrangement = Arrangement.spacedBy(ACTION_CARD_SPACING),
        ) {
            Text(stringResource(R.string.quick_actions_title), style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(ACTION_BUTTON_SPACING)) {
                FilledTonalButton(
                    onClick = onHandleUrl,
                    modifier = Modifier.testTag(TestTags.HANDLE_URL_BUTTON),
                ) { Text(stringResource(R.string.action_handle_url)) }
                FilledTonalButton(onClick = onAddWallet) { Text(stringResource(R.string.action_add_wallet)) }
                FilledTonalButton(onClick = onRefresh) { Text(stringResource(R.string.action_refresh)) }
            }
        }
    }
}

private val ACTION_CARD_PADDING = 20.dp
private val ACTION_CARD_SPACING = 16.dp
private val ACTION_BUTTON_SPACING = 12.dp

@Preview(showBackground = true)
@Composable
private fun QuickActionsCardPreview() {
    QuickActionsCard(onHandleUrl = {}, onAddWallet = {}, onRefresh = {})
}
