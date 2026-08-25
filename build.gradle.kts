plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.android.application) apply false
  `maven-publish`
}

// The adapter *is* the root project (`:sample` is the only subproject), so the
// JitPack coordinates are `com.github.rozdonmobile:user-kit-firebase:<tag>`.
group = property("GROUP") as String
version = property("VERSION_NAME") as String

android {
  namespace = "io.github.rozd.userkit.firebase"
  compileSdk {
    version = release(37)
  }
  defaultConfig {
    minSdk = 26
    consumerProguardFiles("consumer-rules.pro")
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  buildFeatures {
    compose = true
  }
  publishing {
    singleVariant("release") {
      withSourcesJar()
    }
  }
}

dependencies {
  // The core, resolved from JitPack (see settings.gradle.kts). `api` so an app
  // that links the adapter gets `User`/`LocalUser` without a second line.
  api(libs.user.kit)

  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.compose.runtime)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.material3)

  // `api`, not `implementation`: the BOM must reach consumers, or the version-less
  // `firebase-auth` this module exposes cannot be resolved from an app.
  api(platform(libs.firebase.bom))
  api(libs.firebase.auth)
  api(libs.firebase.ui.auth)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.coroutines.play.services)

  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
}

publishing {
  publications {
    register<MavenPublication>("release") {
      groupId = project.group.toString()
      artifactId = "user-kit-firebase"
      version = project.version.toString()
      afterEvaluate { from(components["release"]) }
      pom {
        name.set("UserKit Firebase adapter for Compose")
        description.set("Firebase Authentication provider adapter for the Compose UserKit.")
        url.set("https://github.com/rozdonmobile/user-kit-firebase")
        licenses {
          license {
            name.set("MIT")
            url.set("https://opensource.org/licenses/MIT")
          }
        }
        scm {
          url.set("https://github.com/rozdonmobile/user-kit-firebase")
        }
      }
    }
  }
}
