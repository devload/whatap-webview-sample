pluginManagement {
    repositories {
        // 🎯 Local Maven for WhatapAgent plugin
        mavenLocal()
        // 🎯 Local JAR file for WhatapAgent plugin
        flatDir {
            dirs("libs")
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
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "io.whatap.plugin") {
                useModule("io.whatap:WhatapAndroidPlugin:${requested.version}")
            }
        }
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
 