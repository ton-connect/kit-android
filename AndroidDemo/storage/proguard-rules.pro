# WalletKit Storage Security ProGuard Rules
# These rules protect cryptographic code and prevent sensitive data leakage

# Keep encryption classes and methods
-keep class io.ton.walletkit.storage.encryption.** { *; }
-keep class io.ton.walletkit.storage.impl.SecureWalletKitStorage { *; }

# Keep WalletKitStorage interface for library consumers
-keep interface io.ton.walletkit.storage.WalletKitStorage { *; }
-keep class io.ton.walletkit.storage.model.** { *; }

# AndroidX Security Crypto
-keep class androidx.security.crypto.** { *; }
-dontwarn androidx.security.crypto.**

# Android Keystore
-keep class android.security.keystore.** { *; }
-dontwarn android.security.keystore.**

# Prevent optimization of security-critical code
-keep class javax.crypto.** { *; }
-keep class java.security.** { *; }
-dontwarn javax.crypto.**
-dontwarn java.security.**

# Remove all logging in release builds for security
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}

# Keep line numbers for debugging stack traces
-keepattributes SourceFile,LineNumberTable

# Rename classes for additional obfuscation (but keep interfaces)
-repackageclasses 'io.ton.walletkit.storage.internal'
