# Android WalletKit Demo

Demo app for TON WalletKit SDK.

## Setup

Build SDK first:

```bash
cd ../TONWalletKit-Android
./gradlew buildAndCopyWebviewToDemo
```

Then open this project in Android Studio and run.

## Features

- Wallet creation and import
- TON Connect integration
- Transaction signing
- Balance display and auto-refresh

## Engine Selection

Edit `WalletKitDemoApp.kt`:

```kotlin
val defaultEngineKind = WalletKitEngineKind.WEBVIEW  // Recommended
```

For QuickJS: use full variant and uncomment OkHttp in `app/build.gradle.kts`.

## Structure

- `ui/` - Compose screens
- `viewmodel/` - State management  
- `storage/` - Secure wallet storage
- `libs/bridge-release.aar` - SDK (copied by build task)
