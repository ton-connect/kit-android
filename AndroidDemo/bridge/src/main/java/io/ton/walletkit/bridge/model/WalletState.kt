package io.ton.walletkit.bridge.model

import org.json.JSONArray

data class WalletState(
    val balance: String?,
    val transactions: JSONArray,
)
