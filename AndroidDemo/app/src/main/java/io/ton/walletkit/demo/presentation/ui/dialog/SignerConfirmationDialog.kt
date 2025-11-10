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
package io.ton.walletkit.demo.presentation.ui.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import io.ton.walletkit.demo.R
import io.ton.walletkit.demo.presentation.model.SignDataRequestUi

@Composable
fun SignerConfirmationDialog(
    request: SignDataRequestUi,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(stringResource(R.string.signer_confirm_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.action_cancel))
            }
        },
        title = {
            Text(stringResource(R.string.signer_confirm_title))
        },
        text = {
            // Calculate approximate data size from payload
            val dataSize = request.payloadContent.length
            Text(
                stringResource(
                    R.string.signer_confirm_message,
                    dataSize,
                    request.walletAddress.take(10) + "..."
                )
            )
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun SignerConfirmationDialogPreview() {
    SignerConfirmationDialog(
        request = SignDataRequestUi(
            id = "preview-request",
            dAppName = "DApp Example",
            walletAddress = "EQDxyz...",
            payloadType = "Cell",
            payloadContent = "te6ccg...",
            preview = "Preview data",
            raw = org.json.JSONObject(),
        ),
        onConfirm = {},
        onCancel = {},
    )
}
