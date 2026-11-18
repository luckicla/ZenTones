plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// En GitHub Actions GITHUB_REPOSITORY = "usuario/repo"; en local se usa gradle.properties
val ghRepo: String = System.getenv("GITHUB_REPOSITORY")
    ?: (project.findProperty("zentones.repo") as String? ?: "luckicla/Zentones")
// Versión: sale del contador de ejecuciones de GitHub Actions, así cada compilación es mayor que la anterior
val runNumber: String = System.getenv("GITHUB_RUN_NUMBER") ?: "1"
val freesoundKey: String = System.getenv("FREESOUND_KEY") ?: (project.findProperty("zentones.freesound") as String? ?: "")

android {
    namespace = "com.zentones.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.zentones.app"
        minSdk = 29
        targetSdk = 35
        versionCode = Integer.parseInt(runNumber)
        versionName = "1.0.$runNumber"
        buildConfigField("String", "GITHUB_REPO", "\"$ghRepo\"")
        buildConfigField("String", "FREESOUND_KEY", "\"$freesoundKey\"")
        // Momento de la compilación: el actualizador lo compara con la fecha del APK publicado
        buildConfigField("long", "BUILD_TIME", "${System.currentTimeMillis()}L")
        // yt-dlp trae Python nativo: solo móviles ARM para que el APK no se dispare
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a") }
    }

    // youtubedl-android necesita las librerías nativas extraídas
    packaging { jniLibs { useLegacyPackaging = true } }

    signingConfigs {
        create("release") {
            // Siempre el MISMO keystore: si cambia, el móvil no deja instalar encima
            storeFile = file("release.keystore")
            storePassword = "zentones"
            keyAlias = "app"
            keyPassword = "zentones"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
    // yt-dlp en Android (Python 3.11 + yt-dlp incluidos)
    implementation("io.github.junkfood02.youtubedl-android:library:0.17.2")
}
