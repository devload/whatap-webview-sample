# WebView 개선사항 테스트 계획

## 전달받은 내용

### AAR 파일
📁 whatap-agent-bom/build/outputs/aar/
- **whatap-agent-bom-debug.aar** (208KB)
- **whatap-agent-bom-release.aar** (199KB)

### 문서
📄 **QA_REQUEST_WEBVIEW_IMPROVEMENTS.md**
- 개선사항 요약
- 상세 테스트 시나리오 4개
- 확인해야 할 로그 메시지
- 측정 지표 및 환경 설정

## 테스트 우선순위

### 1. High Priority
- **기본 WebView 페이지 로드 후 종료 이벤트 확인**
  - 현재 문제: WebViewPage-xxx-start는 있지만 end가 누락
  - 목표: 완전한 페이지 라이프사이클 추적

- **동일 페이지 재방문 시 독립적인 taskId 생성 확인**
  - TaskId 중복 방지
  - 세션별 고유성 보장

### 2. Medium Priority
- **SPA 라우트 변경 테스트**
  - routeChange 메서드 호출 확인
  
- **URL 변형 테스트**
  - 다양한 URL 패턴 처리

## 테스트 방법

### AAR 적용
```bash
# 기존 AAR 백업
cp app/lib/whatap-agent-bom-release-pageload-fix.aar app/lib/backup/

# 새 AAR 적용
cp /path/to/whatap-agent-bom-release.aar app/lib/
```

### 로그 모니터링
```bash
adb logcat | grep -E "(WhatapWebviewBridge|ScreenGroupManager)"
```

### 성공 지표
- ✅ WebViewPage-xxx-start와 WebViewPage-xxx-end 쌍으로 기록
- ✅ TaskId 고유성 보장
- ✅ JavaScript Bridge 정상 호출

## 다음 단계

1. 새 AAR 파일을 프로젝트에 적용
2. 테스트 시나리오별 실행
3. 로그 분석 및 결과 정리
4. 개발팀에 피드백 전달

테스트 준비 완료! 🚀