# Android WalletKit - Native Kotlin Implementation

A native Android implementation (Compose + Kotlin) that mirrors the iOS native architecture: native UI + Kotlin wrapper + off‑screen WebView engine executing the JavaScript WalletKit adapter bundle.

## Architecture Overview

### Parallel to iOS
| Layer | iOS (Swift) | Android (Kotlin) |
|-------|-------------|------------------|
| UI | SwiftUI Views (`WalletKitView`) | Jetpack Compose (`MainActivity` + composables) |
| Wrapper | `TonWalletKitSwift` | `TonWalletKitAndroid` |
| Engine | `WalletKitEngine.swift` (hidden WebView) | `WalletKitEngine.kt` (off‑screen WebView) or `QuickJsWalletKitEngine` (embedded QuickJS) |
| JS Adapter | WalletKit JS bundle | Same bundle produced by `apps/androidkit` |
| Bridge | Swift <-> JS via message handlers | Kotlin <-> JS via `addJavascriptInterface` + `window.walletkit_request` (WebView) or JNI bridge (QuickJS) |

### Flow
1. `WalletKitEngine` loads `file:///android_asset/walletkit/index.html` (built adapter) inside an invisible WebView, while `QuickJsWalletKitEngine` loads the same adapter into the vendored `quickjs-ng` runtime.
2. On ready, Kotlin wrapper (`TonWalletKitAndroid`) calls `init` RPC.
3. Wallet import, queries, and TON Connect requests go through a narrow JSON RPC (id/method/params).
4. JS emits async events (connectRequest, transactionRequest, signDataRequest, disconnect) via `AndroidBridge.postMessage` → mapped into `Flow<JsEvent>`.

## Execution Modes

The Android demo can run WalletKit in two different engines behind the same `TonWalletKitAndroid` API surface:

- **Invisible WebView** – original implementation backed by `WalletKitEngine`. Use this when WebView usage is acceptable and you want to avoid native code.
- **Embedded QuickJS** – new implementation backed by `QuickJsWalletKitEngine`, powered by a vendored build of `quickjs-ng` with native fetch/timer/EventSource polyfills. This option removes the WebView dependency and works in headless/background flows.

Switching between the two is as simple as constructing the appropriate engine implementation.

## Project Structure

```
apps/androidkit/
  src-js/                 # JS adapter sources (polyfills, bridge, APIs)
  dist-android/           # Build output copied into Android assets
  AndroidDemo/
    app/
      src/main/
        assets/walletkit/ # (after copy) JS bundle (index.html + assets)
        java/io/ton/walletkit/demo/
          MainActivity.kt                 # Compose demo UI
          sdk/WalletKitEngine.kt          # WebView + bridge engine
          sdk/TonWalletKitAndroid.kt      # High-level wrapper (state & RPC)
        res/...
      build.gradle
    README_NATIVE.md       # (this doc)
    bridge/
      src/main/cpp/        # quickjs-ng sources + JNI bridge
      src/main/java/io/ton/walletkit/quickjs/  # QuickJS Kotlin façade
      src/androidTest/     # Instrumentation smoke tests for QuickJS engine
```

## Components

### 1. WalletKitEngine.kt
Responsibilities:
- Owns an off‑screen WebView (no UI chrome) with JS enabled + DOM storage.
- Injects a `JavascriptInterface` named `AndroidBridge` that JS uses: `AndroidBridge.postMessage(json)`.
- Evaluates RPC calls by invoking `window.walletkit_request(json)` in the WebView.
- Tracks pending RPCs via `CompletableDeferred` keyed by `id`.
- Converts raw JSON events into sealed `JsEvent` types and emits them over a shared flow.

### 2. TonWalletKitAndroid.kt
High‑level wrapper akin to iOS `TonWalletKitSwift`:
- Provides suspend functions: `initialize`, `addWalletFromMnemonic`, `refreshWallets`, `getBalance`.
- Maintains a `StateFlow<List<WalletInfo>>` for Compose observation.
- Future extension: session tracking, connect/transaction/sign approval flows.

### 3. Compose UI (MainActivity)
Demonstrates consumption of the wrapper:
- Launch effect performs: `initialize` → import hardcoded mnemonic → refresh → display first wallet + balance.
- Shows a simple `WalletCard` with address + balance (PoC acceptance criterion).
@ton connect request UIs will later subscribe to `engine.events` and surface modals.

## Bridge Contract

| Direction | Call | Shape |
|-----------|------|-------|
| Kotlin → JS | `window.walletkit_request(JSON.stringify({ id, method, params }))` | JSON RPC request |
| JS → Kotlin (response) | `AndroidBridge.postMessage({ id, result?, error? })` | Completes pending RPC |
| JS → Kotlin (event) | `AndroidBridge.postMessage({ type, data })` | Emitted as `JsEvent` |

All payloads are plain JSON objects; binary data must be hex/base64 encoded by the JS layer before crossing the bridge.

## Usage Example (Wrapper)

```kotlin
val engine = WalletKitEngine(context)
engine.initialize { /* optional hook */ }
val sdk = TonWalletKitAndroid(engine, scope)
scope.launch {
    sdk.initialize("testnet")
    sdk.addWalletFromMnemonic(words)
    val first = sdk.wallets.value.firstOrNull()
    if (first != null) {
        val balance = sdk.getBalance(first.address)
        println(balance)
    }
}
```

## Building & Running

1. Build JS adapter (from repo root):
   - `pnpm -w --filter androidkit build`
2. Copy bundle into demo assets:
   - `pnpm -w --filter androidkit copy:demo`
3. Build the QuickJS runtime bundle (optional if you only need WebView):
  - `pnpm -w --filter androidkit build:quickjs`
4. Build the Android bridge modules (generates `.aar` with both engines):
  - `cd AndroidDemo && ./gradlew :bridge:assembleDebug`
5. Open `AndroidDemo` in Android Studio, run the app.
6. You should see the imported test wallet address and its balance. QuickJS consumers load the native interpreter automatically when `QuickJsWalletKitEngine` is instantiated.

### Additional tasks

- Instrumentation smoke test for the QuickJS bridge: `./gradlew :bridge:connectedDebugAndroidTest`
- Release-ready artifacts: `./gradlew :bridge:assembleRelease :storage:assembleRelease`

## Extending to TON Connect Flows

Planned additions mirror iOS request handling views:
- Observe `engine.events` for `ConnectRequest`, `TransactionRequest`, `SignDataRequest`.
- Present Compose dialogs with request details.
- Approve / reject by invoking new RPC methods (`approveConnectRequest`, etc.) already exposed in JS adapter.

## Security Considerations

- Mnemonic currently hardcoded for PoC: replace with secure input + Android Keystore (encrypt + store).
- WebView is local asset only (no remote code injection). Disable universal navigation.
- Consider enabling strict CSP in `index.html` and removing eval usage (current RPC uses direct string injection—safe with controlled inputs, but can be hardened).

## Advantages of Native Wrapper Approach
1. Native Compose UI performance and theming.
2. Clear separation of concerns: JS handles blockchain logic; Kotlin handles presentation/state.
3. Future pluggability for alternative execution engines (e.g., Wasm or direct JVM SDK) without UI rewrite.
4. Reduced memory overhead vs. full in-WebView UI.

## Roadmap (Next Steps)
- Session list + connect / disconnect state flows.
- Transaction & sign request Compose dialogs.
- Recent transactions list using `getRecentTransactions` RPC.
- Error & loading state surfaces (Snackbar / banners).
- Unit tests for `WalletKitEngine.rpc` (timeout, error path) and wrapper parsing.

## License
Same license as the parent WalletKit project.
