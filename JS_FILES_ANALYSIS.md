# Android SDK JavaScript Files Analysis

## Summary

After analyzing the Android SDK bridge and the JavaScript source code, here's what each file does and whether it's needed:

---

## 📊 File Usage Status

### ✅ **USED FILES** (Keep these)

#### 1. `index.ts` ✅ **ENTRY POINT - REQUIRED**
- **Purpose**: Main entry point for the WebView build
- **What it does**: 
  - Sets up polyfills
  - Dynamically imports `bridge.ts`
- **Used by**: `index.html` (referenced as `/js/src/index.ts`)
- **Status**: **CRITICAL - DO NOT REMOVE**

#### 2. `bridge.ts` ✅ **CORE FUNCTIONALITY - REQUIRED**
- **Purpose**: Main bridge implementation for Android-JS communication
- **What it does**:
  - Implements all wallet operations (init, add wallet, transactions, etc.)
  - Handles TON Connect events (connect, transaction, signData requests)
  - Manages session state
  - Provides the API that Android Kotlin code calls via WebView
- **Used by**: `index.ts` (dynamically imported)
- **Exposes**: `window.walletkitBridge` and `window.__walletkitCall` for Android
- **Status**: **CRITICAL - DO NOT REMOVE**

#### 3. `setupPolyfills.ts` ✅ **POLYFILLS - REQUIRED**
- **Purpose**: Provides browser APIs that may not exist in WebView
- **What it does**:
  - Sets up TextEncoder/TextDecoder
  - Adds Buffer polyfill
  - Adds URL/URLSearchParams polyfills
  - Adds fetch and AbortController if missing
- **Used by**: `index.ts`
- **Status**: **REQUIRED - DO NOT REMOVE**

#### 4. `textEncoder.ts` ✅ **POLYFILL - REQUIRED**
- **Purpose**: TextEncoder/TextDecoder polyfill for WebView
- **What it does**: Provides UTF-8 encoding/decoding functionality
- **Used by**: `setupPolyfills.ts`
- **Status**: **REQUIRED - DO NOT REMOVE**

#### 5. `types.ts` ✅ **TYPE DEFINITIONS - REQUIRED**
- **Purpose**: TypeScript type definitions
- **What it does**: Defines `WalletKitBridgeEvent` and `WalletKitBridgeInitConfig` types
- **Used by**: `bridge.ts`, `globals.d.ts`
- **Status**: **REQUIRED - DO NOT REMOVE**

---

### ❌ **UNUSED FILES** (Can be removed)

#### 6. `main.ts` ❌ **NOT USED - CAN REMOVE**
- **Purpose**: OLD/DUPLICATE bridge implementation
- **What it does**: 
  - Alternative/older implementation of the bridge API
  - Provides same functionality as `bridge.ts` but with simpler/older design
  - Exposes `walletkit_request` API (which is now also in `bridge.ts`)
  - Less comprehensive than `bridge.ts`
- **Used by**: **NOTHING** - not imported anywhere in the build chain
- **Why it exists**: Appears to be an older version that was replaced by `bridge.ts`
- **Important**: `bridge.ts` already implements both APIs (`walletkitBridge` + `walletkit_request`), so `main.ts` is redundant
- **Android uses**: `__walletkitCall` from `bridge.ts`, not `walletkit_request` from `main.ts`
- **Status**: ⚠️ **SAFE TO REMOVE** - Complete duplicate, never imported

---

### 🔄 **TYPE DEFINITION FILES** (Keep for TypeScript)

#### 7. `types.d.ts` ✅ **TYPE DEFINITIONS**
- **Purpose**: Additional TypeScript declarations
- **What it does**:
  - Declares module types for `@ton/walletkit`, `whatwg-url`, `buffer`
  - Duplicates some types from `types.ts` (could be merged)
- **Used by**: TypeScript compiler
- **Status**: **KEEP** (helps TypeScript understand external modules)

#### 8. `globals.d.ts` ✅ **GLOBAL TYPE DEFINITIONS**
- **Purpose**: Global type augmentations
- **What it does**:
  - Extends the `Window` interface with bridge types
  - Declares Android/WalletKit native bridge interfaces
- **Used by**: TypeScript compiler for type checking
- **Status**: **KEEP** (needed for proper typing)

---

## 🔍 Build Flow Analysis

### Current Build Chain:
```
index.html
  └── /js/src/index.ts (entry)
       ├── setupPolyfills.ts
       │    └── textEncoder.ts
       └── bridge.ts (dynamic import)
            └── types.ts
```

### What Android Actually Uses:
The Android WebView loads `index.html` which:
1. Runs `index.ts` (sets up polyfills + imports bridge)
2. Executes `bridge.ts` (exposes `window.walletkitBridge` API)
3. Android Kotlin code calls methods via `evaluateJavascript()`:
   ```kotlin
   webView.evaluateJavascript(
     "window.__walletkitCall('$id', '$method', $paramsJson)",
     callback
   )
   ```

### Built Output:
- `assets/index.js` - Main bundle (includes index.ts, setupPolyfills.ts, textEncoder.ts)
- `assets/bridge.js` - Bridge code chunk (bridge.ts)
- `assets/index2.js` - @ton/walletkit dependency
- `assets/index3.js` - @ton/core dependency

---

## 📝 Recommendations

### 1. **Remove `main.ts`** ✅ SAFE
- **File**: `kit/apps/androidkit/js/src/main.ts`
- **Reason**: Not imported or used anywhere in the build
- **Risk**: None - it's completely disconnected from the build chain
- **Action**: Delete this file

### 2. **Consider Merging Type Files** (Optional)
- `types.ts` and `types.d.ts` have some duplicate definitions
- Could consolidate into single file for clarity
- **Risk**: Low (just cleanup)

### 3. **Keep Everything Else**
All other files are part of the active build chain and used by Android

---

## 🎯 Specific Answer to Your Question

**Files you DON'T need and CAN remove:**
- ❌ `main.ts` - Old/unused bridge implementation

**Files you MUST keep:**
- ✅ `index.ts` - Entry point
- ✅ `bridge.ts` - Main functionality
- ✅ `setupPolyfills.ts` - Polyfills
- ✅ `textEncoder.ts` - Text encoding polyfill
- ✅ `types.ts` - Type definitions
- ✅ `types.d.ts` - Type declarations
- ✅ `globals.d.ts` - Global type augmentations

---

## 🔬 How I Determined This

1. **Traced the build entry point**: `index.html` → `index.ts`
2. **Followed imports**: `index.ts` → `setupPolyfills.ts` + `bridge.ts`
3. **Checked polyfill dependencies**: `setupPolyfills.ts` → `textEncoder.ts`
4. **Searched for references to `main.ts`**: Found **zero references** in the entire androidkit project
5. **Verified Android bridge calls**: Confirmed Android uses `window.__walletkitCall` from `bridge.ts`
   - WebViewWalletKitEngine.kt: calls `window.__walletkitCall(...)`
   - QuickJsWalletKitEngine.kt: calls `globalThis.__walletkitCall(...)`
6. **Checked build output**: Only sees chunks from the active chain (bridge.js, not main.js)
7. **Discovered**: `bridge.ts` already implements BOTH APIs:
   - `window.walletkitBridge` (modern API, used via `__walletkitCall`)
   - `window.walletkit_request` (legacy API, NOT used by Android)
8. **Conclusion**: `main.ts` is a complete orphan file with duplicate functionality

---

## 🚀 Action Item

**Safe to delete now:**
```bash
rm kit/apps/androidkit/js/src/main.ts
```

This will not break anything because it's not referenced in the build chain.
