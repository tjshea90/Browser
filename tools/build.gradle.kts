import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Desktop-side CLI: compiles filter lists into the binary .kfs format using the SAME code as the on-device updater.
plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

application {
    mainClass.set("app.kestrel.tools.ListCompilerKt")
}

dependencies {
    implementation(project(":core"))
    testImplementation(libs.junit)
}
