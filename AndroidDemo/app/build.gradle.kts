import org.gradle.api.GradleException
import org.gradle.api.tasks.Copy
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.kotlinCompose)
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

val walletKitDistDir =
    rootProject.rootDir
        .toPath()
        .resolve("../dist-android")
        .toFile()
val walletKitAssetsDir = layout.projectDirectory.dir("src/main/assets/walletkit").asFile

val syncWalletKitAssets =
    tasks.register<Copy>("syncWalletKitAssets") {
        group = "walletkit"
        description = "Copy WalletKit Web bundle into Android assets."
        from(walletKitDistDir)
        into(walletKitAssetsDir)
        includeEmptyDirs = false
        doFirst {
            if (!walletKitDistDir.exists()) {
                throw GradleException(
                    "WalletKit bundle not found at $walletKitDistDir. Run `pnpm -w --filter androidkit build` before building the Android demo.",
                )
            }
        }
    }

tasks.named("preBuild").configure {
    dependsOn(syncWalletKitAssets)
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
    implementation(project(":bridge"))
    implementation(project(":storage"))
}
