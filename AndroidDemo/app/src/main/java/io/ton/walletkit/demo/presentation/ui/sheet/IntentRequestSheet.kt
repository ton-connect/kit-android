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
import io.ton.walletkit.demo.presentation.model.IntentRequestUi
import io.ton.walletkit.event.TONIntentEvent
import io.ton.walletkit.event.TONIntentItem

@Composable
fun IntentRequestSheet(
    request: IntentRequestUi,
    onApprove: () -> Unit,
    onReject: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "Intent Request",
            style = MaterialTheme.typography.titleLarge,
        )

        Surface(shape = RoundedCornerShape(12.dp), tonalElevation = 2.dp) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LabelValue("Type", request.intentType)
                LabelValue("Origin", request.origin)
                LabelValue("ID", request.id)
                if (request.hasConnectRequest) {
                    LabelValue("Connect", "Will connect after approval")
                }
            }
        }

        // Intent-specific details
        Column(
            modifier = Modifier
                .heightIn(max = 400.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when (val event = request.event) {
                is TONIntentEvent.TransactionIntent -> TransactionIntentDetails(event)
                is TONIntentEvent.SignDataIntent -> SignDataIntentDetails(event)
                is TONIntentEvent.ActionIntent -> ActionIntentDetails(event)
            }
        }

        // Approve / Reject buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
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

@Composable
private fun TransactionIntentDetails(event: TONIntentEvent.TransactionIntent) {
    event.network?.let { LabelValue("Network", it) }
    event.validUntil?.let { LabelValue("Valid Until", it.toString()) }
    LabelValue("Delivery Mode", event.deliveryMode)

    if (event.items.isNotEmpty()) {
        Text(
            "Items (${event.items.size})",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 4.dp),
        )
        event.items.forEach { item ->
            Surface(shape = RoundedCornerShape(8.dp), tonalElevation = 1.dp) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    when (item) {
                        is TONIntentItem.SendTon -> {
                            LabelValue("Type", "Send TON")
                            LabelValue("To", item.address)
                            LabelValue("Amount", "${item.amount} nanoTON")
                        }
                        is TONIntentItem.SendJetton -> {
                            LabelValue("Type", "Send Jetton")
                            LabelValue("Jetton", item.jettonMasterAddress)
                            LabelValue("Amount", item.jettonAmount)
                            LabelValue("To", item.destination)
                        }
                        is TONIntentItem.SendNft -> {
                            LabelValue("Type", "Send NFT")
                            LabelValue("NFT", item.nftAddress)
                            LabelValue("New Owner", item.newOwnerAddress)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SignDataIntentDetails(event: TONIntentEvent.SignDataIntent) {
    event.network?.let { LabelValue("Network", it) }
    LabelValue("Manifest URL", event.manifestUrl)
    LabelValue("Payload Type", event.payload.type)
}

@Composable
private fun ActionIntentDetails(event: TONIntentEvent.ActionIntent) {
    LabelValue("Action URL", event.actionUrl)
}

@Composable
private fun LabelValue(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}
