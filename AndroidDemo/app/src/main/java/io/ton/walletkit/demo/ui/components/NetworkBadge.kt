package io.ton.walletkit.demo.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.ton.walletkit.demo.model.TonNetwork

@Composable
fun NetworkBadge(network: TonNetwork) {
    val color = when (network) {
        TonNetwork.MAINNET -> Color(0xFF2E7D32)
        TonNetwork.TESTNET -> Color(0xFFF57C00)
    }
    Surface(shape = MaterialTheme.shapes.medium, color = color.copy(alpha = 0.12f)) {
        Text(
            text = when (network) {
                TonNetwork.MAINNET -> "Mainnet"
                TonNetwork.TESTNET -> "Testnet"
            },
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            color = color,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Preview
@Composable
private fun NetworkBadgePreview() {
    NetworkBadge(network = TonNetwork.MAINNET)
}
