# walletkitquickjs native runtime

This module builds a local copy of [quickjs-ng](https://github.com/quickjs-ng/quickjs) v0.10.1 and exposes a JNI facade that mirrors the subset of the CashApp QuickJs API used by the WalletKit bridge.

## Layout

```
src/main/cpp/
├── CMakeLists.txt         # Native build definition for Gradle externalNativeBuild
├── quickjs_bridge.cpp     # JNI entry points and host binding dispatcher
└── third_party/
    └── quickjs-ng/        # quickjs-ng sources (extracted tarball)
```

The tarball is pinned to commit `3c9afc9943323ee9c7dbd123c0cd991448f4b6c2` (tag `v0.10.1`). SHA-256:

```
63e40c77ef1141941d2dc8eaf1512434a145b97f3c8c092298d2dec9d829a2bc
```

## Updating quickjs-ng

1. Download the desired tag archive:
   ```bash
   curl -L -o quickjs-ng-vX.Y.Z.tar.gz https://github.com/quickjs-ng/quickjs/tarball/refs/tags/vX.Y.Z
   ```
2. Verify the checksum (update the value above):
   ```bash
   shasum -a 256 quickjs-ng-vX.Y.Z.tar.gz
   ```
3. Extract into `src/main/cpp/third_party/quickjs-ng`:
   ```bash
   rm -rf src/main/cpp/third_party/quickjs-ng
   mkdir -p src/main/cpp/third_party/quickjs-ng
   tar -xzf quickjs-ng-vX.Y.Z.tar.gz --strip-components=1 -C src/main/cpp/third_party/quickjs-ng
   ```
4. Update `CONFIG_VERSION` in `CMakeLists.txt` and commit the refreshed sources.

## Build

Gradle is configured to include this `CMakeLists.txt` via `externalNativeBuild`. The bridge AAR packages `libwalletkitquickjs.so` for all supported Android ABIs (arm64-v8a, armeabi-v7a, x86, x86_64).

A small instrumentation test (`QuickJsBridgeInstrumentedTest`) provides smoke coverage for `create`, `evaluate`, and reflective host bindings.

### 16 KB page-size compatibility

To meet Google Play’s Android 15+ requirement the linker is invoked with:

- `-Wl,-z,max-page-size=16384`
- `-Wl,-z,common-page-size=4096`
- `-Wl,--hash-style=both`

These flags align load segments on 16 KB boundaries and ensure both `DT_HASH` and `DT_GNU_HASH` tables are present in the shared object. Keep them when adjusting the native build.
