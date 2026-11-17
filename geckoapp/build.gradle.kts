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
// LAB5 packaging switches (research/LAB5_PACKAGING.md):
//  -PomniStored=false (DEFAULT ON since alpha64) turns OFF the stored layout of assets/omni.ja. Stored = UNCOMPRESSED in the APK (it was deflated: 13.96 MB -> 14.51 MB, re-deflating a zip gains nothing). Gecko maps a stored entry
//                     straight from the APK (shared, evictable file pages); a deflated one has to be inflated into a private heap buffer in EVERY Gecko process that opens it. Measured by the pack lab run
//                     (same code, 4 builds): warm start -11 %, engine ready -12 %, memory after the page set -28 MB. tools/verify-apk.sh fails a build whose omni.ja is deflated.
//  -PlibsInApk=true   (OFF) native libraries stay uncompressed inside the APK (no extraction at install) instead of the legacy compressed-and-extracted layout: +65 MB download, no start-up or memory gain in the lab. Opt-in only.
val omniStored = (findProperty("omniStored") as String?) != "false"
val libsInApk = (findProperty("libsInApk") as String?) == "true"

android {
    namespace = "app.kestrel.browser"   // shared sources and resources use app.kestrel.browser.R
    // GeckoView 160 is built against API 37 and its AAR refuses lower; AGP 8.13 only 'recommends' up to 36, hence suppressUnsupportedCompileSdk in gradle.properties.
    compileSdk { version = release(37) { minorApiLevel = 0 } }

    defaultConfig {
        applicationId = "app.kestrel.browser.gecko"  // installs BESIDE the WebView build (app.kestrel.browser)
        minSdk = 29
        targetSdk = 36
        versionCode = 164
        versionName = "0.3.0-alpha63"
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

    androidResources {
        if (omniStored) noCompress += "ja"
    }

    packaging {
        // Compressed libs: the APK is ~92 MB instead of ~157 MB to download over the owner's metered hotspot; Android extracts them at install (extra ~135 MB on disk).
        jniLibs { useLegacyPackaging = !libsInApk }
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
    sourceSets.getByName("main").kotlin.srcDir("../shared-android/kotlin") // code the phone app (:courier) shares: the platform video converter
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

// ANS52i (research/VIDEO_ACCEL_THIRDPARTY.md): Kestrel's own player runs on Media3 ExoPlayer. 1.11.1 is the version GeckoView 160's own POM names (the engine's HLS player is built on it and its classes already
// travel in the APK), so naming the two modules we call adds no second copy: keep this equal to what `./gradlew :geckoapp:dependencies` shows for media3 under GeckoView.
val media3Version = "1.11.1"

dependencies {
    implementation(project(":core"))
    implementation("androidx.media3:media3-exoplayer:$media3Version")
    implementation("androidx.media3:media3-exoplayer-hls:$media3Version")
    implementation("org.mozilla.geckoview:geckoview-$geckoAbi:$geckoVersion") {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
        exclude(group = "com.google.android.gms") // WebAuthn via Google Play services: this tablet has none, and this project never depends on GMS
    }
}
