# Keep WalletKit bridge JS interface methods
-keepclassmembers class io.ton.walletkit.bridge.WalletKitBridge$JsBinding {
    @android.webkit.JavascriptInterface <methods>;
}
