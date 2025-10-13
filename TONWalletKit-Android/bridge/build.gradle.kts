import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.kotlinSerialization)
}

android {
    namespace = "io.ton.walletkit.bridge"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
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
        unitTests.isReturnDefaultValues = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

// WalletKit Bundle Build & Copy Tasks
val walletKitDistDir =
    rootProject.rootDir
        .toPath()
        .resolve("../../dist-android")
        .normalize()
        .toFile()
val walletKitQuickJsDistDir =
    rootProject.rootDir
        .toPath()
        .resolve("../../dist-android-quickjs")
        .normalize()
        .toFile()
val walletKitAssetsDir = layout.projectDirectory.dir("src/main/assets/walletkit").asFile

// Task to build both WebView and QuickJS bundles using pnpm
val buildWalletKitBundles =
    tasks.register<Exec>("buildWalletKitBundles") {
        group = "walletkit"
        description = "Build both WebView and QuickJS WalletKit bundles using pnpm"
        workingDir = rootProject.rootDir.parentFile
        commandLine("sh", "-c", "cd ${rootProject.rootDir.parentFile.absolutePath} && pnpm run build:all")
        // Only fail if pnpm is not found and we're actually building (not testing)
        isIgnoreExitValue = true
        doLast {
            val result = executionResult.get()
            if (result.exitValue != 0) {
                logger.warn("pnpm build failed or pnpm not found. Skipping bundle generation.")
            }
        }
    }

// Task to copy WebView bundle
val syncWalletKitWebViewAssets =
    tasks.register<Copy>("syncWalletKitWebViewAssets") {
        group = "walletkit"
        description = "Copy WalletKit WebView bundle into bridge module assets (packaged in AAR)."
        dependsOn(buildWalletKitBundles)
        from(walletKitDistDir)
        into(walletKitAssetsDir)
        includeEmptyDirs = false
        doFirst {
            if (!walletKitDistDir.exists()) {
                logger.warn(
                    "WebView bundle not found at $walletKitDistDir. Skipping asset copy.",
                )
                // Don't throw exception, just skip
                throw StopActionException()
            }
        }
    }

// Task to copy QuickJS bundle
val syncWalletKitQuickJsAssets =
    tasks.register<Copy>("syncWalletKitQuickJsAssets") {
        group = "walletkit"
        description = "Copy WalletKit QuickJS bundle into bridge module assets (packaged in AAR)."
        dependsOn(buildWalletKitBundles)
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

dependencies {
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

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinxCoroutinesTest)
    testImplementation(libs.androidxTestCore)
    testImplementation(libs.robolectric)
    testImplementation(libs.shadowsFramework)
    androidTestImplementation(libs.androidxTestExt)
    androidTestImplementation(libs.androidxTestRunner)
    testImplementation(kotlin("test"))
}
