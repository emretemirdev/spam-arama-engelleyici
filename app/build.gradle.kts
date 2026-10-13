import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Yayın imzası: proje kökündeki keystore.properties (git'e girmez) okunur.
// Dosya yoksa release derlemesi imzasız kalır; debug derlemeleri etkilenmez.
val keystoreProps = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "tr.com.covolt.aramaengelleyici"
    compileSdk = 36

    defaultConfig {
        // DİKKAT: applicationId Play'e yüklendikten sonra değiştirilemez.
        applicationId = "tr.com.covolt.aramaengelleyici"
        // Arama filtreleme (CallScreeningService rolü) Android 10 = API 29 ile geldi.
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        if (keystoreProps.getProperty("storeFile") != null) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            signingConfigs.findByName("release")?.let { signingConfig = it }
            isMinifyEnabled = true
            isShrinkResources = true
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

    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.08.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.core:core-ktx:1.16.0")
    // Billing kütüphanesi eski bir fragment sürümünü getiriyor; Activity Result API için güncel sürüm.
    implementation("androidx.fragment:fragment-ktx:1.8.6")

    // Google Play Billing: abonelik ve ömür boyu Pro satın alımı.
    implementation("com.android.billingclient:billing:8.0.0")

    testImplementation("junit:junit:4.13.2")
}
