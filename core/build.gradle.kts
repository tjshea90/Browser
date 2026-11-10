import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Pure Kotlin/JVM: ALL testable browser logic lives here. No Android imports allowed.
// It runs on an Android 10 (API 29) runtime, so it is compiled against the Java 8 API surface:
// -Xjdk-release=8 makes the compiler reject JDK 9+ APIs (and the ByteBuffer.flip() covariance trap)
// that exist on the build JDK (21) but NOT on the tablet. Android lint does not check pure-JVM modules,
// so this flag is the guard.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

kotlin {
    compilerOptions {
        // -Xjdk-release=8 requires the JVM target to be 1.8 as well (Kotlin rejects any other combination).
        jvmTarget.set(JvmTarget.JVM_1_8)
        freeCompilerArgs.addAll(
            "-Xjdk-release=8",
            "-Xno-param-assertions",
            "-Xno-call-assertions",
            "-Xno-receiver-assertions",
        )
    }
}

dependencies {
    testImplementation(libs.junit)
}
