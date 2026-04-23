import java.util.Locale
import java.util.Properties
import java.util.concurrent.TimeUnit

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10" // Compose Compiler plugin for Kotlin 2.0+
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use(::load)
    }
}

val googleWeatherApiKey = (localProperties.getProperty("google.weather.api.key") ?: "")
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")

val versionPropertiesFile = rootProject.file("version.properties")
val versionProperties = Properties().apply {
    if (versionPropertiesFile.exists()) {
        versionPropertiesFile.inputStream().use(::load)
    }
}

fun shouldIncrementBuildNumber(): Boolean {
    val buildTaskPrefixes = listOf("assemble", "bundle", "install")
    return gradle.startParameter.taskNames.any { requestedTask ->
        val taskName = requestedTask.substringAfterLast(":")
        buildTaskPrefixes.any { taskName.startsWith(it, ignoreCase = true) }
    }
}

fun readGitCommitCount(rootDir: java.io.File): Int? {
    return try {
        val process = ProcessBuilder("git", "rev-list", "--count", "HEAD")
            .directory(rootDir)
            .redirectErrorStream(true)
            .start()
        if (!process.waitFor(2, TimeUnit.SECONDS)) {
            process.destroy()
            return null
        }
        if (process.exitValue() != 0) return null
        process.inputStream.bufferedReader().readText().trim().toIntOrNull()
    } catch (_: Exception) {
        null
    }
}

val rawCommitCount = readGitCommitCount(rootProject.rootDir)
val versionCommitOffset = versionProperties.getProperty("commitOffset")?.toIntOrNull() ?: 0
val versionCommit = ((rawCommitCount ?: versionProperties.getProperty("commit")?.toIntOrNull() ?: 0) + versionCommitOffset)
    .coerceAtLeast(1)
val previousBuildNumber = versionProperties.getProperty("build")?.toIntOrNull() ?: 0
val versionBuildNumber = if (shouldIncrementBuildNumber()) previousBuildNumber + 1 else previousBuildNumber
val buildHigh = versionBuildNumber / 100
val buildLow = versionBuildNumber % 100
val generatedVersionName = String.format(Locale.US, "%d.%02d.%02d", versionCommit, buildHigh, buildLow)
val generatedVersionCode = (versionCommit * 10_000L + versionBuildNumber)
    .coerceIn(1L, Int.MAX_VALUE.toLong())
    .toInt()

if (versionBuildNumber != previousBuildNumber || versionProperties.getProperty("commit") != versionCommit.toString()) {
    versionProperties["commit"] = versionCommit.toString()
    versionProperties["build"] = versionBuildNumber.toString()
    val offsetLine = if (versionCommitOffset != 0) {
        "commitOffset=$versionCommitOffset${System.lineSeparator()}"
    } else {
        ""
    }
    versionPropertiesFile.writeText(
        "commit=$versionCommit${System.lineSeparator()}build=$versionBuildNumber${System.lineSeparator()}$offsetLine"
    )
}

android {
    namespace = "com.example.overlaybar"

    defaultConfig {
        applicationId = "com.example.overlaybar"
        minSdk = 29  // Android 10+
        targetSdk = 35
        versionCode = generatedVersionCode
        versionName = generatedVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField("String", "GOOGLE_WEATHER_API_KEY", "\"$googleWeatherApiKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    // No longer need composeOptions with Kotlin 2.0+
    // The compose compiler plugin handles this automatically

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    compileSdk = 35
}

dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")

    // Compose BOM - manages Compose versions
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)

    // Compose UI
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")

    // Material 3 with Expressive design
    implementation("androidx.compose.material3:material3:1.3.1") // Latest M3 with expressive components
    implementation("androidx.compose.material3:material3-window-size-class")

    // Compose Activity integration
    implementation("androidx.activity:activity-compose:1.9.3")

    // Lifecycle for Compose
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    // SavedState for proper state management
    implementation("androidx.savedstate:savedstate-ktx:1.2.1")

    // DataStore for preferences
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")

    // Coil for image and GIF loading
    implementation("io.coil-kt.coil3:coil-compose:3.0.4")
    implementation("io.coil-kt.coil3:coil-gif:3.0.4")

    // Optional: Material Icons Extended
    implementation("androidx.compose.material:material-icons-extended")

    // Fused location for low-power one-shot weather positioning
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Debug tools
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Tests
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
