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
  namespace = "org.tfv.ata"
  compileSdk = 35

  defaultConfig {
    applicationId = "org.tfv.ata"
    minSdk = 31
    targetSdk = 35
    versionCode = (System.getenv("ATA_VERSION_CODE")?.toIntOrNull()) ?: versionProps.getProperty("versionCode").toInt()
    versionName = versionProps.getProperty("versionName")
    ndk { abiFilters += listOf("arm64-v8a") }
    vectorDrawables.useSupportLibrary = true
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
  kotlinOptions { jvmTarget = "17" }
  lint { abortOnError = true }
}

dependencies {
  implementation(project(":core"))

  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.activity.compose)
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.ui)
  implementation(libs.androidx.ui.graphics)
  implementation(libs.androidx.ui.tooling.preview)
  implementation(libs.androidx.material3)
  implementation(libs.androidx.material.icons.extended)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.glance.appwidget)
  implementation(libs.androidx.work.runtime.ktx)
  implementation(libs.androidx.splashscreen)
  implementation(libs.androidx.biometric)
  implementation(libs.androidx.fragment.ktx)
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.play.services.wearable)

  debugImplementation(libs.androidx.ui.tooling)
}
