import java.util.Properties
import org.gradle.api.GradleException

val restorativeValidation = providers.gradleProperty("restorativeValidation").orNull
  ?.trim()
  ?.lowercase()
  ?.let { value ->
    when (value) {
      "true" -> true
      "false" -> false
      else -> throw GradleException(
        "restorativeValidation must be 'true' or 'false', but was '$value'."
      )
    }
  }
  ?: false

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
  alias(libs.plugins.firebase.crashlytics.plugin)
}

android {
  namespace = "com.example"
  compileSdk = 37
  ndkVersion = "26.1.10909125"

  defaultConfig {
    applicationId = "com.aistudio.dreamgardendesigner.fhqpvw"
    minSdk = 24
    targetSdk = 37
    versionCode = 22
    versionName = "9.0.1.3"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    manifestPlaceholders["automaticStartupEnabled"] = "true"
    ndk {
      abiFilters.addAll(listOf("arm64-v8a"))
      debugSymbolLevel = "FULL"
    }
  }

  signingConfigs {
    create("release") {
      val properties = Properties()
      val localPropertiesFile = rootProject.file("local.properties")
      if (localPropertiesFile.exists()) {
        val inputStream = localPropertiesFile.inputStream()
        properties.load(inputStream)
        inputStream.close()
      }

      val keystorePath = properties.getProperty("release.keystore.path")
        ?: System.getenv("KEYSTORE_PATH")
        ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)

      storePassword = properties.getProperty("release.keystore.password") ?: System.getenv("STORE_PASSWORD")
      keyAlias = properties.getProperty("release.key.alias") ?: System.getenv("KEY_ALIAS") ?: "upload"
      keyPassword = properties.getProperty("release.key.password") ?: System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      buildConfigField("boolean", "RESTORATIVE_VALIDATION", "false")
      manifestPlaceholders["automaticStartupEnabled"] = "true"
      isCrunchPngs = false
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
      ndk {
        debugSymbolLevel = "FULL"
      }
    }
    debug {
      buildConfigField("boolean", "RESTORATIVE_VALIDATION", restorativeValidation.toString())
      manifestPlaceholders["automaticStartupEnabled"] = (!restorativeValidation).toString()
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }
  packaging {
  }
  compileOptions {
    isCoreLibraryDesugaringEnabled = true
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  experimentalProperties["android.nativeLibraryAlignmentPageSize"] = "16k"
  testOptions {
    unitTests {
      isIncludeAndroidResources = true
      all {
        it.jvmArgs(
          "--add-opens=java.base/java.lang=ALL-UNNAMED",
          "--add-opens=java.base/java.util=ALL-UNNAMED",
          "-Dnet.bytebuddy.experimental=true"
        )
      }
    }
  }
}

// Room schema history — every @Database version bump exports a new JSON
// snapshot here, which GardenDatabaseMigrationTest uses to verify each
// migration actually produces the expected schema. Keep this directory
// committed to git.
ksp {
  arg("room.schemaLocation", "$projectDir/schemas")
}


// Ensure the SQLite temp directory exists for Room compiler on Windows
val sqliteTmpDir = layout.buildDirectory.dir("tmp/sqlite").get().asFile
if (!sqliteTmpDir.exists()) {
    sqliteTmpDir.mkdirs()
}
System.setProperty("org.sqlite.tmpdir", sqliteTmpDir.absolutePath)

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.3")
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.accompanist.permissions)
  implementation(libs.androidx.camera.camera2)
  implementation(libs.androidx.camera.core)
  implementation(libs.androidx.camera.lifecycle)
  implementation(libs.androidx.camera.view)
  implementation(libs.arsceneview)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.text.google.fonts)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.work.runtime.ktx)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  // implementation(libs.firebase.ai)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  testImplementation(libs.androidx.work.testing)
  testImplementation(libs.mockito.core)
  testImplementation(libs.mockito.kotlin)
  testImplementation(libs.androidx.room.testing)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
  implementation(libs.firebase.analytics)
  implementation(libs.firebase.crashlytics)
  implementation(libs.androidx.core.splashscreen)
  implementation(libs.androidx.fragment.ktx)
  implementation(libs.play.billing)
  implementation(libs.play.billing.ktx)
  implementation(libs.androidx.security.crypto)
}
