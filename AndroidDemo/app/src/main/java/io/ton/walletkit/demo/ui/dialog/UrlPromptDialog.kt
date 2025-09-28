package io.ton.walletkit.demo.ui.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun UrlPromptDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var url by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(url)
                    url = ""
                },
                enabled = url.isNotBlank(),
            ) { Text("Handle") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Handle TON Connect URL") },
        text = {
            TextField(
                value = url,
                onValueChange = { url = it },
                placeholder = { Text("https:// or ton://") },
                singleLine = true,
            )
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun UrlPromptDialogPreview() {
    UrlPromptDialog(onDismiss = {}, onConfirm = {})
}
