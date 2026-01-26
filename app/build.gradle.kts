import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.dxtr.routini"
    compileSdk = 36

    buildFeatures {
        viewBinding = false
        dataBinding = false
        compose = true
        buildConfig = true
    }

    val localProperties = Properties()
    rootProject.file("local.properties").let { file ->
        if (file.exists()) {
            file.inputStream().use { localProperties.load(it) }
        }
    }

    defaultConfig {
        applicationId = "com.dxtr.routini"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "BUY_ME_A_COFFEE_URL", "\"${localProperties.getProperty("BUY_ME_A_COFFEE_URL") ?: ""}\"")
        buildConfigField("String", "USDT_ADDRESS", "\"${localProperties.getProperty("USDT_ADDRESS") ?: ""}\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
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

    kotlinOptions {
        jvmTarget = "11"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// Map for the version code that gives each ABI a value.
val abiCodes = mapOf("armeabi-v7a" to 1, "arm64-v8a" to 2, "x86" to 3, "x86_64" to 4)

// ANDROID_APP specific: Update version code for each ABI split
android.applicationVariants.all {
    outputs.forEach { output ->
        val outputImpl = output as com.android.build.gradle.internal.api.ApkVariantOutputImpl
        val abi = outputImpl.getFilter("ABI") // Use the casted object to access the internal API
        if (abi != null) {
            val abiCode = abiCodes[abi] ?: 0
            outputImpl.versionCodeOverride = abiCode * 1000 + versionCode
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("com.google.android.material:material:1.13.0")
    implementation("com.google.code.gson:gson:2.10.1")

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.places)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.compose.ui.text)
    ksp(libs.androidx.room.compiler)

    // Removed unused View-based dependencies
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}