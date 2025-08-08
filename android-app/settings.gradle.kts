pluginManagement {
    repositories {
        // 🎯 Local Maven for WhatapAgent plugin
        mavenLocal()
        // 🎯 Local JAR file for WhatapAgent plugin
        flatDir {
            dirs("app/lib")
        }
        // 🎯 WhatAp Nexus Repository for plugins
        maven {
            url = uri("http://192.168.1.73:8081/repository/maven-releases/")
            credentials {
                username = "admin"
                password = "admin1234"
            }
            isAllowInsecureProtocol = true
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
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        // 🎯 WhatAp Nexus Repository 추가
        maven {
            url = uri("http://192.168.1.73:8081/repository/maven-releases/")
            credentials {
                username = "admin"
                password = "admin1234"
            }
            isAllowInsecureProtocol = true
        }
        flatDir {
            dirs ("app/lib")
        }
    }
}

rootProject.name = "webview"
include(":app")
 