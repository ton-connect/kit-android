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
package io.ton.walletkit.config

/**
 * Helper functions for creating WalletKit configurations.
 *
 * These functions mirror the JS WalletKit API for cross-platform consistency:
 * - createDeviceInfo() → Device information with sensible defaults
 * - createWalletManifest() → Wallet manifest with sensible defaults
 */

/**
 * Device information for wallet capabilities.
 *
 * This matches the JS WalletKit's DeviceInfo interface.
 * Note: On Android, appVersion is auto-detected from PackageInfo at initialization time,
 * but you can override it here if needed.
 *
 * @property appName Name of the wallet application
 * @property platform Platform the wallet runs on (always "android" on Android)
 * @property appVersion Version of the wallet application (optional, auto-detected if null)
 * @property maxProtocolVersion Maximum TON Connect protocol version supported (default: 2)
 * @property features List of features supported by the wallet
 */
data class DeviceInfo(
    val appName: String,
    val platform: String = "android",
    val appVersion: String? = null,
    val maxProtocolVersion: Int = 2,
    val features: List<TONWalletKitConfiguration.Feature>,
)

/**
 * Create device information with sensible defaults.
 *
 * This matches the JS WalletKit's createDeviceInfo() helper function.
 *
 * Example:
 * ```kotlin
 * val deviceInfo = createDeviceInfo(
 *     appName = "MyWallet",
 *     appVersion = "1.0.0"
 * )
 * ```
 *
 * @param appName Name of the wallet application (required)
 * @param appVersion Version of the wallet application (optional, will be auto-detected if not provided)
 * @param platform Platform the wallet runs on (default: "android")
 * @param maxProtocolVersion Maximum TON Connect protocol version supported (default: 2)
 * @param features List of features supported by the wallet (default: SendTransaction + SignData)
 * @return DeviceInfo object with the specified configuration
 */
fun createDeviceInfo(
    appName: String,
    appVersion: String? = null,
    platform: String = "android",
    maxProtocolVersion: Int = 2,
    features: List<TONWalletKitConfiguration.Feature> = listOf(
        TONWalletKitConfiguration.SendTransactionFeature(
            maxMessages = 4,
            extraCurrencySupported = false,
        ),
        TONWalletKitConfiguration.SignDataFeature(
            types = listOf(SignDataType.TEXT, SignDataType.BINARY, SignDataType.CELL),
        ),
    ),
): DeviceInfo {
    return DeviceInfo(
        appName = appName,
        platform = platform,
        appVersion = appVersion,
        maxProtocolVersion = maxProtocolVersion,
        features = features,
    )
}

/**
 * Create a wallet manifest with sensible defaults.
 *
 * This matches the JS WalletKit's createWalletManifest() helper function.
 *
 * Example:
 * ```kotlin
 * val manifest = createWalletManifest(
 *     name = "My TON Wallet",
 *     appName = "mywallet",
 *     imageUrl = "https://example.com/icon.png",
 *     aboutUrl = "https://example.com",
 *     universalLink = "https://example.com/tonconnect",
 *     bridgeUrl = "https://connect.ton.org/bridge"
 * )
 * ```
 *
 * @param name Human-readable name of the wallet (required)
 * @param appName Application identifier (required, should match deviceInfo.appName)
 * @param imageUrl URL to wallet icon, resolution 288×288px, PNG format (required)
 * @param aboutUrl Info or landing page of your wallet (required)
 * @param universalLink Universal link for TON Connect (required)
 * @param bridgeUrl TON Connect bridge URL (default: official bridge)
 * @param tondns TON DNS name (optional)
 * @param deepLink Deep link URL scheme (optional)
 * @return Manifest object with the specified configuration
 */
fun createWalletManifest(
    name: String,
    appName: String,
    imageUrl: String,
    aboutUrl: String,
    universalLink: String,
    bridgeUrl: String = "https://connect.ton.org/bridge",
    tondns: String? = null,
    deepLink: String? = null,
): TONWalletKitConfiguration.Manifest {
    return TONWalletKitConfiguration.Manifest(
        name = name,
        appName = appName,
        imageUrl = imageUrl,
        tondns = tondns,
        aboutUrl = aboutUrl,
        universalLink = universalLink,
        deepLink = deepLink,
        bridgeUrl = bridgeUrl,
    )
}

/**
 * Default wallet ID for V5R1 wallets.
 *
 * This matches the JS WalletKit's defaultWalletIdV5R1 constant.
 * Use this when creating multiple wallets from the same mnemonic.
 *
 * Note: Uses camelCase to match JS API naming exactly.
 */
@Suppress("ktlint:standard:property-naming")
const val defaultWalletIdV5R1: Long = 2147483409L

/**
 * Default wallet ID for V4R2 wallets.
 *
 * This matches the JS WalletKit's defaultWalletIdV4R2 constant.
 * Use this when creating multiple wallets from the same mnemonic.
 *
 * Note: Uses camelCase to match JS API naming exactly.
 */
@Suppress("ktlint:standard:property-naming")
const val defaultWalletIdV4R2: Long = 698983191L
