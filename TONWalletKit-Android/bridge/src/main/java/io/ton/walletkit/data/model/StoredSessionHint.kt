package io.ton.walletkit.data.model

/**
 * Lightweight session metadata for quick lookups and UI display.
 *
 * This model stores minimal information about a session that can be used
 * to display session cards or lists without loading full session data.
 * It's particularly useful for showing dApp icons and URLs in session lists.
 *
 * @property manifestUrl URL to the dApp's TON Connect manifest
 * @property dAppUrl Main URL of the dApp's website
 * @property iconUrl URL to the dApp's icon/logo for UI display
 */
data class StoredSessionHint(
    val manifestUrl: String?,
    val dAppUrl: String?,
    val iconUrl: String?,
)
