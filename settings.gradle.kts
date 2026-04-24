/*
 * Copyright (c) 2025 TonTech
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

rootProject.name = "kit-android-workspace"

// Composite build: open this root in Android Studio to sync and build the SDK and the demo
// app together. Each sub-project still has its own settings.gradle.kts, so you can also open
// either of them standalone — nothing about the existing layouts changed.

// Flag the demo app that the SDK is available as a composite sibling, so its build.gradle.kts
// switches from the pre-built AAR fallback to `implementation("org.ton:walletkit:…")` (which
// is then substituted to the live :impl project via the block below).
System.setProperty("walletkit.compositeSdk", "true")

includeBuild("TONWalletKit-Android") {
    // The SDK's :impl module publishes under the Maven coords `org.ton:walletkit:<version>`
    // (see TONWalletKit-Android/impl/build.gradle.kts → mavenPublishing{}). Gradle's automatic
    // composite-build substitution keys off `project.group + project.name`, which doesn't
    // match the publish coords, so we wire the substitution explicitly here. This makes the
    // demo's `implementation("org.ton:walletkit:…")` resolve to the live :impl project.
    //
    // :impl declares `compileOnly(project(":api"))` — :api classes end up merged into the
    // published fat AAR so downstream consumers don't need a separate :api dep. In composite
    // mode there's no fat AAR merge, so we also substitute an (otherwise non-existent)
    // `org.ton:walletkit-api` coord down to the :api project; the demo depends on that coord
    // only in composite mode.
    dependencySubstitution {
        substitute(module("org.ton:walletkit")).using(project(":impl"))
        substitute(module("org.ton:walletkit-api")).using(project(":api"))
    }
}

includeBuild("AndroidDemo")
