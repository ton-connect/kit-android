/*
 * Copyright (c) 2025 TonTech
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

tasks.register("assembleAll") {
    group = "build"
    description = "Build+copy the SDK AAR and assemble the demo APK."
    dependsOn(
        gradle.includedBuild("TONWalletKit-Android").task(":buildAndCopyToDemo"),
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
    description = "Run SDK unit tests + demo unit tests."
    dependsOn(
        gradle.includedBuild("TONWalletKit-Android").task(":api:testDebugUnitTest"),
        gradle.includedBuild("TONWalletKit-Android").task(":impl:testDebugUnitTest"),
        gradle.includedBuild("AndroidDemo").task(":app:testDebugUnitTest"),
    )
}

tasks.register("spotlessApply") {
    group = "formatting"
    description = "Run spotlessApply across the SDK and demo."
    dependsOn(
        gradle.includedBuild("TONWalletKit-Android").task(":api:spotlessApply"),
        gradle.includedBuild("TONWalletKit-Android").task(":impl:spotlessApply"),
        gradle.includedBuild("AndroidDemo").task(":spotlessApply"),
    )
}

tasks.register("spotlessCheck") {
    group = "verification"
    description = "Run spotlessCheck across the SDK and demo."
    dependsOn(
        gradle.includedBuild("TONWalletKit-Android").task(":api:spotlessCheck"),
        gradle.includedBuild("TONWalletKit-Android").task(":impl:spotlessCheck"),
        gradle.includedBuild("AndroidDemo").task(":spotlessCheck"),
    )
}
