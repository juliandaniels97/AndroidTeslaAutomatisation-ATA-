import org.jetbrains.kotlin.gradle.dsl.JvmTarget
plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.serialization)
}

android {
  namespace = "org.tfv.ata.core"
  compileSdk = 37

  defaultConfig {
    minSdk = 30
    ndk { abiFilters += listOf("arm64-v8a") }
    consumerProguardFiles("consumer-rules.pro")
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  lint { abortOnError = true }
  testOptions { unitTests.isReturnDefaultValues = true }
}

kotlin {
  compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {
  implementation(libs.androidx.core.ktx)
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.kotlinx.coroutines.android)

  testImplementation(libs.junit)
}
