// SPDX-FileCopyrightText: 2024 Swarovski-Optik AG & Co KG.
// SPDX-License-Identifier: Apache-2.0

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven(url = uri("https://artifacts.swarovskioptik.com/repository/maven-swarovski-optik/"))
    }
}

rootProject.name = "OpenAPIDeveloperExampleApp"
include(":app")
