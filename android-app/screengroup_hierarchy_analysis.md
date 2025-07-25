# ScreenGroup 계층 구조 분석 리포트

## 테스트 정보
- **테스트 일시**: 2025-07-24 11:37 ~ 11:38
- **테스트 환경**: Samsung Galaxy A35 (SM-A356N), Android 14
- **프로젝트**: sampleapp_workspace/webview

## ScreenGroup 계층 구조 분석

### 1. 확인된 Group 생성 순서

로그 분석 결과 다음과 같은 순서로 ScreenGroup이 생성됩니다:

1. **MainActivity group** (최상위)
   - Group ID: MainActivity
   - parentSpanId: null (최상위 그룹)
   - screen_name: MainActivity group

2. **WebViewFlow** (중간 계층)
   - Group ID: WebViewFlow
   - 이미 존재하는 그룹으로 스킵됨 (⚠️ ScreenGroup already exists)

3. **WebViewGroup** (하위 계층)
   - Group ID: WebViewGroup
   - WebView 관련 이벤트 수집

### 2. Task 실행 순서 (이벤트 시퀀스)

```
1. MainActivity-[id]-start
2. MainActivity-main-activity-start
3. MainActivity-[id]-end
4. WebLoadChain-webchain[id]-start
5. WebViewPage-webview[id]-start
6. http://10.160.136.223:18000/-webview[id]-start
7. MainActivity-main-activity-end
8. WebLoadChain-webchain[id]-end
9. http://10.160.136.223:18000/-webview[id]-end
```

### 3. 계층 구조 문제점 발견

**예상했던 구조**:
```
MainActivity → WebViewFlow → WebViewGroup
(각각 독립된 ScreenGroup으로 부모-자식 관계)
```

**실제 동작**:
- MainActivity group이 생성됨 (parentSpanId: null)
- WebViewFlow는 "already exists"로 스킵됨
- WebViewGroup도 "already exists"로 스킵됨
- 모든 이벤트가 하나의 ScreenGroup에 누적되는 것으로 보임

### 4. 로그 증거

```
📊 Group ID: MainActivity
✅ ScreenGroup created with parentSpan: Span{..., parentSpanId='null', ...}

📊 Group ID: WebViewFlow  
⚠️ ScreenGroup already exists, skipping startGroup for: WebViewFlow

📊 Group ID: WebViewGroup
⚠️ ScreenGroup already exists, skipping startGroup for: WebViewGroup
```

### 5. 결론

현재 구현에서는 여러 개의 독립적인 ScreenGroup이 생성되지 않고, 하나의 ScreenGroup 안에 모든 이벤트가 순차적으로 기록되는 것으로 보입니다.

**실제 동작 방식**:
1. MainActivity에서 하나의 ScreenGroup 생성
2. 이후 모든 Task들이 이 ScreenGroup에 이벤트로 추가
3. WebViewFlow, WebViewGroup은 논리적 구분자 역할만 수행

## 권장사항

개발팀에 다음 사항을 확인 요청:
1. ScreenGroup 계층 구조가 의도된 동작인지 확인
2. 부모-자식 관계의 독립적인 ScreenGroup 생성이 필요한지 검토
3. "already exists" 메시지가 정상 동작인지 확인