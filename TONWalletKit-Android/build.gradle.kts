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
            targetExclude("**/build/**/*.kt")
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

// Build the SDK AAR (debug).
tasks.register("buildSdk") {
    group = "build"
    description = "Build the SDK AAR (debug, with logs)"

    dependsOn(":impl:bundleDebugAar")

    doLast {
        println("✅ SDK debug AAR built: impl-debug.aar")
        println("   🐛 DEBUG build with full logging enabled")
    }
}

// Build the SDK AAR (release).
tasks.register("buildSdkRelease") {
    group = "build"
    description = "Build the SDK AAR (release)"

    dependsOn(":impl:bundleReleaseAar")

    doLast {
        println("✅ SDK release AAR built: impl-release.aar")
    }
}

// Build the SDK AAR and copy it into the demo app's libs/.
tasks.register<Copy>("buildAndCopyToDemo") {
    group = "build"
    description = "Build the SDK debug AAR and copy it into AndroidDemo/app/libs"

    dependsOn(":api:build", ":impl:bundleDebugAar", ":api:sourcesJar")

    from(layout.projectDirectory.file("impl/build/outputs/aar/impl-debug.aar"))
    from(layout.projectDirectory.file("api/build/libs/api-sources.jar"))
    into(layout.projectDirectory.dir("../AndroidDemo/app/libs"))
    rename("impl-debug.aar", "tonwalletkit-release.aar")
    rename("api-sources.jar", "tonwalletkit-release-sources.jar")

    doLast {
        println("✅ Debug AAR copied to AndroidDemo/app/libs/tonwalletkit-release.aar")
        println("✅ Sources JAR copied to AndroidDemo/app/libs/tonwalletkit-release-sources.jar")
        println("   🐛 DEBUG build — full logging enabled")
        println("   💡 Sources JAR enables KDoc viewing in Android Studio")
    }
}

// Copy the existing AAR without rebuilding.
tasks.register<Copy>("copyToDemo") {
    group = "build"
    description = "Copy the existing debug AAR to AndroidDemo/app/libs (no rebuild)"

    from(layout.projectDirectory.file("impl/build/outputs/aar/impl-debug.aar"))
    into(layout.projectDirectory.dir("../AndroidDemo/app/libs"))
    rename("impl-debug.aar", "tonwalletkit-release.aar")

    doLast {
        println("✅ Copied existing debug AAR to AndroidDemo/app/libs/tonwalletkit-release.aar")
    }
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
