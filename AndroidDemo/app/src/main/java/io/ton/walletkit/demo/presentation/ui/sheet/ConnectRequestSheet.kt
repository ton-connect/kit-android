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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.ton.walletkit.demo.R
import io.ton.walletkit.demo.designsystem.components.button.TonButton
import io.ton.walletkit.demo.designsystem.components.button.TonButtonConfig
import io.ton.walletkit.demo.designsystem.theme.TonTheme
import io.ton.walletkit.demo.presentation.model.ConnectRequestUi
import io.ton.walletkit.demo.presentation.model.WalletSummary
import io.ton.walletkit.demo.presentation.ui.preview.PreviewData
import io.ton.walletkit.demo.presentation.ui.sheet.components.TonConnectPermissionRow
import io.ton.walletkit.demo.presentation.ui.sheet.components.TonConnectSheetDisclaimer
import io.ton.walletkit.demo.presentation.ui.sheet.components.TonConnectSheetHeader
import io.ton.walletkit.demo.presentation.ui.sheet.components.TonConnectSheetSection
import io.ton.walletkit.demo.presentation.ui.sheet.components.TonConnectWalletPicker
import io.ton.walletkit.demo.presentation.util.TestTags

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
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .testTag(TestTags.CONNECT_REQUEST_SHEET),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        TonConnectSheetHeader(
            titleLeading = stringResource(R.string.connect_request_title_leading),
            titleAccent = request.dAppUrl,
            titleTrailing = stringResource(R.string.connect_request_title_trailing),
            subtitle = stringResource(R.string.connect_request_subtitle_format, request.dAppName),
            dAppIconUrl = request.iconUrl,
            onClose = { onReject(request) },
            modifier = Modifier.testTag(TestTags.CONNECT_REQUEST_TITLE),
            closeButtonModifier = Modifier.testTag(TestTags.CONNECT_REJECT_BUTTON),
        )

        TonConnectWalletPicker(
            wallets = wallets,
            selected = selectedWallet,
            onSelect = { selectedWallet = it },
        )

        if (request.permissions.isNotEmpty()) {
            TonConnectSheetSection(label = stringResource(R.string.connect_request_section_permissions)) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    request.permissions.forEach { permission ->
                        TonConnectPermissionRow(
                            title = permission.title.ifBlank { permission.name },
                            description = permission.description,
                        )
                    }
                }
            }
        }

        TonButton(
            text = stringResource(R.string.connect_request_action),
            onClick = { selectedWallet?.let { w -> onApprove(request, w) } },
            enabled = selectedWallet != null,
            config = TonButtonConfig.Primary,
            modifier = Modifier.testTag(TestTags.CONNECT_APPROVE_BUTTON),
        )

        TonConnectSheetDisclaimer(text = stringResource(R.string.connect_request_disclaimer))
    }
}

@Preview(showBackground = true)
@Composable
private fun ConnectRequestSheetPreview() {
    TonTheme {
        ConnectRequestSheet(
            request = PreviewData.connectRequest,
            wallets = listOf(PreviewData.wallet),
            onApprove = { _, _ -> },
            onReject = { _ -> },
        )
    }
}
