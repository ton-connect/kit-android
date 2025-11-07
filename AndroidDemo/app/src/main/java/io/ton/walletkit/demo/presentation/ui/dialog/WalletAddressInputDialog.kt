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
package io.ton.walletkit.demo.presentation.ui.dialog

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletAddressInputDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    isValidAddress: (String) -> Boolean,
) {
    var address by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Enter Recipient Address")
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = address,
                    onValueChange = { newValue ->
                        address = newValue
                        errorMessage = null // Clear error when user types
                    },
                    label = { Text("TON Address") },
                    placeholder = { Text("EQxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx") },
                    isError = errorMessage != null,
                    supportingText = {
                        errorMessage?.let {
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val pastedText = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
                                if (!pastedText.isNullOrBlank()) {
                                    address = pastedText.trim()
                                    errorMessage = null
                                }
                            },
                        ) {
                            Icon(
                                Icons.Default.ContentPaste,
                                contentDescription = "Paste from clipboard",
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (validateAndConfirm(address, isValidAddress, onConfirm) { errorMessage = it }) {
                                // Success handled in validateAndConfirm
                            }
                        },
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                Text(
                    text = "Address must be 48 characters and start with EQ or UQ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    validateAndConfirm(address, isValidAddress, onConfirm) { errorMessage = it }
                },
                enabled = address.isNotBlank(),
            ) {
                Text("Transfer NFT")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

/**
 * Validates the address and calls onConfirm if valid, or sets error message if invalid
 * Returns true if validation passed
 */
private fun validateAndConfirm(
    address: String,
    isValidAddress: (String) -> Boolean,
    onConfirm: (String) -> Unit,
    onError: (String) -> Unit,
): Boolean {
    val trimmedAddress = address.trim()

    when {
        trimmedAddress.isBlank() -> {
            onError("Address cannot be empty")
            return false
        }
        trimmedAddress.length != 48 -> {
            onError("Address must be exactly 48 characters (current: ${trimmedAddress.length})")
            return false
        }
        !trimmedAddress.startsWith("EQ") && !trimmedAddress.startsWith("UQ") -> {
            onError("Address must start with EQ or UQ")
            return false
        }
        !isValidAddress(trimmedAddress) -> {
            onError("Invalid address format")
            return false
        }
        else -> {
            onConfirm(trimmedAddress)
            return true
        }
    }
}
