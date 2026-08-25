// A throwaway consumer: proves the adapter links into an app and lets the
// FirebaseUI sheet be exercised on a device against the Auth emulator. Not
// published. Firebase is initialised by hand with a `demo-` project id, so the
// sample carries no google-services.json and no real key.
plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
}

android {
  namespace = "io.github.rozd.userkit.sample"
  compileSdk {
    version = release(37)
  }
  defaultConfig {
    applicationId = "io.github.rozd.userkit.sample"
    minSdk = 26
    targetSdk = 37
    versionCode = 1
    versionName = "0.1.0"
    // 10.0.2.2 is the Android emulator's alias for the host's loopback.
    buildConfigField("String", "AUTH_EMULATOR_HOST", "\"10.0.2.2\"")
    buildConfigField("int", "AUTH_EMULATOR_PORT", "9099")
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
}

dependencies {
  implementation(project(":"))
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
}
