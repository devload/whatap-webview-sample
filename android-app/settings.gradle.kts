pluginManagement {
    repositories {
        // 🎯 Local Maven for WhatapAgent plugin
        mavenLocal()
        // 🎯 Local JAR file for WhatapAgent plugin
        flatDir {
            dirs("app/lib")
        }
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        flatDir {
            dirs ("app/lib")
        }
    }
}

rootProject.name = "webview"
include(":app")
 