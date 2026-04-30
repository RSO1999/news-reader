import java.util.Properties

fun loadPropertiesFromRoot(fileName: String): Properties = Properties().apply {
    val propertiesFile = rootProject.file(fileName)
    if (propertiesFile.exists()) {
        propertiesFile.inputStream().use(::load)
    }
}

fun String.ensureTrailingSlash(): String = if (endsWith('/')) this else "$this/"

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

val exampleEnvProperties = loadPropertiesFromRoot("env.example.properties")
val localEnvProperties = loadPropertiesFromRoot("env.local.properties")

val configuredBaseUrl = (
    localEnvProperties.getProperty("BASE_URL")
        ?: exampleEnvProperties.getProperty("BASE_URL")
        ?: "http://10.0.2.2:8080/"
).trim()

require(configuredBaseUrl.isNotBlank()) {
    "BASE_URL must be set in env.local.properties or env.example.properties"
}

val storyStreamBaseUrl = configuredBaseUrl.ensureTrailingSlash()
val usesCleartextTraffic = storyStreamBaseUrl.startsWith("http://", ignoreCase = true)

android {
    namespace = "com.storystream.reader_app"

    compileSdk = 36

    defaultConfig {
        applicationId = "com.storystream.reader_app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "BASE_URL", "\"$storyStreamBaseUrl\"")
        manifestPlaceholders["usesCleartextTraffic"] = usesCleartextTraffic.toString()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
}

kotlin {
    jvmToolchain(11)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Image loading (Compose)
    implementation(libs.coil.compose)

    // Tink (encryption primitives) - added so imports resolve
    implementation(libs.tink.android)

    // Security (EncryptedSharedPreferences)
    // Removed per request: implementation(libs.security.crypto)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}