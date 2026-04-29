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

import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.ton.walletkit.api.ChainIds
import io.ton.walletkit.api.MAINNET
import io.ton.walletkit.api.TESTNET
import io.ton.walletkit.api.TETRA
import io.ton.walletkit.api.WalletVersions
import io.ton.walletkit.api.generated.TONNetwork
import io.ton.walletkit.demo.R
import io.ton.walletkit.demo.domain.model.WalletInterfaceType
import io.ton.walletkit.demo.presentation.ui.icons.ContentPaste
import io.ton.walletkit.demo.presentation.util.TestTags

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AddWalletSheet(
    onDismiss: () -> Unit,
    onImportWallet: (String, TONNetwork, List<String>, String, String, WalletInterfaceType) -> Unit,
    onGenerateWallet: (String, TONNetwork, String, WalletInterfaceType) -> Unit,
    walletCount: Int,
) {
    var selectedTab by remember { mutableStateOf(AddWalletTab.Import) }
    val defaultName = stringResource(R.string.wallet_default_name, walletCount + 1)
    var walletName by rememberSaveable(walletCount) { mutableStateOf(defaultName) }
    // Use remember instead of rememberSaveable - TONNetwork is a data class that can't be saved in Bundle
    var network by remember { mutableStateOf(TONNetwork.MAINNET) }
    var walletVersion by rememberSaveable { mutableStateOf(DEFAULT_WALLET_VERSION) }
    var interfaceType by rememberSaveable { mutableStateOf(WalletInterfaceType.MNEMONIC) }
    // Per-slot MutableState instead of a single SnapshotStateList<String>. A list would
    // invalidate every reader on any mutation — with 24 TextFields that means one keystroke
    // triggers 24 recompositions and 24 layout measures. Per-slot state keeps writes local:
    // typing in field 5 only recomposes field 5.
    val mnemonicWords = remember {
        Array(MNEMONIC_WORD_COUNT) { mutableStateOf("") }
    }
    var secretKeyHex by rememberSaveable { mutableStateOf("") }
    var pasteField by rememberSaveable { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current

    // Parse pasted text into the 24 word slots. Writes each slot once; the per-slot state
    // means only fields whose value actually changed recompose.
    fun parseSeedPhrase(text: String) {
        val words = text
            .trim()
            .lowercase()
            .split(Regex("\\s+")) // Split by any whitespace (space, tab, newline)
            .filter { it.isNotBlank() }
            .take(MNEMONIC_WORD_COUNT)

        for (i in 0 until MNEMONIC_WORD_COUNT) {
            val next = words.getOrNull(i).orEmpty()
            if (mnemonicWords[i].value != next) {
                mnemonicWords[i].value = next
            }
        }

        // Clear paste field after parsing
        pasteField = ""
    }

    Column(
        modifier = Modifier
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.add_wallet_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        SingleChoiceSegmentedButtonRow {
            AddWalletTab.entries.forEachIndexed { index, tab ->
                SegmentedButton(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    shape = SegmentedButtonDefaults.itemShape(index, AddWalletTab.entries.size),
                    label = { Text(stringResource(tab.labelRes)) },
                    modifier = Modifier.testTag(
                        when (tab) {
                            AddWalletTab.Import -> TestTags.ADD_WALLET_TAB_IMPORT
                            AddWalletTab.Generate -> TestTags.ADD_WALLET_TAB_GENERATE
                        },
                    ),
                )
            }
        }

        TextField(
            value = walletName,
            onValueChange = { walletName = it },
            label = { Text(stringResource(R.string.label_wallet_name)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Text(stringResource(R.string.label_network), style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(TONNetwork.MAINNET, TONNetwork.TESTNET, TONNetwork.TETRA).forEach { option ->
                FilterChip(
                    selected = network == option,
                    onClick = { network = option },
                    label = {
                        Text(
                            when (option.chainId) {
                                ChainIds.MAINNET -> stringResource(R.string.network_mainnet)
                                ChainIds.TESTNET -> stringResource(R.string.network_testnet)
                                ChainIds.TETRA -> stringResource(R.string.network_tetra)
                                else -> "Unknown"
                            },
                        )
                    },
                )
            }
        }

        Text(stringResource(R.string.label_wallet_version), style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(WalletVersions.V5R1, WalletVersions.V4R2).forEach { version ->
                FilterChip(
                    selected = walletVersion == version,
                    onClick = { walletVersion = version },
                    label = {
                        Column {
                            Text(version, fontWeight = FontWeight.Bold)
                            if (version == DEFAULT_WALLET_VERSION) {
                                Text(stringResource(R.string.add_wallet_version_default), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    },
                )
            }
        }

        Text(stringResource(R.string.label_wallet_interface_type), style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            WalletInterfaceType.entries.forEach { type ->
                FilterChip(
                    selected = interfaceType == type,
                    onClick = { interfaceType = type },
                    label = {
                        Column {
                            Text(
                                when (type) {
                                    WalletInterfaceType.MNEMONIC -> stringResource(R.string.interface_type_mnemonic)
                                    WalletInterfaceType.SECRET_KEY -> stringResource(R.string.interface_type_secret_key)
                                    WalletInterfaceType.SIGNER -> stringResource(R.string.interface_type_signer)
                                },
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                when (type) {
                                    WalletInterfaceType.MNEMONIC -> stringResource(R.string.interface_type_mnemonic_desc)
                                    WalletInterfaceType.SECRET_KEY -> stringResource(R.string.interface_type_secret_key_desc)
                                    WalletInterfaceType.SIGNER -> stringResource(R.string.interface_type_signer_desc)
                                },
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    },
                )
            }
        }

        when (selectedTab) {
            AddWalletTab.Import -> {
                when (interfaceType) {
                    WalletInterfaceType.SECRET_KEY -> {
                        // Secret Key Import UI
                        Text(
                            stringResource(R.string.add_wallet_secret_key_title),
                            style = MaterialTheme.typography.titleSmall,
                        )

                        OutlinedTextField(
                            value = secretKeyHex,
                            onValueChange = { secretKeyHex = it.trim() },
                            label = { Text(stringResource(R.string.add_wallet_secret_key_label)) },
                            placeholder = { Text(stringResource(R.string.add_wallet_secret_key_placeholder)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            supportingText = {
                                Text(
                                    stringResource(R.string.add_wallet_secret_key_supporting_text),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        clipboardManager.getText()?.text?.let { clipboardText ->
                                            secretKeyHex = clipboardText.trim()
                                        }
                                    },
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentPaste,
                                        contentDescription = stringResource(R.string.add_wallet_clipboard_content_description),
                                    )
                                }
                            },
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { onImportWallet(walletName, network, emptyList(), secretKeyHex, walletVersion, interfaceType) },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(stringResource(R.string.action_import_wallet)) }
                    }

                    else -> {
                        // Mnemonic/Signer Import UI
                        Text(
                            stringResource(R.string.add_wallet_recovery_title, MNEMONIC_WORD_COUNT),
                            style = MaterialTheme.typography.titleSmall,
                        )

                        // Paste field for quick input
                        OutlinedTextField(
                            value = pasteField,
                            onValueChange = {
                                pasteField = it
                                // Auto-parse when text is pasted (contains multiple words).
                                // Cheap whitespace check — avoid splitting + regex per keystroke;
                                // parseSeedPhrase does the full parse only when whitespace appears.
                                if (it.any(Char::isWhitespace)) {
                                    parseSeedPhrase(it)
                                }
                            },
                            label = { Text(stringResource(R.string.add_wallet_paste_label)) },
                            placeholder = { Text(stringResource(R.string.add_wallet_recovery_placeholder)) },
                            modifier = Modifier.fillMaxWidth().testTag(TestTags.MNEMONIC_FIELD),
                            minLines = 2,
                            maxLines = 3,
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        clipboardManager.getText()?.text?.let { clipboardText ->
                                            parseSeedPhrase(clipboardText)
                                        }
                                    },
                                    modifier = Modifier.testTag(TestTags.PASTE_ALL_BUTTON),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentPaste,
                                        contentDescription = stringResource(R.string.add_wallet_clipboard_content_description),
                                    )
                                }
                            },
                            supportingText = {
                                Text(
                                    stringResource(R.string.add_wallet_paste_supporting_text),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            },
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (rowIndex in 0 until MNEMONIC_WORD_COUNT / MNEMONIC_GRID_COLUMNS) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    for (colIndex in 0 until MNEMONIC_GRID_COLUMNS) {
                                        val index = rowIndex * MNEMONIC_GRID_COLUMNS + colIndex
                                        val wordState = mnemonicWords[index]
                                        TextField(
                                            value = wordState.value,
                                            onValueChange = { wordState.value = it.lowercase().trim() },
                                            singleLine = true,
                                            label = { Text("${index + 1}") },
                                            modifier = Modifier.weight(1f),
                                            textStyle = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                onImportWallet(
                                    walletName,
                                    network,
                                    mnemonicWords.map { it.value },
                                    "",
                                    walletVersion,
                                    interfaceType,
                                )
                            },
                            modifier = Modifier.fillMaxWidth().testTag(TestTags.IMPORT_WALLET_PROCESS_BUTTON),
                        ) { Text(stringResource(R.string.action_import_wallet)) }
                    }
                }
            }

            AddWalletTab.Generate -> {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        stringResource(R.string.add_wallet_generate_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    // Show info message if Secret Key is selected
                    if (interfaceType == WalletInterfaceType.SECRET_KEY) {
                        Text(
                            stringResource(R.string.wallet_error_secret_key_cannot_generate),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }

                    Button(
                        onClick = { onGenerateWallet(walletName, network, walletVersion, interfaceType) },
                        modifier = Modifier.fillMaxWidth().testTag(TestTags.GENERATE_WALLET_PROCESS_BUTTON),
                        enabled = interfaceType != WalletInterfaceType.SECRET_KEY,
                    ) { Text(stringResource(R.string.action_generate_wallet)) }
                }
            }
        }

        TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
    }
}

private enum class AddWalletTab(@StringRes val labelRes: Int) {
    Import(R.string.add_wallet_tab_import),
    Generate(R.string.add_wallet_tab_generate),
}

private const val MNEMONIC_WORD_COUNT = 24
private const val MNEMONIC_GRID_COLUMNS = 3
private const val DEFAULT_WALLET_VERSION = WalletVersions.V5R1

@Preview(showBackground = true)
@Composable
private fun AddWalletSheetPreview() {
    AddWalletSheet(
        onDismiss = {},
        onImportWallet = { _, _, _, _, _, _ -> },
        onGenerateWallet = { _, _, _, _ -> },
        walletCount = 1,
    )
}
