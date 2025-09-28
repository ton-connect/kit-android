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
2. **Build the adapter bundle**
   ```bash
   pnpm -w --filter androidkit build
   ```
   Output lands in `apps/androidkit/dist-android/` with `index.html`, `assets/`, and `manifest.json`.
3. **Open the demo in Android Studio**
   ```bash
   cd AndroidDemo
   ./gradlew :app:assembleDebug
   ```
   or simply `Open` the project and run the `app` configuration on an API 24+ emulator/device.

## Bridge Overview

- JavaScript exposes `window.walletkitBridge` with `init`, `addWalletFromMnemonic`, `getWallets`, `getWalletState`, `handleTonConnectUrl`, and approval helpers. Events (`connectRequest`, `transactionRequest`, etc.) stream through `walletkitBridge.onEvent` and `WalletKitNative.postMessage`.
- The Android `walletkit-bridge` module hosts a dedicated `WebView`, wires `window.__walletkitCall`, and converts responses into Kotlin coroutines. It mirrors MyTonWallet’s `JSWebViewBridge` pattern.
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
