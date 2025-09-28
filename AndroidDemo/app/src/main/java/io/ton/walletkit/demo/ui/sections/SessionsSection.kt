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
import io.ton.walletkit.demo.model.SessionSummary
import io.ton.walletkit.demo.ui.components.EmptyStateCard
import io.ton.walletkit.demo.ui.components.SessionCard
import io.ton.walletkit.demo.ui.preview.PreviewData

@Composable
fun SessionsSection(sessions: List<SessionSummary>, onDisconnect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Active Sessions",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "${sessions.size}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        if (sessions.isEmpty()) {
            EmptyStateCard(
                title = "No active sessions",
                description = "Use TON Connect to pair with a dApp.",
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                sessions.forEach { session ->
                    SessionCard(
                        session = session,
                        onDisconnect = { onDisconnect(session.sessionId) },
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SessionsSectionPreview() {
    SessionsSection(sessions = listOf(PreviewData.session), onDisconnect = {})
}
