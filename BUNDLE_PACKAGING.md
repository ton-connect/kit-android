# Bundle Packaging Architecture

## Overview

JavaScript bundles are now **automatically included in the bridge AAR** during the build process. Partners integrating the SDK only need to add the AAR dependency - no manual asset copying required.

## Build Flow

### 1. Bridge Module Build (`bridge/build.gradle.kts`)

The bridge module's `preBuild` task automatically:

```kotlin
tasks.named("preBuild").configure {
    dependsOn(syncWalletKitWebViewAssets, syncWalletKitQuickJsAssets)
}
```

This triggers:
1. **Bundle Build**: Executes `pnpm run --filter androidkit build:all` from repository root
   - Builds WebView bundle → `dist-android/` (HTML + modular JS)
   - Builds QuickJS bundle → `dist-android-quickjs/walletkit.quickjs.js`

2. **Asset Copy**: Copies bundles into bridge module assets
   - WebView: `bridge/src/main/assets/walletkit/` (index.html + assets/*.js)
   - QuickJS: `bridge/src/main/assets/walletkit/quickjs/index.js`

3. **AAR Packaging**: Assets are included in the AAR during `assembleDebug`/`assembleRelease`

### 2. App Module (`app/build.gradle.kts`)

The demo app module is now simplified - it just depends on the bridge:

```kotlin
dependencies {
    implementation(project(":bridge"))
    implementation(project(":storage"))
}
```

The app inherits assets from the bridge AAR dependency automatically.

## For Partners

To integrate WalletKit Android SDK:

1. **Add AAR dependencies**:
   ```kotlin
   dependencies {
       implementation(files("libs/bridge-release.aar"))
       implementation(files("libs/storage-release.aar"))
       // Add required transitive dependencies
   }
   ```

2. **That's it!** The JavaScript bundles are already packaged in the bridge AAR.

## For SDK Developers

### Building the AAR

```bash
cd AndroidDemo
./gradlew :bridge:assembleRelease :storage:assembleRelease
```

**What happens**:
- Bridge `preBuild` runs `pnpm` to build JS bundles
- Bundles are copied to `bridge/src/main/assets/walletkit/`
- AAR is assembled with assets included
- Output: `bridge/build/outputs/aar/bridge-release.aar`

### Manual Bundle Build (Optional)

If you want to build bundles separately for testing:

```bash
# From repository root
pnpm -w --filter androidkit build:all
```

### Verifying AAR Contents

```bash
# Extract AAR (it's a ZIP)
unzip -l bridge/build/outputs/aar/bridge-release.aar | grep walletkit

# Should show:
# assets/walletkit/index.html
# assets/walletkit/assets/bridge.js
# assets/walletkit/assets/index.js
# assets/walletkit/quickjs/index.js
```

## Migration Notes

**Before** (old approach):
- ❌ App module had bundle copy tasks
- ❌ Partners had to manually copy bundles to their app's assets
- ❌ Bridge AAR didn't include JS bundles

**After** (current approach):
- ✅ Bridge module has bundle copy tasks
- ✅ Bundles are packaged in bridge AAR
- ✅ Partners just add AAR dependency
- ✅ Demo app inherits assets from bridge dependency

## Troubleshooting

### "Bundle not found" error during build

**Solution**: Ensure you have pnpm and Node.js 18+ installed:
```bash
npm install -g pnpm
pnpm --version
```

### Assets not found at runtime

**Solution**: Check that bundle copy tasks ran successfully:
```bash
ls -la AndroidDemo/bridge/src/main/assets/walletkit/
# Should show index.html and quickjs/index.js
```

### Clean build

```bash
# Clean and rebuild everything
cd AndroidDemo
./gradlew clean
./gradlew :bridge:assembleDebug
```
