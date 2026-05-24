import java.net.URI
import java.net.URL

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.nakamura.ajsye"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
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
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  // implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
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
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}

// Automatically download custom high-res logo from Cloudinary to compile as app drawer icon
val downloadLauncherLogo = tasks.register("downloadLauncherLogo") {
    notCompatibleWithConfigurationCache("Custom script task downloading launcher logo")
    doLast {
        val destFile = file("src/main/res/drawable/nakamura_logo.png")
        destFile.parentFile.mkdirs()

        // Automatically download custom Cherry Bomb One font
        val fontDestFile = file("src/main/res/font/cherry_bomb_one.ttf")
        fontDestFile.parentFile.mkdirs()
        if (!fontDestFile.exists()) {
            try {
                val url = java.net.URI("https://raw.githubusercontent.com/google/fonts/main/ofl/cherrybombone/CherryBombOne-Regular.ttf").toURL()
                val connection = url.openConnection()
                connection.setRequestProperty("User-Agent", "Mozilla/5.0")
                connection.getInputStream().use { ins ->
                    fontDestFile.outputStream().use { outs ->
                        ins.copyTo(outs)
                    }
                }
                println("SUCCESS: Cherry Bomb One font downloaded successfully!")
            } catch (e: Exception) {
                println("WARNING: Could not download Cherry Bomb One font: ${e.message}")
            }
        }
        
        // Search for user uploaded file
        var foundFile: File? = null
        val searchRoots = listOf(
            file(".."),
            file("."),
            file("../.."),
            file("/tmp")
        )
        for (root in searchRoots) {
            if (root.exists()) {
                try {
                    root.walkTopDown()
                        .maxDepth(10)
                        .onFail { _, _ -> }
                        .forEach {
                            if (it.name.contains("file_00000000766871f58d60c50e95a6167f") || it.name.contains("766871f58d60")) {
                                foundFile = it
                            }
                        }
                } catch (e: Exception) {
                    // Ignore search errors on specific root
                }
            }
            if (foundFile != null) break
        }
        
        if (foundFile != null) {
            println("SUCCESS: Found user uploaded file at ${foundFile!!.absolutePath}")
            foundFile!!.inputStream().use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            println("SUCCESS: Copied user image to drawable/nakamura_logo.png")
        } else {
            println("WARNING: User uploaded file not found locally. Downloading fallback logo from Cloudinary...")
            if (!destFile.exists()) {
                try {
                    val url = URI("https://res.cloudinary.com/di9jolpim/image/upload/v1779405775/9_Sem_T%C3%ADtulo_20260521202134_kko5m8.png").toURL()
                    val connection = url.openConnection()
                    connection.setRequestProperty("User-Agent", "Mozilla/5.0")
                    val ins = connection.getInputStream()
                    val outs = destFile.outputStream()
                    ins.copyTo(outs)
                    ins.close()
                    outs.close()
                    println("SUCCESS: Nakamura AI launcher logo downloaded successfully!")
                } catch (e: Exception) {
                    println("WARNING: Could not download Nakamura AI logo automatically: ${e.message}")
                }
            }
        }
    }
}

tasks.named("preBuild") {
    dependsOn(downloadLauncherLogo)
}

