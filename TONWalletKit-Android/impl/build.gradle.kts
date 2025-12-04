import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.mavenPublish)
    jacoco
}

// Coverage exclusions: classes that cannot be unit tested (WebView/Android runtime dependencies)
val coverageExclusions =
    listOf(
        // === engine package ===
        // WebViewManager: Creates/manages WebView, uses Handler, Looper, WebViewClient
        "**/engine/infrastructure/WebViewManager*",
        // MessageDispatcher: Depends on WebViewManager for JS execution
        "**/engine/infrastructure/MessageDispatcher*",
        // WebViewWalletKitEngine: Top-level orchestrator, creates WebViewManager in constructor
        "**/engine/WebViewWalletKitEngine*",
        // QuickJsWalletKitEngine: Deprecated, replaced by WebView implementation
        "**/engine/QuickJsWalletKitEngine*",
        // WalletKitEngine: Interface with no executable code
        "**/engine/WalletKitEngine.class",
        "**/engine/WalletKitEngine\$*.class",
        // === browser package ===
        // TonConnectInjector: Depends on WebView for JavaScript injection
        "**/browser/TonConnectInjector*",
        // BridgeInterface: Uses @JavascriptInterface, requires WebView runtime
        "**/browser/BridgeInterface*",
        // NOTE: PendingRequest is NOT excluded - it's a pure data class
        // === storage package ===
        // Storage classes create MasterKey and EncryptedSharedPreferences internally (not injected),
        // so they cannot be mocked. Requires instrumented tests on device/emulator.
        // SecureBridgeStorageAdapter: Uses EncryptedSharedPreferences
        "**/storage/SecureBridgeStorageAdapter*",
        // SecureWalletKitStorage: Uses EncryptedSharedPreferences, MasterKey, Android Keystore
        "**/storage/SecureWalletKitStorage*",
        // BridgeStorageAdapter: Interface with no executable code
        "**/storage/BridgeStorageAdapter.class",
        // === core package ===
        // TONWallet: Thin delegation layer, all methods delegate to WalletKitEngine
        "**/walletkit/core/TONWallet*.class",
        // TONWalletKit: Main SDK facade, all methods delegate to WalletKitEngine
        "**/walletkit/core/TONWalletKit*.class",
        // WalletKitEngineFactory: Requires Android Context, creates WebView-dependent engines
        "**/walletkit/core/WalletKitEngineFactory*.class",
        // WalletKitEngineKind: Simple enum with no executable logic
        "**/walletkit/core/WalletKitEngineKind*.class",
        // === internal package ===
        // Logger: Uses android.util.Log, requires Android runtime
        "**/internal/util/Logger*",
        // All constants files: Just const val declarations, no executable logic
        "**/internal/constants/**",
    )

android {
    namespace = "io.ton.walletkit.bridge"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Default log level (can be overridden per build type)
        buildConfigField("String", "LOG_LEVEL", "\"DEBUG\"")
    }

    flavorDimensions += "engine"
    productFlavors {
        create("webview") {
            dimension = "engine"
            // WebView-only: lighter AAR, no native libs, no OkHttp
        }
        create("full") {
            dimension = "engine"
            // Both engines: includes QuickJS with native libs + OkHttp

            @Suppress("UnstableApiUsage")
            externalNativeBuild {
                cmake {
                    arguments += listOf("-DANDROID_STL=c++_shared")
                }
            }

            ndk {
                abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
            }
        }
    }

    androidComponents {
        onVariants { variant ->
            // Exclude native libs from webview variant only
            if (variant.flavorName == "webview") {
                variant.packaging.jniLibs.excludes.add("**/*.so")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")

            // Release: errors and warnings only (WARN level)
            buildConfigField("String", "LOG_LEVEL", "\"WARN\"")
        }
        debug {
            isMinifyEnabled = false
            enableUnitTestCoverage = true // Enable JaCoCo for unit tests

            // Debug: all logs including detailed debugging (DEBUG level)
            buildConfigField("String", "LOG_LEVEL", "\"DEBUG\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
    }

    // Only build native libs for full variant
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
        }
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
            isIncludeAndroidResources = true

            all {
                // Add JVM args for Robolectric and coverage compatibility
                it.jvmArgs(
                    "-XX:+IgnoreUnrecognizedVMOptions",
                    "--add-opens=java.base/java.lang=ALL-UNNAMED",
                    "--add-opens=java.base/java.util=ALL-UNNAMED",
                    // Required for IntelliJ/Android Studio coverage instrumentation
                    "-noverify",
                )

                // Required for JaCoCo to work with Robolectric
                it.extensions.configure(JacocoTaskExtension::class.java) {
                    isIncludeNoLocationClasses = true
                    excludes = listOf("jdk.internal.*")
                }
            }
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

// WalletKit Bundle Build & Copy Tasks
// Note: The JavaScript bundles are built in the main monorepo at:
// https://github.com/ton-connect/kit/tree/main/packages/walletkit-android-bridge
// Pre-built bundles should be placed in dist-android/ directory
val walletKitDistDir: File =
    rootProject.rootDir
        .toPath()
        .resolve("../dist-android")
        .normalize()
        .toFile()
val walletKitQuickJsDistDir: File =
    rootProject.rootDir
        .toPath()
        .resolve("../dist-android-quickjs")
        .normalize()
        .toFile()
val walletKitAssetsDir: File = layout.projectDirectory.dir("src/main/assets/walletkit").asFile

// Task to copy WebView bundle
val syncWalletKitWebViewAssets =
    tasks.register<Copy>("syncWalletKitWebViewAssets") {
        group = "walletkit"
        description = "Copy WalletKit WebView bundle from dist-android into impl module assets (packaged in AAR)."

        doFirst {
            if (!walletKitDistDir.exists()) {
                logger.error(
                    """
                    ❌ WebView bundle not found at $walletKitDistDir
                    
                    The JavaScript bridge bundles must be built from the monorepo:
                    https://github.com/ton-connect/kit/tree/main/packages/walletkit-android-bridge
                    
                    Then copy the dist-android/ directory to the kit-android repository root.
                    """.trimIndent(),
                )
                throw StopActionException()
            }

            // Clean old structure before copying new files
            if (walletKitAssetsDir.exists()) {
                // Remove old messy files
                walletKitAssetsDir.resolve("assets").deleteRecursively()
                walletKitAssetsDir.resolve(".vite").deleteRecursively()
                walletKitAssetsDir.resolve("index.html").delete()
                // Keep quickjs folder (handled by separate task)
            } else {
                walletKitAssetsDir.mkdirs()
            }
        }

        // Copy JS bundles and HTML from dist-android
        from(walletKitDistDir) {
            include("walletkit-android-bridge.mjs", "walletkit-android-bridge.mjs.map")
            include("inject.mjs", "inject.mjs.map")
            include("index.html")
        }

        into(walletKitAssetsDir)
        includeEmptyDirs = false

        doLast {
            logger.lifecycle("✅ Copied clean WebView bundles:")
            logger.lifecycle("   - walletkit-android-bridge.mjs (Main RPC bridge)")
            logger.lifecycle("   - inject.mjs (Internal browser injection)")
            logger.lifecycle("   - index.html (WebView entry point)")
        }
    }

// Task to copy QuickJS bundle
val syncWalletKitQuickJsAssets =
    tasks.register<Copy>("syncWalletKitQuickJsAssets") {
        group = "walletkit"
        description = "Copy WalletKit QuickJS bundle from dist-android-quickjs into impl module assets (packaged in AAR)."
        from(walletKitQuickJsDistDir) {
            include("walletkit.quickjs.js")
            rename("walletkit.quickjs.js", "index.js")
        }
        into(walletKitAssetsDir.resolve("quickjs"))
        doFirst {
            if (!walletKitQuickJsDistDir.exists()) {
                logger.warn(
                    "QuickJS bundle not found at $walletKitQuickJsDistDir. Skipping asset copy.",
                )
                // Don't throw exception, just skip
                throw StopActionException()
            }
        }
    }

// Ensure bundles are built and copied before assembling the AAR (but not for tests)
tasks.matching { it.name.contains("assemble") && !it.name.contains("Test") }.configureEach {
    dependsOn(syncWalletKitWebViewAssets, syncWalletKitQuickJsAssets)
}

// Fix implicit dependency warnings by explicitly declaring dependencies on merge tasks
tasks.matching { it.name.contains("merge") && it.name.contains("Assets") }.configureEach {
    dependsOn(syncWalletKitWebViewAssets, syncWalletKitQuickJsAssets)
}

dependencies {
    // API module - classes are merged into this AAR via the fat AAR task
    // Use compileOnly to avoid adding it as a dependency in the published POM
    // The fat AAR already contains all API classes merged in
    compileOnly(project(":api"))

    implementation(libs.androidxCoreKtx)
    implementation(libs.androidxLifecycleRuntimeKtx)
    implementation(libs.kotlinxCoroutinesAndroid)
    implementation(libs.kotlinxSerializationJson)
    implementation(libs.androidxWebkit)

    // OkHttp only for Full variant (includes QuickJS)
    "fullImplementation"(libs.okhttp)

    // Storage classes are now included in this module (merged from storage module)
    implementation(libs.androidxDatastorePreferences)
    implementation(libs.androidxSecurityCrypto)

    // API module needed for tests to access WalletKitBridgeException
    testImplementation(project(":api"))
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinxCoroutinesTest)
    testImplementation(libs.androidxTestCore)
    testImplementation(libs.robolectric)
    testImplementation(libs.shadowsFramework)

    // androidTest needs api module too
    androidTestImplementation(project(":api"))
    androidTestImplementation(libs.androidxTestExt)
    androidTestImplementation(libs.androidxTestRunner)
    androidTestImplementation(libs.kotlinxCoroutinesTest)
    testImplementation(kotlin("test"))
}

// Maven Publishing Configuration for both variants
mavenPublishing {
    publishToMavenCentral()

    // Only sign if credentials are configured (CI/CD or manual local publish)
    if (project.hasProperty("signing.keyId") || System.getenv("ORG_GRADLE_PROJECT_signingInMemoryKeyId") != null) {
        signAllPublications()
    }

    // Publish the webview release variant by default
    // Disable Javadoc for now due to Dokka compatibility issues
    // This publishes the FAT AAR that includes merged API + impl classes
    configure(
        com.vanniktech.maven.publish.AndroidSingleVariantLibrary(
            variant = "webviewRelease",
            sourcesJar = true,
            publishJavadocJar = false,
        ),
    )

    // Single artifact containing the complete SDK (API + impl merged)
    coordinates(
        project.property("GROUP").toString(),
        project.property("POM_ARTIFACT_ID").toString(),
        project.property("VERSION_NAME").toString(),
    )

    pom {
        name.set("TON WalletKit for Android")
        description.set("Android SDK for integrating TON Wallet functionality with dApp support")
        inceptionYear.set("2025")
        url.set("https://github.com/ton-connect/kit-android")

        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
                distribution.set("repo")
            }
        }

        developers {
            developer {
                id.set("ton-connect")
                name.set("TON")
                email.set("dnikulin@ton.org")
                url.set("https://github.com/ton-connect")
                organization.set("TonTech")
                organizationUrl.set("https://github.com/ton-connect")
            }
        }

        scm {
            url.set("https://github.com/ton-connect/kit-android")
            connection.set("scm:git:git://github.com/ton-connect/kit-android.git")
            developerConnection.set("scm:git:ssh://git@github.com/ton-connect/kit-android.git")
        }
    }
}

// Task to create fat AAR with embedded API module classes
// This ensures the AAR is self-contained and includes all public API types
afterEvaluate {
    tasks.matching { it.name.startsWith("bundle") && it.name.endsWith("Aar") }.configureEach {
        dependsOn(":api:bundleReleaseAar")

        // Use doLast to merge after classes are compiled but before AAR is finalized
        doLast {
            val variantName = name.removePrefix("bundle").removeSuffix("Aar")

            val apiAar =
                project(":api").layout.buildDirectory
                    .file("outputs/aar/api-release.aar")
                    .get().asFile

            if (!apiAar.exists()) {
                logger.warn("⚠️  API AAR not found, skipping merge: ${apiAar.absolutePath}")
                return@doLast
            }

            // Extract API AAR
            val apiExtractDir = layout.buildDirectory.dir("tmp/api-extract").get().asFile
            apiExtractDir.deleteRecursively()
            apiExtractDir.mkdirs()

            copy {
                from(zipTree(apiAar))
                into(apiExtractDir)
            }

            val apiClassesJar = File(apiExtractDir, "classes.jar")
            if (!apiClassesJar.exists()) {
                logger.warn("⚠️  API classes.jar not found")
                return@doLast
            }

            // Get the output AAR that was just created
            // Variant name is like "WebviewRelease", AAR name is like "impl-webview-release.aar"
            val aarName = "impl-${variantName.replace(Regex("([a-z])([A-Z])"), "$1-$2").lowercase()}.aar"
            val outputAar =
                layout.buildDirectory
                    .file("outputs/aar/$aarName")
                    .get().asFile

            if (!outputAar.exists()) {
                logger.warn("⚠️  Output AAR not found: ${outputAar.absolutePath}")
                return@doLast
            }

            // Extract the bridge AAR
            val bridgeExtractDir = layout.buildDirectory.dir("tmp/bridge-extract").get().asFile
            bridgeExtractDir.deleteRecursively()
            bridgeExtractDir.mkdirs()

            copy {
                from(zipTree(outputAar))
                into(bridgeExtractDir)
            }

            val bridgeClassesJar = File(bridgeExtractDir, "classes.jar")
            if (!bridgeClassesJar.exists()) {
                logger.warn("⚠️  Bridge classes.jar not found")
                return@doLast
            }

            // Extract both JARs to merge
            val bridgeClassesDir = layout.buildDirectory.dir("tmp/merge-classes/bridge").get().asFile
            val apiClassesDir = layout.buildDirectory.dir("tmp/merge-classes/api").get().asFile
            bridgeClassesDir.deleteRecursively()
            apiClassesDir.deleteRecursively()
            bridgeClassesDir.mkdirs()
            apiClassesDir.mkdirs()

            copy {
                from(zipTree(bridgeClassesJar))
                into(bridgeClassesDir)
            }

            copy {
                from(zipTree(apiClassesJar))
                into(apiClassesDir)
            }

            // Merge API classes into bridge classes
            copy {
                from(apiClassesDir)
                into(bridgeClassesDir)
            }

            // Create new classes.jar with merged content
            val mergedJar = File(bridgeExtractDir, "classes.jar")
            ant.withGroovyBuilder {
                "zip"("destFile" to mergedJar, "basedir" to bridgeClassesDir)
            }

            // Repackage AAR with merged classes.jar
            ant.withGroovyBuilder {
                "zip"("destFile" to outputAar, "basedir" to bridgeExtractDir)
            }

            logger.lifecycle("✅ Merged API classes into $variantName AAR: ${outputAar.name}")
        }
    }
}

// Disable all CMake tasks for webview variants (no native code needed)
// This prevents CMake configuration errors when QuickJS sources are missing
tasks.configureEach {
    val taskNameLower = name.lowercase()
    val isWebviewVariant = taskNameLower.contains("webview")
    val isCMakeTask =
        taskNameLower.contains("cmake") ||
            taskNameLower.contains("nativebuild") ||
            taskNameLower.contains("externalNative".lowercase())

    if (isWebviewVariant && isCMakeTask) {
        enabled = false
        logger.info("Disabled CMake task for webview variant: $name")
    }
}

// JaCoCo coverage report with exclusions
// Run: ./gradlew :impl:jacocoTestReport
// Report: impl/build/reports/jacoco/jacocoTestReport/html/index.html
tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testWebviewDebugUnitTest")

    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    val debugTree =
        fileTree("${layout.buildDirectory.get()}/tmp/kotlin-classes/webviewDebug") {
            exclude(coverageExclusions)
        }

    classDirectories.setFrom(debugTree)
    sourceDirectories.setFrom(files("src/main/java", "src/main/kotlin"))
    executionData.setFrom(
        fileTree(layout.buildDirectory) {
            include("outputs/unit_test_code_coverage/webviewDebugUnitTest/testWebviewDebugUnitTest.exec")
        },
    )
}

// Configure the built-in Android coverage report task to use exclusions
tasks.matching { it.name == "createWebviewDebugUnitTestCoverageReport" }.configureEach {
    if (this is JacocoReport) {
        classDirectories.setFrom(
            fileTree("${layout.buildDirectory.get()}/tmp/kotlin-classes/webviewDebug") {
                exclude(coverageExclusions)
            },
        )
    }
}
