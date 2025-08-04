plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    
    // 🎯 최신 WhatapAndroidPlugin 활성화
    id("io.whatap.plugin") version "1.0.0"
}

// 🎯 WhatapAndroidPlugin 설정
whatap {
    isEnabled = true
    
    fragment {
        enabled = true
    }
    
    okhttp {
        enabled = true
    }
    
    httpurlconnection {
        enabled = true
    }
    
    httpclient {
        enabled = true
    }
    
    volley {
        enabled = true
    }
}

android {
    namespace = "io.whatap.webview.sample"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.whatap.webview.sample"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }

    lint {
        disable += "NullSafeMutableLiveData"
    }

}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    implementation ("org.mozilla:rhino:1.7.14")
    
    // 🌐 OkHttp for network instrumentation
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // 🎯 lifecycle_state가 추가된 AAR 사용
    implementation(files("lib/whatap-agent-bom-lifecycle.aar"))
    
    // 필수 의존성
    implementation("androidx.core:core:1.10.1")
}