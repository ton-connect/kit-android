# Android WalletKit - Native Kotlin Implementation

A native Android implementation (Compose + Kotlin) that mirrors the iOS native architecture: native UI + ViewModel layer + dual-engine execution (WebView or QuickJS) for the JavaScript WalletKit adapter bundle.

## Architecture Overview

### Parallel to iOS
| Layer | iOS (Swift) | Android (Kotlin) |
|-------|-------------|------------------|
| UI | SwiftUI Views (`WalletKitView`) | Jetpack Compose (`MainActivity` + composables) |
| State Management | Swift ObservableObject | `WalletKitViewModel` (Android ViewModel + StateFlow) |
| Engine Interface | `WalletKitEngine` (protocol) | `WalletKitEngine` (Kotlin interface) |
| Engine Implementations | WebView-based | `WebViewWalletKitEngine` + `QuickJsWalletKitEngine` |
| JS Adapter | WalletKit JS bundle | Same bundle produced by `apps/androidkit` (two formats) |
| Bridge | Swift <-> JS via message handlers | Kotlin <-> JS via `WalletKitNative.postMessage` + `window.__walletkitCall` (WebView) or JNI (QuickJS) |

### Flow
1. **Engine Initialization**: `WebViewWalletKitEngine` loads `file:///android_asset/walletkit/index.html` inside an invisible WebView, while `QuickJsWalletKitEngine` loads the single-file bundle into the embedded `quickjs-ng` runtime via JNI.
2. **Ready Handshake**: On load, JS sends `{kind: "ready"}` message to native, completing engine initialization.
3. **RPC Communication**: Wallet operations (init, import, queries, TON Connect) go through JSON-RPC (`id`/`method`/`params` format).
4. **Event Stream**: JS emits async events (connectRequest, transactionRequest, signDataRequest, disconnect) via `WalletKitNative.postMessage` (WebView) or JNI callbacks (QuickJS), mapped into Kotlin `WalletKitEngineListener` callbacks.

## Execution Modes

The Android WalletKit provides **two interchangeable engine implementations** behind the unified `WalletKitEngine` interface:

### WebView Engine (`WebViewWalletKitEngine`)

Loads the modular WalletKit bundle (HTML + JS) inside an invisible, headless WebView.

**Pros**:
- **Small size**: ~0.04 MiB overhead (system WebView)
- **Simpler**: No native compilation, easier maintenance
- **Full browser APIs**: Complete Web API support, easier third-party JS library integration
- **Debugging**: Chrome DevTools (chrome://inspect)
- **Future-proof**: Can keep as-is if core is rewritten in another language

**Cons**:
- **Slower**: Cold start takes more time and memory (~20-30MB)
- **Fragmentation**: OEM WebView implementations vary; Play Services dependency
- **Policy friction**: Some enterprises flag WebView (dynamic code concerns)
- **Limited contexts**: Restricted in background/headless scenarios

**Polyfills needed**: 6 (TextEncoder, Buffer, URL, URLSearchParams, AbortController, fetch fallback)

### QuickJS Engine (`QuickJsWalletKitEngine`)

Executes the single-file WalletKit bundle in embedded `quickjs-ng` v0.10.1 interpreter (C++ via JNI).

**Pros**:
- **Lightweight memory**: ~2-4MB memory (vs WebView's ~20-30MB = 5-10x smaller)
- **Faster micro-operations**: Engine creation (84% faster), account queries (73% faster)
- **Offline & sandboxed**: Complete control, background execution support
- **Native integration**: JNI layer for timers, fetch, storage; can add more native helpers

**Cons**:
- **Size**: +3 MiB compressed AAR, ~7 MiB uncompressed installed
- **NDK maintenance**: C++ compilation, version management
- **Native crashes**: C++ bugs harder to debug
- **Play Store requirements**:
  - May trigger security audits, Play Integrity declarations
  - **Mandatory 64-bit**: Must ship arm64-v8a, x86_64 (cannot strip; Play rejects)
- **Limited Web APIs**: 15+ polyfills needed (vs 6 for WebView)
- **Future migration**: Harder to migrate if core rewritten in non-JS language

**Polyfills needed**: 15+ (WebView's 6 PLUS console, timers, crypto, storage, HTTP, EventSource, etc.)

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
| Memory | ~20-30MB | ~2-4MB | **QuickJS (5-10x smaller)** |
| APK Size | ~0.04MB | ~3MB compressed | **WebView (smaller)** |

### Switching Engines

```kotlin
class WalletKitDemoApp : Application() {
    val defaultEngineKind = WalletKitEngineKind.QUICKJS // or WEBVIEW
    
    fun obtainEngine(kind: WalletKitEngineKind = defaultEngineKind): WalletKitEngine {
        return when (kind) {
            WalletKitEngineKind.WEBVIEW -> WebViewWalletKitEngine(this)
            WalletKitEngineKind.QUICKJS -> QuickJsWalletKitEngine(this)
        }
    }
}
```

**Recommendation**: Use **WebView** for most production apps (2x faster cold start, better wallet performance). Use **QuickJS** for memory-constrained scenarios or background execution needs.

All higher-level code (ViewModels, UI) remains unchanged regardless of engine choice.

## Project Structure

```
apps/androidkit/
  src-js/                 # TypeScript sources for WalletKit bridge
    ├── index.ts          # Entry point (imports polyfills + bridge)
    ├── setupPolyfills.ts # Environment polyfills for both engines
    ├── bridge.ts         # window.walletkitBridge implementation
    └── types.ts          # TypeScript type definitions
  dist-android/           # WebView bundle output (HTML + modular JS)
  dist-android-quickjs/   # QuickJS bundle output (single-file JS)
  
  AndroidDemo/
    bridge/               # Reusable SDK module (packaged as AAR)
      src/main/
        java/io/ton/walletkit/
          bridge/
            ├── WalletKitEngine.kt            # Common engine interface
            ├── WebViewWalletKitEngine.kt     # WebView implementation
            ├── QuickJsWalletKitEngine.kt     # QuickJS implementation
            ├── listener/
            │   └── WalletKitEngineListener.kt  # Event callback interface
            ├── model/                         # Data models (WalletAccount, WalletState, etc.)
            └── config/                        # Configuration classes
          quickjs/
            └── QuickJs.kt                    # JNI facade for native QuickJS
        cpp/
          ├── quickjs_bridge.cpp              # JNI bindings
          ├── CMakeLists.txt                  # Native build config
          └── third_party/
              └── quickjs-ng/                 # quickjs-ng v0.10.1 sources
        androidTest/                          # Instrumentation tests
      
    storage/              # Storage abstraction module (AAR)
      src/main/java/io/ton/walletkit/storage/
        ├── WalletKitStorage.kt               # Storage interface
        └── impl/
            ├── InMemoryWalletKitStorage.kt   # In-memory implementation
            └── DebugSharedPrefsStorage.kt    # SharedPreferences implementation
    
    app/                  # Demo application
      src/main/
        java/io/ton/walletkit/demo/
          ├── WalletKitDemoApp.kt             # Application class (engine factory)
          ├── WalletKitViewModel.kt           # Main ViewModel (wallet state)
          ├── PerformanceBenchmarkViewModel.kt  # Performance testing ViewModel
          ├── PerformanceCollector.kt         # Metrics collection
          └── ui/
              ├── MainActivity.kt             # Wallet demo UI
              ├── PerformanceActivity.kt      # Performance comparison UI
              └── screen/                     # Compose screens
        res/                                  # Android resources
      build.gradle.kts                        # App build config
```

**Note**: JavaScript bundles are packaged inside the `bridge` AAR's assets. The bridge module's `preBuild` task automatically:
1. Runs `pnpm run --filter androidkit build:all` to build both JS bundles
2. Copies WebView bundle to `bridge/src/main/assets/walletkit/`
3. Copies QuickJS bundle to `bridge/src/main/assets/walletkit/quickjs/`
4. Packages them in the AAR during assembly

Partners just add the AAR dependency - no manual asset copying needed!

```

## Components

### 1. WalletKitEngine Interface (bridge/src/main/java/)
Core abstraction for both execution engines:
```kotlin
interface WalletKitEngine {
    val kind: WalletKitEngineKind
    fun addListener(listener: WalletKitEngineListener): Closeable
    suspend fun init(config: WalletKitBridgeConfig): JSONObject
    suspend fun addWalletFromMnemonic(words: List<String>, version: String, network: String?): JSONObject
    suspend fun getWallets(): List<WalletAccount>
    suspend fun getWalletState(address: String): WalletState
    suspend fun handleTonConnectUrl(url: String): JSONObject
    suspend fun approveConnect/rejectConnect/approveTransaction/rejectTransaction/...
    suspend fun destroy()
}
```

### 2. WebViewWalletKitEngine (bridge/)
**Responsibilities**:
- Owns an invisible, headless WebView with JavaScript and DOM storage enabled
- Loads `file:///android_asset/walletkit/index.html` via `WebViewAssetLoader`
- Injects `@JavascriptInterface` named `WalletKitNative` for JS → Kotlin messaging
- Calls JS via `webView.evaluateJavascript("window.__walletkitCall(...)")` for Kotlin → JS RPC
- Tracks pending RPCs using `CompletableDeferred<BridgeResponse>` keyed by UUID
- Parses JSON responses/events and invokes registered listeners
- Chrome DevTools debugging enabled for development

### 3. QuickJsWalletKitEngine (bridge/)
**Responsibilities**:
- Manages embedded QuickJS runtime lifecycle via JNI (`QuickJs` class)
- Loads single-file JS bundle from `assets/walletkit/quickjs/index.js`
- Implements polyfill backends as Kotlin classes with JNI bindings:
  - `NativeFetch`: OkHttp-backed fetch API
  - `NativeEventSource`: SSE client for TON API subscriptions
  - `NativeTimers`: setTimeout/setInterval via ScheduledThreadPoolExecutor
- Dedicated thread executor for JavaScript evaluation (thread safety)
- JSON serialization for all bridge communications
- Memory-efficient: ~2-4MB vs WebView's ~20-30MB

### 4. WalletKitViewModel (app/)
High-level state management for the demo UI:
- Wraps `WalletKitEngine` with ViewModel lifecycle
- Provides suspend functions: `importWallet`, `refreshAll`, `handleTonConnectUrl`
- Maintains `StateFlow<WalletKitState>` for Compose observation
- Handles TON Connect events (connectRequest, transactionRequest, signDataRequest)
- Manages approval/rejection flows with sheet UI state
- Auto-refreshes wallet balance periodically (15 seconds)

### 5. Compose UI (app/src/main/java/.../ui/)
**MainActivity** - Full-featured wallet demo:
- Auto-initializes engine on launch
- Imports hardcoded testnet mnemonic (for testing)
- Displays wallet address, balance, active sessions
- TON Connect URL input for testing
- Approval/rejection sheets for connect/transaction/sign requests
- Event log (last 5 events)

**PerformanceActivity** - Engine benchmarking:
- Side-by-side performance comparison (WebView vs QuickJS)
- Measures: engine creation, init, wallet operations, API calls
- Run 1, 3, or 5 iterations per engine
- Statistical summary (min/avg/max)
- CSV export and clipboard copy

## Bridge Contract

### Message Flow

| Direction | WebView Engine | QuickJS Engine | Payload Format |
|-----------|----------------|----------------|----------------|
| **Kotlin → JS** (RPC call) | `webView.evaluateJavascript("window.__walletkitCall('$id','$method',$params)")` | `quickJs.evaluate("__walletkitCall('$id','$method',$params)")` | `{id: "uuid", method: "methodName", params: {...}}` |
| **JS → Kotlin** (response) | `WalletKitNative.postMessage(JSON.stringify({kind:"response",id:"uuid",result:{...}}))` | JNI callback with JSON string | `{kind: "response", id: "uuid", result: {...}}` |
| **JS → Kotlin** (event) | `WalletKitNative.postMessage(JSON.stringify({kind:"event",event:{type:"...",data:{...}}}))` | JNI callback with JSON string | `{kind: "event", event: {type: "connectRequest", data: {...}}}` |
| **JS → Kotlin** (ready) | `WalletKitNative.postMessage(JSON.stringify({kind:"ready"}))` | JNI callback with JSON string | `{kind: "ready"}` |
| **JS → Kotlin** (error) | `WalletKitNative.postMessage(JSON.stringify({kind:"response",id:"uuid",error:{message:"..."}}))` | JNI callback with JSON string | `{kind: "response", id: "uuid", error: {message: "..."}}` |

### Key Differences from Old Implementation

- ❌ **OLD**: `AndroidBridge.postMessage` + `window.walletkit_request`
- ✅ **NEW**: `WalletKitNative.postMessage` + `window.__walletkitCall`
- Message envelopes include `kind` field (`"response"` | `"event"` | `"ready"`)
- All data is JSON-serialized; binary data uses hex/base64 encoding

## Usage Example

### Direct Engine Usage

```kotlin
// Create engine (choose implementation)
val engine: WalletKitEngine = QuickJsWalletKitEngine(context)
// or: WebViewWalletKitEngine(context)

// Initialize
val config = WalletKitBridgeConfig(
    network = "testnet",
    apiBaseUrl = "https://testnet.tonapi.io",
    tonApiKey = "YOUR_API_KEY"
)
engine.init(config)

// Import wallet
val mnemonic = listOf("word1", "word2", /* ... 24 words */)
engine.addWalletFromMnemonic(mnemonic, version = "v5r1", network = "testnet")

// Query wallets
val wallets = engine.getWallets()
val firstWallet = wallets.firstOrNull()

// Get balance
if (firstWallet != null) {
    val state = engine.getWalletState(firstWallet.address)
    println("Balance: ${state.balance} TON")
}

// Listen for events
val listener = WalletKitEngineListener { event ->
    when (event.type) {
        "connectRequest" -> handleConnectRequest(event.data)
        "transactionRequest" -> handleTransactionRequest(event.data)
        // ...
    }
}
val subscription = engine.addListener(listener)

// Clean up
engine.destroy()
subscription.close()
```

### ViewModel Pattern (Recommended for UI)

```kotlin
class WalletKitViewModel(
    private val engine: WalletKitEngine,
    private val storage: WalletKitStorage
) : ViewModel() {
    
    private val _state = MutableStateFlow(WalletKitState())
    val state: StateFlow<WalletKitState> = _state.asStateFlow()
    
    init {
        // Auto-initialize engine
        viewModelScope.launch {
            engine.init(WalletKitBridgeConfig(network = "testnet"))
            loadSavedWallet()
        }
        
        // Listen for events
        engine.addListener { event ->
            viewModelScope.launch {
                handleEvent(event)
            }
        }
    }
    
    suspend fun importWallet(mnemonic: List<String>) {
        engine.addWalletFromMnemonic(mnemonic, "v5r1", "testnet")
        storage.saveMnemonic(mnemonic)
        refreshWallets()
    }
    
    private suspend fun refreshWallets() {
        val wallets = engine.getWallets()
        _state.update { it.copy(wallets = wallets) }
    }
}

// In Activity/Fragment:
val viewModel: WalletKitViewModel by viewModels {
    val app = application as WalletKitDemoApp
    WalletKitViewModel.factory(app.obtainEngine(), app.storage)
}
```

## Building & Running

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17 or later
- Android SDK with API 24+ (minSdk 24, compileSdk 36)
- Android NDK 25+ (for QuickJS native compilation)
- Node.js 18+ and pnpm

### Build Steps

1. **Build JavaScript bundles** (from repository root `kit/`):
   ```bash
   pnpm -w --filter androidkit install
   pnpm -w --filter androidkit build:all
   ```
   This creates:
   - `dist-android/` - WebView bundle (HTML + modular JS)
   - `dist-android-quickjs/walletkit.quickjs.js` - QuickJS single-file bundle

2. **Build native bridge** (optional, for AAR distribution):
   ```bash
   cd AndroidDemo
   ./gradlew :bridge:assembleDebug :storage:assembleDebug
   ```

3. **Run the demo app**:
   - **Option A**: Open `AndroidDemo/` in Android Studio and run `app` configuration
   - **Option B**: Command line: `./gradlew :app:installDebug`

The app's Gradle `preBuild` task automatically:
- Runs `pnpm run --filter androidkit build:all` to build both bundles
- Copies bundles into `app/src/main/assets/walletkit/`

First build takes 2-5 minutes (downloads deps, builds native libraries for all ABIs).

### What You'll See

**MainActivity**:
- Auto-initializes QuickJS engine (or WebView, based on `WalletKitDemoApp.defaultEngineKind`)
- Imports hardcoded testnet mnemonic
- Displays wallet address, balance, and active TON Connect sessions
- Paste TON Connect URLs to test connection/transaction flows
- Approve/reject requests via Compose sheets

**PerformanceActivity**:
- Benchmark WebView vs QuickJS engines side-by-side
- Run 1, 3, or 5 iterations per engine
- View detailed timing metrics
- Export results as CSV

### Additional Tooling

```bash
# Run instrumentation tests for QuickJS engine (requires device/emulator)
./gradlew :bridge:connectedDebugAndroidTest

# Build release AARs for distribution
./gradlew :bridge:assembleRelease :storage:assembleRelease

# Build release APK
./gradlew :app:assembleRelease

# Manually sync bundles without full rebuild
pnpm -w --filter androidkit copy:demo
```

## Current Features

### ✅ Implemented
- [x] Dual-engine architecture (WebView + QuickJS)
- [x] Wallet import from mnemonic (v3r2, v4r2, v5r1)
- [x] Balance queries via TON API
- [x] TON Connect URL handling
- [x] Connection request approval/rejection
- [x] Transaction request approval/rejection
- [x] Sign data request approval/rejection
- [x] Session management (list, disconnect)
- [x] Event streaming (connectRequest, transactionRequest, signDataRequest, disconnect)
- [x] Performance benchmarking UI
- [x] Metrics collection and CSV export
- [x] Pluggable storage interface
- [x] Native polyfills for QuickJS (fetch, timers, EventSource)
- [x] Multi-ABI native libraries (arm64-v8a, armeabi-v7a, x86, x86_64)
- [x] Automatic bundle building and copying via Gradle

### 🚧 In Progress / Planned
- [ ] Secure storage (EncryptedSharedPreferences / Keystore integration)
- [ ] Durable session persistence (Room + WorkManager)
- [ ] Transaction history display
- [ ] Multi-wallet support
- [ ] Network switching (mainnet/testnet toggle)
- [ ] ProGuard/R8 consumer rules
- [ ] Comprehensive error handling and user feedback
- [ ] Unit and integration test coverage
- [ ] AAR publishing to Maven Central

## TON Connect Integration

The demo includes complete TON Connect flow implementation:

### Event Handling

```kotlin
engine.addListener { event ->
    when (event.type) {
        "connectRequest" -> {
            val requestId = event.data.get("id")
            val manifestUrl = event.data.getString("manifestUrl")
            val items = event.data.getJSONArray("items")
            
            // Show approval UI
            showConnectApprovalSheet(requestId, manifestUrl, items)
        }
        
        "transactionRequest" -> {
            val requestId = event.data.get("id")
            val messages = event.data.getJSONArray("messages")
            val validUntil = event.data.optLong("validUntil", 0)
            
            // Show transaction approval UI
            showTransactionApprovalSheet(requestId, messages, validUntil)
        }
        
        "signDataRequest" -> {
            val requestId = event.data.get("id")
            val payload = event.data.getString("payload")
            
            // Show sign data approval UI
            showSignDataApprovalSheet(requestId, payload)
        }
        
        "disconnect" -> {
            val sessionId = event.data.optString("sessionId")
            // Handle disconnection
            refreshSessions()
        }
    }
}
```

### Approval Actions

```kotlin
// Approve connection
suspend fun approveConnection(requestId: Any, walletAddress: String) {
    val result = engine.approveConnect(requestId, walletAddress)
    // Session established
}

// Reject connection
suspend fun rejectConnection(requestId: Any) {
    engine.rejectConnect(requestId, "User declined")
}

// Approve transaction
suspend fun approveTransaction(requestId: Any) {
    val result = engine.approveTransaction(requestId)
    // Transaction signed and sent
}

// Reject transaction
suspend fun rejectTransaction(requestId: Any) {
    engine.rejectTransaction(requestId, "User declined")
}
```

### Session Management

```kotlin
// List active sessions
val sessions = engine.listSessions()

// Disconnect specific session
engine.disconnectSession(sessionId = "session-id")

// Disconnect all sessions
engine.disconnectSession(sessionId = null)
```

## Security Considerations

### Current State (Demo)
- ⚠️ **Mnemonic storage**: Uses `DebugSharedPrefsStorage` (SharedPreferences) - **NOT secure for production**
- ⚠️ **Hardcoded mnemonic**: Demo auto-imports a test mnemonic - **remove before distribution**
- ✅ **Local-only WebView**: Loads only local assets, no remote code execution
- ✅ **HTTPS enforcement**: All API calls use HTTPS (TON API, SSE endpoints)

### Production Recommendations

1. **Secure Key Storage**:
   ```kotlin
   // Use EncryptedSharedPreferences
   class SecureWalletKitStorage(context: Context) : WalletKitStorage {
       private val masterKey = MasterKey.Builder(context)
           .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
           .build()
       
       private val prefs = EncryptedSharedPreferences.create(
           context,
           "walletkit_secure",
           masterKey,
           EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
           EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
       )
   }
   
   // Or use Android Keystore for hardware-backed security
   ```

2. **WebView Hardening**:
   - ✅ Already disabled: universal navigation, file access outside assets
   - ✅ Already enabled: strict CSP in index.html
   - Consider: Additional WebView security settings for production

3. **Network Security**:
   - Implement certificate pinning for TON API endpoints
   - Validate SSL certificates
   - Use `network_security_config.xml`

4. **Input Validation**:
   - Validate mnemonic words against BIP-39 word list
   - Sanitize TON Connect URLs before processing
   - Validate transaction parameters before approval

5. **ProGuard/R8**:
   - Obfuscate release builds
   - Keep essential bridge classes (see INTEGRATION.md for rules)
   - Test thoroughly after obfuscation

## Advantages of Native Wrapper Approach

1. **Native Performance**: Jetpack Compose UI with native rendering (no WebView UI overhead)
2. **Separation of Concerns**: JavaScript handles blockchain logic; Kotlin handles presentation/state
3. **Engine Flexibility**: Swap execution engines without changing UI code
4. **Memory Efficiency**: Reduced overhead vs full in-WebView UI (~50MB savings with QuickJS)
5. **Android Integration**: First-class support for Material Design, system theming, accessibility
6. **Offline Capability**: Core wallet operations work without network (with cached bundle)
7. **Testing**: Easy to mock engine implementations for UI tests

## Roadmap

### Phase 1: ✅ PoC Complete
- [x] Dual-engine architecture (WebView + QuickJS)
- [x] Basic wallet operations (import, balance, transactions)
- [x] TON Connect flow (connect, transaction, sign data requests)
- [x] Demo UI with approval sheets
- [x] Performance benchmarking

### Phase 2: 🚧 MVP (In Progress)
- [ ] Secure storage implementation (EncryptedSharedPreferences)
- [ ] Durable session persistence (Room database)
- [ ] Background sync (WorkManager for balance updates)
- [ ] Error handling and user feedback
- [ ] Multi-wallet support
- [ ] Network switching (mainnet/testnet)
- [ ] Transaction history display

### Phase 3: Production Ready
- [ ] AAR packaging and distribution (Maven Central)
- [ ] Comprehensive ProGuard/R8 rules
- [ ] API documentation (KDoc)
- [ ] Integration guides and examples
- [ ] Unit and instrumentation test suite (>80% coverage)
- [ ] Performance profiling and optimization
- [ ] Accessibility compliance
- [ ] Localization support

### Phase 4: Advanced Features
- [ ] NFT support and display
- [ ] Jetton (token) transfers
- [ ] DEX integration
- [ ] Staking operations
- [ ] Multi-signature wallet support
- [ ] Hardware wallet integration
- [ ] WalletConnect v2 support

## Related Documentation

- [Main README](../README.md) - Project overview and quick start
- [INTEGRATION.md](../INTEGRATION.md) - Detailed integration guide
- [Bridge C++ README](bridge/src/main/cpp/README.md) - QuickJS native implementation details

## License
Same license as the parent WalletKit project.
