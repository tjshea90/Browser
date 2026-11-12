import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Kestrel Courier: the small app for the owner's PHONE. It downloads files for the tablet's Kestrel browser and hands them over the hotspot wifi
// (research/PHONE_OFFLOAD.md). The protocol, the download engine and the MP4 writer are all :core; this module is only the Android shell
// (foreground service, one screen, notification). Framework Views only, like the other apps.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "app.kestrel.courier"
    compileSdk = 36

    defaultConfig {
        applicationId = "app.kestrel.courier"
        minSdk = 29            // the same floor as the browser, so the emulator rig (Android 10) runs both; the owner's phone runs Android 16
        targetSdk = 36         // below 37 on purpose: Android 17's local-network permission is enforced only from there (research/PHONE_OFFLOAD.md 6.1)
        versionCode = 3
        versionName = "0.1.0-alpha3"
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

    packaging {
        resources {
            excludes += setOf("kotlin/**", "META-INF/*.kotlin_module", "META-INF/*.version", "META-INF/com.android.tools/**", "DebugProbesKt.bin")
        }
    }

    testOptions { unitTests { isIncludeAndroidResources = true; isReturnDefaultValues = true } }

    lint {
        abortOnError = true
        checkReleaseBuilds = true
        warningsAsErrors = false
        lintConfig = file("lint.xml")
    }
}

kotlin {
    sourceSets.getByName("main").kotlin.srcDir("../shared-android/kotlin") // the same platform video converter as the tablet apps (picture + sound merge, H.265 ...)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
        freeCompilerArgs.addAll("-Xno-param-assertions", "-Xno-call-assertions", "-Xno-receiver-assertions")
    }
}

dependencies {
    implementation(project(":core"))
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
}
