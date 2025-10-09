# WalletKit Storage - Consumer ProGuard Rules
# These rules are automatically applied to apps that use this library

# Keep public API
-keep public interface io.ton.walletkit.storage.WalletKitStorage { *; }
-keep public class io.ton.walletkit.storage.impl.SecureWalletKitStorage { *; }
-keep public class io.ton.walletkit.storage.impl.InMemoryWalletKitStorage { *; }
-keep public class io.ton.walletkit.storage.impl.DebugSharedPrefsStorage { *; }
-keep public class io.ton.walletkit.storage.model.** { *; }

# AndroidX Security Crypto (required for runtime)
-keep class androidx.security.crypto.** { *; }
-dontwarn androidx.security.crypto.**

# Android Keystore (required for runtime)
-keep class android.security.keystore.** { *; }
-dontwarn android.security.keystore.**
