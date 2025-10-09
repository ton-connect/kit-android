# WalletKit Storage Security Implementation

## Overview

This module provides **production-ready secure storage** for the Android WalletKit bridge. It uses industry best practices to protect sensitive wallet data including mnemonic phrases, session keys, and user metadata.

## 🔐 Security Architecture

### Multi-Layer Encryption

1. **Android Keystore Layer**
   - Hardware-backed key storage (StrongBox on supported devices)
   - Keys never leave the secure hardware
   - Automatic key rotation and lifecycle management

2. **EncryptedSharedPreferences Layer**
   - AES-256-GCM encryption for all data at rest
   - Automatic key derivation from MasterKey
   - Protection against unauthorized access

3. **Additional Mnemonic Protection**
   - Double encryption for mnemonic phrases
   - Separate encryption key for maximum security
   - Secure memory clearing after use

### Security Features

- ✅ **AES-256-GCM Encryption**: Industry-standard authenticated encryption
- ✅ **Hardware-Backed Keys**: Keys stored in Android Keystore (hardware when available)
- ✅ **StrongBox Support**: Enhanced security on supported devices (Pixel 3+, Samsung S9+, etc.)
- ✅ **Secure Memory Clearing**: Sensitive data zeroed after use
- ✅ **ProGuard Protection**: Code obfuscation and log removal in release builds
- ✅ **URL Validation**: Protection against injection attacks
- ✅ **Data Sanitization**: Input validation and length limits
- ✅ **Security Validation**: Device security checks and warnings

## 🚀 Usage

### Basic Usage

```kotlin
// In your Application class or dependency injection setup
class MyApp : Application() {
    val storage: WalletKitStorage by lazy {
        SecureWalletKitStorage(this)
    }
}
```

### Saving a Wallet

```kotlin
val mnemonic = listOf("word1", "word2", ..., "word24")
val record = StoredWalletRecord(
    mnemonic = mnemonic,
    name = "My Wallet",
    network = "mainnet",
    version = "v4r2"
)

storage.saveWallet(accountId = "EQD...", record = record)
```

### Loading a Wallet

```kotlin
val wallet = storage.loadWallet(accountId = "EQD...")
if (wallet != null) {
    println("Wallet: ${wallet.name}")
    // Use wallet.mnemonic securely
}
```

### Security Validation

```kotlin
val status = SecurityValidator.checkDeviceSecurity(context)
if (!status.isProductionReady) {
    // Show warning to user
    Log.w("Security", status.warnings.joinToString("\n"))
}
```

## 🔄 Migration from DebugSharedPrefsStorage

If you were using `DebugSharedPrefsStorage` (which is **NOT SECURE**), use the migration utility:

```kotlin
val migrator = StorageMigrator(context)
val migratedCount = migrator.migrateFromDebugToSecure(deleteOldData = true)
Log.d("Migration", "Migrated $migratedCount wallets to secure storage")
```

## 📋 Implementation Classes

### Core Classes

| Class | Purpose |
|-------|---------|
| `SecureWalletKitStorage` | Main secure storage implementation |
| `CryptoManager` | Android Keystore encryption manager |
| `SecurityValidator` | Device security validation |
| `StorageMigrator` | Migration from insecure storage |

### Storage Implementations

| Implementation | Security Level | Use Case |
|----------------|----------------|----------|
| `SecureWalletKitStorage` | **HIGH** ✅ | **Production use** |
| `DebugSharedPrefsStorage` | **NONE** ⚠️ | Debug/testing only |
| `InMemoryWalletKitStorage` | **NONE** ⚠️ | Testing/temporary |

## 🔒 What's Protected

### Critical Data (Double Encrypted)
- ✅ Mnemonic phrases (24/12 words)
- ✅ Private keys (if stored)

### High Priority Data (Encrypted)
- ✅ Wallet metadata (name, network, version)
- ✅ Session hints (dApp URLs, icons)
- ✅ API keys (if stored in config)

### Metadata (Encrypted for Consistency)
- ✅ Wallet addresses
- ✅ Public keys
- ✅ Session identifiers

## 🛡️ Security Best Practices

### For App Developers

1. **Always use SecureWalletKitStorage in production**
   ```kotlin
   // ✅ GOOD - Production
   val storage = SecureWalletKitStorage(context)
   
   // ❌ BAD - Never in production!
   val storage = DebugSharedPrefsStorage(context)
   ```

2. **Check device security before storing wallets**
   ```kotlin
   val status = SecurityValidator.checkDeviceSecurity(context)
   if (!status.hasSecureLockScreen) {
       // Prompt user to enable lock screen
   }
   ```

3. **Clear sensitive data from memory**
   ```kotlin
   val mnemonic = wallet.mnemonic
   // Use mnemonic...
   CryptoManager.secureClear(mnemonic.joinToString(" "))
   ```

4. **Enable ProGuard/R8 in release builds**
   ```gradle
   buildTypes {
       release {
           minifyEnabled true
           proguardFiles getDefaultProguardFile('proguard-android-optimize.txt')
       }
   }
   ```

### For Library Users

1. **Never log sensitive data**
   ```kotlin
   // ❌ BAD
   Log.d("Wallet", "Mnemonic: ${wallet.mnemonic}")
   
   // ✅ GOOD
   Log.d("Wallet", "Loaded wallet: ${wallet.name}")
   ```

2. **Handle encryption errors gracefully**
   ```kotlin
   try {
       storage.saveWallet(id, record)
   } catch (e: SecurityException) {
       // Handle security error - don't crash the app
       Log.e("Storage", "Failed to save wallet securely", e)
   }
   ```

3. **Validate device security**
   ```kotlin
   if (SecurityValidator.isDeviceRooted()) {
       // Warn user about security risks
   }
   ```

## 🔧 Configuration

### Custom Keystore Alias

```kotlin
val cryptoManager = CryptoManager(keystoreAlias = "my_custom_key")
```

### Custom SharedPreferences Name

```kotlin
val storage = SecureWalletKitStorage(
    context = context,
    sharedPrefsName = "my_custom_storage"
)
```

## 📊 Security Validation Results

The `SecurityValidator` provides comprehensive device security checks:

```kotlin
val status = SecurityValidator.checkDeviceSecurity(context)

// Check individual aspects
if (status.hasStrongBoxKeystore) {
    Log.d("Security", "Device has StrongBox - maximum security")
}

if (!status.hasSecureLockScreen) {
    // Show warning to user
    Toast.makeText(context, 
        "Please enable a lock screen for better security", 
        Toast.LENGTH_LONG).show()
}

// Get security recommendation
val message = SecurityValidator.getSecurityRecommendation(context)
```

## 🧪 Testing

### Unit Tests

```kotlin
@Test
fun testEncryptDecrypt() {
    val crypto = CryptoManager("test_key")
    val plaintext = "sensitive data"
    
    val encrypted = crypto.encrypt(plaintext)
    val decrypted = crypto.decrypt(encrypted)
    
    assertEquals(plaintext, decrypted)
}
```

### Integration Tests

```kotlin
@Test
fun testWalletStorage() = runTest {
    val storage = SecureWalletKitStorage(context)
    val mnemonic = List(24) { "word$it" }
    val record = StoredWalletRecord(mnemonic, "Test", "testnet", "v4")
    
    storage.saveWallet("test_id", record)
    val loaded = storage.loadWallet("test_id")
    
    assertEquals(mnemonic, loaded?.mnemonic)
}
```

## ⚠️ Important Security Notes

### DO ✅
- Use `SecureWalletKitStorage` in production
- Enable ProGuard/R8 in release builds
- Check device security before storing wallets
- Clear sensitive data from memory after use
- Validate user input (wallet names, URLs)
- Keep the library updated

### DON'T ❌
- Use `DebugSharedPrefsStorage` in production
- Log mnemonic phrases or private keys
- Store wallets on rooted devices without warning
- Disable ProGuard in release builds
- Bypass security validation
- Store unencrypted backups

## 🔐 Encryption Details

### Algorithm: AES-256-GCM

- **Key Size**: 256 bits
- **Block Mode**: GCM (Galois/Counter Mode)
- **IV Size**: 12 bytes (96 bits) - recommended for GCM
- **Tag Size**: 16 bytes (128 bits) - authentication tag
- **Key Storage**: Android Keystore (hardware-backed)

### Data Format

Encrypted data structure:
```
[IV (12 bytes)] [Ciphertext + Auth Tag (variable)]
```

### Key Hierarchy

```
Device Master Key (Hardware)
    └── MasterKey (EncryptedSharedPreferences)
        ├── Wallet Encryption Key (Android Keystore)
        │   └── Encrypted Mnemonics
        └── Metadata Encryption (Automatic)
            ├── Wallet Metadata
            └── Session Hints
```

## 📱 Device Support

### Minimum Requirements
- Android 7.0 (API 24) - Minimum SDK
- Secure lock screen recommended
- Hardware keystore recommended

### Optimal Security
- Android 9.0+ (API 28) - StrongBox support
- Device with StrongBox Keystore
- Secure lock screen enabled

### Supported Devices with StrongBox
- Google Pixel 3 and newer
- Samsung Galaxy S9 and newer
- OnePlus 6T and newer
- Many other modern flagship devices

## 🐛 Troubleshooting

### "Failed to initialize secure storage"

**Cause**: Android Keystore or EncryptedSharedPreferences initialization failed

**Solutions**:
1. Ensure device has Android 6.0+
2. Check if device storage is available
3. Verify app has storage permissions
4. Check logcat for detailed error

### "Failed to decrypt data"

**Cause**: Data corruption or key loss

**Solutions**:
1. Data may have been corrupted
2. App was reinstalled (keys are lost)
3. Device was factory reset
4. Migration from another device failed

**Prevention**:
- Implement backup/recovery mechanism
- Warn users about data loss on app uninstall
- Consider cloud backup with user-controlled encryption

### "Device not recommended for production"

**Cause**: Device lacks security features

**Solutions**:
1. Enable secure lock screen
2. Use a device with hardware keystore
3. Avoid rooted devices for production wallets
4. Consider hardware wallet integration

## 📚 References

- [Android Keystore System](https://developer.android.com/training/articles/keystore)
- [EncryptedSharedPreferences](https://developer.android.com/reference/androidx/security/crypto/EncryptedSharedPreferences)
- [Security Best Practices](https://developer.android.com/topic/security/best-practices)
- [StrongBox Keystore](https://developer.android.com/training/articles/keystore#HardwareSecurityModule)

## 📄 License

Part of the TON Connect WalletKit - see main project license.

## 🤝 Contributing

Security-related contributions are especially welcome! Please report security issues privately.

---

**⚡ Remember**: The security of user funds depends on proper implementation. Always follow best practices and test thoroughly!
