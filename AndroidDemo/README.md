# Android WalletKit Demo

Native demo mirroring the iOS WalletKit sample. The project is split into modules so the bridge can graduate into a reusable SDK.

## Modules

- `bridge/` – hosts a dedicated WebView, exposes coroutine APIs (`WalletKitBridge`), and streams WalletKit events.
- `storage/` – pluggable storage facade; PoC uses in-memory/shared prefs, production swaps in Keystore.
- `app/` – Compose UI that consumes the bridge, shows address/balance, and surfaces TON Connect events.

## Running the Demo

1. Build the JS adapter from the repo root (once per change):
   ```bash
   pnpm -w --filter androidkit install
   pnpm -w --filter androidkit build
   ```
2. Open `AndroidDemo/` in Android Studio (or run `./gradlew :app:installDebug`).
   The Gradle build runs `syncWalletKitAssets`, copying the bundle into `src/main/assets/walletkit/`.
3. Launch the `app` module on API 24+.

The demo loads a **testnet** mnemonic, displays the first wallet’s balance, and lets you paste TON Connect URLs to see events flow through the bridge.

## Bridge Contract

- Native → JS: `window.__walletkitCall(id, method, jsonParams)` invoked via `WalletKitBridge.call`.
- JS → Native: `WalletKitNative.postMessage({ kind: 'response' | 'event' | 'ready', ... })`.
- Legacy `walletkit_request` shim remains for backwards compatibility.

## Todo for MVP

- Secure key custody (Keystore-backed signer).
- Compose sheets for connect/transaction/sign approvals that call `approve*`/`reject*`.
- Durable session cache + reconnect (Room/WorkManager).
- Packaging the bridge as an AAR with ProGuard/R8 configuration.
