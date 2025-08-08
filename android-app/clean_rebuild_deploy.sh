#!/bin/bash

set -e  # 에러 시 즉시 종료

echo "🚀 완전 클린 빌드 및 배포 스크립트 시작"
echo "======================================="

# 1. 샘플 앱 libs 폴더 완전 삭제 (플러그인 제외)
echo "🗑️  1단계: 샘플 앱 libs 폴더 정리 중..."
cd /Users/devload/whatap/android_workspace/whatap-webview-sample/android-app

# 플러그인을 제외한 모든 AAR 파일 삭제
find app/libs -name "*.aar" ! -name "*plugin*" -delete
echo "   ✅ 기존 AAR 파일들 삭제 완료 (플러그인 제외)"

# Gradle 캐시 삭제
rm -rf .gradle/
rm -rf app/build/
echo "   ✅ 샘플 앱 Gradle 캐시 삭제 완료"

# 2. AndroidAgent 라이브러리 완전 클린 빌드
echo "🧹 2단계: AndroidAgent 라이브러리 완전 클린..."
cd /Users/devload/whatap/android_workspace/androidAgent

# Gradle 캐시 및 빌드 디렉터리 삭제
rm -rf .gradle/
rm -rf build/
find . -name "build" -type d -exec rm -rf {} + 2>/dev/null || true
echo "   ✅ AndroidAgent 전체 빌드 캐시 삭제 완료"

# Gradle daemon 중지
./gradlew --stop
echo "   ✅ Gradle daemon 중지 완료"

# 3. 모든 필요한 AAR 모듈들 빌드
echo "🔨 3단계: 필요한 모든 AAR 모듈 빌드..."
./gradlew clean
./gradlew assemble
echo "   ✅ 모든 모듈 빌드 완료"

# 4. 새로운 AAR들을 샘플 앱으로 복사
echo "📦 4단계: 모든 AAR 파일 복사..."
SAMPLE_LIBS="/Users/devload/whatap/android_workspace/whatap-webview-sample/android-app/app/libs/"

# 복사할 AAR 모듈 목록
declare -a AAR_MODULES=(
    "api/build/outputs/aar/api-debug.aar"
    "core/build/outputs/aar/core-debug.aar" 
    "whatap-logger/build/outputs/aar/whatap-logger-debug.aar"
    "jsonparser/build/outputs/aar/jsonparser-debug.aar"
    "session/build/outputs/aar/session-debug.aar"
    "sdk/build/outputs/aar/sdk-debug.aar"
    "agent/build/outputs/aar/agent-debug.aar"
    "instrumentation/common-api/build/outputs/aar/common-api-debug.aar"
    "instrumentation/activity/build/outputs/aar/activity-debug.aar"
    "instrumentation/fragment/build/outputs/aar/fragment-debug.aar"
    "instrumentation/screengroup/build/outputs/aar/screengroup-debug.aar"
    "instrumentation/webview/build/outputs/aar/webview-debug.aar"
    "instrumentation/network/build/outputs/aar/network-debug.aar"
    "instrumentation/crash/build/outputs/aar/crash-debug.aar"
    "instrumentation/anr/build/outputs/aar/anr-debug.aar"
    "instrumentation/userlog/build/outputs/aar/userlog-debug.aar"
    "instrumentation/stacktrace/build/outputs/aar/stacktrace-debug.aar"
    "instrumentation/extra/build/outputs/aar/extra-debug.aar"
    "exporter/build/outputs/aar/exporter-debug.aar"
    "instrumentation/resource/cpu/build/outputs/aar/cpu-debug.aar"
    "instrumentation/resource/memory/build/outputs/aar/memory-debug.aar"
    "instrumentation/resource/temperature/build/outputs/aar/temperature-debug.aar"
    "diskbuffering/build/outputs/aar/diskbuffering-debug.aar"
    "instrumentationExtension/okhttp/build/outputs/aar/okhttp-debug.aar"
    "instrumentationExtension/volley/build/outputs/aar/volley-debug.aar"
    "instrumentationExtension/httpclient/build/outputs/aar/httpclient-debug.aar"
    "instrumentationExtension/httpurlconnection/build/outputs/aar/httpurlconnection-debug.aar"
)

# AAR 파일들 복사
COPIED_COUNT=0
for aar_path in "${AAR_MODULES[@]}"; do
    if [ -f "$aar_path" ]; then
        cp "$aar_path" "$SAMPLE_LIBS"
        AAR_NAME=$(basename "$aar_path")
        echo "   ✅ $AAR_NAME 복사 완료"
        ((COPIED_COUNT++))
    else
        echo "   ⚠️  $aar_path 파일 없음 (건너뜀)"
    fi
done

echo "   📦 총 $COPIED_COUNT 개 AAR 파일 복사 완료"

# screengroup AAR이 제대로 복사되었는지 특별히 확인
SCREENGROUP_AAR="$SAMPLE_LIBS/screengroup-debug.aar"
if [ -f "$SCREENGROUP_AAR" ]; then
    AAR_SIZE=$(ls -lh "$SCREENGROUP_AAR" | awk '{print $5}')
    echo "   🎯 screengroup AAR 확인: 크기 $AAR_SIZE"
else
    echo "   ❌ screengroup AAR 복사 실패!"
    exit 1
fi

# 5. 샘플 앱 클린 빌드
echo "🔨 5단계: 샘플 앱 클린 빌드..."
cd /Users/devload/whatap/android_workspace/whatap-webview-sample/android-app

./gradlew --stop  # 다시 한번 daemon 중지
./gradlew clean
./gradlew assembleDebug
echo "   ✅ 샘플 앱 빌드 완료"

# 6. 디바이스에 설치
echo "📱 6단계: 디바이스에 앱 설치..."
APK_FILE="app/build/outputs/apk/debug/app-debug.apk"

if [ -f "$APK_FILE" ]; then
    APK_SIZE=$(ls -lh "$APK_FILE" | awk '{print $5}')
    echo "   📦 APK 크기: $APK_SIZE"
    
    # 연결된 디바이스에 설치
    for device in RFCX919P8ZF 3062821163005VC emulator-5554; do
        if adb -s $device get-state >/dev/null 2>&1; then
            echo "   📱 Installing on device: $device"
            if adb -s $device install -r "$APK_FILE" >/dev/null 2>&1; then
                echo "   ✅ $device 설치 완료"
            else
                echo "   ⚠️  $device 설치 실패 (연결 확인 필요)"
            fi
        else
            echo "   ⏭️  $device 연결되지 않음 (건너뜀)"
        fi
    done
else
    echo "   ❌ APK 파일을 찾을 수 없습니다!"
    exit 1
fi

# 7. 앱 실행 (첫 번째 연결된 디바이스에서)
echo "🚀 7단계: 앱 실행..."
FIRST_DEVICE="RFCX919P8ZF"
if adb -s $FIRST_DEVICE get-state >/dev/null 2>&1; then
    adb -s $FIRST_DEVICE shell am start -n io.whatap.webview.sample/.MainActivity
    echo "   ✅ 앱이 $FIRST_DEVICE 에서 실행되었습니다"
    
    echo ""
    echo "🎉 완전 클린 빌드 및 배포 완료!"
    echo "======================================="
    echo "다음 명령어로 로그를 확인하세요:"
    echo "adb -s $FIRST_DEVICE logcat -s \"ChainView:*,WebViewSample:*,TestFragment:*\""
else
    echo "   ⚠️  기본 디바이스 연결 안됨. 수동으로 앱을 실행하세요."
fi

echo ""
echo "✨ 스크립트 실행 완료!"