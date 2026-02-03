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
package io.ton.walletkit.event

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Intent event from TonConnect intent deep links.
 *
 * Intents allow dApps to request actions via deep links without a prior session.
 * This is used for gasless transactions (signMsg) and direct transaction requests (txIntent).
 */
@Serializable
sealed class TONIntentEvent {
    /** Unique event ID */
    abstract val id: String

    /** Client public key (hex) */
    abstract val clientId: String

    /** Whether a connect flow should follow */
    abstract val hasConnectRequest: Boolean

    /** Intent type */
    abstract val type: String

    /** Optional connect request to establish connection after intent */
    abstract val connectRequest: TONIntentConnectRequest?

    /**
     * Transaction intent event (txIntent or signMsg).
     *
     * For txIntent: transaction is signed and sent to the blockchain.
     * For signMsg: transaction is signed but NOT sent (for gasless transactions).
     */
    @Serializable
    @SerialName("transaction")
    data class TransactionIntent(
        override val id: String,
        override val clientId: String,
        override val hasConnectRequest: Boolean,
        override val type: String,
        override val connectRequest: TONIntentConnectRequest? = null,
        /** Network chain ID ("-239" for mainnet, "-3" for testnet) */
        val network: String? = null,
        /** Valid until timestamp */
        val validUntil: Long? = null,
        /** Intent items to execute */
        val items: List<TONIntentItem>,
    ) : TONIntentEvent()

    /**
     * Sign data intent event (signIntent).
     */
    @Serializable
    @SerialName("signData")
    data class SignDataIntent(
        override val id: String,
        override val clientId: String,
        override val hasConnectRequest: Boolean,
        override val type: String = "signIntent",
        override val connectRequest: TONIntentConnectRequest? = null,
        /** Network chain ID */
        val network: String? = null,
        /** Manifest URL for domain binding */
        val manifestUrl: String,
        /** Sign data payload */
        val payload: TONSignDataPayload,
    ) : TONIntentEvent()

    /**
     * Action intent event (actionIntent).
     *
     * The wallet should fetch action details from the provided URL.
     */
    @Serializable
    @SerialName("action")
    data class ActionIntent(
        override val id: String,
        override val clientId: String,
        override val hasConnectRequest: Boolean,
        override val type: String = "actionIntent",
        override val connectRequest: TONIntentConnectRequest? = null,
        /** Action URL to fetch details from */
        val actionUrl: String,
    ) : TONIntentEvent()
}

/**
 * Connect request embedded in an intent.
 */
@Serializable
data class TONIntentConnectRequest(
    /** Manifest URL for the dApp */
    val manifestUrl: String,
    /** Requested items (ton_addr, ton_proof, etc.) */
    val items: List<TONIntentConnectItem>? = null,
)

/**
 * Connect item in an intent connect request.
 */
@Serializable
data class TONIntentConnectItem(
    /** Item name (e.g., "ton_addr", "ton_proof") */
    val name: String,
    /** Optional payload (e.g., for ton_proof) */
    val payload: String? = null,
)

/**
 * Intent item - a single action in a transaction intent.
 */
@Serializable
sealed class TONIntentItem {
    /** Item type */
    abstract val type: String

    /**
     * TON transfer intent item.
     */
    @Serializable
    @SerialName("ton")
    data class SendTon(
        override val type: String = "ton",
        /** Destination address (user-friendly format) */
        @SerialName("a")
        val address: String,
        /** Amount in nanotons */
        @SerialName("am")
        val amount: String,
        /** Optional payload (base64 BoC) */
        @SerialName("p")
        val payload: String? = null,
        /** Optional stateInit (base64 BoC) */
        @SerialName("si")
        val stateInit: String? = null,
    ) : TONIntentItem()

    /**
     * Jetton transfer intent item.
     */
    @Serializable
    @SerialName("jetton")
    data class SendJetton(
        override val type: String = "jetton",
        /** Jetton master contract address */
        @SerialName("ma")
        val masterAddress: String,
        /** Amount of jettons */
        @SerialName("ja")
        val jettonAmount: String,
        /** Destination address */
        @SerialName("d")
        val destination: String,
        /** Query ID */
        @SerialName("qi")
        val queryId: Long? = null,
        /** Response destination */
        @SerialName("rd")
        val responseDestination: String? = null,
        /** Custom payload (base64 BoC) */
        @SerialName("cp")
        val customPayload: String? = null,
        /** Forward TON amount */
        @SerialName("fta")
        val forwardTonAmount: String? = null,
        /** Forward payload (base64 BoC) */
        @SerialName("fp")
        val forwardPayload: String? = null,
    ) : TONIntentItem()

    /**
     * NFT transfer intent item.
     */
    @Serializable
    @SerialName("nft")
    data class SendNft(
        override val type: String = "nft",
        /** NFT item address */
        @SerialName("na")
        val nftAddress: String,
        /** New owner address */
        @SerialName("no")
        val newOwner: String,
        /** Query ID */
        @SerialName("qi")
        val queryId: Long? = null,
        /** Response destination */
        @SerialName("rd")
        val responseDestination: String? = null,
        /** Custom payload (base64 BoC) */
        @SerialName("cp")
        val customPayload: String? = null,
        /** Forward TON amount */
        @SerialName("fta")
        val forwardTonAmount: String? = null,
        /** Forward payload (base64 BoC) */
        @SerialName("fp")
        val forwardPayload: String? = null,
    ) : TONIntentItem()
}

/**
 * Sign data payload for signIntent.
 */
@Serializable
sealed class TONSignDataPayload {
    abstract val type: String

    @Serializable
    @SerialName("text")
    data class Text(
        override val type: String = "text",
        val text: String,
    ) : TONSignDataPayload()

    @Serializable
    @SerialName("binary")
    data class Binary(
        override val type: String = "binary",
        /** Base64 encoded bytes */
        val bytes: String,
    ) : TONSignDataPayload()

    @Serializable
    @SerialName("cell")
    data class Cell(
        override val type: String = "cell",
        /** TL-B schema */
        val schema: String,
        /** Base64 encoded BoC */
        val cell: String,
    ) : TONSignDataPayload()
}
