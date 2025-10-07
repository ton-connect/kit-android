# Android WalletKit Integration

Android counterpart to the iOS WalletKit demo. It mirrors the MyTonWallet architecture: a small Web bundle exposing WalletKit Web APIs, a dedicated bridge library module,
## Demo App (Compose)

The demo includes **two activities** showcasing different aspects of the SDK:

### MainActivity – Wallet Demo
- Automatically initializes WalletKit on startup with the selected engine (default: WebView)
- Imports a hardcoded **testnet** mnemonic for quick testing
- Displays:
  - First wallet's address and balance
  - Active TON Connect sessions
  - Recent bridge events (last 5)
- Features:
  - TON Connect URL input for testing connect/transaction flows
  - Manual wallet import/generation
  - Session management (disconnect)
  - Approval/rejection UI for connect/transaction/sign requests
  - Auto-refresh balance every 15 seconds

### PerformanceActivity – Engine Comparison
- **Purpose**: Side-by-side performance benchmarking of WebView vs QuickJS engines
- **Metrics measured**:
  - Engine creation time
  - `init()` call duration
  - `addWalletFromMnemonic()` execution time
  - `getWallets()` response time
  - `getWalletState()` (balance fetch) duration
  - Total end-to-end workflow time
- **Features**:
  - Run 1, 3, or 5 benchmark iterations per engine
  - Real-time progress display
  - Statistical summary (min/avg/max) for each operation
  - Export results as CSV or plain text
  - Copy to clipboard for analysis

### Engine Selection
Both activities use the engine specified in `WalletKitDemoApp.defaultEngineKind`:
```kotlin
val defaultEngineKind: WalletKitEngineKind = WalletKitEngineKind.WebView
```
Change to `QuickJS` to test the QuickJS engine.

## Project Layout

# Android WalletKit Integration

Android counterpart to the iOS WalletKit demo. It mirrors the MyTonWallet architecture with a modern dual-engine approach: JavaScript bundles exposing WalletKit Web APIs, a bridge library supporting both WebView and QuickJS runtimes, pluggable storage, and a Compose demo app with performance benchmarking.

## Project Layout

```
apps/androidkit/
├── src-js/                 # Vite bundles exposing window.walletkitBridge
│   ├── index.ts            # Entry point that bootstraps polyfills and bridge
│   ├── setupPolyfills.ts   # TextEncoder/Buffer/AbortController/fetch polyfills
│   ├── bridge.ts           # WalletKit wiring + JSON RPC handlers
│   └── types.ts            # TypeScript type definitions
├── AndroidDemo/            # Gradle project with multiple modules
│   ├── bridge/             # `walletkit-bridge` AAR (dual-engine architecture)
│   │   ├── src/main/java/  # Kotlin bridge implementations
│   │   │   ├── WalletKitEngine.kt          # Common engine interface
│   │   │   ├── WebViewWalletKitEngine.kt   # WebView implementation
│   │   │   └── QuickJsWalletKitEngine.kt   # QuickJS implementation
│   │   └── src/main/cpp/   # Native QuickJS runtime (C++)
│   │       ├── quickjs_bridge.cpp          # JNI bindings
│   │       └── third_party/quickjs-ng/     # Embedded quickjs-ng v0.10.1
│   ├── storage/            # Pluggable storage abstraction (in-memory & SharedPrefs)
│   └── app/                # Compose demo app with dual activities
│       ├── ui/MainActivity.kt          # Main wallet demo
│       └── ui/PerformanceActivity.kt   # Engine performance comparison
├── dist-android/           # Generated WebView bundle (HTML + JS assets)
├── dist-android-quickjs/   # Generated QuickJS single-file bundle
├── INTEGRATION.md          # Detailed integration guide
└── README.md               # This file
```

## Quick Start

### For SDK Users (Partners)

To integrate WalletKit into your Android app:

1. **Add the AAR dependencies** to your `build.gradle.kts`:
   ```kotlin
   dependencies {
       implementation(files("libs/bridge-release.aar"))
       implementation(files("libs/storage-release.aar"))
       // Add required transitive dependencies (see INTEGRATION.md)
   }
   ```

2. **That's it!** The JavaScript bundles are automatically included in the AAR. See [INTEGRATION.md](INTEGRATION.md) for full usage guide.

### For SDK Developers (Building from Source)

1. **Install JS dependencies**
   ```bash
   pnpm -w --filter androidkit install
   ```

2. **Build the bridge AAR** (automatically builds and includes JS bundles)
   ```bash
   cd AndroidDemo
   ./gradlew :bridge:assembleDebug :storage:assembleDebug
   ```
   
   **What happens**: The bridge module's `preBuild` task automatically:
   - Runs `pnpm run --filter androidkit build:all` to build both JS bundles
   - Copies WebView bundle to `bridge/src/main/assets/walletkit/`
   - Copies QuickJS bundle to `bridge/src/main/assets/walletkit/quickjs/`
   - Packages everything into the AAR

3. **Run the demo app**
   ```bash
   ./gradlew :app:assembleDebug
   ```
   or open in Android Studio and run the `app` configuration.

### Build Bundles Separately (Optional)

If you want to build bundles without assembling the AAR:

```bash
pnpm -w --filter androidkit run build:all       # Both bundles
pnpm -w --filter androidkit run build            # WebView only
pnpm -w --filter androidkit run build:quickjs    # QuickJS only
```

**Output**:
- WebView: `apps/androidkit/dist-android/` (HTML + modular JS)
- QuickJS: `apps/androidkit/dist-android-quickjs/walletkit.quickjs.js`

**Output**:
- WebView: `apps/androidkit/dist-android/` (HTML + modular JS)
- QuickJS: `apps/androidkit/dist-android-quickjs/walletkit.quickjs.js`

## Execution Engines

WalletKit Android ships **two interchangeable runtimes** behind a unified coroutine-based `WalletKitEngine` interface. Both engines execute the same JavaScript bundle and expose identical Kotlin APIs—the choice is purely operational.

### WebView Engine (`WebViewWalletKitEngine`)

**Runtime**: Loads the WalletKit bundle inside an invisible, headless `WebView`

**Communication**: JavaScript-to-Kotlin bridge via `WalletKitNative.postMessage` and `window.__walletkitCall`

**Bundle format**: Modular HTML + JavaScript assets (Vite build output)

**Pros**:
- **Small APK size**: ~0.04 MiB overhead (WebView is system-provided)
- **Faster performance**: 2x faster cold start (917ms vs 1881ms average)
  - Excellent Init and Add Wallet operations (critical for startup)
  - Better overall user-facing performance
- **Simpler architecture**: No native code compilation, easier to maintain
- **Full browser APIs**: Complete Web API support (DOM, standard APIs)
  - Easier to integrate third-party JS libraries that expect a browser environment
- **Chrome DevTools debugging**: Mature debugging via chrome://inspect
- **Future flexibility**: If core is rewritten in another language (not JS), can keep WebView as-is

**Cons**:
- **Higher memory footprint**: ~20-30MB (vs QuickJS 2-4MB)
- **Device fragmentation**: Some OEM images ship outdated or buggy WebView implementations
  - Inherits device fragmentation and Play Services dependencies
- **Policy friction**: Some app security policies discourage WebView usage (dynamic code loading concerns)
  - Loading local assets mitigates this, but may still trigger security reviews
- **Restricted contexts**: Limited functionality in background/headless scenarios

**Polyfills needed**: 6 polyfills
- `TextEncoder`, `Buffer`, `URL`, `URLSearchParams`, `AbortController`, fetch fallback

### QuickJS Engine (`QuickJsWalletKitEngine`)

**Runtime**: Embedded `quickjs-ng` v0.10.1 interpreter compiled from C++ sources

**Communication**: JNI bindings in `quickjs_bridge.cpp` with JSON serialization

**Bundle format**: Single-file JavaScript (all dependencies bundled)

**Pros**:
- **No WebView initialization delays**: Instant startup, no system WebView dependency
- **Minimal memory footprint**: ~2-4MB (5-10x smaller than WebView's 20-30MB)
- **Offline & sandboxed**: Complete control over execution environment
- **Background execution**: Works in headless/background processes without restrictions
- **Native integration**: JNI layer already maps timers, fetch, storage
  - Can add more native helpers (crypto, storage) without JS polyfill hacks
  - Direct access to Android APIs via JNI
- **Fast micro-operations**: Faster engine creation and account queries

**Cons**:
- **Slower crypto/wallet operations**: 2x slower total cold start (1881ms vs 917ms)
  - Significantly slower at Init (490ms vs 248ms) and Add Wallet (775ms vs 89ms)
  - Crypto-heavy operations perform worse than WebView's optimized JS engine
- **APK size increase**: +3 MiB compressed per AAR, ≈7 MiB uncompressed once installed
- **NDK maintenance**: Requires C++ compilation, NDK version management
- **Native crash risk**: C++ bugs can cause native crashes (harder to debug than JS errors)
- **Play Store requirements**:
  - System load of native library may trigger extra security audits
  - May require Play Integrity declarations (Play Console "Native code" flag)
  - **64-bit requirement**: Since August 2019, Play requires 64-bit support for native code
    - Must ship arm64-v8a and x86_64 ABIs (already included)
    - Partners cannot strip 64-bit; Play will reject APK/AAB
- **Limited Web APIs**: No built-in browser APIs, must implement polyfills
  - More maintenance burden for Web API compatibility
- **Future migration**: If core is rewritten in another language (not JS), will need significant refactoring

**Polyfills needed**: 15 polyfills
- Everything WebView needs (6) PLUS: `console`, timers (`setTimeout`/`setInterval`), `crypto`, storage APIs, HTTP client, EventSource, and more

### Performance Comparison (Mid-range Device)

**Based on 15 benchmark runs per engine:**

| Metric | WebView | QuickJS | Winner |
|--------|---------|---------|--------|
| Engine Creation | 27ms (9-195ms) | 4ms (1-20ms) | QuickJS (84% faster) |
| Init Call | 248ms (162-743ms) | 490ms (358-1143ms) | **WebView (97% faster)** |
| Add Wallet | 89ms (53-263ms) | 775ms (569-1122ms) | **WebView (775% faster)** |
| Get Account | 10ms (6-22ms) | 3ms (1-7ms) | QuickJS (73% faster) |
| Get Balance | 540ms (351-1020ms) | 574ms (369-1170ms) | WebView (6% faster) |
| **Total Cold Start** | **917ms (601-2250ms)** | **1881ms (1376-3412ms)** | **WebView (105% faster)** |
| Memory Footprint | ~20-30MB | ~2-4MB | QuickJS (5-10x smaller) |
| APK Size | ~0.04MB | ~3MB (compressed) | WebView |

**Key Findings**:
- **WebView is 2x faster** for total cold start (917ms vs 1881ms average)
- **WebView excels** at Init and Add Wallet operations (critical for startup)
- **QuickJS wins** on memory footprint (2-4MB vs 20-30MB)
- QuickJS has faster engine creation and account queries, but slower crypto operations

### Recommendation

**Performance-focused**: Use **WebView** for faster cold start and wallet operations
- 2x faster total startup
- Better for user-facing wallet initialization
- Predictable performance on modern devices

**Resource-focused**: Use **QuickJS** for minimal memory footprint
- 5-10x smaller memory usage (2-4MB vs 20-30MB)
- Better for background services or memory-constrained devices
- Smaller impact on overall app performance

**Other considerations**:
- **WebView**: Easier debugging (Chrome DevTools), simpler build, full Web APIs, smaller APK
- **QuickJS**: No WebView dependency, works offline/sandboxed, native integration via JNI

**General recommendation**: Use **WebView** for most apps unless memory constraints are critical. The 2x performance advantage and simpler architecture outweigh the memory overhead for typical wallet use cases.

### Recommendation

- **Use WebView** if you need:
  - **Fast cold start and wallet operations** (2x faster - critical for UX)
  - Minimal APK size overhead
  - Full Web API compatibility without polyfills
  - Easier third-party JS library integration
  - Simpler build process (no NDK)
  - Chrome DevTools debugging
  
- **Use QuickJS** if you need:
  - **Minimal memory footprint** (2-4MB vs 20-30MB - critical for resource-constrained devices)
  - Background/headless execution
  - Independence from system WebView versions
  - Native integration via JNI (custom crypto, storage implementations)
  - Sandboxed/offline execution

**General recommendation**: Use **WebView** for most production wallet apps. The 2x performance advantage, simpler architecture, and better developer experience outweigh the memory overhead. Only choose QuickJS if memory constraints are critical or you need background execution capabilities.

### Architecture

Both engines implement the same `WalletKitEngine` interface:

```kotlin
interface WalletKitEngine {
    suspend fun init(config: WalletKitBridgeConfig): JSONObject
    suspend fun addWalletFromMnemonic(words: List<String>, version: String, network: String?): JSONObject
    suspend fun getWallets(): List<WalletAccount>
    suspend fun getWalletState(address: String): WalletState
    suspend fun handleTonConnectUrl(url: String): JSONObject
    suspend fun approveConnect/rejectConnect/approveTransaction/rejectTransaction/...
    fun addListener(listener: WalletKitEngineListener): Closeable
    suspend fun destroy()
}
```

### Switching Engines

Select your engine during initialization:

```kotlin
// In your Application class or dependency injection setup
val engine: WalletKitEngine = when (preferredEngine) {
    WalletKitEngineKind.WEBVIEW -> WebViewWalletKitEngine(context)
    WalletKitEngineKind.QUICKJS -> QuickJsWalletKitEngine(context)
}
```

All higher-level SDK APIs remain unchanged. The demo app defaults to **WebView** for performance, with a performance comparison activity to benchmark both engines side-by-side.

## Bridge Architecture

### JavaScript Layer (`src-js/`)
- **Polyfills** (`setupPolyfills.ts`): Patches environment for both WebView and QuickJS
  - `TextEncoder`/`TextDecoder` (QuickJS)
  - `fetch` API (proxied to native OkHttp in QuickJS)
  - `EventSource` for SSE (proxied to native in QuickJS)
  - `setTimeout`/`setInterval` (proxied to ScheduledThreadPoolExecutor in QuickJS)
  - `Buffer`, `URL`, `AbortController`
- **Bridge interface** (`bridge.ts`): Exposes `window.walletkitBridge` with methods:
  - `init(config)` – Initialize WalletKit with network configuration
  - `addWalletFromMnemonic(words, version, network)` – Import wallet from mnemonic
  - `getWallets()` – List all imported wallets
  - `getWalletState(address)` – Fetch balance and transaction history
  - `handleTonConnectUrl(url)` – Process TON Connect deep links
  - `approveConnect/rejectConnect` – Respond to connection requests
  - `approveTransaction/rejectTransaction` – Respond to transaction signing requests
  - `approveSignData/rejectSignData` – Respond to arbitrary data signing requests
  - `listSessions()`, `disconnectSession(sessionId)` – Manage active TON Connect sessions
- **Events**: Streams to native via `WalletKitNative.postMessage` (WebView) or direct JNI callbacks (QuickJS):
  - `ready` – Engine initialization complete
  - `connectRequest` – TON Connect app requests wallet connection
  - `transactionRequest` – App requests transaction signing
  - `signDataRequest` – App requests data signing
  - `disconnect` – Session terminated

### Native Layer (Kotlin + C++)

#### Common Interface
- **`WalletKitEngine`**: Abstract interface for both engines
- **`WalletKitEngineListener`**: Event callback interface
- **Data models**: `WalletAccount`, `WalletState`, `WalletSession`, `WalletKitEvent`

#### WebView Implementation
- **`WebViewWalletKitEngine.kt`**: 
  - Hosts invisible WebView with `WebViewAssetLoader`
  - Loads `walletkit/index.html` from assets
  - Bidirectional bridge via `@JavascriptInterface` and `evaluateJavascript`
  - Chrome DevTools debugging enabled

#### QuickJS Implementation
- **`QuickJsWalletKitEngine.kt`**:
  - Manages QuickJS runtime lifecycle
  - Implements polyfill backends:
    - `NativeFetch`: OkHttp-backed fetch API
    - `NativeEventSource`: SSE client for TON API subscriptions
    - `NativeTimers`: setTimeout/setInterval implementation
  - Thread safety via dedicated executor and coroutine scopes
- **`quickjs_bridge.cpp`**:
  - JNI bindings for `QuickJs.create()`, `evaluate()`, `close()`
  - Type conversion between Java/Kotlin and QuickJS values
  - Method reflection for host object bindings
- **`third_party/quickjs-ng/`**:
  - Embedded quickjs-ng v0.10.1 (commit `3c9afc99`)
  - Built via CMake with 16KB page-size compatibility
  - Native libraries for arm64-v8a, armeabi-v7a, x86, x86_64

#### Storage
- **`WalletKitStorage`**: Interface for mnemonic persistence
  - `InMemoryWalletKitStorage`: PoC in-memory storage
  - `DebugSharedPrefsStorage`: SharedPreferences-backed (for demo only)
  - Production: Integrate EncryptedSharedPreferences or Android Keystore

## Demo App (Compose)

- Automatically initialises WalletKit on startup, imports a hardcoded **testnet** mnemonic, and displays the first wallet’s address & balance.
- Provides a simple UI to paste TON Connect URLs and observe incoming bridge events (connect/transaction/sign requests).
- Periodically refreshes the balance (15s) and keeps a rolling log of the last five bridge events.

## Next Steps

### For MVP/Production
- **Secure storage**: Replace `DebugSharedPrefsStorage` with EncryptedSharedPreferences or Keystore-backed signer
- **Approval UIs**: Build production-ready Compose sheets for connect/transaction/sign requests
- **Session persistence**: Implement durable session cache with Room + WorkManager for reconnection after process death
- **ProGuard/R8**: Add consumer ProGuard rules for AAR distribution
- **Error handling**: Enhance error propagation and user-facing error messages
- **Logging**: Implement structured logging and crash reporting
- **Testing**: Expand instrumentation test coverage for both engines

### Performance Optimization
- **Bundle size**: Analyze and optimize JavaScript bundle size
- **QuickJS tuning**: Experiment with memory limits and garbage collection settings
- **Asset loading**: Implement asset caching and preloading strategies
- **Background execution**: Optimize for Android Doze mode and background restrictions

### Documentation
- Add API reference documentation (KDoc)
- Create integration examples for common use cases
- Document ProGuard/R8 configuration
- Add troubleshooting guide for common issues

### Distribution
- Publish bridge AAR to Maven Central or GitHub Packages
- Version and release strategy aligned with WalletKit Web
- Changelog and migration guides for breaking changes

See [INTEGRATION.md](INTEGRATION.md) for deep integration notes and SOW alignment.
