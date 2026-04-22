# TON WalletKit Android

TON blockchain wallet SDK for Android.

## Structure

- **[TONWalletKit-Android](TONWalletKit-Android/)** - SDK library (Kotlin/Java)
- **[AndroidDemo](AndroidDemo/)** - Demo application

## Quick Start

See [TONWalletKit-Android/README.md](TONWalletKit-Android/README.md) for SDK documentation and usage examples.

## API Keys

Add to `AndroidDemo/local.properties` (gitignored):

```properties
# TonCenter API key — enables real-time balance/transaction streaming in the demo
tonCenterApiKey=...

# TonAPI key — used by TonAPIClient for balance and masterchain queries
tonApiKey=...

```

Keys are optional; features that require them are disabled or fall back to unauthenticated requests when absent.

## Rebuilding the SDK

Use [Scripts/rebuild-sdk.sh](Scripts/rebuild-sdk.sh) when you've made changes to the TypeScript bridge in the [walletkit](https://github.com/ton-blockchain/walletkit) repo and need to propagate them into the Android SDK.

The script:
1. Builds `@ton/walletkit` (TypeScript → CJS + ESM) and the `walletkit-android-bridge` Vite bundle
2. Copies the bundle into `dist-android/`
3. Builds the SDK AAR and installs the demo app

**Prerequisites:** `pnpm`, `npx`, Android SDK with a connected device or emulator.

**Setup:** Place the `kit` and `kit-android` repos as siblings in the same directory, or set `KIT_DIR` to the walletkit repo path.

```sh
# Standard usage
./Scripts/rebuild-sdk.sh

# Also regenerate OpenAPI Kotlin models (requires openapi-generator)
./Scripts/rebuild-sdk.sh --regen-models

# Custom kit location
KIT_DIR=/path/to/walletkit ./Scripts/rebuild-sdk.sh
```
