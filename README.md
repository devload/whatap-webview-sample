# WhaTap WebView Sample

Android WebView 앱과 WhaTap APM 통합 샘플 프로젝트입니다. WebView ScreenGroup 수집, 10초 자동 리로드, HttpLogExporter/HttpSpanExporter 로깅을 포함합니다.

## 🚀 프로젝트 구조

```
whatap-webview-sample/
├── android-app/        # Android WebView 애플리케이션
│   ├── app/
│   ├── gradle/
│   └── build.gradle.kts
├── web-server/         # WebView에서 로드할 웹 페이지
│   ├── index.html
│   ├── whatap-browser-agent.js
│   ├── test_xhr.py
│   └── Caddyfile
└── README.md
```

## 📱 Android 앱 기능

### 주요 기능
- **WebView 통합**: WhatapWebviewBridge를 통한 WebView 모니터링
- **10초 자동 리로드**: LaunchedEffect를 사용한 주기적 페이지 리로드
- **ScreenGroup 추적**: Activity/Fragment/WebView Chain 관리
- **상세 로깅**: HttpLogExporter/HttpSpanExporter 디버그 로그
- **QAFileLogger**: 파일 기반 로그 수집

### 기술 스택
- Kotlin
- Jetpack Compose
- WhatapAgent BOM AAR
- Android API 35 (compileSdk)
- Gradle 8.11.1

## 🌐 웹 서버 설정

### Caddy 서버 실행
```bash
cd web-server
caddy run --config Caddyfile
```

### 웹 페이지 기능
- WhaTap Browser Agent 통합
- XHR/Fetch 테스트 버튼
- 페이지 로드 시 자동 XHR 요청
- WebView Bridge 지원

## 🛠️ 설치 및 실행

### 사전 요구사항
- Android Studio Arctic Fox 이상
- JDK 17
- Android SDK (API Level 35)
- Caddy 웹 서버

### Android 앱 빌드 및 실행

1. **프로젝트 클론**
```bash
git clone https://github.com/yourusername/whatap-webview-sample.git
cd whatap-webview-sample/android-app
```

2. **WhaTap Agent 설정**
`app/src/main/java/io/whatap/webview/sample/App.java` 파일에서 프로젝트 키 설정:
```java
String projectKey = "YOUR-PROJECT-KEY"; // WhaTap 프로젝트 키로 변경
```

3. **IP 주소 설정**
`MainActivity.kt`에서 웹 서버 IP 주소 변경:
```kotlin
val defaultUrl = "http://YOUR-IP:18000/" // 실제 IP로 변경
```

4. **앱 빌드 및 설치**
```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 웹 서버 실행

1. **웹 서버 디렉토리로 이동**
```bash
cd web-server
```

2. **Caddyfile 수정**
```
:18000 {
    root * /absolute/path/to/web-server
    file_server
    
    header {
        Access-Control-Allow-Origin *
        Access-Control-Allow-Methods "GET, POST, PUT, DELETE, OPTIONS"
        Access-Control-Allow-Headers *
    }
}
```

3. **Caddy 실행**
```bash
caddy run
```

## 📊 QA 테스트 및 로그 수집

### QAFileLogger 로그 위치
```
/storage/emulated/0/Android/data/io.whatap.webview.sample/files/WhatapQALogs/
```

### 로그 추출
```bash
adb pull /storage/emulated/0/Android/data/io.whatap.webview.sample/files/WhatapQALogs/
```

### 주요 로그 확인 포인트
- **HttpLogExporter**: `name="userlog"` 확인
- **HttpSpanExporter**: `name="AppStart"`, `name="Created"` 확인
- **ScreenGroup**: Chain 시작/종료 로그
- **WebView 이벤트**: 페이지 로드 시작/완료

## 🔧 설정 변경

### 자동 리로드 간격 변경
`MainActivity.kt`:
```kotlin
const val RELOAD_INTERVAL_MS = 10000L // 밀리초 단위
```

### WhaTap 서버 URL 변경
`App.java`:
```java
.setServerUrl("https://api.whatap.io") // 기본값
```

## 📝 알려진 이슈

1. **샤오미 디바이스**: SIM 카드 없이 USB 디버깅 제한
2. **WebView 로그**: QAFileLogger에 WebView 이벤트가 기록되지 않을 수 있음

## 🤝 기여하기

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 라이선스

이 프로젝트는 MIT 라이선스 하에 배포됩니다.

## 👥 팀

- Android 개발: WhatapAndroidAgent 팀
- QA 테스트: android-tester
- 프로젝트 관리: pm

## 📞 문의

- WhaTap 기술 지원: support@whatap.io
- 프로젝트 이슈: GitHub Issues