package io.ton.walletkit.demo.ui.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.ton.walletkit.demo.model.WalletSummary
import io.ton.walletkit.demo.ui.components.EmptyStateCard
import io.ton.walletkit.demo.ui.components.WalletCard
import io.ton.walletkit.demo.ui.preview.PreviewData

@Composable
fun WalletsSection(wallets: List<WalletSummary>, onWalletSelected: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Wallets",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "${wallets.size}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        if (wallets.isEmpty()) {
            EmptyStateCard(
                title = "No wallets",
                description = "Import or generate a wallet to get started.",
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                wallets.forEach { wallet ->
                    WalletCard(
                        wallet = wallet,
                        onDetails = { onWalletSelected(wallet.address) },
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WalletsSectionPreview() {
    WalletsSection(wallets = listOf(PreviewData.wallet), onWalletSelected = {})
}
