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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.ton.walletkit.demo.R
import io.ton.walletkit.demo.presentation.model.SignDataRequestUi
import io.ton.walletkit.demo.presentation.ui.components.CodeBlock
import io.ton.walletkit.demo.presentation.ui.preview.PreviewData
import io.ton.walletkit.demo.presentation.util.TestTags
import io.ton.walletkit.demo.presentation.util.abbreviated

@Composable
fun SignDataSheet(
    request: SignDataRequestUi,
    onApprove: () -> Unit,
    onReject: () -> Unit,
) {
    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .padding(20.dp)
            .testTag(TestTags.SIGN_DATA_REQUEST_SHEET),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            stringResource(R.string.sign_request_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.testTag(TestTags.SIGN_DATA_REQUEST_TITLE),
        )
        request.dAppName?.let {
            Text(stringResource(R.string.sign_request_from_format, it), style = MaterialTheme.typography.bodyMedium)
        }
        Text(stringResource(R.string.sign_request_wallet_format, request.walletAddress.abbreviated()), style = MaterialTheme.typography.bodyMedium)
        Text(stringResource(R.string.sign_request_type_format, request.payloadType), style = MaterialTheme.typography.bodyMedium)

        // Show preview if available (human-readable), otherwise show raw payload
        Text(stringResource(R.string.sign_request_data_hint), style = MaterialTheme.typography.titleSmall)
        Column(
            modifier = Modifier.clickable {
                clipboardManager.setText(AnnotatedString(request.payloadContent))
            },
        ) {
            CodeBlock(request.preview ?: request.payloadContent)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            TextButton(
                onClick = onReject,
                modifier = Modifier.weight(1f).testTag(TestTags.SIGN_DATA_REJECT_BUTTON),
            ) { Text(stringResource(R.string.action_reject)) }
            Button(
                onClick = onApprove,
                modifier = Modifier.weight(1f).testTag(TestTags.SIGN_DATA_APPROVE_BUTTON),
            ) { Text(stringResource(R.string.action_sign)) }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SignDataSheetPreview() {
    SignDataSheet(
        request = PreviewData.signDataRequest,
        onApprove = {},
        onReject = {},
    )
}
