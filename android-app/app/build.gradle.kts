plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    
    // 🎯 최신 WhatapAndroidPlugin - 제품 필수 구성요소 (비활성화 금지)
    id("io.whatap.plugin") version "1.0.0"
}

// 🎯 WhatapAndroidPlugin - 모든 기능 자동 활성화

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
        
        // WhatAp 서버 설정 - 한 곳에서 관리
        buildConfigField("String", "WHATAP_SERVER_URL", "\"https://rumote.whatap-mobile-agent.io/m\"")
        buildConfigField("int", "WHATAP_PCODE", "3447")
        buildConfigField("String", "WHATAP_PROJECT_KEY", "\"x43bn212o2cou-z5207095h6tmkj-z3k5tbqb529h1q\"")
        buildConfigField("String", "WHATAP_PROXY_SERVER", "\"http://192.168.1.73:8080\"")
    }
    
    compileOptions {
        // Core library desugaring 활성화
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
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
        buildConfig = true
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

    // 🔧 개별 AAR 방식 (안정적) - Event attributes 수정사항 포함
    implementation(files(
        "libs/api-debug.aar",
        "libs/core-debug.aar", 
        "libs/whatap-logger-debug.aar",
        "libs/jsonparser-debug.aar",
        "libs/session-debug.aar",
        "libs/sdk-debug.aar",
        "libs/agent-debug.aar",
        "libs/common-api-debug.aar",
        "libs/activity-debug.aar",
        "libs/fragment-debug.aar",
        "libs/screengroup-debug.aar",
        "libs/webview-debug.aar",
        "libs/network-debug.aar",
        "libs/crash-debug.aar",
        "libs/anr-debug.aar",
        "libs/userlog-debug.aar",
        "libs/stacktrace-debug.aar",
        "libs/extra-debug.aar",
        "libs/exporter-debug.aar",  // 🎯 수정된 Event attributes 포함
        "libs/cpu-debug.aar",
        "libs/memory-debug.aar",
        "libs/temperature-debug.aar",
        "libs/diskbuffering-debug.aar",  // 🔧 디스크 버퍼링 추가
        "libs/okhttp-debug.aar",  // 🌐 OkHttp 네트워크 수집
        "libs/volley-debug.aar",
        "libs/httpclient-debug.aar",
        "libs/httpurlconnection-debug.aar"
    ))
    
    // Core library desugaring (2.1.2 이상 필요)
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.2")
    
    // 필수 의존성
    implementation("androidx.core:core:1.10.1")
}