// SPDX-FileCopyrightText: 2024 Swarovski-Optik AG & Co KG.
// SPDX-License-Identifier: Apache-2.0

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

    // Include the "Swarovski Optik SO Comm Outside API"
    implementation("com.swarovskioptik.comm:SOCommOutsideAPI:1.0.0")
    implementation("io.reactivex.rxjava2:rxkotlin:2.4.0")
    implementation("io.reactivex.rxjava2:rxandroid:2.1.1")
}
