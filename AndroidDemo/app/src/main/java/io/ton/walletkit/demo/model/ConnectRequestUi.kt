package io.ton.walletkit.demo.model

import io.ton.walletkit.presentation.request.ConnectRequest
import org.json.JSONObject

data class ConnectRequestUi(
    val id: String,
    val dAppName: String,
    val dAppUrl: String,
    val manifestUrl: String,
    val iconUrl: String?,
    val permissions: List<ConnectPermissionUi>,
    val requestedItems: List<String>,
    val raw: JSONObject,
    val connectRequest: ConnectRequest? = null, // Request object with approve/reject helpers
)
