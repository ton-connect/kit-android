# Android WalletKit

TON blockchain wallet SDK for Android.

## Structure

- `TONWalletKit-Android/` - SDK library (Kotlin/Java)
- `AndroidDemo/` - Demo app

> **Note**: The JavaScript bridge code is maintained in the [main monorepo](https://github.com/ton-connect/kit) at `packages/walletkit-android-bridge`. Pre-built bundles are included in the `dist-android/` directory.

## Quick Start

```bash
# Build SDK and copy to demo
cd TONWalletKit-Android
./gradlew buildAndCopyWebviewToDemo

# Run demo
cd ../AndroidDemo
./gradlew installDebug
```

## Build Variants

- **webview** (1.2MB): WebView only, no native libs
- **full** (4.3MB): WebView + QuickJS with native libs

Use webview for production.

## Development

Use Android Studio run configurations:
- "Build WebView & Copy to Demo" - default workflow
- "Build Full & Copy to Demo" - test QuickJS variant
