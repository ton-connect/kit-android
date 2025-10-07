# Android WalletKit Integration Guide

> **⚠️ QUICKJS DEPRECATION NOTICE**: As of October 2025, the QuickJS engine is **deprecated** due to performance issues (2x slower than WebView). **Use WebView for all production applications.** QuickJS documentation is preserved for reference only.

This guide explains how to integrate WalletKit Android into your application. The SDK provides WebView as the primary runtime, with a deprecated QuickJS option preserved for potential future optimization experiments.

## Architecture Overview

WalletKit Android consists of three main components:

1. **JavaScript Bundles** (`src-js/`): TypeScript/JavaScript code built with Vite
   - WebView bundle: Modular HTML + JS for browser environment ✅ **RECOMMENDED**
   - QuickJS bundle: Single-file JS ⚠️ **DEPRECATED** (preserved for reference)
2. **Native Bridge** (`bridge/`): Kotlin + C++ module packaged as AAR
   - `WalletKitEngine` interface with WebView implementation ✅ **ACTIVELY MAINTAINED**
   - QuickJS implementation ⚠️ **DEPRECATED** (no longer maintained)
   - Native QuickJS runtime (quickjs-ng v0.10.1) ⚠️ **DEPRECATED**
   - Polyfill backends for fetch, timers, EventSource
3. **Storage** (`storage/`): Pluggable persistence layer
   - Interface for mnemonic/key management
   - Demo implementations (in-memory, SharedPreferences)

## 1. Integration Steps

### Prerequisites
- Node.js 18+ and pnpm (only needed if building from source)
- Android Studio Hedgehog or later
- Android SDK with API 24+ (minSdk 24, targetSdk 36)
- NDK 25+ (automatically installed by Android Studio)

### Option A: Use Pre-built AAR (Recommended for Partners)

1. **Add AAR dependencies** to your app's `build.gradle.kts`:
   ```kotlin
   dependencies {
       // WalletKit SDK modules
       implementation(files("libs/bridge-release.aar"))
       implementation(files("libs/storage-release.aar"))
       
       // Required transitive dependencies
       implementation("androidx.core:core-ktx:1.12.0")
       implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
       implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
       implementation("androidx.webkit:webkit:1.8.0")
       implementation("com.squareup.okhttp3:okhttp:4.12.0")
   }
   ```

2. **Sync Gradle** and you're ready to use WalletKit!

**That's it!** The JavaScript bundles are automatically included in the `bridge.aar` assets, so no manual copying is needed.

### Option B: Build from Source (For Development)

If you want to modify the SDK or build it yourself:

#### 1. Build JavaScript Bundles

From the repository root:

```bash
# Install dependencies
pnpm -w --filter androidkit install

# Build both bundles
pnpm -w --filter androidkit build:all
```

**Output**:
- WebView: `apps/androidkit/dist-android/` (index.html, assets/*.js, manifest.json) ✅
- QuickJS: `apps/androidkit/dist-android-quickjs/walletkit.quickjs.js` ⚠️ **DEPRECATED**

#### 2. Build Bridge AAR

The bridge module automatically builds and includes the JavaScript bundles:

```bash
cd apps/androidkit/AndroidDemo

# Debug AAR (for development)
./gradlew :bridge:assembleDebug :storage:assembleDebug

# Release AAR (for distribution)
./gradlew :bridge:assembleRelease :storage:assembleRelease
```

**What happens automatically**:
1. `preBuild` task triggers bundle build via `pnpm run --filter androidkit build:all`
2. Bundles are copied into `bridge/src/main/assets/walletkit/`
3. Assets are packaged inside the AAR during assembly

**Output**:
- `bridge/build/outputs/aar/bridge-debug.aar` (or `bridge-release.aar`)
- `storage/build/outputs/aar/storage-debug.aar` (or `storage-release.aar`)

The bridge AAR includes:
- ✅ JavaScript bundles (WebView HTML/JS + QuickJS single-file for compatibility)
- ✅ WebView engine Kotlin code **[ACTIVELY MAINTAINED]**
- ⚠️ QuickJS engine Kotlin code **[DEPRECATED - preserved for reference]**
- ⚠️ Native libraries (`libwalletkitquickjs.so`) **[DEPRECATED - no longer maintained]**

## 2. Engine Selection & Initialization

### Recommended: WebView Engine Only

**For all production applications, use WebView:**

### Recommended: WebView Engine Only

**For all production applications, use WebView:**

```kotlin
import io.ton.walletkit.bridge.*
import io.ton.walletkit.bridge.config.WalletKitBridgeConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class YourApplication : Application() {
    lateinit var walletEngine: WalletKitEngine
    
    override fun onCreate() {
        super.onCreate()
        
        // Use WebView - the only recommended engine
        walletEngine = WebViewWalletKitEngine(this)
        
        // Initialize in background
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val config = WalletKitBridgeConfig(
                    network = "testnet", // or "mainnet"
                    apiBaseUrl = "https://testnet.tonapi.io",
                    tonApiKey = "YOUR_API_KEY" // optional
                )
                walletEngine.init(config)
                Log.i("WalletKit", "Engine initialized successfully")
            } catch (e: Exception) {
                Log.e("WalletKit", "Initialization failed", e)
            }
        }
    }
    
    override fun onTerminate() {
        super.onTerminate()
        // Clean up engine resources
        CoroutineScope(Dispatchers.IO).launch {
            walletEngine.destroy()
        }
    }
}
```

---

## DEPRECATED: Engine Comparison Reference

> **⚠️ The following sections document the deprecated QuickJS engine. Do not use QuickJS in production. This information is preserved for historical reference and potential future optimization experiments only.**

### Choosing an Engine (DEPRECATED DECISION - USE WEBVIEW)

#### WebView Engine (`WebViewWalletKitEngine`) — ✅ **RECOMMENDED**

**Use WebView when:**
- **Performance is critical**: 2x faster cold start (917ms vs 1881ms average)
  - Significantly faster Init (248ms vs 490ms) and Add Wallet (89ms vs 775ms)
  - Better for user-facing wallet operations
- **APK size matters**: Only ~0.04 MiB overhead (WebView is system-provided)
- **Simpler is better**: No native code compilation, easier maintenance
- **Full browser APIs needed**: Complete Web API support, easier third-party JS library integration
- **Debugging is critical**: Chrome DevTools remote debugging (chrome://inspect)
- **Future flexibility**: If you plan to potentially rewrite core in another language

**Avoid WebView when:**
- Memory constraints are critical (uses ~20-30MB vs QuickJS 2-4MB)
- Background/headless execution is required (restricted in these contexts)
- You need to avoid device fragmentation (OEM WebView implementations vary)
- Security policies discourage WebView (some enterprises flag dynamic code loading)

**Polyfills needed**: 6 (TextEncoder, Buffer, URL, URLSearchParams, AbortController, fetch fallback)

#### QuickJS Engine (`QuickJsWalletKitEngine`) — ⚠️ **DEPRECATED - DO NOT USE**

> **DEPRECATED**: This engine is 2x slower than WebView (1881ms vs 917ms cold start) and is no longer maintained. Code preserved for potential future optimization experiments only. **Do not use in production under any circumstances.**

**Use QuickJS when:**
- **Memory is critical**: ~2-4MB footprint vs WebView's ~20-30MB (5-10x smaller)
- **Background execution**: Works in headless/background processes without restrictions
- **Native integration**: Can add native helpers (crypto, storage) via JNI without JS polyfills
- **Sandboxing**: Complete control over execution environment, offline capability
- **WebView independence**: No dependency on system WebView version/quality

**Avoid QuickJS when:**
- **Performance is critical**: 2x slower cold start (1881ms vs 917ms)
  - Much slower crypto operations (Init: 490ms vs 248ms, Add Wallet: 775ms vs 89ms)
- APK size is critical (+3 MiB compressed, ~7 MiB uncompressed installed)
- You want to avoid NDK maintenance and C++ compilation
- You need to minimize native crash risk (C++ bugs harder to debug)
- You want to avoid Play Store native code requirements:
  - Extra security audits, Play Integrity declarations
  - Mandatory 64-bit support (arm64-v8a, x86_64) - cannot strip
- Full Web API compatibility without polyfills is required
- You may rewrite core in another language in the future (harder to migrate)

**Polyfills needed**: 15+ (everything WebView needs PLUS console, timers, crypto, storage, HTTP, EventSource, etc.)

**Play Store Requirements for QuickJS**:
- ⚠️ Must include 64-bit ABIs (arm64-v8a, x86_64) - required since August 2019
- ⚠️ May trigger "Native code" flag in Play Console requiring additional declarations
- ⚠️ Partners cannot strip 64-bit ABIs; Play will reject the APK/AAB

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
| Memory Footprint | ~20-30MB | ~2-4MB | **QuickJS (5-10x smaller)** |
| APK Size Impact | ~0.04MB | ~3MB compressed, ~7MB installed | **WebView (smaller)** |

### Recommendation Summary

**Use WebView for all applications. QuickJS is deprecated.**

WebView is the only supported engine:
- ✅ 2x faster cold start (917ms vs 1881ms)
- ✅ Better wallet operation performance
- ✅ Simpler architecture, smaller APK
- ✅ Actively maintained and supported
- ✅ Chrome DevTools debugging

**Do not use QuickJS.** Even for memory-constrained scenarios, the 2x performance penalty is unacceptable. Modern Android devices handle WebView's memory footprint without issue.

---

### Basic Initialization (DEPRECATED - SEE ABOVE FOR CURRENT GUIDANCE)

> **⚠️ The example below shows QuickJS which is deprecated. Use `WebViewWalletKitEngine` instead as shown in the recommended section above.**

```kotlin
import io.ton.walletkit.bridge.*
import io.ton.walletkit.bridge.config.WalletKitBridgeConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class YourApplication : Application() {
    lateinit var walletEngine: WalletKitEngine
    
    override fun onCreate() {
        super.onCreate()
        
        // Choose your engine
        walletEngine = QuickJsWalletKitEngine(this) // or WebViewWalletKitEngine(this)
        
        // Initialize in background
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val config = WalletKitBridgeConfig(
                    network = "testnet", // or "mainnet"
                    apiBaseUrl = "https://testnet.tonapi.io",
                    tonApiKey = "YOUR_API_KEY" // optional
                )
                walletEngine.init(config)
                Log.i("WalletKit", "Engine initialized successfully")
            } catch (e: Exception) {
                Log.e("WalletKit", "Initialization failed", e)
            }
        }
    }
    
    override fun onTerminate() {
        super.onTerminate()
        // Clean up engine resources
        CoroutineScope(Dispatchers.IO).launch {
            walletEngine.destroy()
        }
    }
}
```

### Advanced Configuration

```kotlin
// Custom asset paths
val webViewEngine = WebViewWalletKitEngine(
    context = context,
    assetPath = "custom/path/to/index.html"
)

val quickJsEngine = QuickJsWalletKitEngine(
    context = context,
    assetPath = "custom/path/to/bundle.js",
    httpClient = customOkHttpClient // optional: custom HTTP client
)

// Listen for bridge events
val listener = WalletKitEngineListener { event ->
    when (event.type) {
        "connectRequest" -> handleConnectRequest(event.data)
        "transactionRequest" -> handleTransactionRequest(event.data)
        "signDataRequest" -> handleSignDataRequest(event.data)
        "disconnect" -> handleDisconnect(event.data)
    }
}

val subscription = walletEngine.addListener(listener)

// Later: unsubscribe
subscription.close()
```

## 3. Core API Usage

### Wallet Management

```kotlin
import io.ton.walletkit.bridge.model.WalletAccount

// Import wallet from mnemonic
suspend fun importWallet(mnemonic: List<String>) {
    val result = walletEngine.addWalletFromMnemonic(
        words = mnemonic,
        version = "v5r1", // or "v4r2", "v3r2"
        network = "testnet" // or "mainnet"
    )
    Log.i("WalletKit", "Wallet imported: $result")
}

// Get all wallets
suspend fun getWallets(): List<WalletAccount> {
    return walletEngine.getWallets()
}

// Get wallet state (balance, transactions, etc.)
suspend fun getBalance(address: String) {
    val state = walletEngine.getWalletState(address)
    Log.i("WalletKit", "Balance: ${state.balance} TON")
    Log.i("WalletKit", "Transactions: ${state.transactions.size}")
}
```

### TON Connect Integration

```kotlin
import io.ton.walletkit.bridge.model.WalletKitEvent
import org.json.JSONObject

// Handle TON Connect deep link
suspend fun handleDeepLink(url: String) {
    try {
        val result = walletEngine.handleTonConnectUrl(url)
        Log.i("WalletKit", "TON Connect URL processed: $result")
    } catch (e: Exception) {
        Log.e("WalletKit", "Failed to handle URL", e)
    }
}

// Listen for connection requests
walletEngine.addListener { event ->
    if (event.type == "connectRequest") {
        val requestId = event.data.get("id")
        val manifestUrl = event.data.getString("manifestUrl")
        val items = event.data.getJSONArray("items")
        
        // Show UI to user, then approve or reject
        showConnectApprovalDialog(requestId, manifestUrl) { approved, walletAddress ->
            lifecycleScope.launch {
                if (approved) {
                    walletEngine.approveConnect(requestId, walletAddress)
                } else {
                    walletEngine.rejectConnect(requestId, "User declined")
                }
            }
        }
    }
}

// Approve connection request
suspend fun approveConnection(requestId: Any, walletAddress: String) {
    val result = walletEngine.approveConnect(requestId, walletAddress)
    Log.i("WalletKit", "Connection approved: $result")
}

// Reject connection request
suspend fun rejectConnection(requestId: Any, reason: String = "User declined") {
    walletEngine.rejectConnect(requestId, reason)
}
```

### Transaction Signing

```kotlin
// Listen for transaction requests
walletEngine.addListener { event ->
    if (event.type == "transactionRequest") {
        val requestId = event.data.get("id")
        val messages = event.data.getJSONArray("messages")
        val validUntil = event.data.optLong("validUntil", 0)
        
        // Parse and show transaction details to user
        showTransactionApprovalDialog(requestId, messages) { approved ->
            lifecycleScope.launch {
                if (approved) {
                    walletEngine.approveTransaction(requestId)
                } else {
                    walletEngine.rejectTransaction(requestId, "User declined")
                }
            }
        }
    }
}

// Approve transaction
suspend fun approveTransaction(requestId: Any) {
    val result = walletEngine.approveTransaction(requestId)
    Log.i("WalletKit", "Transaction approved: $result")
}

// Reject transaction
suspend fun rejectTransaction(requestId: Any) {
    walletEngine.rejectTransaction(requestId, "User declined")
}
```

### Session Management

```kotlin
import io.ton.walletkit.bridge.model.WalletSession

// List active sessions
suspend fun getActiveSessions(): List<WalletSession> {
    return walletEngine.listSessions()
}

// Disconnect specific session
suspend fun disconnect(sessionId: String) {
    walletEngine.disconnectSession(sessionId)
}

// Disconnect all sessions
suspend fun disconnectAll() {
    walletEngine.disconnectSession(null)
}

// Listen for disconnect events
walletEngine.addListener { event ->
    if (event.type == "disconnect") {
        val sessionId = event.data.optString("sessionId")
        Log.i("WalletKit", "Session disconnected: $sessionId")
        // Update UI
    }
}
```

## 4. Storage Integration

### Interface

```kotlin
interface WalletKitStorage {
    suspend fun saveMnemonic(mnemonic: List<String>)
    suspend fun loadMnemonic(): List<String>?
    suspend fun clearMnemonic()
}
```

### Demo Implementations

**In-Memory Storage** (for testing):
```kotlin
class InMemoryWalletKitStorage : WalletKitStorage {
    private var mnemonic: List<String>? = null
    
    override suspend fun saveMnemonic(mnemonic: List<String>) {
        this.mnemonic = mnemonic
    }
    
    override suspend fun loadMnemonic(): List<String>? = mnemonic
    
    override suspend fun clearMnemonic() {
        mnemonic = null
    }
}
```

**SharedPreferences Storage** (demo only, not secure):
```kotlin
class DebugSharedPrefsStorage(context: Context) : WalletKitStorage {
    private val prefs = context.getSharedPreferences("walletkit", Context.MODE_PRIVATE)
    
    override suspend fun saveMnemonic(mnemonic: List<String>) {
        prefs.edit().putString("mnemonic", mnemonic.joinToString(" ")).apply()
    }
    
    override suspend fun loadMnemonic(): List<String>? {
        return prefs.getString("mnemonic", null)?.split(" ")
    }
    
    override suspend fun clearMnemonic() {
        prefs.edit().remove("mnemonic").apply()
    }
}
```

### Production Implementation

**EncryptedSharedPreferences** (recommended):
```kotlin
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

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
    
    override suspend fun saveMnemonic(mnemonic: List<String>) {
        prefs.edit().putString("mnemonic", mnemonic.joinToString(" ")).apply()
    }
    
    override suspend fun loadMnemonic(): List<String>? {
        return prefs.getString("mnemonic", null)?.split(" ")
    }
    
    override suspend fun clearMnemonic() {
        prefs.edit().remove("mnemonic").apply()
    }
}
```

**Android Keystore** (most secure, for signing keys):
```kotlin
// For production apps, consider storing only a reference to the mnemonic
// and performing all signing operations through secure hardware-backed keys
// See Android Keystore documentation for implementation details
```

### Usage

```kotlin
class YourApplication : Application() {
    val storage: WalletKitStorage by lazy {
        SecureWalletKitStorage(this) // or DebugSharedPrefsStorage for testing
    }
    
    suspend fun restoreWallet() {
        val mnemonic = storage.loadMnemonic()
        if (mnemonic != null) {
            walletEngine.addWalletFromMnemonic(mnemonic, "v5r1", "mainnet")
        }
    }
}
```

## 5. Error Handling

### Exception Types

```kotlin
import io.ton.walletkit.bridge.WalletKitBridgeException

try {
    val wallets = walletEngine.getWallets()
} catch (e: WalletKitBridgeException) {
    // JavaScript bridge error with structured information
    Log.e("WalletKit", "Bridge error: ${e.message}")
} catch (e: Exception) {
    // Other errors (network, JSON parsing, etc.)
    Log.e("WalletKit", "Unexpected error", e)
}
```

### Common Error Scenarios

| Error | Cause | Solution |
|-------|-------|----------|
| "WalletKit not initialized" | Called methods before `init()` completes | Await `init()` before making calls |
| "Unknown method" | Method name mismatch | Check method names match `WalletKitEngine` interface |
| "Asset not found" | Bundle missing from assets | Run build and copy tasks |
| "Network error" | API endpoint unreachable | Check network connectivity and API key |
| "Invalid mnemonic" | Malformed seed phrase | Validate mnemonic before importing |
| "Session not found" | Invalid session ID | Verify session exists via `listSessions()` |

### Debugging

**WebView Engine**:
```kotlin
// Enable remote debugging (already enabled in bridge)
// Chrome: chrome://inspect/#devices
```

**QuickJS Engine**:
```kotlin
// Add detailed logging
walletEngine.addListener { event ->
    Log.d("WalletKit", "Event: ${event.type}, Data: ${event.data}")
}

// Check logs for JavaScript errors
adb logcat | grep "WalletKitQuickJs"
```

## 6. ProGuard/R8 Configuration

When building a release AAR or app, add these ProGuard rules:

```proguard
# WalletKit Bridge - Keep public API
-keep class io.ton.walletkit.bridge.** { *; }
-keep class io.ton.walletkit.storage.** { *; }

# QuickJS JNI
-keep class io.ton.walletkit.quickjs.QuickJs { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}

# WebView JavaScript Interface
-keepclassmembers class io.ton.walletkit.bridge.WebViewWalletKitEngine$JsBinding {
    @android.webkit.JavascriptInterface <methods>;
}

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# OkHttp (if not already included)
-dontwarn okhttp3.**
-dontwarn okio.**

# Data classes (preserve for JSON serialization)
-keep class io.ton.walletkit.bridge.model.** { *; }

# Reflection (for QuickJS host bindings)
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
```

Add to `bridge/proguard-rules.pro` and `app/proguard-rules.pro`.
