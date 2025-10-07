# ⚠️ QuickJS Engine - DEPRECATED

**Status**: DEPRECATED as of October 2025  
**Action**: Use `WebViewWalletKitEngine` instead  
**Performance**: QuickJS is 2x slower than WebView

## Quick Facts

- ❌ **Not recommended** for production use
- ❌ **No active maintenance** - frozen code
- ❌ **No bug fixes** or new features
- ❌ **No support** for QuickJS users
- ✅ **Code preserved** for potential future optimization
- ✅ **WebView is 2x faster** and actively maintained

## Migration

Change one line of code:

```kotlin
// Don't use this
val engine = QuickJsWalletKitEngine(context)

// Use this instead
val engine = WebViewWalletKitEngine(context)
```

## Performance Data

QuickJS vs WebView (15 benchmark runs):
- **Cold start**: 1881ms vs 917ms (2x slower ❌)
- **Init**: 490ms vs 248ms (97% slower ❌)
- **Add wallet**: 775ms vs 89ms (775% slower ❌)

**Conclusion**: Memory benefits (2-4MB vs 20-30MB) don't justify the massive performance penalty.

## Why Deprecated?

WebView is:
- ✅ 2x faster for user-facing operations
- ✅ Simpler (no NDK, no native code)
- ✅ Smaller APK (~0.04MB vs 3MB)
- ✅ Better debugging (Chrome DevTools)
- ✅ Actively maintained

QuickJS only advantage:
- Lower memory (2-4MB vs 20-30MB) - not worth 2x slower performance

## Learn More

- **Full details**: See `/kit/apps/androidkit/QUICKJS_DEPRECATION.md`
- **Integration guide**: See `/kit/apps/androidkit/INTEGRATION.md`
- **Main README**: See `/kit/apps/androidkit/README.md`

---

**Last updated**: October 2025  
**Recommendation**: Use WebView instead  
**This code**: Preserved for reference, not for production use
