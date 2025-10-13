package io.ton.walletkit.presentation.model

import kotlinx.serialization.Serializable

/**
 * Information about a dApp requesting wallet interaction.
 *
 * @property name Name of the dApp
 * @property url Main URL of the dApp
 * @property iconUrl Icon URL of the dApp (nullable)
 * @property manifestUrl TON Connect manifest URL (nullable)
 */
@Serializable
data class DAppInfo(
    val name: String,
    val url: String,
    val iconUrl: String? = null,
    val manifestUrl: String? = null,
)

/**
 * Transaction request data from a dApp.
 *
 * @property recipient Recipient address
 * @property amount Amount in nanoTON
 * @property comment Optional comment/message
 * @property payload Optional raw payload
 */
data class TransactionRequest(
    val recipient: String,
    val amount: String,
    val comment: String? = null,
    val payload: String? = null,
)

/**
 * Sign data request from a dApp.
 *
 * @property payload Data to be signed (base64 or hex)
 * @property schema Optional schema identifier
 */
data class SignDataRequest(
    val payload: String,
    val schema: String? = null,
)
