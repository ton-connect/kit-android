# TON WalletKit Android SDK

Android library for TON blockchain wallet integration.

## Variants

- **webview**: 1.2MB, WebView only (recommended)
- **full**: 4.3MB, WebView + QuickJS (deprecated, 2x slower)

## QuickJS Source (Reference Only)

QuickJS source removed from repo. If needed locally:
```bash
cd bridge/src/main/cpp/third_party/
git clone https://github.com/bellard/quickjs-ng.git
```

## Building

```bash
./gradlew assembleWebviewRelease
./gradlew assembleFullRelease

# Build and copy to demo app
./gradlew buildAndCopyWebviewToDemo
```

## Integration

Copy AAR to your `app/libs/` and add:

```kotlin
dependencies {
    implementation(files("libs/bridge-release.aar"))
    implementation("androidx.webkit:webkit:1.12.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    // For full variant only:
    // implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
```

## Usage

```kotlin
val walletKit = WalletKitEngineFactory.create(
    context = applicationContext,
    kind = WalletKitEngineKind.WEBVIEW
)
```

## Structure

- `src/main/` - Common code, WebView engine
- `src/full/` - QuickJS-specific code
- `src/main/cpp/` - Native libraries
