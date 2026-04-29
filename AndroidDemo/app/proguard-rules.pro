# Keep default rules + demo-app overrides used with `isMinifyEnabled = true`.
#
# Add rules sparingly. Each `-keep*` directive disables tree-shaking for the matched
# classes/members, so over-broad rules erode the size wins we get from R8.

# ─── kotlinx.serialization ────────────────────────────────────────────────
# Companion serializers are discovered via reflection; their INSTANCE fields must survive.
# Standard rules shipped with the kotlinx-serialization Gradle plugin.
-keepattributes InnerClasses,RuntimeVisibleAnnotations,AnnotationDefault
-keepclassmembers class ** {
    *** Companion;
}
-keepclasseswithmembers class ** {
    kotlinx.serialization.KSerializer serializer(...);
}
-if class **$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class <1>$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}
# @Serializable data classes used over the bridge — keep their generated $serializer / fields.
-keep,includedescriptorclasses class io.ton.walletkit.**$$serializer { *; }
-keepclassmembers class io.ton.walletkit.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep @kotlinx.serialization.Serializable class io.ton.walletkit.demo.** { *; }

# ─── WebView @JavascriptInterface ────────────────────────────────────────
# Methods called from the JS side over the walletkit bridge are invoked via reflection by
# the WebView. R8 would otherwise shrink them.
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keep class * extends android.webkit.WebViewClient
-keep class * extends android.webkit.WebChromeClient

# ─── Hilt / Dagger ───────────────────────────────────────────────────────
# Hilt ships consumer proguard rules via its AAR, but keep a belt-and-braces rule for the
# @HiltAndroidApp / @AndroidEntryPoint entry points and their generated components.
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ApplicationComponentManager { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }

# ─── ton-kotlin (compileOnly inside the SDK) ────────────────────────────
# The walletkit SDK AAR references `org.ton.block.AddrStd` etc. internally but ships
# without the ton-kotlin dependency on the consumer classpath (they are compileOnly there).
# These references are only hit on error paths the demo never exercises, so instruct R8
# to ignore the missing classes rather than fail the whole build.
-dontwarn org.ton.**
-keep class org.ton.** { *; }

# ─── Coroutines / Compose internals ──────────────────────────────────────
# Compose runtime is already covered by its own consumer rules; keep line numbers for
# readable crash reports when something slips through.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
