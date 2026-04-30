import java.util.Properties

plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.kotlinSerialization)
}

android {
    namespace = "io.ton.walletkit"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // API module doesn't need obfuscation - it's the public interface
            // Consumer rules will keep everything public
        }
    }

    // Enable generation of BuildConfig (needed for sources JAR task)
    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        freeCompilerArgs.add("-opt-in=kotlin.time.ExperimentalTime")
    }
}

// Forward `walletkit.path` from local.properties (or the WALLETKIT_PATH env var) to the test
// JVM so GeneratedModelsSnapshotTest can find the kit monorepo. Unset → the test self-skips.
tasks.withType<Test>().configureEach {
    val localPropsFile = rootProject.file("local.properties")
    val fromLocal: String? =
        if (localPropsFile.exists()) {
            val props = Properties()
            localPropsFile.inputStream().use { props.load(it) }
            props.getProperty("walletkit.path")
        } else {
            null
        }
    val resolved: String? = fromLocal?.takeIf { it.isNotBlank() } ?: System.getenv("WALLETKIT_PATH")
    if (!resolved.isNullOrBlank()) environment("WALLETKIT_PATH", resolved)
}

// Generate sources JAR with KDocs for better IDE experience
val sourcesJar =
    tasks.register<Jar>("sourcesJar") {
        archiveClassifier.set("sources")
        from(android.sourceSets["main"].java.srcDirs)
        // Include all Kotlin source files with KDocs
        include("**/*.kt")
        include("**/*.java")
        // Exclude internal implementation details
        exclude("**/internal/**")
    }

// Attach sources JAR to artifacts (for Maven publishing)
artifacts {
    archives(sourcesJar)
}

dependencies {
    // Minimal dependencies - only what public API needs
    api(libs.kotlinxSerializationJson)
    api(libs.kotlinxDatetime)
    api(libs.kotlinxCoroutinesAndroid)

    // TON Kotlin for address handling and crypto
    implementation(libs.tonKotlinBlockTlb)

    // Test dependencies
    testImplementation(libs.junit)
    testImplementation(libs.kotlinxCoroutinesTest)
    // kotlin-reflect is used by GeneratedModelsTest to introspect the shape of
    // models generated from the walletkit fixture (primary constructor params,
    // sealed subclasses, etc.). Test-only — kept out of the published API.
    testImplementation(libs.kotlinReflect)
}
