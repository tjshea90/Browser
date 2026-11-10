import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

// Private signing key (gitignored keystore.properties) wins; otherwise the PUBLIC dev key in signing/dev/ is used
// so APKs built by ANY future session/CI are update-compatible. See docs/ARCHITECTURE.md section 8.
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "app.kestrel.browser"
    compileSdk = 36

    defaultConfig {
        applicationId = "app.kestrel.browser"
        minSdk = 29            // the tablet is Android 10 (API 29); anything newer must be guarded
        targetSdk = 36         // also runs on Android 16 (edge-to-edge + predictive back handled in code)
        versionCode = 1
        versionName = "0.1.0"
    }

    signingConfigs {
        create("kestrel") {
            storeFile = rootProject.file(keystoreProps.getProperty("storeFile") ?: "signing/dev/kestrel-dev.jks")
            storePassword = keystoreProps.getProperty("storePassword") ?: "kestrel-dev"
            keyAlias = keystoreProps.getProperty("keyAlias") ?: "kestrel-dev"
            keyPassword = keystoreProps.getProperty("keyPassword") ?: "kestrel-dev"
            enableV2Signing = true
            enableV3Signing = true   // allows a later key rotation without uninstalling
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
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
            excludes += setOf(
                "kotlin/**",
                "META-INF/*.kotlin_module",
                "META-INF/*.version",
                "META-INF/com.android.tools/**",
                "DebugProbesKt.bin",
            )
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = true
        warningsAsErrors = false
        lintConfig = file("lint.xml")
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
        freeCompilerArgs.addAll(
            "-Xno-param-assertions",
            "-Xno-call-assertions",
            "-Xno-receiver-assertions",
        )
    }
}

dependencies {
    implementation(project(":core"))
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
}

// Robolectric downloads its Android framework jar at test time; the sandbox's Maven Central route is the Google-hosted mirror.
tasks.withType<Test>().configureEach {
    systemProperty("robolectric.dependency.repo.url", "https://maven-central.storage-download.googleapis.com/maven2/")
    systemProperty("robolectric.dependency.repo.id", "central")
    jvmArgs("-Xmx2g")
}
