# WhaTap WebView Sample App

Android WebView 샘플 애플리케이션으로 WhaTap Agent의 WebView 통합 기능을 테스트합니다. Long Hash TraceId, JavaScript 브리지, 실시간 로그 모니터링, 백그라운드 HTTP 요청 등 고급 기능을 포함합니다.

## 🚀 최신 업데이트 (v2.0)

### 새로운 주요 기능
- **Long Hash TraceId/SpanId**: UUID 대신 Long 숫자 형식 사용
- **JavaScript 브리지 로깅**: generateUUID, pageLoad, webVitals 함수 호출 로그
- **실시간 로그 모니터링**: 앱 내에서 Export 로그 실시간 표시 (화면 40% 영역)
- **백그라운드 HTTP 요청**: 5초마다 자동 네트워크 요청으로 Agent 테스트
- **WebView traceId 처리**: WebView 이벤트는 taskId(UUID), 일반 이벤트는 Long hash 사용

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

### 핵심 기능
- **Long Hash ID 생성**: `DefaultIdGenerator`에서 `Math.abs(random.nextLong())` 사용
- **JavaScript 브리지**: `generateUUID()`, `pageLoad(data, uuid)`, `webVitals(data)` 함수
- **실시간 로그 UI**: HttpSpanExporter와 브리지 로그를 앱 내에서 실시간 표시
- **백그라운드 네트워크**: 5초마다 HTTP 요청 (httpbin.org, jsonplaceholder 등)
- **WebView 특화 traceId**: WebView 이벤트만 taskId(UUID) 사용, 나머지는 Long hash
- **ScreenGroup Chain**: Activity → Fragment → WebView 전환 추적
- **10초 자동 리로드**: WebView 페이지 주기적 리로드

### 기술 스택
- **언어**: Kotlin + Java
- **UI**: Jetpack Compose (실시간 로그 LazyColumn)
- **WebView**: WhatapWebViewClient + WhatapWebviewBridge
- **비동기**: Coroutines + lifecycleScope
- **Agent**: WhatapAgent BOM AAR (with-logging 버전)
- **API**: Android 35, minSdk 24

### 실시간 모니터링
- **로그 수집**: `logcat -v brief`로 HttpSpanExporter 로그 수집
- **UI 표시**: 화면 하단 40% 영역에 실시간 로그 (100개 버퍼)
- **자동 스크롤**: 새 로그 추가 시 자동으로 최신 로그로 스크롤
- **필터링**: HttpSpanExporter, Bridge 관련 로그만 표시

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

---

## 🍎 iOS 구현 가이드

### 핵심 구현 포인트

#### 1. Long Hash TraceId/SpanId 생성
```swift
// Swift 구현 예시
func generateTraceId() -> String {
    let hashValue = Int64.random(in: Int64.min...Int64.max)
    return String(abs(hashValue))
}

func generateSpanId() -> String {
    let hashValue = Int64.random(in: Int64.min...Int64.max)
    return String(abs(hashValue))
}
```

#### 2. WKWebView JavaScript 브리지
```swift
// WhatapWKScriptMessageHandler.swift
class WhatapWKScriptMessageHandler: NSObject, WKScriptMessageHandler {
    func userContentController(_ userContentController: WKUserContentController, 
                             didReceive message: WKScriptMessage) {
        
        switch message.name {
        case "generateUUID":
            let uuid = UUID().uuidString
            let script = "window.whatap_bridge_callback('\(uuid)')"
            message.webView?.evaluateJavaScript(script)
            
        case "pageLoad":
            if let body = message.body as? [String: Any] {
                let data = body["data"] as? String ?? ""
                let uuid = body["uuid"] as? String ?? ""
                handlePageLoad(data: data, uuid: uuid)
            }
            
        case "webVitals":
            if let data = message.body as? [String: Any] {
                sendWebVitals(data: data)
            }
        }
    }
}
```

#### 3. 실시간 로그 모니터링 UI (SwiftUI)
```swift
// LogMonitorView.swift
struct LogMonitorView: View {
    @StateObject private var logCollector = LogCollector()
    
    var body: some View {
        VStack {
            Text("📡 Export 로그 & 🌏 WebView 브리지 (실시간)")
                .font(.system(size: 14, design: .monospaced))
                .foregroundColor(.green)
            
            List(logCollector.logs, id: \.self) { log in
                Text(log)
                    .font(.system(size: 11, design: .monospaced))
                    .foregroundColor(.white)
                    .padding(.vertical, 1.5)
            }
            .background(Color.black)
            .frame(maxHeight: .infinity)
        }
    }
}

// LogCollector.swift
class LogCollector: ObservableObject {
    @Published var logs: [String] = []
    private let maxLogs = 100
    
    func addLog(_ message: String) {
        let timestamp = DateFormatter.logTimestamp.string(from: Date())
        let logEntry = "[\(timestamp)] \(message)"
        
        DispatchQueue.main.async {
            self.logs.append(logEntry)
            if self.logs.count > self.maxLogs {
                self.logs.removeFirst(self.logs.count - self.maxLogs)
            }
        }
    }
}
```

#### 4. 백그라운드 HTTP 요청
```swift
// BackgroundNetworkManager.swift
class BackgroundNetworkManager {
    private let testURLs = [
        "https://httpbin.org/get",
        "https://jsonplaceholder.typicode.com/posts/1",
        "https://api.github.com/zen",
        "https://httpbin.org/uuid",
        "https://httpbin.org/delay/1"
    ]
    
    private var timer: Timer?
    private var requestCount = 0
    
    func startBackgroundRequests() {
        timer = Timer.scheduledTimer(withTimeInterval: 5.0, repeats: true) { [weak self] _ in
            self?.makeHTTPRequest()
        }
    }
    
    private func makeHTTPRequest() {
        guard let url = URL(string: testURLs[requestCount % testURLs.count]) else { return }
        requestCount += 1
        
        let task = URLSession.shared.dataTask(with: url) { data, response, error in
            DispatchQueue.main.async {
                if let error = error {
                    LogCollector.shared.addLog("❌ HTTP 요청 실패: \(error.localizedDescription)")
                } else if let data = data {
                    LogCollector.shared.addLog("✅ HTTP 응답 #\(self.requestCount) 수신 (\(data.count) bytes)")
                }
            }
        }
        task.resume()
        
        LogCollector.shared.addLog("🔗 HTTP 요청 #\(requestCount): \(url.host ?? "unknown")")
    }
}
```

### iOS vs Android 구현 차이점

| 기능 | Android | iOS |
|------|---------|-----|
| **ID 생성** | `random.nextLong()` | `Int64.random()` |
| **WebView** | `WebView` + JavaScript Interface | `WKWebView` + `WKScriptMessageHandler` |
| **로그 수집** | `logcat` 명령어 | `OSLog` + 파일 로깅 |
| **HTTP 요청** | `HttpURLConnection` | `URLSession` |
| **비동기 처리** | Coroutines | async/await 또는 DispatchQueue |
| **UI** | Jetpack Compose | SwiftUI 또는 UIKit |
| **백그라운드** | 제한적 지원 | Background App Refresh |

### iOS 구현 체크리스트

#### 필수 구현
- [ ] Long hash 형식 traceId/spanId 생성기
- [ ] WKWebView JavaScript 브리지 (generateUUID, pageLoad, webVitals)
- [ ] 실시간 로그 수집 및 UI 표시
- [ ] 5초 간격 백그라운드 HTTP 요청
- [ ] WebView와 일반 이벤트의 traceId 구분 처리
- [ ] ScreenGroup/Span 체인 관리 시스템

#### 권장 구현
- [ ] 메모리 사용량 모니터링 및 제한
- [ ] 네트워크 상태 확인 및 오류 처리
- [ ] 로그 레벨별 필터링 시스템
- [ ] 설정 UI (서버 URL, 요청 간격 등)
- [ ] Background App Refresh 상태 확인

#### 테스트 포인트
- [ ] traceId 형식 검증 (Long 숫자 vs UUID 문자열)
- [ ] JavaScript 브리지 함수 호출 성공률
- [ ] 실시간 로그 표시 성능 (100개 버퍼)
- [ ] 메모리 누수 없음 확인 (Instruments)
- [ ] 백그라운드 동작 제한 준수

---

## 📞 문의

- WhaTap 기술 지원: support@whatap.io
- 프로젝트 이슈: GitHub Issues
- **Android 구현**: 완료 (이 레포지토리)
- **iOS 구현**: 위 가이드 참고하여 구현 진행