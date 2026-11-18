plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// En GitHub Actions GITHUB_REPOSITORY = "usuario/repo"; en local se usa gradle.properties
val ghRepo: String = System.getenv("GITHUB_REPOSITORY")
    ?: (project.findProperty("zentones.repo") as String? ?: "luckicla/Zentones")
// La versión se cambia en gradle.properties (zentones.version). Al subir a main, si es nueva, se publica sola.
val vName: String = (project.findProperty("appVersion") as String? ?: project.findProperty("zentones.version") as String? ?: "1.0.0").removePrefix("v")
val vNums = vName.split('.').map { it.takeWhile(Char::isDigit).toIntOrNull() ?: 0 } + listOf(0, 0, 0)
val vCode: Int = vNums[0] * 10000 + vNums[1] * 100 + vNums[2]
val freesoundKey: String = System.getenv("FREESOUND_KEY") ?: (project.findProperty("zentones.freesound") as String? ?: "")

android {
    namespace = "com.zentones.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.zentones.app"
        minSdk = 29
        targetSdk = 35
        versionCode = vCode
        versionName = vName
        buildConfigField("String", "GITHUB_REPO", "\"$ghRepo\"")
        buildConfigField("String", "FREESOUND_KEY", "\"$freesoundKey\"")
    }

    signingConfigs {
        create("release") {
            // Misma firma siempre: así cada APK nuevo se instala encima del anterior sin desinstalar
            storeFile = file(System.getenv("KS_FILE") ?: rootProject.file("zentones.jks").path)
            storePassword = System.getenv("KS_PASS") ?: "zentones"
            keyAlias = "zentones"
            keyPassword = System.getenv("KS_PASS") ?: "zentones"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    val bom = platform("androidx.compose:compose-bom:2025.05.01")
    implementation(bom)
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    // Cristal / blur real detrás de barras y menús
    implementation("dev.chrisbanes.haze:haze:1.6.0")
}
