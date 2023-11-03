plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

android {
    namespace = "com.example.openapideveloperexampleapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.openapideveloperexampleapp"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        val openapi_api_key: String = gradleLocalProperties(rootDir).getProperty("OPENAPI_API_KEY")
        debug {
            buildConfigField("String", "OPENAPI_API_KEY", "\"$openapi_api_key\"")
        }
        release {
            buildConfigField("String", "OPENAPI_API_KEY", "\"$openapi_api_key\"")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.3"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Include the "Swarovski SOComm Outside API / SDK":
    //
    // - From a maven repository
    // implementation("com.swarovskioptik.comm:SOCommOutsideAPI:0.13.0")
    //
    // - From a local copy of the jar/aar files
    implementation(files("../libs/mqtt-client-wrapper-rx-0.1.0.jar"))
    implementation(files("../libs/ResubscribingReplayingShare-0.13.0.jar"))
    implementation(files("../libs/SharedDefinitions-0.13.0.jar"))
    implementation(files("../libs/SOBase-0.13.0.jar"))
    implementation(files("../libs/SOCommMediaClient-0.13.0.aar"))
    implementation(files("../libs/SOCommOutsideAPI-0.13.0.aar"))
    implementation(files("../libs/SOLogger-0.13.0.jar"))
    implementation(files("../libs/WifiMqttApi-0.13.0.aar"))
    implementation(files("../libs/FileTransferManager-0.13.0.aar"))
    implementation("io.reactivex.rxjava2:rxjava:2.2.21")
    implementation("io.reactivex.rxjava2:rxandroid:2.1.1")
    implementation("io.reactivex.rxjava2:rxkotlin:2.4.0")
    implementation("com.jakewharton.rxrelay2:rxrelay:2.1.1")
    implementation("no.nordicsemi.android:ble:2.5.1")
    implementation("org.bouncycastle:bcpkix-jdk15to18:1.70")
    implementation("com.google.protobuf:protobuf-javalite:3.21.1")
}
