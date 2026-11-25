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
            // feature-webcompat: Mozilla's site-compatibility extension (research/REVIEW_2026-11-22_NEWLEVERS.md NL1); only its assets are used, no class of it enters the app
            filter { includeGroup("org.mozilla.geckoview"); includeModule("org.mozilla.components", "feature-webcompat") }
        }
        google()
        mavenCentral()
    }
}

rootProject.name = "Kestrel"

include(":core", ":app", ":geckoapp", ":courier", ":tools")
