plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.erdinc1911.karar"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.erdinc1911.karar"
        minSdk = 26
        targetSdk = 35
        versionCode = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()
        versionName = System.getenv("KARAR_SURUM") ?: "test"
    }
    signingConfigs {
        // YALNIZ TEST İMZASI: herkese açık depodadır. Google Play sürümü ayrı ve gizli bir anahtarla imzalanacak.
        create("test") {
            storeFile = file("../imza/test-imza.jks")
            storePassword = "karar-test-2026"
            keyAlias = "karar"
            keyPassword = "karar-test-2026"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("test")
        }
        getByName("debug") { signingConfig = signingConfigs.getByName("test") }
    }
    androidResources { noCompress += listOf("onnx", "wasm") }   // büyük dosyalar sıkıştırılmadan: doğrudan okunur
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.webkit:webkit:1.12.1")
}
