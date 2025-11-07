# ============================================================
# TON WalletKit Android SDK - Library Development ProGuard Rules
# These rules are used during library module builds (debug/release)
# For consumer apps, see consumer-rules.pro
# ============================================================

# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /usr/local/Cellar/android-sdk/24.3.3/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.kts.

# For more details, see:
#   http://developer.android.com/guide/developing/tools/proguard.html

# ------------------------------------------------------------
# Development Rules
# ------------------------------------------------------------

# Keep source file names and line numbers for debugging during development
-keepattributes SourceFile,LineNumberTable

# Don't obfuscate during library development for easier debugging
# (Only applies to library module builds, not consumer apps)
-dontobfuscate

# Keep all implementation classes readable during development
-keep class io.ton.walletkit.bridge.** { *; }

# If you want to enable obfuscation during library builds, comment out
# the -dontobfuscate line above and the -keep rule, then uncomment below:
#
# -keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
# 
# -keepclassmembers class * {
#     @android.webkit.JavascriptInterface <methods>;
# }
#
# -keepclasseswithmembernames class ** {
#     native <methods>;
# }

# ------------------------------------------------------------
# Remove Logging in Release Builds
# ------------------------------------------------------------

# Remove all Log.v (VERBOSE) calls
-assumenosideeffects class android.util.Log {
    public static *** v(...);
}

# Remove all Log.d (DEBUG) calls
-assumenosideeffects class android.util.Log {
    public static *** d(...);
}

# Remove all Log.i (INFO) calls
-assumenosideeffects class android.util.Log {
    public static *** i(...);
}

# Keep Log.w (WARNING) and Log.e (ERROR) for important messages
# Uncomment below to also remove warnings and errors:
# -assumenosideeffects class android.util.Log {
#     public static *** w(...);
#     public static *** e(...);
# }

# Remove println statements
-assumenosideeffects class java.io.PrintStream {
    public void println(%);
    public void println(**);
}

# Remove System.out and System.err print statements
-assumenosideeffects class java.lang.System {
    public static java.io.PrintStream out;
    public static java.io.PrintStream err;
}

