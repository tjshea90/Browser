pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // Firefox's engine (GeckoView) is published only on Mozilla's Maven; exclusiveContent keeps Gradle from asking the other repositories
        // about that group at all (Maven Central answers such probes with 429 rate-limit errors in the build sandbox).
        exclusiveContent {
            forRepository { maven { url = uri("https://maven.mozilla.org/maven2/") } }
            filter { includeGroup("org.mozilla.geckoview") }
        }
        google()
        mavenCentral()
    }
}

rootProject.name = "Kestrel"

include(":core", ":app", ":geckoapp", ":tools")
