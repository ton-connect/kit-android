package io.ton.walletkit.bridge.model

import org.json.JSONObject

data class WalletKitEvent(
    val type: String,
    val data: JSONObject,
    val raw: JSONObject,
)
