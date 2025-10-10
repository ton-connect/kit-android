package io.ton.walletkit.demo.model

import io.ton.walletkit.presentation.request.SignDataRequest
import org.json.JSONObject

data class SignDataRequestUi(
    val id: String,
    val walletAddress: String,
    val payloadType: String,
    val payloadContent: String,
    val preview: String?,
    val raw: JSONObject,
    val iosStyleRequest: SignDataRequest? = null, // Request object with approve/reject helpers
)
