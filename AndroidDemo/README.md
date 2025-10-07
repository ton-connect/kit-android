# Android WalletKit Demo

Native Compose demo showcasing the dual-engine WalletKit architecture. The project is modularized so the bridge can graduate into a reusable SDK with minimal changes.

## Architecture

### Modules

- **`bridge/`** – Core SDK module packaged as AAR
  - **Engines**: `WebViewWalletKitEngine` (invisible WebView) + `QuickJsWalletKitEngine` (embedded quickjs-ng runtime)
  - **Interface**: `WalletKitEngine` – unified coroutine-based API for both engines
  - **Native**: C++ JNI bridge (`quickjs_bridge.cpp`) and quickjs-ng v0.10.1 sources
  - **Polyfills**: Native implementations of fetch, timers, EventSource for QuickJS
  - **Tests**: Instrumentation smoke tests for both engines
  
- **`storage/`** – Pluggable storage facade
  - `WalletKitStorage` interface for mnemonic/key persistence
  - `InMemoryWalletKitStorage` – PoC implementation (volatile)
  - `DebugSharedPrefsStorage` – SharedPreferences-backed (demo only, not secure)
  - Production should use EncryptedSharedPreferences or Android Keystore
  
- **`app/`** – Compose UI demo consuming the bridge
  - `MainActivity` – Main wallet demo (balance, TON Connect, session management)
  - `PerformanceActivity` – Side-by-side engine performance comparison
  - `WalletKitViewModel` – State management and bridge interaction
  - `PerformanceBenchmarkViewModel` – Benchmarking logic and metrics collection

### Execution Flow

1. **JavaScript Layer** (`apps/androidkit/src-js/`):
   - Built by Vite into two bundle formats (WebView: modular; QuickJS: single-file)
   - Exposes `window.walletkitBridge` with WalletKit APIs
   - Includes polyfills for both environments

2. **Bridge Layer** (`bridge/src/main/java/`):
   - Kotlin engines load appropriate bundle and manage runtime
   - WebView: HTML + JS loaded via `WebViewAssetLoader`
   - QuickJS: Single JS file evaluated in native runtime via JNI
   - Bidirectional communication via JSON-RPC

3. **Native Layer** (`bridge/src/main/cpp/`):
   - QuickJS runtime compiled from C++ sources
   - JNI bindings for script evaluation and host object bindings
   - Supports all Android ABIs: arm64-v8a, armeabi-v7a, x86, x86_64

4. **UI Layer** (`app/src/main/java/`):
   - Jetpack Compose Material 3 UI
   - ViewModel pattern with StateFlow
   - Coroutine-based async operations

## Running the Demo

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17 or later
- Android SDK with API 24+ (minSdk 24, compileSdk 36)
- Android NDK 25+ (automatically installed)
- Node.js 18+ and pnpm (only for building from source)

### Quick Start

1. **Open in Android Studio**:
   - File → Open → Select `AndroidDemo/`
   - Wait for Gradle sync
   - Run `app` configuration on API 24+ device/emulator

2. **Or build from command line**:
   ```bash
   cd AndroidDemo
   ./gradlew :app:installDebug
   ```

**What happens automatically**: The bridge module's `preBuild` task:
- Builds JavaScript bundles via `pnpm run --filter androidkit build:all`
- Copies bundles into `bridge/src/main/assets/walletkit/`
- Packages them into the bridge AAR
- App module inherits assets from bridge dependency

First build takes 2-5 minutes.

### Demo Features

**MainActivity** – Wallet demo:
- Auto-initializes with QuickJS engine (configurable in `WalletKitDemoApp`)
- Imports testnet mnemonic, displays balance and sessions
- Test TON Connect by pasting deep links
- Approve/reject connection and transaction requests

**PerformanceActivity** – Benchmarking:
- Compare WebView vs QuickJS performance
- Run 1, 3, or 5 iterations per engine
- Export results as CSV or copy to clipboard

## Engine Selection & Bridge Contract

Switch engines by instantiating `WebViewWalletKitEngine` or `QuickJsWalletKitEngine` in `WalletKitDemoApp`:

```kotlin
val defaultEngineKind: WalletKitEngineKind = WalletKitEngineKind.QUICKJS // or WEBVIEW
```

**Communication**:
- Native → JS: `WalletKitEngine` forwards RPCs to `window.__walletkitCall` (WebView) or JNI (QuickJS)
- JS → Native: Events via `WalletKitNative.postMessage` (WebView) or direct JNI callbacks (QuickJS)
- Message format: `{kind: 'response' | 'event' | 'ready', ...}`

### Additional Tooling

```bash
./gradlew :bridge:connectedDebugAndroidTest   # Run instrumentation tests (requires device/emulator)
./gradlew :bridge:assembleRelease             # Build release AAR for distribution
./gradlew :app:assembleRelease                # Build release APK
```

## Production Roadmap

### Security
- **Storage**: Replace `DebugSharedPrefsStorage` with `EncryptedSharedPreferences` or Keystore-backed signer
- **ProGuard/R8**: Add consumer ProGuard rules for AAR obfuscation
- **Network**: Implement certificate pinning for API calls

### Features
- **Approval UIs**: Production Compose sheets for connect/transaction/sign requests
- **Session persistence**: Durable cache with Room + WorkManager for reconnect after process death
- **Background execution**: Support for Doze mode and background restrictions
- **Multi-wallet**: Support for multiple accounts and wallet switching

### Distribution
- **AAR packaging**: Publish to Maven Central or GitHub Packages
- **Documentation**: API reference (KDoc), integration guides, migration notes
- **Testing**: Expanded test coverage for both engines and edge cases

## Performance Notes
```
=== Performance Comparison ===

QUICKJS (15 runs):
  Engine Creation: 4.2ms (1ms - 20ms)
  Init:            489.5ms (358ms - 1143ms)
  Add Wallet:      775.3ms (569ms - 1122ms)
  Get Account:     2.7ms (1ms - 7ms)
  Get Balance:     574.0ms (369ms - 1170ms)
  Total Startup:   1881.0ms (1376ms - 3412ms)

WEBVIEW (15 runs):
  Engine Creation: 27.1ms (9ms - 195ms)
  Init:            247.9ms (162ms - 743ms)
  Add Wallet:      88.6ms (53ms - 263ms)
  Get Account:     10.2ms (6ms - 22ms)
  Get Balance:     539.7ms (351ms - 1020ms)
  Total Startup:   917.0ms (601ms - 2250ms)

=== Speed Comparison (QuickJS vs WebView) ===
Engine Creation: QuickJS faster by 84% (4.2ms vs 27.1ms)
Init:            WebView faster by 97% (489.5ms vs 247.9ms)
Add Wallet:      WebView faster by 775% (775.3ms vs 88.6ms)
Get Account:     QuickJS faster by 73% (2.7ms vs 10.2ms)
Get Balance:     WebView faster by 6% (574.0ms vs 539.7ms)
Total Startup:   WebView faster by 105% (1881.0ms vs 917.0ms)
```