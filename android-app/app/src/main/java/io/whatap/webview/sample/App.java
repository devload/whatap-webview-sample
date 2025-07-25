package io.whatap.webview.sample;

import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;

import io.whatap.android.agent.WhatapAgent;
import io.whatap.android.logger.common.WLoggerFactory;

public class App extends Application {
    private static final String TAG = "WhatapSampleApp";

    @Override
    public void onCreate() {
        super.onCreate();

        // 1. QA 파일 로깅 활성화 시도 (통합 버전)
        try {
            WLoggerFactory.initialize(this);
            WLoggerFactory.enableFileLogging(this);
            Log.i(TAG, "✅ QA 파일 로깅 활성화 성공 (통합 버전)");
        } catch (Exception e) {
            Log.w(TAG, "⚠️ QA 파일 로깅 활성화 실패 (API 미지원 가능): " + e.getMessage());
        }

        // 2. WhatapAgent 초기화 (통합 버전 - 모든 PR 포함)
        String defaultUrl = "https://rumote.whatap-mobile-agent.io/m";
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        String serverUrl = prefs.getString("server_url", defaultUrl);

        try {
            WhatapAgent.Builder.newBuilder()
                    .setServerUrl(serverUrl)
                    .setPCode(3447)
                    .setProjectKey("x43bn212o2cou-z5207095h6tmkj-z3k5tbqb529h1q")
                    .build(this);
            
            Log.i(TAG, "🚀 WhatapAgent 초기화 성공 (통합 버전 - PR #7,#8,#9 포함)");
            Log.i(TAG, "🔧 TaskId 더블 대시 문제 해결 적용됨");
            Log.i(TAG, "📱 Android 10+ READ_PHONE_STATE 권한 제거 적용됨");
            Log.i(TAG, "🌐 WebView Task 종료 누락 문제 해결 적용됨");
        } catch (Exception e) {
            Log.e(TAG, "❌ WhatapAgent 초기화 실패: " + e.getMessage());
        }
    }
}
