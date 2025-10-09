# WalletKit Bridge Module

## Overview

The WalletKit Bridge is an Android SDK that enables integration of TON blockchain wallet functionality into Android applications. It executes the WalletKit JavaScript bundle in a WebView and exposes wallet APIs to Kotlin/Java code via a JSON-RPC bridge.

## Key Features

✅ **WebView-based Runtime**: Runs WalletKit JavaScript bundle in hidden WebView  
✅ **Persistent Storage**: Automatic encrypted storage for wallets and sessions  
✅ **TonConnect Support**: Full TonConnect protocol implementation  
✅ **Auto-initialization**: Automatic SDK initialization on first use  
✅ **Production Ready**: Hardware-backed encryption, comprehensive error handling  

## Quick Start

### 1. Add Dependency

```gradle
dependencies {
    implementation(project(":bridge"))
}
```

### 2. Initialize Engine

```kotlin
class MyApp : Application() {
    val walletKit: WalletKitEngine by lazy {
        WebViewWalletKitEngine(this)
    }
}
```

### 3. Use in Activity/ViewModel

```kotlin
class WalletViewModel(application: Application) : AndroidViewModel(application) {
    private val walletKit = (application as MyApp).walletKit
    
    suspend fun createWallet() {
        // Wallet is automatically persisted
        val result = walletKit.addWalletFromMnemonic(
            words = mnemonic,
            version = "v4R2",
            network = "testnet"
        )
    }
    
    suspend fun loadWallets() {
        // Wallets automatically restored from secure storage
        val wallets = walletKit.getWallets()
    }
}
```

That's it! **No manual storage management needed.**

## Persistent Storage

The bridge **automatically persists** wallet and session data using AES-256-GCM encrypted storage. Data is automatically restored on app restart.

**Key Features:**
- ✅ Zero configuration required
- ✅ Hardware-backed encryption (Android Keystore)
- ✅ Automatic persistence and restore
- ✅ Secure by default

**For detailed storage documentation, see:**
- 📖 [`README_STORAGE.md`](./README_STORAGE.md) - Complete storage guide
- 🔒 [`../storage/README_SECURITY.md`](../storage/README_SECURITY.md) - Security details

## API Reference

### Initialization

```kotlin
// Optional: Configure before use
suspend fun init(config: WalletKitBridgeConfig = WalletKitBridgeConfig()): JSONObject

// Auto-initialization: SDK initializes automatically on first use
// You can call any method directly without calling init()
```

### Wallet Management

```kotlin
// Add wallet from mnemonic (automatically persisted)
suspend fun addWalletFromMnemonic(
    words: List<String>,
    version: String,
    network: String? = null
): JSONObject

// Get all wallets (automatically restored from storage)
suspend fun getWallets(): List<WalletAccount>

// Remove wallet
suspend fun removeWallet(address: String): JSONObject

// Get wallet state (balance, etc.)
suspend fun getWalletState(address: String): WalletState

// Get recent transactions
suspend fun getRecentTransactions(address: String, limit: Int = 10): JSONArray
```

### TonConnect

```kotlin
// Handle TonConnect URL (returns request details)
suspend fun handleTonConnectUrl(url: String): JSONObject

// Approve connection request
suspend fun approveConnect(requestId: Any, walletAddress: String): JSONObject

// Reject connection request
suspend fun rejectConnect(requestId: Any, reason: String? = null): JSONObject

// Approve transaction
suspend fun approveTransaction(requestId: Any): JSONObject

// Reject transaction
suspend fun rejectTransaction(requestId: Any, reason: String? = null): JSONObject

// List active sessions
suspend fun listSessions(): List<WalletSession>

// Disconnect session
suspend fun disconnectSession(sessionId: String? = null): JSONObject
```

### Transactions

```kotlin
// Send transaction
suspend fun sendTransaction(
    walletAddress: String,
    recipient: String,
    amount: String,
    comment: String? = null
): JSONObject
```

### Events

```kotlin
// Listen for events (connection requests, transactions, etc.)
fun addListener(listener: WalletKitEngineListener): Closeable

interface WalletKitEngineListener {
    fun onEvent(event: WalletKitEvent)
}
```

## Configuration

```kotlin
data class WalletKitBridgeConfig(
    val network: String = "testnet",           // "mainnet" or "testnet"
    val apiUrl: String? = null,                // Custom API URL
    val bridgeUrl: String? = null,             // Custom bridge URL
    val bridgeName: String? = null,            // Custom bridge name
    val tonClientEndpoint: String? = null,     // Custom TON client endpoint
    val tonApiUrl: String? = null,             // Custom TON API URL
    val apiKey: String? = null,                // API key for TON API
)
```

**Note:** Storage is always persistent. No configuration needed.

## Architecture

```
┌─────────────────────────────────────────┐
│  Your App (Kotlin/Java)                 │
│  - Activities, ViewModels, etc.         │
└───────────────┬─────────────────────────┘
                │ WalletKitEngine interface
                ▼
┌─────────────────────────────────────────┐
│  WebViewWalletKitEngine                 │
│  - Hosts JavaScript bundle in WebView   │
│  - JSON-RPC bridge                      │
│  - Persistent storage (automatic)       │
└───────────────┬─────────────────────────┘
                │ JavascriptInterface
                ▼
┌─────────────────────────────────────────┐
│  WalletKit JavaScript Bundle            │
│  - Wallet management logic              │
│  - TonConnect protocol                  │
│  - Transaction building                 │
└───────────────┬─────────────────────────┘
                │ Storage API
                ▼
┌─────────────────────────────────────────┐
│  SecureBridgeStorageAdapter             │
│  - EncryptedSharedPreferences           │
│  - Android Keystore encryption          │
│  - AES-256-GCM                          │
└─────────────────────────────────────────┘
```

## Building the JavaScript Bundle

The bridge requires the WalletKit JavaScript bundle to be built and copied to assets:

```bash
# From repository root
pnpm run --filter androidkit build:all
```

This is automatically done by Gradle when building the bridge module.

## Testing

### Unit Tests

```kotlin
@Test
fun `wallets persist across engine recreations`() = runTest {
    val engine1 = WebViewWalletKitEngine(context)
    engine1.addWalletFromMnemonic(testMnemonic, "v4R2")
    
    engine1.destroy()
    
    val engine2 = WebViewWalletKitEngine(context)
    val wallets = engine2.getWallets()
    
    assertEquals(1, wallets.size)
}
```

### Integration Tests

See `README_STORAGE.md` for testing examples.

## Security

### Encryption

- **Algorithm**: AES-256-GCM
- **Key Storage**: Android Keystore (hardware-backed)
- **Key Size**: 256 bits
- **IV Size**: 12 bytes (GCM standard)
- **Auth Tag**: 16 bytes (128 bits)

### Best Practices

1. **Enable ProGuard in release builds**
   ```gradle
   buildTypes {
       release {
           minifyEnabled true
       }
   }
   ```

2. **Don't log sensitive data**
   ```kotlin
   // ❌ BAD
   Log.d("Wallet", "Mnemonic: $mnemonic")
   
   // ✅ GOOD
   Log.d("Wallet", "Wallet created: $address")
   ```

3. **Handle errors gracefully**
   ```kotlin
   try {
       walletKit.addWalletFromMnemonic(mnemonic, version)
   } catch (e: Exception) {
       showError("Failed to create wallet")
   }
   ```

## Migration from Demo App Storage

If you were manually managing wallet persistence, you can now remove that code:

**Before:**
```kotlin
// Manual wallet restoration on every bootstrap
val wallets = storage.loadAllWallets()
wallets.forEach { (_, record) ->
    walletKit.addWalletFromMnemonic(record.mnemonic, record.version)
}
```

**After:**
```kotlin
// Bridge handles persistence automatically
walletKit.init() // Wallets automatically restored
```

See `DEMO_APP_MIGRATION_GUIDE.md` for detailed migration instructions.

## FAQ

**Q: Do I need to configure storage?**  
A: No, storage is automatic and always enabled.

**Q: Where is data stored?**  
A: In EncryptedSharedPreferences at `/data/data/your.package/shared_prefs/walletkit_bridge_storage.xml` (encrypted).

**Q: What happens on app uninstall?**  
A: All data is deleted. Users should export mnemonics before uninstalling.

**Q: Is it secure on rooted devices?**  
A: Encryption is enabled but keys may be extractable. Warn users about rooted device risks.

**Q: Can I disable persistence?**  
A: No, the bridge requires persistent storage for proper functionality.

## Troubleshooting

### "Failed to load WalletKit bundle"

**Cause:** JavaScript bundle not built or not in assets

**Fix:** Run `pnpm run --filter androidkit build:all`

### "Wallets not restored after app restart"

**Cause:** JavaScript bundle not using Android storage adapter

**Fix:** Ensure JavaScript bundle is updated with `AndroidStorageAdapter`

### "Storage encryption failed"

**Cause:** Android Keystore initialization failed

**Fix:** 
- Enable device lock screen
- Check Android version (6.0+ required)
- Verify storage permissions

## Documentation

- 📖 [`README_STORAGE.md`](./README_STORAGE.md) - Storage guide
- 🔒 [`../storage/README_SECURITY.md`](../storage/README_SECURITY.md) - Security details
- 📝 `DEMO_APP_MIGRATION_GUIDE.md` - Migration from demo storage
- 🎯 `BRIDGE_STORAGE_SENIOR_SUMMARY.md` - Implementation details

## License

Part of the TON Connect WalletKit - see main project license.

## Support

- GitHub Issues: [ton-connect/kit](https://github.com/ton-connect/kit/issues)
- Documentation: See links above
- Security Issues: Report privately

---

**Version:** 1.0.0  
**Status:** Production Ready ✅  
**Min Android:** API 24 (Android 7.0)  
**Target Android:** API 36
