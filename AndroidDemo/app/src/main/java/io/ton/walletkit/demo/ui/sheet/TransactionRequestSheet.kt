package io.ton.walletkit.demo.ui.sheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.ton.walletkit.demo.model.TransactionRequestUi
import io.ton.walletkit.demo.ui.components.CodeBlock
import io.ton.walletkit.demo.ui.preview.PreviewData
import io.ton.walletkit.demo.util.abbreviated

@Composable
fun TransactionRequestSheet(
    request: TransactionRequestUi,
    onApprove: () -> Unit,
    onReject: () -> Unit,
) {
    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Transaction Request", style = MaterialTheme.typography.titleLarge)
        Text("From: ${request.walletAddress.abbreviated()}", style = MaterialTheme.typography.bodyMedium)
        Text("dApp: ${request.dAppName}", style = MaterialTheme.typography.bodyMedium)
        request.validUntil?.let { Text("Valid until: $it", style = MaterialTheme.typography.bodyMedium) }

        if (request.messages.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Messages", style = MaterialTheme.typography.titleSmall)
                request.messages.forEachIndexed { index, message ->
                    Surface(shape = RoundedCornerShape(12.dp), tonalElevation = 2.dp) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text("Message ${index + 1}", style = MaterialTheme.typography.titleMedium)
                            Text("To: ${message.to}")
                            Text("Amount: ${message.amount}")
                            message.payload?.takeIf { it.isNotBlank() }?.let {
                                Text("Payload: $it", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }

        request.preview?.let {
            Text("Preview", style = MaterialTheme.typography.titleSmall)
            CodeBlock(content = it)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onReject, modifier = Modifier.weight(1f)) { Text("Reject") }
            Button(onClick = onApprove, modifier = Modifier.weight(1f)) { Text("Approve") }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TransactionRequestSheetPreview() {
    TransactionRequestSheet(
        request = PreviewData.transactionRequest,
        onApprove = {},
        onReject = {},
    )
}
