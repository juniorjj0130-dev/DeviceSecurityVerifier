plugins {
    alias(libs.plugins.android.application)
    // Desativado o plugin do Compose para melhorar o desempenho no Android Go
}

android {
    namespace = "com.labredteam.devicesecurityverifier"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.labredteam.devicesecurityverifier"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true // Ativado para reduzir o tamanho do APK
            isShrinkResources = true
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
        compose = false // Desativado para economizar RAM
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    // Versão mais estável para dispositivos de baixo custo
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Dependências essenciais
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
