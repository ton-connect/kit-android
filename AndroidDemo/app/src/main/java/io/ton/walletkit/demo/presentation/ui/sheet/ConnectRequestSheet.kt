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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
import io.ton.walletkit.demo.R
import io.ton.walletkit.demo.presentation.model.ConnectRequestUi
import io.ton.walletkit.demo.presentation.model.WalletSummary
import io.ton.walletkit.demo.presentation.ui.preview.PreviewData
import io.ton.walletkit.demo.presentation.util.TestTags
import io.ton.walletkit.demo.presentation.util.abbreviated

@Composable
fun ConnectRequestSheet(
    request: ConnectRequestUi,
    wallets: List<WalletSummary>,
    onApprove: (ConnectRequestUi, WalletSummary) -> Unit,
    onReject: (ConnectRequestUi) -> Unit,
) {
    var selectedWallet by remember { mutableStateOf(wallets.firstOrNull()) }

    Column(
        modifier = Modifier
            .padding(20.dp)
            .testTag(TestTags.CONNECT_REQUEST_SHEET),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            stringResource(R.string.connect_request_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.testTag(TestTags.CONNECT_REQUEST_TITLE)
        )
        Text(request.dAppName, style = MaterialTheme.typography.titleMedium)
        Text(
            request.dAppUrl,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (request.permissions.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.connect_request_requested_permissions), style = MaterialTheme.typography.titleSmall)
                request.permissions.forEach { permission ->
                    AssistChip(onClick = {}, label = { Text(permission.title.ifBlank { permission.name }) })
                    Text(
                        permission.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.connect_request_select_wallet), style = MaterialTheme.typography.titleSmall)
            wallets.forEach { wallet ->
                ElevatedCard(
                    modifier = Modifier.clickable { selectedWallet = wallet },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(wallet.name, style = MaterialTheme.typography.titleMedium)
                            Text(wallet.address.abbreviated(), style = MaterialTheme.typography.bodySmall)
                        }
                        RadioIndicator(selected = selectedWallet?.address == wallet.address)
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            TextButton(
                onClick = { onReject(request) },
                modifier = Modifier.weight(1f).testTag(TestTags.CONNECT_REJECT_BUTTON)
            ) { Text(stringResource(R.string.action_reject)) }
            Button(
                onClick = { selectedWallet?.let { w -> onApprove(request, w) } },
                enabled = selectedWallet != null,
                modifier = Modifier.weight(1f).testTag(TestTags.CONNECT_APPROVE_BUTTON),
            ) { Text(stringResource(R.string.action_connect)) }
        }
    }
}

@Composable
private fun RadioIndicator(selected: Boolean) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .background(
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(MaterialTheme.colorScheme.onPrimary, CircleShape),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ConnectRequestSheetPreview() {
    ConnectRequestSheet(
        request = PreviewData.connectRequest,
        wallets = listOf(PreviewData.wallet),
        onApprove = { _, _ -> },
        onReject = { _ -> },
    )
}
