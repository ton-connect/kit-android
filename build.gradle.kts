/*
 * Copyright (c) 2025 TonTech
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

// Aggregate tasks across the two included builds so you can drive everything from the root
// (e.g. from Android Studio's "root" project context).

tasks.register("assembleAll") {
    group = "build"
    description = "Assemble the SDK (webview debug AAR) and the demo app debug APK."
    dependsOn(
        gradle.includedBuild("TONWalletKit-Android").task(":impl:bundleWebviewDebugAar"),
        gradle.includedBuild("AndroidDemo").task(":app:assembleDebug"),
    )
}

tasks.register("cleanAll") {
    group = "build"
    description = "Clean both included builds."
    dependsOn(
        gradle.includedBuild("TONWalletKit-Android").task(":clean"),
        gradle.includedBuild("AndroidDemo").task(":clean"),
    )
}

tasks.register("testAll") {
    group = "verification"
    description = "Run SDK webview-variant unit tests + demo unit tests."
    dependsOn(
        gradle.includedBuild("TONWalletKit-Android").task(":api:testDebugUnitTest"),
        gradle.includedBuild("TONWalletKit-Android").task(":impl:testWebviewDebugUnitTest"),
        gradle.includedBuild("AndroidDemo").task(":app:testDebugUnitTest"),
    )
}
