package io.ton.walletkit.demo.util

import org.json.JSONObject

fun JSONObject.optNullableString(key: String): String? {
    val value = optString(key)
    return value.takeUnless { it.isNullOrBlank() }
}
