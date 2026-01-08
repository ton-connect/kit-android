# TON WalletKit Android

Kotlin library providing TON wallet capabilities for Android.

- Minimum: Android 8.0 (API 26)
- Requires up-to-date Android System WebView (tested with 138.0.7204.179+)

## Installation

#### Gradle

Add to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation("io.github.ton-connect:tonwalletkit-android:1.0.0-beta01")
}
```

## Quick start

#### Initialize TONWalletKit:
```kotlin
import io.ton.walletkit.ITONWalletKit
import io.ton.walletkit.config.TONWalletKitConfiguration
import io.ton.walletkit.model.TONNetwork

// Create configuration that fits your app
val configuration = TONWalletKitConfiguration(
    network = TONNetwork.TESTNET,
    walletManifest = TONWalletKitConfiguration.Manifest(
        name = "MyTONWallet",
        appName = "MyTONWalletIdentifier",
        imageUrl = "https://example.com/image.png",
        aboutUrl = "https://example.com/about",
        universalLink = "https://example.com/universal-link",
        bridgeUrl = "https://bridge.tonapi.io/bridge"
    ),
    // Additional configuration options as needed
)

// Initialize the kit
val kit = ITONWalletKit.initialize(
    context = context,
    config = configuration
)
```

#### Add events listener:
```kotlin
import io.ton.walletkit.listener.TONBridgeEventsHandler
import io.ton.walletkit.event.TONWalletKitEvent

class MyAppEventsListener : TONBridgeEventsHandler {
    override suspend fun handle(event: TONWalletKitEvent) {
        println("TONWalletKit event: $event")
    }
}

val events = MyAppEventsListener()
kit.addEventsHandler(events)
```

#### Create and add a v5r1 wallet using mnemonic:
```kotlin
import io.ton.walletkit.model.TONNetwork

// Generate a new mnemonic
val mnemonic = kit.createTonMnemonic()

// 3-step wallet creation pattern:
// Step 1: Create signer from mnemonic
val signer = kit.createSignerFromMnemonic(mnemonic)

// Step 2: Create V5R1 adapter
val adapter = kit.createV5R1Adapter(
    signer = signer,
    network = TONNetwork.TESTNET
)

// Step 3: Add wallet
val wallet = kit.addWallet(adapter.adapterId)
```

#### Read wallet address and balance:
```kotlin
val address = wallet.address
val balance = wallet.balance()

println("Address: ${address ?: "<none>"}")
println("Balance: ${balance ?: "<unknown>"}")
```#### Add wallet with external signer (e.g., hardware wallet):
```kotlin
import io.ton.walletkit.model.WalletSigner
import io.ton.walletkit.model.KeyPair

// Create custom signer implementation
val customSigner = object : WalletSigner {
    override val publicKey: ByteArray = // ... your public key from hardware wallet
    
    override suspend fun sign(data: ByteArray): ByteArray {
        // Forward to external signing service (e.g., hardware wallet)
        // Show confirmation dialog and get signature
        return signature
    }
}

// Step 1: Create signer from custom implementation
val signer = kit.createSignerFromCustom(customSigner)

// Step 2: Create adapter
val adapter = kit.createV4R2Adapter(
    signer = signer,
    network = TONNetwork.MAINNET
)

// Step 3: Add wallet
val wallet = kit.addWallet(adapter.adapterId)
```

#### Get all wallets:
```kotlin
val wallets = kit.getWallets()
```

#### Remove wallet:
```kotlin
kit.removeWallet(wallet.address)
```

#### Clean up when done:
```kotlin
// Remove all event handlers and release resources
kit.destroy()
```

#### Notes
- For persistent storage, configure `storage` in `TONWalletKitConfiguration`.
- To add wallets using a secret key, use `createSignerFromSecretKey()`.
- [Demo app](../AndroidDemo) shows a more complete integration.
