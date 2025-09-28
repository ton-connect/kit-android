# Android WalletKit Integration Guide

The goal is parity with the iOS WalletKit demo and MyTonWallet’s Android architecture. This document explains how the pieces inside `apps/androidkit` fit together and how to embed them in another app.

## 1. Build & Bundle Workflow

1. Install dependencies and build the adapter:
   ```bash
   pnpm -w --filter androidkit install
   pnpm -w --filter androidkit build
   ```
   Generates `dist-android/` with:
   - `index.html`
   - `assets/*.js`
   - `manifest.json`

2. Gradle copies the bundle into `AndroidDemo/app/src/main/assets/walletkit/` automatically during `preBuild`. If the bundle is missing you’ll get a clear build failure explaining to run the command above.

3. For downstream apps, you can reuse the same `syncWalletKitAssets` task or copy `dist-android/` into your own assets directory before packaging.

## 2. JavaScript Adapter (`src-js/`)

- `setupPolyfills.ts` patches `TextEncoder`, `fetch`, `Buffer`, `URL`, and `AbortController` so the WalletKit bundle works inside Android WebView.
- `bridge.ts` exposes `window.walletkitBridge`:
  - Methods: `init`, `addWalletFromMnemonic`, `getWallets`, `getWalletState`, `handleTonConnectUrl`, `approve*/reject*`.
  - Events: `connectRequest`, `transactionRequest`, `signDataRequest`, `disconnect`, `ready`.
  - Responses and events are posted to native through `WalletKitNative.postMessage(JSON)`.
- A legacy shim `window.walletkit_request` remains for the first PoC; native code can move to the newer `__walletkitCall` contract when ready.

## 3. Native Modules

### `bridge/` (walletkit-bridge)
- Hosts a dedicated `WebView` and exposes coroutine-based APIs.
- Key class: `WalletKitBridge`
  ```kotlin
  val bridge = WalletKitBridge(context)
  bridge.init()
  bridge.addWalletFromMnemonic(words, version = "v5r1", network = "testnet")
  val wallets = bridge.getWallets()
  val state = bridge.getWalletState(wallets.first().address)
  bridge.addListener { event -> /* render connect/tx/sign requests */ }
  ```
- Designed to be packaged as an AAR and reused by other modules/apps. `attachTo(parent)` mounts the WebView if you need direct inspection.

### `storage/`
- Defines `WalletKitStorage` (save/load/clear mnemonic) with `InMemoryWalletKitStorage` for PoC and `DebugSharedPrefsStorage` as a quick persistence option.
- Swap the implementation for EncryptedSharedPreferences or a Keystore-backed signer during MVP.

### `app/`
- Compose demo that consumes the bridge + storage. It mirrors the SwiftUI demo structure:
  - `WalletKitDemoApp` initialises the bridge in `Application.onCreate()`.
  - `WalletKitViewModel` wraps bridge calls, auto-imports a testnet mnemonic, polls balance, and exposes state to Compose.
  - `WalletScreen` renders address/balance, TonConnect URL input, and recent event log.

## 4. Message Contract

JSON payload sent to native:
```json
// Response
{"kind":"response","id":"<uuid>","result":{...}}
// Event
{"kind":"event","event":{"type":"connectRequest","data":{...}}}
// Ready handshake
{"kind":"ready"}
```

Native must reply by evaluating:
```kotlin
webView.evaluateJavascript("window.__walletkitCall('$id','$method',${JSONObject.quote(paramsJson)})")
```
where `paramsJson` is a JSON stringified object or `null`.

Errors are returned as:
```json
{"kind":"response","id":"<uuid>","error":{"message":"..."}}
```
`WalletKitBridge` converts this into a thrown `WalletKitBridgeException`.

## 5. Extending Towards MVP / Production

- **Storage**: Replace `InMemoryWalletKitStorage` with a Keystore-backed signer and route signing requests back to JS (`approve*`) only with signature payloads (never raw keys).
- **Durable events**: Mirror MyTonWallet by persisting events in Room and replaying after process death. Add WorkManager integration for SSE backoff.
- **Bridge schema**: Version messages (`v1`), include feature flags, and coordinate with WalletKit Web team.
- **UI**: Build Compose sheets for connect/transaction/sign approvals and align copy with iOS demo.

## 6. Known Limitations (PoC)

- Keys live in memory; there is no secure storage yet.
- Demo auto-imports a hardcoded mnemonic—replace with credential entry before sharing builds.
- No reconnect/durable session handling; a process death requires re-importing the wallet.
- Event handling logs to UI only. Approval actions still need to call back into the bridge.

## 7. Troubleshooting

| Symptom | Likely Cause | Fix |
| --- | --- | --- |
| Blank screen | Bundle not copied to assets | Re-run `pnpm ... copy:demo`, ensure `index.html` exists under `walletkit/`. |
| `WalletKit not initialized` | Native called `getWallets` before `init` completed | Await `bridge.init()` before making calls. |
| `Unknown method` error in logs | Method name mismatch | Use the method names defined in `bridge.ts`. |
| Balance stays at zero | Using testnet mnemonic on mainnet | Pass `network = "mainnet"` when importing or update config. |

This guide pairs with the high-level delivery plan in `android_walletkit_plan.md` and the architecture research recorded in `mytonwallet_architecture_analysis.md`.
