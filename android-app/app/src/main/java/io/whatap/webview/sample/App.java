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

        // 1. WLogger 초기화 및 디버그 모드 활성화
        try {
            WLoggerFactory.initialize(this);
            WLoggerFactory.setDebug(true);  // 디버그 모드 활성화
            Log.i(TAG, "✅ WLogger 디버그 모드 활성화 - HttpSpanExporter JSON 로그 출력됨");
            
            // QA 파일 로깅도 활성화 시도
            try {
                WLoggerFactory.enableFileLogging(this);
                Log.i(TAG, "✅ QA 파일 로깅도 활성화됨");
            } catch (Exception e) {
                Log.w(TAG, "⚠️ QA 파일 로깅은 지원되지 않음: " + e.getMessage());
            }
        } catch (Exception e) {
            Log.w(TAG, "⚠️ WLogger 초기화 실패: " + e.getMessage());
        }

        // 2. WhatapAgent 초기화 (Long hash traceId/spanId 테스트)
        // For emulator use 10.0.2.2, for real device use actual IP
        String proxyServerUrl = "http://192.168.1.73:8080"; // Real device proxy server
        
        try {
            WhatapAgent.Builder.newBuilder()
                    .setServerUrl(proxyServerUrl)
                    .setPCode(3447)
                    .setProjectKey("test-project-key")
                    .build(this);
            
            Log.i(TAG, "🚀 WhatapAgent 초기화 성공 (Long hash traceId/spanId)");
            Log.i(TAG, "🌐 프록시 서버: " + proxyServerUrl);
            Log.i(TAG, "🔢 traceId/spanId 형식: Long 숫자");
            Log.i(TAG, "📊 각 screengroup 이벤트마다 고유한 traceId 사용");
        } catch (Exception e) {
            Log.e(TAG, "❌ WhatapAgent 초기화 실패: " + e.getMessage());
        }
    }
}
