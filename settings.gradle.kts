pluginManagement {
  repositories {
    google {
      content {
        includeGroupByRegex("com\\.android.*")
        includeGroupByRegex("com\\.google.*")
        includeGroupByRegex("androidx.*")
      }
    }
    mavenCentral()
    gradlePluginPortal()
  }
}
plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
    // The core (`com.github.rozdonmobile:user-kit`) is served by JitPack straight
    // from its GitHub tag. Scoped to that group so nothing else is looked up there.
    maven("https://jitpack.io") {
      content { includeGroup("com.github.rozdonmobile") }
    }
  }
}

// The adapter is the root project; `:sample` is a throwaway consumer app.
rootProject.name = "user-kit-firebase"
include(":sample")

// To develop against an uncommitted core instead of the published tag, uncomment:
// Gradle then substitutes every `com.github.rozdonmobile:user-kit` dependency
// with the sibling checkout — the Android counterpart of a local SPM override.
// includeBuild("../user-kit")
