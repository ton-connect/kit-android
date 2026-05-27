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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.ton.walletkit.demo.presentation.model.SignMessageRequestUi
import io.ton.walletkit.demo.presentation.util.abbreviated
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Bottom sheet for a sign-message (sign-only) request. The wallet will sign the
 * transaction but will NOT broadcast it — the dApp relays the resulting BoC.
 */
@Composable
fun SignMessageRequestSheet(
    request: SignMessageRequestUi,
    onApprove: () -> Unit,
    onReject: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Sign message request", style = MaterialTheme.typography.titleLarge)
        Text(
            "From wallet: ${request.walletAddress.abbreviated()}",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            "Requested by: ${request.dAppName.ifBlank { "Unknown dApp" }}",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            "Approving will sign the transaction without broadcasting it. The dApp relays the result.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (request.messages.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                request.messages.forEach { message ->
                    Surface(shape = RoundedCornerShape(12.dp), tonalElevation = 2.dp) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text("To", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    message.to.abbreviated(),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text("Amount", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    "${formatNanoToTon(message.amount)} TON",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                )
                            }
                        }
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onReject, modifier = Modifier.weight(1f)) { Text("Reject") }
            Button(onClick = onApprove, modifier = Modifier.weight(1f)) { Text("Sign") }
        }
    }
}

private fun formatNanoToTon(nanotons: String): String = try {
    BigDecimal(nanotons)
        .divide(BigDecimal("1000000000"), 9, RoundingMode.DOWN)
        .stripTrailingZeros()
        .toPlainString()
} catch (_: Exception) {
    nanotons
}
