// build.gradle.kts (app level)

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
    kotlin("plugin.serialization") version "2.0.21"
    id("com.google.gms.google-services") // Google Services plugini
}

android {
    namespace = "com.acagribahar.muscleandmindapp"
    compileSdk = 35 // En son stabil SDK'yı kullanmanız önerilir (34 olabilir)

    defaultConfig {
        applicationId = "com.acagribahar.muscleandmindapp"
        minSdk = 24
        targetSdk = 35 // compileSdk ile aynı olması önerilir
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }

}

dependencies {

    // AndroidX Core ve Lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose) // collectAsStateWithLifecycle
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Compose
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom)) // Compose BOM
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended) // Material Icons

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx) // Coroutine desteği için
    ksp(libs.androidx.room.compiler) // KSP kullanılıyor
    // annotationProcessor(libs.androidx.room.compiler) // KSP varken bu kaldırılmalı

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Navigation
    implementation(libs.androidx.navigation.compose)
    // İsteğe bağlı diğer navigation modülleri (kullanılıyorsa kalabilir)
    // implementation(libs.androidx.navigation.fragment)
    // implementation(libs.androidx.navigation.ui)
    // implementation(libs.androidx.navigation.dynamic.features.fragment)

    // Kotlinx Serialization
    implementation(libs.kotlinx.serialization.json) // Versiyonu toml'dan alır (1.6.3)

    // Firebase
    implementation(platform(libs.firebase.bom)) // Firebase BOM
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth.ktx) // Sadece KTX versiyonu
    implementation("com.google.firebase:firebase-firestore-ktx") // Firestore KTX (alias yoksa direkt)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // Test Bağımlılıkları
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom)) // Compose test için BOM
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    // İsteğe bağlı diğer test bağımlılıkları
    // testImplementation(libs.androidx.room.testing)
    // androidTestImplementation(libs.androidx.navigation.testing)

}