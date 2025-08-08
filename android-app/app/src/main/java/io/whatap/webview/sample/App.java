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
            
            // QA 파일 로깅 (기본 AAR에서는 지원하지 않음)
            Log.i(TAG, "ℹ️ 기본 AAR - 파일 로깅 기능 없음");
        } catch (Exception e) {
            Log.w(TAG, "⚠️ WLogger 초기화 실패: " + e.getMessage());
        }

        // 2. WhatapAgent 초기화 (BuildConfig에서 서버 설정 읽기)
        String serverUrl = BuildConfig.WHATAP_SERVER_URL;
        int pCode = BuildConfig.WHATAP_PCODE;
        String projectKey = BuildConfig.WHATAP_PROJECT_KEY;
        
        try {
            WhatapAgent.Builder.newBuilder()
                    .setServerUrl(serverUrl)
                    .setPCode(pCode)
                    .setProjectKey(projectKey)
                    .build(this);
            
            Log.i(TAG, "🚀 WhatapAgent 초기화 성공 (RUMOTE 서버)");
            Log.i(TAG, "🌐 서버 URL: " + serverUrl);
            Log.i(TAG, "📊 프로젝트 코드: " + pCode);
            Log.i(TAG, "🔑 프로젝트 키: " + projectKey.substring(0, 10) + "...");
            Log.i(TAG, "🔢 traceId/spanId 형식: Long 숫자");
            Log.i(TAG, "📊 각 screengroup 이벤트마다 고유한 traceId 사용");
            Log.i(TAG, "✅ WhatapAndroidPlugin이 Fragment instrumentation 자동 처리");
            
            // ScreenGroup은 라이브러리에서 자동으로 처리됨
            Log.i(TAG, "ℹ️ ScreenGroup은 Activity 생명주기에 따라 자동으로 관리됩니다");
            
        } catch (Exception e) {
            Log.e(TAG, "❌ WhatapAgent 초기화 실패: " + e.getMessage());
        }
    }
}
