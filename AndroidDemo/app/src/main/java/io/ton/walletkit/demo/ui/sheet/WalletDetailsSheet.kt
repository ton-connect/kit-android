package io.ton.walletkit.demo.ui.sheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.ton.walletkit.demo.model.WalletSummary
import io.ton.walletkit.demo.ui.components.CodeBlock
import io.ton.walletkit.demo.ui.components.NetworkBadge
import io.ton.walletkit.demo.ui.preview.PreviewData

@Composable
fun WalletDetailsSheet(wallet: WalletSummary, onDismiss: () -> Unit) {
    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(wallet.name, style = MaterialTheme.typography.titleLarge)
        NetworkBadge(wallet.network)
        DetailRow("Address", wallet.address)
        DetailRow("Version", wallet.version)
        wallet.publicKey?.let { DetailRow("Public key", it) }
        DetailRow("Balance", wallet.balance ?: "—")

        wallet.transactions?.let { transactions ->
            if (transactions.length() > 0) {
                Text("Recent transactions", style = MaterialTheme.typography.titleSmall)
                CodeBlock(transactions.toString(2))
            }
        }

        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Close") }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Preview(showBackground = true)
@Composable
private fun WalletDetailsSheetPreview() {
    WalletDetailsSheet(wallet = PreviewData.wallet, onDismiss = {})
}
