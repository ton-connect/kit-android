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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.ton.walletkit.demo.R
import io.ton.walletkit.demo.presentation.model.SignMessageRequestUi
import io.ton.walletkit.demo.presentation.util.TestTags
import io.ton.walletkit.demo.presentation.util.abbreviated
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Sheet for displaying sign message requests (gasless transactions).
 * Similar to TransactionRequestSheet but for internal messages that will be
 * sponsored by a gasless service.
 */
@Composable
fun SignMessageRequestSheet(
    request: SignMessageRequestUi,
    onApprove: () -> Unit,
    onReject: () -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(20.dp)
            .testTag(TestTags.SIGN_MESSAGE_REQUEST_SHEET),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        val dAppNameDisplay = if (request.dAppName.isBlank()) {
            stringResource(R.string.transaction_request_unknown_dapp)
        } else {
            request.dAppName
        }

        // Title
        Text(
            stringResource(R.string.sign_message_request_title),
            style = MaterialTheme.typography.titleLarge,
        )

        // Gasless hint badge
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Text(
                stringResource(R.string.sign_message_request_gasless_hint),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }

        // From wallet
        Text(
            stringResource(R.string.transaction_request_from_format, request.walletAddress.abbreviated()),
            style = MaterialTheme.typography.bodyMedium,
        )

        // Requested by dApp
        Text(
            stringResource(R.string.transaction_request_requested_by_format, dAppNameDisplay),
            style = MaterialTheme.typography.bodyMedium,
        )

        // Valid until
        request.validUntil?.let {
            Text(
                stringResource(R.string.transaction_request_valid_until_format, formatUnixTimestamp(it)),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        // Messages
        if (request.messages.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                request.messages.forEachIndexed { index, message ->
                    Surface(shape = RoundedCornerShape(12.dp), tonalElevation = 2.dp) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(stringResource(R.string.transaction_request_to_label), style = MaterialTheme.typography.labelMedium)
                                Text(
                                    message.to.abbreviated(),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(stringResource(R.string.transaction_request_amount_label), style = MaterialTheme.typography.labelMedium)
                                Text(
                                    stringResource(R.string.wallet_switcher_balance_format, formatNanoToTon(message.amount)),
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                )
                            }

                            // Show comment if present
                            message.comment?.takeIf { it.isNotBlank() }?.let {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    ),
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            stringResource(R.string.transaction_request_comment_label),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        )
                                        Text(
                                            it,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        )
                                    }
                                }
                            }

                            message.payload?.takeIf { it.isNotBlank() }?.let {
                                val trimmedPayload = it.take(50) + if (it.length > 50) "..." else ""
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        stringResource(R.string.transaction_request_payload_label),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        trimmedPayload,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Action buttons
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            TextButton(
                onClick = onReject,
                modifier = Modifier.weight(1f).testTag(TestTags.SIGN_MESSAGE_REJECT_BUTTON),
            ) { Text(stringResource(R.string.action_reject)) }
            Button(
                onClick = onApprove,
                modifier = Modifier.weight(1f).testTag(TestTags.SIGN_MESSAGE_APPROVE_BUTTON),
            ) { Text(stringResource(R.string.action_sign)) }
        }
    }
}

private fun formatNanoToTon(nanotons: String): String = try {
    BigDecimal(nanotons)
        .divide(BigDecimal("1000000000"), 9, RoundingMode.DOWN)
        .stripTrailingZeros()
        .toPlainString()
} catch (e: Exception) {
    nanotons
}

private fun formatUnixTimestamp(timestamp: Long): String = try {
    val date = Date(timestamp * 1000)
    val formatter = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault())
    formatter.format(date)
} catch (e: Exception) {
    timestamp.toString()
}
