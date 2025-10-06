# Android WalletKit Demo

Native demo mirroring the iOS WalletKit sample. The project is split into modules so the bridge can graduate into a reusable SDK.

## Modules

- `bridge/` – packs both execution engines: `WebViewWalletKitEngine` (invisible WebView host) and `QuickJsWalletKitEngine` (embedded `quickjs-ng` runtime) behind coroutine-first APIs, plus instrumentation smoke tests and vendored native sources.
- `storage/` – pluggable storage facade; PoC uses in-memory/shared prefs, production swaps in Keystore.
- `app/` – Compose UI that consumes the bridge, shows address/balance, and surfaces TON Connect events.

## Running the Demo

1. Install dependencies and build the JS adapters from the repo root (once per change):
   ```bash
   pnpm -w --filter androidkit install
   pnpm -w --filter androidkit build
   ```
2. Produce the QuickJS single-file bundle (optional if you only need WebView):
    ```bash
    pnpm -w --filter androidkit build:quickjs
    ```
3. Copy bundles into the demo app assets (from the repo root):
    ```bash
    pnpm -w --filter androidkit copy:demo
    ```
4. Build bridge artifacts (`.aar` files include both engines):
    ```bash
    cd AndroidDemo
    ./gradlew :bridge:assembleDebug :storage:assembleDebug
    ```
5. Open `AndroidDemo/` in Android Studio (or run `./gradlew :app:installDebug`).
   The Gradle build runs `syncWalletKitAssets`, copying the WebView bundle; the QuickJS runtime is pulled from `dist-android-quickjs` at startup.
6. Launch the `app` module on API 24+.

The demo loads a **testnet** mnemonic, displays the first wallet’s balance, and lets you paste TON Connect URLs to see events flow through the bridge.

## Engine Selection & Bridge Contract

- Native → JS: `WalletKitBridge.call` forwards RPCs either to `window.__walletkitCall` in the WebView engine or to the JNI-bound QuickJS runtime.
- JS → Native: WebView flows through `WalletKitNative.postMessage({ kind: 'response' | 'event' | 'ready', ... })`; QuickJS uses direct JNI callbacks serialised as JSON.
- Legacy `walletkit_request` shim remains for backwards compatibility while the new engine migrates consumers.

Switch engines by instantiating `WebViewWalletKitEngine` or `QuickJsWalletKitEngine`; the surrounding SDK APIs and storage modules remain unchanged.

### Additional tooling

```bash
./gradlew :bridge:connectedDebugAndroidTest   # Exercises QuickJS bridge on device/emulator
./gradlew :bridge:assembleRelease             # Produces release-ready bridge AAR
```

## Todo for MVP

- Secure key custody (Keystore-backed signer).
- Compose sheets for connect/transaction/sign approvals that call `approve*`/`reject*`.
- Durable session cache + reconnect (Room/WorkManager).
- Packaging the bridge as an AAR with ProGuard/R8 configuration.
