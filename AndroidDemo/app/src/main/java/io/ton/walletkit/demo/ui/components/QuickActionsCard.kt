package io.ton.walletkit.demo.ui.components

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun QuickActionsCard(
    onHandleUrl: () -> Unit,
    onAddWallet: () -> Unit,
    onRefresh: () -> Unit,
) {
    ElevatedCard {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Quick Actions", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(onClick = onHandleUrl) { Text("Handle URL") }
                FilledTonalButton(onClick = onAddWallet) { Text("Add Wallet") }
                FilledTonalButton(onClick = onRefresh) { Text("Refresh") }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun QuickActionsCardPreview() {
    QuickActionsCard(onHandleUrl = {}, onAddWallet = {}, onRefresh = {})
}
