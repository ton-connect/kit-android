package io.ton.walletkit.data.storage.util

import io.ton.walletkit.data.model.StoredSessionHint
import io.ton.walletkit.data.model.StoredWalletRecord
import org.json.JSONArray
import org.json.JSONObject

fun String.toStoredWalletRecord(): StoredWalletRecord? = runCatching {
    val json = JSONObject(this)
    val wordsArray = json.optJSONArray("words") ?: return null
    val words = List(wordsArray.length()) { index -> wordsArray.optString(index) }
    StoredWalletRecord(
        mnemonic = words,
        name = json.stringOrNull("name"),
        network = json.stringOrNull("network"),
        version = json.stringOrNull("version"),
    )
}.getOrNull()

fun JSONObject.stringOrNull(key: String): String? {
    if (!has(key) || isNull(key)) return null
    return optString(key)
}

fun StoredWalletRecord.toJson(): JSONObject = JSONObject().apply {
    put("words", JSONArray(mnemonic))
    name?.let { put("name", it) }
    network?.let { put("network", it) }
    version?.let { put("version", it) }
}

fun StoredSessionHint.toJson(): JSONObject = JSONObject().apply {
    manifestUrl?.let { put("manifestUrl", it) }
    dAppUrl?.let { put("dAppUrl", it) }
    iconUrl?.let { put("iconUrl", it) }
}
