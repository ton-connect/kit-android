# Android WalletKit

TON blockchain wallet SDK for Android.

## Structure

- `TONWalletKit-Android/` - SDK library
- `AndroidDemo/` - Demo app
- `js/` - TypeScript source for bridge

## Quick Start

```bash
# 1. Build JavaScript bundles
pnpm install
pnpm run build:all

# 2. Build SDK and copy to demo
cd TONWalletKit-Android
./gradlew buildAndCopyWebviewToDemo

# 3. Run demo
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
