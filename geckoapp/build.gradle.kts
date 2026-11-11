import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Kestrel G: the same Kestrel UI / data / filter engine as :app, but pages are drawn by Firefox's engine (GeckoView) instead of the tablet's
// frozen Android System WebView (Chromium 74 on the owner's tablet cannot run current websites). Shared code = ../app/src/main (UI, data,
// shield, net); everything engine-specific is in this module's src/main. See BRIEF.md "Two builds, one repo".
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

val geckoVersion = "160.0.20261104181552"
// `-PgeckoAbi=x86` builds a debug APK for the x86 emulator; the tablet build is always armeabi-v7a.
val geckoAbi = (findProperty("geckoAbi") as String?) ?: "armeabi-v7a"

android {
    namespace = "app.kestrel.browser"   // shared sources and resources use app.kestrel.browser.R
    // GeckoView 160 is built against API 37 and its AAR refuses lower; AGP 8.13 only 'recommends' up to 36, hence suppressUnsupportedCompileSdk in gradle.properties.
    compileSdk { version = release(37) { minorApiLevel = 0 } }

    defaultConfig {
        applicationId = "app.kestrel.browser.gecko"  // installs BESIDE the WebView build (app.kestrel.browser)
        minSdk = 29
        targetSdk = 36
        versionCode = 109
        versionName = "0.2.0-alpha10"
        ndk { abiFilters += geckoAbi } // the tablet's userspace is 32-bit; only this engine build is bundled (~90 MB)
    }

    signingConfigs {
        create("kestrel") {
            storeFile = rootProject.file(keystoreProps.getProperty("storeFile") ?: "signing/dev/kestrel-dev.jks")
            storePassword = keystoreProps.getProperty("storePassword") ?: "kestrel-dev"
            keyAlias = keystoreProps.getProperty("keyAlias") ?: "kestrel-dev"
            keyPassword = keystoreProps.getProperty("keyPassword") ?: "kestrel-dev"
            enableV2Signing = true
            enableV3Signing = true
        }
    }

    buildTypes {
        debug { applicationIdSuffix = ".debug"; versionNameSuffix = "-debug" }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("kestrel")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    sourceSets.getByName("main") {
        res.srcDir("../app/src/main/res")
        assets.srcDir("../app/src/main/assets")
    }

    packaging {
        // Compressed libs: the APK is ~92 MB instead of ~157 MB to download over the owner's metered hotspot; Android extracts them at install (extra ~135 MB on disk).
        jniLibs { useLegacyPackaging = true }
        resources {
            excludes += setOf("kotlin/**", "META-INF/*.kotlin_module", "META-INF/*.version", "META-INF/com.android.tools/**", "DebugProbesKt.bin", "META-INF/DEPENDENCIES", "META-INF/LICENSE*", "META-INF/NOTICE*")
        }
    }

    lint {
        abortOnError = false   // GeckoView brings its own lint noise; the WebView module keeps the strict gate
        checkReleaseBuilds = false
    }
}

kotlin {
    sourceSets.getByName("main").kotlin.srcDir("../app/src/main/kotlin") // shared UI / data / shield / net (engine-neutral)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
        freeCompilerArgs.addAll("-Xno-param-assertions", "-Xno-call-assertions", "-Xno-receiver-assertions")
    }
}

configurations.all {
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk7")
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
    resolutionStrategy {
        // androidx.core 1.19.x (what GeckoView 160 declares) requires AGP 9.1; 1.17.x is API-compatible for GeckoView's use and builds with AGP 8.13.
        force("androidx.core:core:1.17.0")
    }
}

dependencies {
    implementation(project(":core"))
    implementation("org.mozilla.geckoview:geckoview-$geckoAbi:$geckoVersion") {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
        exclude(group = "com.google.android.gms") // WebAuthn via Google Play services: this tablet has none, and this project never depends on GMS
    }
}
