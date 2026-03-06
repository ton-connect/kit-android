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
import io.ton.walletkit.api.generated.TONIntentActionItem
import io.ton.walletkit.api.generated.TONIntentRequestEvent
import io.ton.walletkit.demo.presentation.model.IntentRequestUi

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
                is TONIntentRequestEvent.Transaction -> TransactionIntentDetails(event)
                is TONIntentRequestEvent.SignData -> SignDataIntentDetails(event)
                is TONIntentRequestEvent.Action -> ActionIntentDetails(event)
                is TONIntentRequestEvent.Connect -> ConnectIntentDetails(event)
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
private fun TransactionIntentDetails(event: TONIntentRequestEvent.Transaction) {
    val tx = event.value
    tx.network?.let { LabelValue("Network", it.chainId) }
    tx.validUntil?.let { LabelValue("Valid Until", it.toString()) }
    LabelValue("Delivery Mode", tx.deliveryMode.value)

    if (tx.items.isNotEmpty()) {
        Text(
            "Items (${tx.items.size})",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 4.dp),
        )
        tx.items.forEach { item ->
            Surface(shape = RoundedCornerShape(8.dp), tonalElevation = 1.dp) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    when (item) {
                        is TONIntentActionItem.SendTon -> {
                            LabelValue("Type", "Send TON")
                            LabelValue("To", item.value.address.value)
                            LabelValue("Amount", "${item.value.amount} nanoTON")
                        }
                        is TONIntentActionItem.SendJetton -> {
                            LabelValue("Type", "Send Jetton")
                            LabelValue("Jetton", item.value.jettonMasterAddress.value)
                            LabelValue("Amount", item.value.jettonAmount)
                            LabelValue("To", item.value.destination.value)
                        }
                        is TONIntentActionItem.SendNft -> {
                            LabelValue("Type", "Send NFT")
                            LabelValue("NFT", item.value.nftAddress.value)
                            LabelValue("New Owner", item.value.newOwnerAddress.value)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SignDataIntentDetails(event: TONIntentRequestEvent.SignData) {
    val sd = event.value
    sd.network?.let { LabelValue("Network", it.chainId) }
    LabelValue("Manifest URL", sd.manifestUrl)
}

@Composable
private fun ActionIntentDetails(event: TONIntentRequestEvent.Action) {
    LabelValue("Action URL", event.value.actionUrl)
}

@Composable
private fun ConnectIntentDetails(event: TONIntentRequestEvent.Connect) {
    val conn = event.value
    conn.dAppInfo?.name?.let { LabelValue("dApp", it) }
    conn.dAppInfo?.url?.let { LabelValue("URL", it) }
    conn.dAppInfo?.manifestUrl?.let { LabelValue("Manifest", it) }
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
