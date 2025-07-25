#!/usr/bin/env python3
import time
from selenium import webdriver
from selenium.webdriver.chrome.service import Service
from selenium.webdriver.chrome.options import Options
# Chrome 옵션 설정
chrome_options = Options()
chrome_options.add_argument('--headless')  # 헤드리스 모드 (GUI 없이 실행)
chrome_options.add_argument('--disable-gpu')
chrome_options.add_argument('--no-sandbox')
chrome_options.add_argument('--disable-dev-shm-usage')

# 네트워크 로그를 캡처하기 위한 설정
chrome_options.set_capability('goog:loggingPrefs', {'performance': 'ALL', 'browser': 'ALL'})

# Chrome 드라이버 생성
driver = webdriver.Chrome(options=chrome_options)

try:
    # 페이지 로드
    print("페이지 로드 중...")
    driver.get("http://localhost:8000/index.html")
    
    # 페이지 로드 대기
    time.sleep(3)
    
    # 브라우저 로그 확인
    print("\n=== 브라우저 콘솔 로그 ===")
    for entry in driver.get_log('browser'):
        print(entry)
    
    # 네트워크 로그에서 XHR 요청과 WhaTap 요청 찾기
    print("\n=== 네트워크 로그 분석 ===")
    logs = driver.get_log('performance')
    
    xhr_found = False
    whatap_agent_loaded = False
    whatap_requests = []
    
    for log in logs:
        message = log['message']
        
        # XHR 요청 확인
        if 'Network.requestWillBeSent' in message and 'jsonplaceholder.typicode.com' in message:
            print(f"✅ XHR 요청 발견")
            xhr_found = True
        
        # WhaTap Agent 스크립트 로드 확인
        if 'Network.requestWillBeSent' in message and 'whatap-browser-agent.js' in message:
            print(f"✅ WhaTap Agent 스크립트 로드 요청")
            whatap_agent_loaded = True
        
        # WhaTap 관련 네트워크 요청 확인
        if 'Network.requestWillBeSent' in message and ('whatap' in message.lower() or 'rumote.whatap' in message):
            print(f"✅ WhaTap 관련 요청: {message[:200]}...")
            whatap_requests.append(message)
        
        # WhaTap RUM 데이터 전송 확인
        if 'Network.requestWillBeSent' in message and 'rumote.whatap-mobile-agent.io' in message:
            print(f"✅ WhaTap RUM 데이터 전송: {message[:200]}...")
    
    print(f"\n=== WhaTap Agent 연동 상태 ===")
    print(f"Agent 스크립트 로드: {'✅ 성공' if whatap_agent_loaded else '❌ 실패'}")
    print(f"RUM 요청 수: {len(whatap_requests)}개")
    print(f"XHR 요청: {'✅ 성공' if xhr_found else '❌ 실패'}")
    
    # WhaTap 전역 객체 확인
    print(f"\n=== WhaTap JavaScript 객체 확인 ===")
    try:
        whatap_exists = driver.execute_script("return typeof window.WhatapBrowserAgent !== 'undefined'")
        print(f"WhatapBrowserAgent 객체: {'✅ 존재' if whatap_exists else '❌ 없음'}")
        
        if whatap_exists:
            config = driver.execute_script("return window.WhatapBrowserAgent ? window.WhatapBrowserAgent.config : null")
            if config:
                print(f"프로젝트 키: {config.get('projectAccessKey', 'N/A')[:20]}...")
                print(f"Pcode: {config.get('pcode', 'N/A')}")
                print(f"Sample Rate: {config.get('sampleRate', 'N/A')}%")
                print(f"XHR Tracing: {'✅ 활성화' if config.get('xhrTracing') else '❌ 비활성화'}")
                print(f"WebView Mode: {'✅ 활성화' if config.get('isWebView') else '❌ 비활성화'}")
    except Exception as e:
        print(f"JavaScript 실행 중 오류: {e}")
    
    # JavaScript 실행으로 추가 확인
    print("\n=== JavaScript 콘솔 로그 확인 ===")
    logs = driver.execute_script("return window.console.logs || []")
    print(f"콘솔 로그: {logs}")
    
finally:
    # 드라이버 종료
    driver.quit()
    print("\n테스트 완료")