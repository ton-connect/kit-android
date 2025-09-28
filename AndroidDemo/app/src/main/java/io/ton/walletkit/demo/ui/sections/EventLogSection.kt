package io.ton.walletkit.demo.ui.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun EventLogSection(events: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Recent events", style = MaterialTheme.typography.titleMedium)
        events.forEach { event ->
            HorizontalDivider()
            Text(event, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EventLogSectionPreview() {
    EventLogSection(events = listOf("Handled TON Connect URL", "Approved transaction"))
}
