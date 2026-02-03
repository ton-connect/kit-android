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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.ton.walletkit.demo.presentation.model.IntentItemUi
import io.ton.walletkit.demo.presentation.model.IntentRequestUi
import io.ton.walletkit.demo.presentation.model.SignDataPayloadUi
import io.ton.walletkit.demo.presentation.util.abbreviated
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Sheet for approving/rejecting transaction intents from deep links.
 */
@Composable
fun IntentTransactionSheet(
    request: IntentRequestUi.Transaction,
    onApprove: () -> Unit,
    onReject: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Header with warning for deep link origin
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Deep link intent",
                tint = MaterialTheme.colorScheme.secondary,
            )
            Column {
                Text(
                    text = if (request.type == "txIntent") "Transaction Request" else "Sign Message Request",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = "Via deep link (no session)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Type badge
        val typeBadge = when (request.type) {
            "txIntent" -> "Sign & Send"
            "signMsg" -> "Sign Only (Gasless)"
            else -> request.type
        }
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (request.type == "signMsg") {
                MaterialTheme.colorScheme.tertiaryContainer
            } else {
                MaterialTheme.colorScheme.primaryContainer
            },
        ) {
            Text(
                text = typeBadge,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                color = if (request.type == "signMsg") {
                    MaterialTheme.colorScheme.onTertiaryContainer
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer
                },
            )
        }

        // Network info
        request.network?.let { network ->
            val networkName = when (network) {
                "-239" -> "Mainnet"
                "-3" -> "Testnet"
                else -> "Network: $network"
            }
            Text(
                text = networkName,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        // Valid until
        request.validUntil?.let { timestamp ->
            Text(
                text = "Valid until: ${formatTimestamp(timestamp)}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        // Items list
        if (request.items.isNotEmpty()) {
            Text(
                text = "${request.items.size} transaction(s):",
                style = MaterialTheme.typography.titleMedium,
            )

            Column(
                modifier = Modifier
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                request.items.forEachIndexed { index, item ->
                    IntentItemCard(item = item, index = index + 1)
                }
            }
        }

        // Connect request notice
        if (request.hasConnectRequest) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
            ) {
                Text(
                    text = "This intent will also establish a TonConnect session.",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }

        // Action buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            TextButton(
                onClick = onReject,
                modifier = Modifier.weight(1f),
            ) {
                Text("Reject")
            }
            Button(
                onClick = onApprove,
                modifier = Modifier.weight(1f),
            ) {
                Text("Approve")
            }
        }
    }
}

/**
 * Card displaying a single intent item.
 */
@Composable
private fun IntentItemCard(item: IntentItemUi, index: Int) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when (item) {
                is IntentItemUi.SendTon -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "#$index TON Transfer",
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("To:", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            item.address.abbreviated(),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Amount:", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${item.amountInTon} TON",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        )
                    }
                    if (item.hasPayload) {
                        Text(
                            "Contains payload",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (item.hasStateInit) {
                        Text(
                            "Contains stateInit (contract deploy)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                is IntentItemUi.SendJetton -> {
                    Text(
                        text = "#$index Jetton Transfer",
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("To:", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            item.destination.abbreviated(),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Amount:", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            item.jettonAmount,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        )
                    }
                    Text(
                        "Jetton: ${item.masterAddress.abbreviated()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                is IntentItemUi.SendNft -> {
                    Text(
                        text = "#$index NFT Transfer",
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("To:", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            item.newOwner.abbreviated(),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        )
                    }
                    Text(
                        "NFT: ${item.nftAddress.abbreviated()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/**
 * Sheet for approving/rejecting sign data intents.
 */
@Composable
fun IntentSignDataSheet(
    request: IntentRequestUi.SignData,
    onApprove: () -> Unit,
    onReject: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Deep link intent",
                tint = MaterialTheme.colorScheme.secondary,
            )
            Column {
                Text(
                    text = "Sign Data Request",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = "Via deep link (no session)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Manifest URL (dApp origin)
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Requested by:",
                    style = MaterialTheme.typography.labelSmall,
                )
                Text(
                    text = request.manifestUrl,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        // Payload content
        Text(
            text = "Data to sign:",
            style = MaterialTheme.typography.titleMedium,
        )

        Surface(
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 2.dp,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                when (val payload = request.payload) {
                    is SignDataPayloadUi.Text -> {
                        Text(
                            text = "Type: Text",
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Text(
                            text = payload.text,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    is SignDataPayloadUi.Binary -> {
                        Text(
                            text = "Type: Binary",
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Text(
                            text = payload.bytesPreview,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    is SignDataPayloadUi.Cell -> {
                        Text(
                            text = "Type: Cell",
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Text(
                            text = "Schema: ${payload.schema}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            text = payload.cellPreview,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // Connect request notice
        if (request.hasConnectRequest) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
            ) {
                Text(
                    text = "This intent will also establish a TonConnect session.",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }

        // Action buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            TextButton(
                onClick = onReject,
                modifier = Modifier.weight(1f),
            ) {
                Text("Reject")
            }
            Button(
                onClick = onApprove,
                modifier = Modifier.weight(1f),
            ) {
                Text("Sign")
            }
        }
    }
}

/**
 * Sheet for action intents (URL-based actions).
 */
@Composable
fun IntentActionSheet(
    request: IntentRequestUi.Action,
    onApprove: () -> Unit,
    onReject: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Deep link intent",
                tint = MaterialTheme.colorScheme.secondary,
            )
            Column {
                Text(
                    text = "Action Intent",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = "Via deep link (no session)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Action URL
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Action URL:",
                    style = MaterialTheme.typography.labelSmall,
                )
                Text(
                    text = request.actionUrl,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        Text(
            text = "The wallet will fetch action details from this URL and display them for approval.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // Action buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            TextButton(
                onClick = onReject,
                modifier = Modifier.weight(1f),
            ) {
                Text("Cancel")
            }
            Button(
                onClick = onApprove,
                modifier = Modifier.weight(1f),
            ) {
                Text("Fetch Action")
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String = try {
    val date = Date(timestamp * 1000)
    val format = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    format.format(date)
} catch (e: Exception) {
    timestamp.toString()
}
