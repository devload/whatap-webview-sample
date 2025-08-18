buildscript {
    dependencies {
        classpath(files("libs/WhatapAndroidPlugin-1.1.3.jar"))
    }
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

apply(plugin = "io.whatap.plugin")

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
        
        // WhatAp 프로덕션 서버 설정 (디바이스용)
        buildConfigField("int", "WHATAP_PCODE", "3447")
        buildConfigField("String", "WHATAP_PROJECT_KEY", "\"x43bn212o2cou-z5207095h6tmkj-z3k5tbqb529h1q\"")
        buildConfigField("String", "WHATAP_SERVER_URL", "\"https://rumote.whatap-mobile-agent.io/m\"")
        
        // Test configuration flags from gradle.properties
        buildConfigField("boolean", "ENABLE_CRASH_TEST", project.properties["enableCrashTest"]?.toString() ?: "false")
        buildConfigField("boolean", "ENABLE_ANR_TEST", project.properties["enableAnrTest"]?.toString() ?: "false")
    }
    
    buildTypes {
        debug {
            // Debug variant - 디바이스용 서버 사용
            buildConfigField("String", "WHATAP_SERVER_URL", "\"https://rumote.whatap-mobile-agent.io/m\"")
            buildConfigField("String", "VARIANT_TYPE", "\"device\"")
            // applicationIdSuffix 제거하여 동일한 앱으로 관리
            // applicationIdSuffix = ".dev"
            // versionNameSuffix = "-dev"
        }
        release {
            // Release variant - 디바이스용 서버 사용  
            buildConfigField("String", "WHATAP_SERVER_URL", "\"https://rumote.whatap-mobile-agent.io/m\"")
            buildConfigField("String", "VARIANT_TYPE", "\"device\"")
            isMinifyEnabled = false
            // debug signing 사용 (테스트용)
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    
    compileOptions {
        // Core library desugaring 활성화
        isCoreLibraryDesugaringEnabled = true
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

    // ✅ BOM AAR 방식 (내부 원칙 준수) - 모든 모듈 통합 패키지  
    // implementation(files("libs/whatap-agent-bom-complete.aar"))  // TODO: BOM에 getInstance() 메서드 누락
    
    // 🔧 개별 AAR 방식 (호환성) - 빌드 성공 보장
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
        "libs/screengroup-debug.aar",  // ChainView.getInstance() 포함
        "libs/webview-debug.aar",
        "libs/network-debug.aar",
        "libs/crash-debug.aar",
        "libs/anr-debug.aar",
        "libs/userlog-debug.aar",
        "libs/stacktrace-debug.aar",
        "libs/extra-debug.aar",
        "libs/exporter-debug.aar",  // 수정된 Event attributes 포함
        "libs/cpu-debug.aar",
        "libs/memory-debug.aar",
        "libs/temperature-debug.aar",
        "libs/diskbuffering-debug.aar",
        "libs/okhttp-debug.aar",  // 네트워크 instrumentation 수정사항 포함
        "libs/volley-debug.aar",
        "libs/httpclient-debug.aar",
        "libs/httpurlconnection-debug.aar"
    ))
    
    // Core library desugaring (2.1.2 이상 필요)
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.2")
    
    // 필수 의존성
    implementation("androidx.core:core:1.10.1")
}