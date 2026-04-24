// Top-level build file for TONWalletKit-Android SDK
plugins {
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.kotlinAndroid) apply false
    alias(libs.plugins.spotless) apply false
}

// Apply Spotless formatting to all subprojects
subprojects {
    apply(plugin = "com.diffplug.spotless")
    
    configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        kotlin {
            target("**/*.kt")
            targetExclude(
                "**/build/**/*.kt",
                "**/WalletKitEngineFactoryTest.kt",
                // Output of Scripts/generate-api/generate-test-models.sh — openapi-generator's
                // Kotlin template formatting clashes with ktlint, and any fix would be wiped
                // on the next regen. The authored test (generationtests/) is NOT excluded.
                "**/generatedtest/**/*.kt",
            )
            licenseHeaderFile(rootProject.file("../LICENSE_HEADER"))
            ktlint("1.0.1")
                .editorConfigOverride(
                    mapOf(
                        "ktlint_standard_no-wildcard-imports" to "disabled",
                        "ktlint_standard_filename" to "disabled",
                    )
                )
        }
        kotlinGradle {
            target("**/*.gradle.kts")
            ktlint("1.0.1")
        }
    }
}

// Task to build WebView variant (Debug)
tasks.register("buildWebview") {
    group = "build"
    description = "Build WebView SDK variant (no QuickJS) - DEBUG with logs"
    
    dependsOn(":impl:bundleWebviewDebugAar")
    
    doLast {
        println("✅ WebView variant built: impl-webview-debug.aar (2.7M)")
        println("   Includes API + implementation, no QuickJS native libs")
        println("   🐛 DEBUG build with full logging enabled")
    }
}

// Task to build WebView variant (Release)
tasks.register("buildWebviewRelease") {
    group = "build"
    description = "Build WebView SDK variant (no QuickJS) - RELEASE"
    
    dependsOn(":impl:bundleWebviewReleaseAar")
    
    doLast {
        println("✅ WebView variant built: impl-webview-release.aar")
        println("   Includes API + implementation, no QuickJS native libs")
        println("   📦 RELEASE build")
    }
}

// Task to build Full variant with QuickJS
tasks.register("buildFull") {
    group = "build"
    description = "Build Full SDK variant (includes QuickJS) - DEBUG with logs"
    
    dependsOn(":impl:bundleFullDebugAar")
    
    doLast {
        println("✅ Full variant built: impl-full-debug.aar (4.3M)")
        println("   Includes API + implementation + QuickJS native libs")
        println("   🐛 DEBUG build with full logging enabled")
    }
}

// Task to build both variants
tasks.register("buildAllVariants") {
    group = "build"
    description = "Build all SDK variants (webview, full)"
    
    dependsOn("buildWebview", "buildFull")
}

// Task to build and copy WebView variant to demo (default, recommended)
tasks.register<Copy>("buildAndCopyWebviewToDemo") {
    group = "build"
    description = "Build WebView SDK variant and copy to AndroidDemo/app/libs - DEBUG with logs"
    
    dependsOn(":api:build", ":impl:bundleWebviewDebugAar", ":api:sourcesJar")
    
    from(layout.projectDirectory.file("impl/build/outputs/aar/impl-webview-debug.aar"))
    from(layout.projectDirectory.file("api/build/libs/api-sources.jar"))
    into(layout.projectDirectory.dir("../AndroidDemo/app/libs"))
    rename("impl-webview-debug.aar", "tonwalletkit-release.aar")
    rename("api-sources.jar", "tonwalletkit-release-sources.jar")
    
    doLast {
        println("✅ WebView DEBUG variant copied to AndroidDemo/app/libs/tonwalletkit-release.aar")
        println("✅ Sources JAR copied to AndroidDemo/app/libs/tonwalletkit-release-sources.jar")
        println("   Size: ~2.7M (fat AAR with API + impl, no QuickJS)")
        println("   🐛 DEBUG build - full logging enabled!")
        println("   Demo app uses: WebView engine only")
        println("   💡 Sources JAR enables KDoc viewing in Android Studio")
    }
}

// Task to build and copy Full variant to demo
tasks.register<Copy>("buildAndCopyFullToDemo") {
    group = "build"
    description = "Build Full SDK variant (with QuickJS) and copy to AndroidDemo/app/libs - DEBUG with logs"
    
    dependsOn(":api:build", ":impl:bundleFullDebugAar", ":api:sourcesJar")
    
    from(layout.projectDirectory.file("impl/build/outputs/aar/impl-full-debug.aar"))
    from(layout.projectDirectory.file("api/build/libs/api-sources.jar"))
    into(layout.projectDirectory.dir("../AndroidDemo/app/libs"))
    rename("impl-full-debug.aar", "tonwalletkit-release.aar")
    rename("api-sources.jar", "tonwalletkit-release-sources.jar")
    
    doLast {
        println("✅ Full DEBUG variant copied to AndroidDemo/app/libs/tonwalletkit-release.aar")
        println("✅ Sources JAR copied to AndroidDemo/app/libs/tonwalletkit-release-sources.jar")
        println("   Size: ~4.3M (fat AAR with API + impl + QuickJS)")
        println("   🐛 DEBUG build - full logging enabled!")
        println("   Demo app uses: WebView + QuickJS engines")
        println("   💡 Sources JAR enables KDoc viewing in Android Studio")
    }
}

// Alias for backward compatibility (uses webview by default)
tasks.register("buildAndCopyToDemo") {
    group = "build"
    description = "Build and copy WebView variant to demo (alias for buildAndCopyWebviewToDemo)"
    
    dependsOn("buildAndCopyWebviewToDemo")
}

// Maven Central Publishing Validation Tasks
tasks.register("checkSigningConfiguration") {
    group = "publishing"
    description = "Check that signing configuration is properly set up for Maven Central"
    
    doLast {
        val requiredProps = listOf(
            "signing.keyId" to System.getenv("ORG_GRADLE_PROJECT_signingInMemoryKeyId"),
            "signing.password" to System.getenv("ORG_GRADLE_PROJECT_signingInMemoryKeyPassword"),
            "signing.key" to System.getenv("ORG_GRADLE_PROJECT_signingInMemoryKey")
        )
        
        val missingProps = requiredProps.filter { it.second.isNullOrEmpty() }
        
        if (missingProps.isNotEmpty()) {
            println("⚠️  Missing signing configuration:")
            missingProps.forEach { println("   - ${it.first}") }
            println("\nSet these as environment variables or Gradle properties:")
            println("   ORG_GRADLE_PROJECT_signingInMemoryKeyId")
            println("   ORG_GRADLE_PROJECT_signingInMemoryKeyPassword")
            println("   ORG_GRADLE_PROJECT_signingInMemoryKey")
        } else {
            println("✅ Signing configuration is present")
        }
    }
}

tasks.register("checkPomFileForMavenPublication") {
    group = "publishing"
    description = "Check that POM files meet Maven Central requirements"
    
    doLast {
        val requiredPomFields = listOf(
            "GROUP" to project.findProperty("GROUP"),
            "VERSION_NAME" to project.findProperty("VERSION_NAME"),
            "POM_ARTIFACT_ID" to project.findProperty("POM_ARTIFACT_ID")
        )
        
        val missingFields = requiredPomFields.filter { it.second == null }
        
        if (missingFields.isNotEmpty()) {
            println("❌ Missing required POM fields in gradle.properties:")
            missingFields.forEach { println("   - ${it.first}") }
            throw GradleException("POM configuration incomplete")
        } else {
            println("✅ POM configuration is complete")
            println("   GROUP: ${project.property("GROUP")}")
            println("   VERSION_NAME: ${project.property("VERSION_NAME")}")
            println("   POM_ARTIFACT_ID: ${project.property("POM_ARTIFACT_ID")}")
        }
    }
}

tasks.register("validatePublishingSetup") {
    group = "publishing"
    description = "Validate all publishing requirements before releasing to Maven Central"
    
    dependsOn("checkSigningConfiguration", "checkPomFileForMavenPublication")
    
    doLast {
        println("\n✅ Publishing setup validation complete!")
        println("   Ready to publish to Maven Central")
    }
}

// Task to just copy existing WebView AAR without rebuilding
tasks.register<Copy>("copyWebviewToDemo") {
    group = "build"
    description = "Copy existing WebView DEBUG AAR to demo (no rebuild)"
    
    from(layout.projectDirectory.file("impl/build/outputs/aar/impl-webview-debug.aar"))
    into(layout.projectDirectory.dir("../AndroidDemo/app/libs"))
    rename("impl-webview-debug.aar", "tonwalletkit-release.aar")
    
    doLast {
        println("✅ Copied existing DEBUG AAR to AndroidDemo/app/libs/tonwalletkit-release.aar")
    }
}

// Task to just copy existing Full AAR without rebuilding
tasks.register<Copy>("copyFullToDemo") {
    group = "build"
    description = "Copy existing Full DEBUG AAR to demo (no rebuild)"
    
    from(layout.projectDirectory.file("impl/build/outputs/aar/impl-full-debug.aar"))
    into(layout.projectDirectory.dir("../AndroidDemo/app/libs"))
    rename("impl-full-debug.aar", "tonwalletkit-release.aar")
    
    doLast {
        println("✅ Copied existing DEBUG AAR to AndroidDemo/app/libs/tonwalletkit-release.aar")
    }
}
