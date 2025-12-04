import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.kotlinCompose)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hiltAndroid)
}

// Force OkHttp version to avoid conflicts between app dependencies and test dependencies
configurations.all {
    resolutionStrategy {
        force("com.squareup.okhttp3:okhttp:5.3.2")
    }
}

android {
    namespace = "io.ton.walletkit.demo"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.ton.walletkit.demo"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidxCoreKtx)
    implementation(libs.androidxAppcompat)
    implementation(libs.googleMaterial)
    implementation(libs.androidxActivityKtx)
    implementation(libs.androidxConstraintLayout)
    implementation(libs.androidxActivityCompose)
    implementation(platform(libs.androidxComposeBom))
    implementation(libs.androidxComposeUi)
    implementation(libs.androidxComposeMaterial3)
    implementation(libs.androidxComposeMaterialIconsExtended)
    implementation(libs.androidxComposeUiToolingPreview)
    debugImplementation(libs.androidxComposeUiTooling)
    implementation(libs.androidxLifecycleRuntimeKtx)
    implementation(libs.androidxLifecycleViewmodelCompose)
    implementation(libs.kotlinxCoroutinesAndroid)
    implementation(libs.androidxSecurityCrypto)
    implementation(libs.coilCompose)
    implementation(libs.coilNetwork)

    // Hilt dependency injection
    implementation(libs.hiltAndroid)
    ksp(libs.hiltCompiler)

    // TONWalletKit SDK - Use local AAR file
    // Build and copy with: cd ../TONWalletKit-Android && ./gradlew buildAndCopyWebviewToDemo
    implementation(files("libs/tonwalletkit-release.aar"))
    // Required transitive dependencies when using AAR:
    implementation(libs.androidxWebkit)
    implementation(libs.androidxDatastorePreferences)
    implementation(libs.kotlinxSerializationJson)

    // Alternative: Use Maven Local for testing published artifact (currently not working - Gradle ignores mavenLocal())
    // Publish to local maven with: cd ../TONWalletKit-Android && ./gradlew publishToMavenLocal
    // implementation("io.github.ton-connect:tonwalletkit-android:1.0.0-alpha01")

    debugImplementation(libs.leakcanaryAndroid)

    // Testing - Unit Tests
    testImplementation(libs.junit)

    // Testing - Instrumentation Tests (Espresso + Compose)
    androidTestImplementation(libs.androidxTestCore)
    androidTestImplementation(libs.androidxTestCoreKtx)
    androidTestImplementation(libs.androidxTestRunner)
    androidTestImplementation(libs.androidxTestRules)
    androidTestImplementation(libs.androidxTestEspressoCore)
    androidTestImplementation(libs.androidxTestEspressoWeb)
    androidTestImplementation(libs.androidxTestEspressoIntents)
    androidTestImplementation(libs.androidxTestUiAutomator)
    androidTestImplementation(platform(libs.androidxComposeBom))
    androidTestImplementation(libs.androidxComposeUiTestJunit4)
    debugImplementation(libs.androidxComposeUiTestManifest)

    // Allure reporting for tests
    androidTestImplementation(libs.allureKotlinAndroid)
    androidTestImplementation(libs.allureKotlinModel)
    androidTestImplementation(libs.allureKotlinCommons)

    // OkHttp for Allure API client
    androidTestImplementation(libs.okhttp)
    androidTestImplementation(libs.kotlinxSerializationJson)

    // Hilt testing
    androidTestImplementation(libs.hiltAndroidTesting)
    kspAndroidTest(libs.hiltCompiler)
}
