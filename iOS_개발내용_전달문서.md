# iOS 개발팀 전달 문서 - Android WebView 브리지 및 TraceId 시스템 개발 완료

## 📋 개요
Android 팀에서 WebView 브리지 함수 로깅 시스템과 Long hash 형식 traceId 시스템을 개발 완료했습니다. iOS 팀에서도 동일한 시스템을 구현해주시기 바랍니다.

## 🚀 완료된 개발 내용

### 1. **JavaScript 브리지 함수 로깅 시스템**

#### 구현된 브리지 함수들:
```java
@JavascriptInterface
public String generateUUID() {
    String uuid = UUID.randomUUID().toString();
    logger.info("🔥 [Bridge] generateUUID() called - Generated UUID: " + uuid);
    Log.i(TAG, "🔥 [Bridge] generateUUID() called - Generated UUID: " + uuid);
    return uuid;
}

@JavascriptInterface
public void pageLoad(String data, String uuid) {
    logger.info("🔥 [Bridge] pageLoad(data, uuid) called - Data: " + data + " | UUID: " + uuid);
    Log.i(TAG, "🔥 [Bridge] pageLoad(data, uuid) called - Data: " + data + " | UUID: " + uuid);
    // UUID를 taskId로 사용하여 ScreenGroup 업데이트
    ScreenGroupManager.getInstance().setWebViewTaskId(uuid);
}

@JavascriptInterface
public void webVitals(String data) {
    logger.info("🔥 [Bridge] webVitals() called - Data: " + data);
    Log.i(TAG, "🔥 [Bridge] webVitals() called - Data: " + data);
    addDataToQueue("/v2/webVitals", data);
}
```

#### iOS 구현 요청사항:
- JavaScript 브리지 함수 호출 시 상세 로깅 추가
- `generateUUID()` 호출 시 생성된 UUID 로깅
- `pageLoad(data, uuid)` 호출 시 데이터와 UUID 파라미터 모두 로깅
- `webVitals()` 호출 시 전송 데이터 로깅

### 2. **Long Hash 형식 TraceId/SpanId 시스템**

#### Android 구현:
```java
@Override
public String generateTraceId() {
    long hashValue = random.nextLong();
    return String.valueOf(Math.abs(hashValue));
}

@Override  
public String generateSpanId() {
    long hashValue = random.nextLong();
    return String.valueOf(Math.abs(hashValue));
}
```

#### iOS 구현 요청사항:
- 기존 UUID 형식 → Long 숫자 형식으로 변경
- `Math.abs(random.nextLong())` 방식으로 고유한 숫자 ID 생성
- 각 screengroup 이벤트마다 고유한 traceId 사용
- **중요**: 기존 traceId 중복 문제 해결이 주목적

### 3. **WebView TraceId 통합 시스템**

#### Android 구현 로직:
```java
// WebView 이벤트에서 UUID를 traceId로 사용
String traceIdValue;
if (currentWebViewTaskId != null && !currentWebViewTaskId.isEmpty() && 
    (screenName.contains("WebView") || cleanedTaskId.contains("webview"))) {
    traceIdValue = currentWebViewTaskId;  // WebView는 UUID를 traceId로 사용
} else {
    traceIdValue = span.getTraceId();  // 일반적인 경우 span의 traceId 사용
}
eventAttrs.put("trace_id", traceIdValue);
```

#### iOS 구현 요청사항:
- WebView 페이지에서 생성된 UUID를 taskId로 사용
- WebView 이벤트 전송 시 UUID를 trace_id로 설정
- JavaScript와 iOS 네이티브 간 traceId 동기화
- WebView/일반 이벤트 구분하여 traceId 설정

### 4. **API 엔드포인트 수정**

#### 변경 내용:
- webVitals: `/webVitals` → `/v2/webVitals`
- serverUrl에 이미 `/m` 접두사가 포함되어 있으므로 중복 방지

#### iOS 구현 요청사항:
- webVitals API 엔드포인트를 `/v2/webVitals`로 수정
- 다른 API들은 기존 유지
- serverUrl 정책 확인 후 적용

## 🧪 테스트 검증 완료 사항

### **Android 실제 디바이스 테스트 결과:**

#### 디바이스 1 (Vivo - 3062821163005VC):
```
07-31 18:27:39.460 I whatap_bridge: 🔥 [Bridge] generateUUID() called - Generated UUID: 1e7163f5-68df-491e-9fd3-963e4aaa940a
07-31 18:27:39.466 I whatap_bridge: 🔥 [Bridge] pageLoad(data, uuid) called - Data: {...} | UUID: 1e7163f5-68df-491e-9fd3-963e4aaa940a
```

#### 디바이스 2 (Samsung - RFCX919P8ZF):
```
07-31 18:27:38.465 I whatap_bridge: 🔥 [Bridge] generateUUID() called - Generated UUID: 9b325569-1217-4d96-a8b6-b3fa6bd30124
07-31 18:27:38.467 I whatap_bridge: 🔥 [Bridge] pageLoad(data, uuid) called - Data: {...} | UUID: 9b325569-1217-4d96-a8b6-b3fa6bd30124
```

### **iOS 테스트 요청사항:**
- [ ] generateUUID() 함수 호출 로깅 검증
- [ ] pageLoad(data, uuid) 두 번째 파라미터 전달 검증
- [ ] webVitals() 함수 호출 로깅 검증
- [ ] Long hash traceId 생성 검증
- [ ] WebView 이벤트의 trace_id 설정 검증

## 📱 실시간 모니터링 UI (선택사항)

### Android 구현:
- WebView 하단에 Export 로그 실시간 표시
- HttpSpanExporter 로그 자동 수집 및 표시
- 최근 50개 로그 유지하는 순환 버퍼

### iOS 구현 제안:
- 디버그 모드에서 WebView 로그 실시간 모니터링
- Console 로그로 브리지 함수 호출 상태 확인
- 필요시 UI 영역에 로그 표시 기능 추가

## 🔗 참고 자료

### GitHub PR:
- **PR 링크**: https://github.com/devload/whatap-webview-sample/pull/1
- **브랜치**: `feature/webview-bridge-logging-and-long-hash-traceid`

### 주요 변경 파일들:
1. **DefaultIdGenerator.java** - Long hash traceId/spanId 생성
2. **WhatapWebviewBridge.java** - JavaScript 브리지 함수 로깅  
3. **ScreenGroupManager.java** - WebView traceId 통합 로직
4. **MainActivity.kt** - 실시간 로그 모니터링 UI

## ⚠️ 중요 포인트

### **1. traceId 중복 방지가 핵심 목적**
- 기존 시스템에서 screengroup 이벤트들이 동일한 traceId를 사용하는 문제 해결
- 각 이벤트마다 고유한 traceId 보장

### **2. WebView와 네이티브 간 동기화**
- JavaScript에서 생성된 UUID가 Android/iOS 네이티브로 전달
- 이 UUID를 WebView 이벤트의 traceId로 사용

### **3. API 호환성 유지**
- 기존 시스템과의 호환성 유지하면서 새로운 기능 추가
- webVitals만 v2 엔드포인트 사용, 나머지는 기존 유지

## 📞 문의사항

구현 중 궁금한 사항이나 추가 정보가 필요하시면 Android 개발팀으로 연락 바랍니다.

---

**작성일**: 2025-07-31  
**작성자**: Android 개발팀  
**PR**: https://github.com/devload/whatap-webview-sample/pull/1