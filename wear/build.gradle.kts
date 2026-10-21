import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.kotlin.serialization)
}

val versionProps = Properties().apply {
  load(rootProject.file("version.properties").inputStream())
}

android {
  namespace = "org.tfv.ata.wear"
  compileSdk = 37

  defaultConfig {
    applicationId = "org.tfv.ata"
    minSdk = 30
    targetSdk = 37
    versionCode = (System.getenv("ATA_VERSION_CODE")?.toIntOrNull()) ?: versionProps.getProperty("versionCode").toInt()
    versionName = versionProps.getProperty("versionName")
    // -PataAllAbis keeps every ABI (used by the CI emulator smoke test on x86_64).
    // 64-bit and 32-bit ARM: e.g. Galaxy Watch 4–6 run a 32-bit (armeabi-v7a) system.
    if (!project.hasProperty("ataAllAbis")) ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a") }
  }

  val releaseStoreFile = System.getenv("ATA_KEYSTORE_FILE")

  // Release builds are signed only with the real key (CI secrets). Without it the APK stays unsigned —
  // never fall back to a debug key, so nobody can ship an update that inherits ATA's vehicle key.
  signingConfigs {
    if (!releaseStoreFile.isNullOrBlank()) {
      create("release") {
        storeFile = file(releaseStoreFile)
        storePassword = System.getenv("ATA_KEYSTORE_PASSWORD")
        keyAlias = System.getenv("ATA_KEY_ALIAS")
        keyPassword = System.getenv("ATA_KEY_PASSWORD")
      }
    }
  }

  buildFeatures {
    compose = true
    buildConfig = true
  }

  buildTypes {
    release {
      if (!releaseStoreFile.isNullOrBlank()) signingConfig = signingConfigs.getByName("release")
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro",
      )
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  lint { abortOnError = true }
}

kotlin {
  compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {
  implementation(project(":core"))

  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.ui)
  implementation(libs.androidx.ui.graphics)
  implementation(libs.androidx.ui.tooling.preview)
  implementation(libs.wear.compose.material)
  implementation(libs.wear.compose.foundation)
  implementation(libs.wear.compose.navigation)
  implementation(libs.wear.tiles)
  implementation(libs.wear.tiles.material)
  implementation(libs.wear.protolayout)
  implementation(libs.wear.protolayout.material)
  implementation(libs.wear.protolayout.expression)
  implementation(libs.watchface.complications.data.source.ktx)
  implementation(libs.play.services.wearable)
  implementation(libs.kotlinx.coroutines.play.services)
  implementation(libs.concurrent.futures)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.serialization.json)

  debugImplementation(libs.androidx.ui.tooling)
}
