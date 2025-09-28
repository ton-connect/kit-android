package io.ton.walletkit.demo.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.ton.walletkit.demo.model.SessionSummary
import io.ton.walletkit.demo.ui.preview.PreviewData
import io.ton.walletkit.demo.util.abbreviated

@Composable
fun SessionCard(session: SessionSummary, onDisconnect: () -> Unit) {
    ElevatedCard {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(session.dAppName, style = MaterialTheme.typography.titleMedium)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Wallet",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(session.walletAddress.abbreviated(), style = MaterialTheme.typography.bodyMedium)
            }
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val resolvedUrl = session.dAppUrl?.takeIf { it.isNotBlank() }
                        ?: session.manifestUrl?.takeIf { it.isNotBlank() }
                    resolvedUrl?.let {
                        Text(
                            "URL",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                TextButton(onClick = onDisconnect) { Text("Disconnect") }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SessionCardPreview() {
    SessionCard(session = PreviewData.session, onDisconnect = {})
}
