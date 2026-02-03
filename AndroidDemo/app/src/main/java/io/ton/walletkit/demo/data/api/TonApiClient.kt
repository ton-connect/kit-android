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
package io.ton.walletkit.demo.data.api

import android.util.Log
import io.ton.walletkit.api.generated.TONNetwork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

/**
 * Native TON API client for fetching jettons and NFTs.
 * Uses TON API REST endpoints directly instead of going through the JS bridge.
 */
class TonApiClient(
    private val network: TONNetwork = TONNetwork("-239"), // Mainnet by default
) {
    private val baseUrl: String
        get() = when (network.chainId) {
            "-239" -> "https://tonapi.io/v2" // Mainnet
            "-3" -> "https://testnet.tonapi.io/v2" // Testnet
            else -> "https://tonapi.io/v2" // Default to mainnet
        }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Fetch jettons owned by a wallet address.
     */
    suspend fun getJettons(ownerAddress: String): JettonsResponse = withContext(Dispatchers.IO) {
        val url = "$baseUrl/accounts/$ownerAddress/jettons"
        Log.d(TAG, "Fetching jettons from: $url")

        val response = httpGet(url)
        Log.d(TAG, "Jettons response: ${response.take(500)}...")

        json.decodeFromString<JettonsResponse>(response)
    }

    /**
     * Fetch NFTs owned by a wallet address.
     */
    suspend fun getNfts(ownerAddress: String, limit: Int = 100, offset: Int = 0): NftsResponse = withContext(Dispatchers.IO) {
        val url = "$baseUrl/accounts/$ownerAddress/nfts?limit=$limit&offset=$offset&indirect_ownership=false"
        Log.d(TAG, "Fetching NFTs from: $url")

        val response = httpGet(url)
        Log.d(TAG, "NFTs response: ${response.take(500)}...")

        json.decodeFromString<NftsResponse>(response)
    }

    private fun httpGet(urlString: String): String {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json")
            connection.connectTimeout = 15000
            connection.readTimeout = 15000

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                val errorStream = connection.errorStream?.bufferedReader()?.readText() ?: ""
                throw ApiException("HTTP $responseCode: $errorStream")
            }

            connection.inputStream.bufferedReader().readText()
        } finally {
            connection.disconnect()
        }
    }

    class ApiException(message: String) : Exception(message)

    companion object {
        private const val TAG = "TonApiClient"
    }
}

// ============== Response Models ==============

@Serializable
data class JettonsResponse(
    val balances: List<JettonBalance> = emptyList(),
)

@Serializable
data class JettonBalance(
    val balance: String,
    @SerialName("wallet_address")
    val walletAddress: AccountAddress,
    val jetton: JettonInfo,
)

@Serializable
data class AccountAddress(
    val address: String,
    val name: String? = null,
    @SerialName("is_scam")
    val isScam: Boolean = false,
    @SerialName("is_wallet")
    val isWallet: Boolean = false,
)

@Serializable
data class JettonInfo(
    val address: String,
    val name: String,
    val symbol: String,
    val decimals: Int,
    val image: String? = null,
    val description: String? = null,
    val verification: String? = null,
)

@Serializable
data class NftsResponse(
    @SerialName("nft_items")
    val nftItems: List<NftItem> = emptyList(),
)

@Serializable
data class NftItem(
    val address: String,
    val index: Long,
    val owner: AccountAddress? = null,
    val collection: NftCollection? = null,
    val metadata: NftMetadata? = null,
    val previews: List<NftPreview>? = null,
    @SerialName("verified")
    val verified: Boolean = false,
)

@Serializable
data class NftCollection(
    val address: String,
    val name: String? = null,
    val description: String? = null,
)

@Serializable
data class NftMetadata(
    val name: String? = null,
    val description: String? = null,
    val image: String? = null,
)

@Serializable
data class NftPreview(
    val resolution: String,
    val url: String,
)
