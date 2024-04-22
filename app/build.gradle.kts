// SPDX-FileCopyrightText: 2024 Swarovski-Optik AG & Co KG.
// SPDX-License-Identifier: Apache-2.0

import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

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
        val openapiApiKey = if (project.hasProperty("OPENAPI_API_KEY")) {
            project.property("OPENAPI_API_KEY") as String
        } else {
            val p = Properties()
            p.load(project.rootProject.file("local.properties").reader())
            if (!p.contains("OPENAPI_API_KEY")) throw Exception("Please add 'OPENAPI_API_KEY' property!")
            p.getProperty("OPENAPI_API_KEY")
        }
        debug {
            buildConfigField("String", "OPENAPI_API_KEY", "\"$openapiApiKey\"")
        }
        release {
            buildConfigField("String", "OPENAPI_API_KEY", "\"$openapiApiKey\"")
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
