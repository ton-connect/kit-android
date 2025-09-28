package io.ton.walletkit.demo.ui.sheet

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.ton.walletkit.demo.model.TonNetwork

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AddWalletSheet(
    onDismiss: () -> Unit,
    onImportWallet: (String, TonNetwork, List<String>) -> Unit,
    onGenerateWallet: (String, TonNetwork) -> Unit,
    walletCount: Int,
) {
    var selectedTab by remember { mutableStateOf(AddWalletTab.Import) }
    var walletName by rememberSaveable { mutableStateOf("Wallet ${walletCount + 1}") }
    var network by rememberSaveable { mutableStateOf(TonNetwork.MAINNET) }
    val mnemonicWords = remember { mutableStateListOf(*Array(24) { "" }) }

    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Add Wallet", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        SingleChoiceSegmentedButtonRow {
            AddWalletTab.entries.forEachIndexed { index, tab ->
                SegmentedButton(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    shape = SegmentedButtonDefaults.itemShape(index, AddWalletTab.entries.size),
                    label = { Text(tab.label) },
                )
            }
        }

        TextField(
            value = walletName,
            onValueChange = { walletName = it },
            label = { Text("Wallet name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Text("Network", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TonNetwork.entries.forEach { option ->
                FilterChip(
                    selected = network == option,
                    onClick = { network = option },
                    label = { Text(if (option == TonNetwork.MAINNET) "Mainnet" else "Testnet") },
                )
            }
        }

        when (selectedTab) {
            AddWalletTab.Import -> {
                Text("Recovery phrase (24 words)", style = MaterialTheme.typography.titleSmall)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    userScrollEnabled = false,
                ) {
                    itemsIndexed(mnemonicWords) { index, word ->
                        TextField(
                            value = word,
                            onValueChange = { mnemonicWords[index] = it.lowercase().trim() },
                            singleLine = true,
                            label = { Text("${index + 1}") },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { onImportWallet(walletName, network, mnemonicWords.toList()) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Import wallet") }
            }

            AddWalletTab.Generate -> {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Generate a demo wallet using a mock mnemonic phrase. Store it securely if you intend to reuse it.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = { onGenerateWallet(walletName, network) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Generate wallet") }
                }
            }
        }

        TextButton(onClick = onDismiss) { Text("Cancel") }
    }
}

private enum class AddWalletTab(val label: String) {
    Import("Import"),
    Generate("Generate"),
}

@Preview(showBackground = true)
@Composable
private fun AddWalletSheetPreview() {
    AddWalletSheet(
        onDismiss = {},
        onImportWallet = { _, _, _ -> },
        onGenerateWallet = { _, _ -> },
        walletCount = 1,
    )
}
