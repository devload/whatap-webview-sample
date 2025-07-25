# WebView pageLoadEnd 404 에러 테스트 리포트

## 테스트 정보
- **테스트 일시**: 2025-07-24 11:21 ~ 11:24
- **테스트 환경**: Samsung Galaxy A35 (SM-A356N), Android 14
- **프로젝트**: whatap-workspace/webview
- **AAR 버전**: whatap-agent-bom-release.aar (2025-07-24 15:39)

## 테스트 결과

### 1. 404 에러 확인 ✅

**발견된 문제**:
- `pageLoadEnd` 엔드포인트 호출 시 404 에러 발생
- `resource` 엔드포인트는 정상 동작 (200 OK)

**로그 증거**:
```
[WhatapWebviewBridge] Upload result: Response Code: 404, Response: 404 page not found
https://rumote.whatap-mobile-agent.io/m/pageLoadEnd
```

### 2. 에러 발생 패턴

10초마다 주기적으로 발생하는 호출 패턴:
- ✅ `resource` 엔드포인트: 200 OK
- ❌ `pageLoadEnd` 엔드포인트: 404 Not Found

시간별 로그:
```
11:22:03.745 - resource: 200 OK
11:22:08.779 - pageLoadEnd: 404 Not Found
11:22:13.743 - resource: 200 OK
11:22:18.760 - pageLoadEnd: 404 Not Found
(패턴 반복)
```

### 3. pageLoadEnd 데이터 구조

JavaScript Bridge에서 전달되는 데이터:
```json
{
  "meta": {
    "sendEventID": "z353s9b8k95als",
    "pageLocation": "http://10.160.136.223:18000/",
    "host": "10.160.136.223:18000",
    "path": "/",
    "pCode": 3447,
    "projectAccessKey": "x43bn212o2cou-z5207095h6tmkj-z3k5tbqb529h1q",
    "sessionID": "x5gtu6hmnri9pd",
    "userAgent": "Mozilla/5.0 (Linux; Android 14; SM-A356N...",
    "pageloadID": "z7q4g05hr2fjfm"
  },
  "pageLoad": {
    "navigationTiming": {
      "startTimeStamp": 1753323837965,
      "data": {
        "domInteractive": 273,
        "domComplete": 2909,
        "loadTime": 2911,
        "backendTime": 37,
        "frontendTime": {"duration": 2638, "start": 273}
      }
    },
    "resource": [/* 리소스 데이터 배열 */],
    "totalDuration": 2911
  }
}
```

### 4. 추가 발견 사항

**QAFileLogger 경고**:
```
[WARNING] [WhatapWebviewBridge] ❌ Could not extract pageUrl from pageLoadEnd data
```
- pageUrl 추출 실패 경고 발생
- 데이터 구조에 pageUrl 필드 없음 (pageLocation은 존재)

### 5. 영향도

- **WebView 페이지 로드 성능 데이터가 서버로 전송되지 않음**
- Navigation Timing API 데이터 수집 불가
- 페이지별 성능 분석 불가능

## 결론

1. **pageLoadEnd API 엔드포인트가 서버에 존재하지 않음** (404 에러)
2. resource 엔드포인트는 정상 동작
3. 클라이언트 SDK는 정상적으로 데이터를 수집하고 전송 시도
4. **서버 측 API 구현 필요**

## 권장 사항

1. 서버팀에 pageLoadEnd 엔드포인트 구현 요청
2. pageUrl 추출 로직 검토 (pageLocation vs pageUrl)
3. 임시 조치: pageLoadEnd 호출 비활성화 검토

## 테스트 파일
- QA 로그: `webview_qa_404_test.log`
- 스크린샷: 해당 없음 (API 에러)