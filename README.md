# Android WalletKit Integration

Android counterpart to the iOS WalletKit demo. It mirrors the MyTonWallet architecture: a small Web bundle exposing WalletKit Web APIs, a dedicated bridge library module, pluggable storage, and a Compose demo app.

## Project Layout

```
apps/androidkit/
├── src-js/                 # Vite bundle exposing window.walletkitBridge
│   ├── index.ts            # Bootstraps polyfills and bridge
│   ├── setupPolyfills.ts   # TextEncoder/Buffer/AbortController polyfills
│   └── bridge.ts           # WalletKit wiring + JSON RPC handlers
├── AndroidDemo/            # Gradle project with multiple modules
│   ├── bridge/             # `walletkit-bridge` AAR (WebView host, event stream)
│   ├── storage/            # Pluggable storage abstraction (in-memory & SharedPrefs)
│   └── app/                # Compose demo app that consumes the bridge
├── dist-android/           # Generated bundle (assets + manifest)
├── INTEGRATION.md          # Detailed integration guide
└── README.md               # This file
```

## Quick Start

1. **Install JS dependencies**
   ```bash
   pnpm -w --filter androidkit install
   ```
2. **Build the adapter bundles**
   ```bash
   pnpm -w --filter androidkit run build            # WebView bundle
   pnpm -w --filter androidkit run build:quickjs    # QuickJS single-file bundle
   ```
   WebView assets land in `apps/androidkit/dist-android/`; the QuickJS runtime is emitted to `apps/androidkit/dist-android-quickjs/walletkit.quickjs.js`.
3. **Build the Android bridge modules** (produces `.aar` artifacts for WebView and QuickJS engines)
   ```bash
   cd AndroidDemo
   ./gradlew :bridge:assembleDebug :storage:assembleDebug
   ```
   Use `assembleRelease` when you need a release-ready `.aar` for distribution.
4. **Run the sample app**
   ```bash
   ./gradlew :app:assembleDebug
   ```
   or simply `Open` the project in Android Studio and run the `app` configuration on an API 24+ emulator/device.

### Optional verification

```bash
./gradlew :bridge:connectedDebugAndroidTest   # Exercises the QuickJS bridge on a device/emulator
pnpm -w --filter androidkit run copy:demo     # Sync bundles into the demo app assets
```

## Execution Engines

WalletKit Android ships two interchangeable runtimes behind the same coroutine API:

- **Invisible WebView** – `WebViewWalletKitEngine` loads the WalletKit bundle inside a hidden WebView. Ideal when WebView is already allowed in your app and you prefer zero native code.
- **Embedded QuickJS** – `QuickJsWalletKitEngine` runs the same bundle inside the vendored `quickjs-ng` interpreter, compiled as part of the `bridge` module. This avoids WebView startup overhead and works in headless/background contexts.

Switching engines is a matter of selecting the appropriate implementation during dependency injection—no changes to higher level SDK APIs.

## Bridge Overview

- JavaScript exposes `window.walletkitBridge` with `init`, `addWalletFromMnemonic`, `getWallets`, `getWalletState`, `handleTonConnectUrl`, and approval helpers. Events (`connectRequest`, `transactionRequest`, etc.) stream through `walletkitBridge.onEvent` and `WalletKitNative.postMessage`.
- The Android `walletkit-bridge` module now bundles both engines and automatically loads the vendored `quickjs-ng` native library when you choose the QuickJS path.
- The `storage` module is a placeholder for secure persistence. For PoC we use an in-memory implementation; swap in EncryptedSharedPreferences / Keystore for MVP.

## Demo App (Compose)

- Automatically initialises WalletKit on startup, imports a hardcoded **testnet** mnemonic, and displays the first wallet’s address & balance.
- Provides a simple UI to paste TON Connect URLs and observe incoming bridge events (connect/transaction/sign requests).
- Periodically refreshes the balance (15s) and keeps a rolling log of the last five bridge events.

## Next Steps

- Harden storage (Keystore-backed signer) and make the bridge module publishable as an AAR.
- Flesh out approval UIs (Compose sheets) to mirror iOS SwiftUI demo interactions.
- Add instrumentation tests that exercise the bridge bundle (load WebView + init + balance fetch).
- Align the JSON schema with the MyTonWallet / web WalletKit team for long-term compatibility (versioned messages, typed errors).

See [INTEGRATION.md](INTEGRATION.md) for deep integration notes and SOW alignment.
