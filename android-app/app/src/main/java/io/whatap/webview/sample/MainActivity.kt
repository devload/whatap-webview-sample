package io.whatap.webview.sample

import android.content.Context
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.LocalActivity
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
// import io.whatap.android.agent.webview.WhatapWebViewClient
// import io.whatap.android.agent.webview.WhatapWebviewBridge
import io.whatap.webview.sample.ui.theme.WebviewTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.LaunchedEffect
import android.util.Log
import io.whatap.android.agent.instrumentation.userlog.UserLogger
import android.os.Handler
import android.os.Looper
// import io.whatap.android.agent.instrumentation.screengroup.ChainView
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.RequestBody
import okhttp3.MediaType.Companion.toMediaType
import java.util.concurrent.TimeUnit
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import io.whatap.android.agent.extra.WhatapExtra
import io.whatap.android.agent.api.Extras
import androidx.compose.runtime.rememberCoroutineScope
import io.whatap.android.agent.webview.WhatapWebviewBridge
import io.whatap.android.agent.instrumentation.screengroup.ChainView
import io.whatap.android.agent.instrumentation.ndkcrash.NativeCrashCollector
import com.android.volley.RequestQueue
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import java.net.HttpURLConnection
import java.net.URL
import io.whatap.android.agent.instrumentation.screengroup.UserScreenGroupManager
import io.whatap.android.agent.sdk.OpenTelemetry

class MainActivity : FragmentActivity() {
    private lateinit var volleyQueue: RequestQueue
    private val okHttpClient = OkHttpClient.Builder().build()

    companion object {
        const val TAG = "WebViewSample"
        const val AUTO_RESTART_INTERVAL_MS = 30000L // 30초마다 자동 재시작
        const val NETWORK_REQUEST_INTERVAL_MS = 5000L // 5초 네트워크 요청 간격

        // Activity→Fragment Chain 관리용 변수
        var activityFragmentChainId: String? = null
        
        // 백그라운드 네트워크 요청용 URLs (WhatapAgent 네트워크 수집 테스트)
        val TEST_URLS = listOf(
            "https://httpbin.org/get",                           // GET 요청 테스트
            "https://jsonplaceholder.typicode.com/posts/1",      // JSON API 테스트  
            "https://api.github.com/zen",                        // GitHub API 테스트
            "https://httpbin.org/uuid",                          // UUID 생성 테스트
            "https://httpbin.org/delay/2",                       // 지연 응답 테스트 (2초)
            "https://httpbin.org/status/200",                    // 정상 응답 테스트
            "https://httpbin.org/status/404",                    // 에러 응답 테스트
            "https://jsonplaceholder.typicode.com/users/1",      // 사용자 정보 API 테스트
            "https://nonexistent-domain-12345.com/api",          // 🔴 존재하지 않는 도메인 (DNS 실패)
            "https://invalid-url-test-987654321.net/data"        // 🔴 무효한 도메인 (서버 응답 없음)
        )
        
        // Export 로그를 위한 StateFlow
        private val _exportLogs = MutableStateFlow<List<String>>(emptyList())
        val exportLogs = _exportLogs.asStateFlow()
        
        // 로그 추가 함수
        fun addExportLog(message: String) {
            val timestamp = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()).format(java.util.Date())
            val logEntry = "[$timestamp] $message"
            _exportLogs.value = (_exportLogs.value + logEntry).takeLast(100) // 최근 100개로 증가
        }
    }
    
    // Test 3: 멀티 스레드 Depth 필터 테스트용 메서드 체인
    // 메인 스레드용 메서드
    private fun testDepth1() {
        Log.d(TAG, "Main Thread Depth 1 called")
        Thread.sleep(100)  // 100ms delay - 더 명확한 추적을 위해 증가
        testDepth2()
    }
    
    private fun testDepth2() {
        Log.d(TAG, "Main Thread Depth 2 called")
        Thread.sleep(150)  // 150ms delay
        testDepth3()
    }
    
    private fun testDepth3() {
        Log.d(TAG, "Main Thread Depth 3 called")
        Thread.sleep(200)  // 200ms delay
        testDepth4()
    }
    
    private fun testDepth4() {
        Log.d(TAG, "Main Thread Depth 4 called - 이 메서드는 추적되지 않아야 함")
        Thread.sleep(250)  // 250ms delay
        testDepth5()
    }
    
    private fun testDepth5() {
        Log.d(TAG, "Main Thread Depth 5 called - 이 메서드는 추적되지 않아야 함")
        Thread.sleep(300)  // 300ms delay
    }
    
    // 백그라운드 스레드용 메서드
    private fun threadDepth1() {
        Log.d(TAG, "Thread Depth 1 called - ${Thread.currentThread().name}")
        Thread.sleep(100)  // 100ms delay
        threadDepth2()
    }
    
    private fun threadDepth2() {
        Log.d(TAG, "Thread Depth 2 called - ${Thread.currentThread().name}")
        Thread.sleep(150)  // 150ms delay
        threadDepth3()
    }
    
    private fun threadDepth3() {
        Log.d(TAG, "Thread Depth 3 called - ${Thread.currentThread().name}")
        Thread.sleep(200)  // 200ms delay
        threadDepth4()
    }
    
    private fun threadDepth4() {
        Log.d(TAG, "Thread Depth 4 called - ${Thread.currentThread().name} - 이 메서드는 추적되지 않아야 함")
        Thread.sleep(250)  // 250ms delay
        threadDepth5()
    }
    
    private fun threadDepth5() {
        Log.d(TAG, "Thread Depth 5 called - ${Thread.currentThread().name} - 이 메서드는 추적되지 않아야 함")
        Thread.sleep(300)  // 300ms delay
    }
    
    private fun testExtrasAndWhatapExtra() {
        // Clear extras first (고객 방식)
        Log.e(TAG, "🧹 Clearing WhatapExtra first")
        WhatapExtra.getMap().clear()

        // 🔥 고객사 방식: WhatapExtra.getMap().put() 사용
        Log.e(TAG, "🔥🔥🔥 Testing WhatapExtra.getMap().put() - 고객사 사용 방식")
        WhatapExtra.getMap().put("extra_user_id", "user_12345")
        WhatapExtra.getMap().put("extra_session_id", "session_abc_def")
        WhatapExtra.getMap().put("extra_page_name", "MainActivity")
        WhatapExtra.getMap().put("extra_counter", 999)
        WhatapExtra.getMap().put("extra_boolean", true)
        WhatapExtra.getMap().put("extra_timestamp", System.currentTimeMillis())

        // Get and log the extras map size - ConcurrentModificationException 방지
        try {
            val extrasMap = WhatapExtra.getMap()
            Log.e(TAG, "✅ WhatapExtra.getMap().put() called with 6 test values")
            Log.e(TAG, "📊 WhatapExtra map size: ${extrasMap.size}")

            // HashMap을 안전하게 복사한 후 로깅
            val safeMap = HashMap(extrasMap)
            Log.e(TAG, "📋 WhatapExtra contents: $safeMap")
        } catch (e: Exception) {
            Log.e(TAG, "⚠️ WhatapExtra 로깅 중 예외 발생: ${e.message}")
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WebView.setWebContentsDebuggingEnabled(true)
        Log.i(TAG, "🚀 MainActivity onCreate")

        // Volley 초기화
        volleyQueue = Volley.newRequestQueue(this)

        // 🌐 백그라운드 네트워크 자동 테스트 시작
        startAutoNetworkTests()

        // SplashActivity에서 전달된 chainId 확인 - WebView 로드 완료 시 ChainView 종료하도록 변경
        val chainId = intent.getStringExtra("chain_id")
        if (chainId != null) {
            Log.i(TAG, "⛓️ ChainView 연결됨 - chainId: $chainId (from SplashActivity)")
            // chainId를 companion object에 저장하여 WebView 로드 완료 시 사용
            activityFragmentChainId = chainId
            Log.i(TAG, "⛓️ ChainView는 WebView 로드 완료 시 종료될 예정")
        } else {
            Log.i(TAG, "ℹ️ ChainView 없이 시작 (독립 실행)")
        }
        
        // ⚡ ScreenGroup 딜레이는 App.java에서 WhatapAgent 초기화 시 설정됨 (5초)
        
        // 🔴 UserLogger 테스트 - .c 접미어 확인 (30초마다 반복)
        startUserLoggerTest()

        // 🔥 WhatapExtra와 Extras 테스트
        testExtrasAndWhatapExtra()

        Log.e(TAG, "🚨🚨🚨 BEFORE startUserScreenGroupTest() - LINE 228")

        // 🧪 UserScreenGroupManager 테스트 시작
        startUserScreenGroupTest()

        Log.e(TAG, "🚨🚨🚨 AFTER startUserScreenGroupTest() - LINE 231")

        // 💥 30% 확률로 ANR 또는 CRASH 발생 (사용자 요청)
        val randomValue = kotlin.random.Random.nextInt(100)
        Log.i(TAG, "🎲 ANR/CRASH 확률 체크: $randomValue (30% = 0-29)")

        if (randomValue < 30) {  // 30% 확률
            val crashType = randomValue % 3  // 0: CRASH, 1: ANR, 2: CRASH (다양한 타입)

            when (crashType) {
                0 -> {
                    // 10% 확률 - NullPointerException 크래시
                    Log.e(TAG, "💥💥💥 CRASH 발생 예정 (NullPointerException)! (확률: $randomValue/100)")
                    addExportLog("💥 CRASH 발생 직전... (5초 후) - NullPointerException")

                    Handler(Looper.getMainLooper()).postDelayed({
                        Log.e(TAG, "💥 실제 CRASH 발생 - NullPointerException")
                        @Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
                        val nullString: String? = null
                        val crash = nullString!!.length  // NPE 크래시
                    }, 5000)  // 5초 후 크래시
                }
                1 -> {
                    // 10% 확률 - ANR 발생
                    Log.e(TAG, "😴😴😴 ANR 발생 예정! (확률: $randomValue/100)")
                    addExportLog("😴 ANR 발생 직전... (5초 후)")

                    Handler(Looper.getMainLooper()).postDelayed({
                        Log.e(TAG, "😴 실제 ANR 발생 - 메인 스레드 10초 블록")
                        Thread.sleep(10000)  // 10초 블록 -> ANR 발생
                    }, 5000)  // 5초 후 ANR
                }
                2 -> {
                    // 10% 확률 - ArithmeticException 크래시
                    Log.e(TAG, "💥💥💥 CRASH 발생 예정 (ArithmeticException)! (확률: $randomValue/100)")
                    addExportLog("💥 CRASH 발생 직전... (5초 후) - ArithmeticException")

                    Handler(Looper.getMainLooper()).postDelayed({
                        Log.e(TAG, "💥 실제 CRASH 발생 - ArithmeticException")
                        @Suppress("DIVISION_BY_ZERO")
                        val crash = 1 / 0  // Division by zero 크래시
                    }, 5000)  // 5초 후 크래시
                }
            }
        } else {
            Log.i(TAG, "✅ 정상 실행 - ANR/CRASH 없음 (확률: $randomValue/100)")
            addExportLog("✅ 앱 정상 실행 (확률: $randomValue/100)")
        }

        // Test 3: 멀티 스레드에서 Depth 필터 테스트 실행 - 주석 처리 (Thread 생성 방지)
        // 화면 로드 중 여러 스레드에서 동시에 실행
        // Thread(Runnable {
        //     Thread.currentThread().name = "DepthTest-Thread1"
        //     Log.i(TAG, "🔵 Thread1 시작: ${Thread.currentThread().name}")
        //     threadDepth1()  // 스레드용 메서드 호출
        //     Log.i(TAG, "🔵 Thread1 완료")
        // }).start()

        // Thread(Runnable {
        //     Thread.currentThread().name = "DepthTest-Thread2"
        //     Thread.sleep(50) // 약간의 딜레이로 시작 시점 분산
        //     Log.i(TAG, "🟢 Thread2 시작: ${Thread.currentThread().name}")
        //     threadDepth1()  // 스레드용 메서드 호출
        //     Log.i(TAG, "🟢 Thread2 완료")
        // }).start()

        // Thread(Runnable {
        //     Thread.currentThread().name = "DepthTest-Thread3"
        //     Thread.sleep(100) // 더 긴 딜레이
        //     Log.i(TAG, "🟡 Thread3 시작: ${Thread.currentThread().name}")
        //     threadDepth1()  // 스레드용 메서드 호출
        //     Log.i(TAG, "🟡 Thread3 완료")
        // }).start()

        // 메인 스레드 블록킹 방지를 위해 백그라운드로 이동 - 주석 처리 (Thread 생성 방지)
        // Thread(Runnable {
        //     Thread.currentThread().name = "DepthTest-MainThread"
        //     Log.i(TAG, "🔴 Main Thread Depth Test 시작: ${Thread.currentThread().name}")
        //     testDepth1()  // 백그라운드 스레드에서 메서드 호출
        //     Log.i(TAG, "🔴 Main Thread Depth Test 완료")
        // }).start()

        // QAFileLogger는 Application에서 이미 설정됨
        Log.i(TAG, "📄 QAFileLogger가 Application에서 설정됨")
        
        // Export 로그 수집 시작
        startLogCollection()

        // 🚨 VIVO 디바이스에서만 ANR/크래시 테스트 실행 - 주석 처리 (Thread 생성 방지)
        // val enableANRTest = BuildConfig.ENABLE_ANR_TEST
        // val enableCrashTest = BuildConfig.ENABLE_CRASH_TEST
        // val isVivoDevice = android.os.Build.MODEL.startsWith("V") && android.os.Build.MANUFACTURER.equals("vivo", ignoreCase = true)
        //
        // Log.i(TAG, "📱 디바이스 정보 - 제조사: ${android.os.Build.MANUFACTURER}, 모델: ${android.os.Build.MODEL}")
        // Log.i(TAG, "🔍 VIVO 디바이스 여부: $isVivoDevice")
        //
        // if ((enableANRTest || enableCrashTest) && isVivoDevice) {
        //     Log.w(TAG, "⚠️ VIVO 디바이스에서 테스트 활성화 - ANR: $enableANRTest, Crash: $enableCrashTest")
        //     Handler(Looper.getMainLooper()).postDelayed({
        //         if (enableANRTest) {
        //             triggerANR()
        //         } else if (enableCrashTest) {
        //             triggerCrash()
        //         }
        //     }, 15000) // 15초 후 실행 (데이터 전송 시간 확보)
        // } else if (enableANRTest || enableCrashTest) {
        //     Log.i(TAG, "ℹ️ 테스트는 활성화되었지만 VIVO 디바이스가 아니므로 크래시/ANR을 건너뜁니다")
        //     addExportLog("ℹ️ 비-VIVO 디바이스: 크래시/ANR 테스트 건너뜀")
        // }
        
        // 백그라운드 네트워크 요청 시작 (임시 비활성화 - ChainView 테스트용)
        // startBackgroundNetworkRequests()

        // OkHttp 네트워크 요청 자동 실행 (3초 후) - 다양한 HTTP 응답 및 확률적 크래시 포함
        Handler(Looper.getMainLooper()).postDelayed({
            Log.i(TAG, "🌐 OkHttp 네트워크 요청 시작 (여러 응답 코드 + 확률적 크래시 테스트)")
            performOkHttpRequests()
        }, 3000)

        // 추가 네트워크 요청들 (onCreate 시점에 즉시 실행) (임시 비활성화)
        Log.i(TAG, "🔥 onCreate 네트워크 요청 스킵 (ChainView 테스트)")
        /*
        lifecycleScope.launch {
            Log.i(TAG, "🔥 onCreate 네트워크 요청 시작")

            // 병렬 네트워크 요청들
            val jobs = listOf(
                launch {
                    delay(100)
                    performNetworkRequest("https://httpbin.org/delay/2", "onCreate-Delay-2s")
                },
                launch {
                    delay(200)
                    performNetworkRequest("https://jsonplaceholder.typicode.com/posts", "onCreate-Posts")
                },
                launch {
                    delay(300)
                    performNetworkRequest("https://httpbin.org/status/500", "onCreate-Error-500")
                },
                launch {
                    delay(400)
                    performNetworkRequest("https://api.github.com/users/github", "onCreate-GitHub-User")
                },
                launch {
                    delay(500)
                    performNetworkRequest("https://httpbin.org/headers", "onCreate-Headers")
                }
            )

            // 모든 요청 완료 대기
            jobs.forEach { it.join() }
            Log.i(TAG, "✅ onCreate 네트워크 요청 모두 완료")
        }
        */
        
        // 테스트용 초기 로그 추가
        addExportLog("🚀 WhatapAgent 모니터링 시작")
        addExportLog("📱 디바이스: ${android.os.Build.MODEL}")
        addExportLog("🔧 Build Variant: ${BuildConfig.VARIANT_TYPE}")
        addExportLog("🌐 WhatAp 서버: ${BuildConfig.WHATAP_SERVER_URL}")
        addExportLog("🔗 백그라운드 HTTP 요청 시작 (5초 간격)")
        
        // WebView 브리지 로그 테스트
        Thread {
            Thread.sleep(3000) // 3초 후
            addExportLog("🔥 [Bridge] 테스트: generateUUID() 호출 시뮬레이션")
            Thread.sleep(2000) // 2초 후
            addExportLog("🔥 [Bridge] 테스트: pageLoad(data, uuid) 호출 시뮬레이션")
            Thread.sleep(2000) // 2초 후
            addExportLog("🔥 [Bridge] 테스트: webVitals() 호출 시뮬레이션")
        }.start()

        // 💥 간헐적 크래시 발생 비활성화 (30초 재시작 테스트를 위해)
        // Handler(Looper.getMainLooper()).postDelayed({
        //     val randomValue = kotlin.random.Random.nextInt(100)
        //     Log.i(TAG, "🎲 크래시 확률 체크: $randomValue (25% 확률 = 0-24)")
        //
        //     if (randomValue < 25) {  // 25% 확률
        //         Log.e(TAG, "💥💥💥 크래시 발생! - ArithmeticException (1/0)")
        //         addExportLog("💥💥💥 크래시 발생 직전... (확률: $randomValue/100)")
        //
        //         // ArithmeticException: divide by zero 발생
        //         val crashTest = 1 / 0
        //
        //         // 이 코드는 실행되지 않음
        //         Log.e(TAG, "이 로그는 출력되지 않음: $crashTest")
        //     } else {
        //         Log.i(TAG, "✅ 정상 실행 - 크래시 발생하지 않음 (확률: $randomValue/100)")
        //         addExportLog("✅ 앱 정상 실행 중... (크래시 확률 체크: $randomValue/100)")
        //     }
        // }, 5000) // 5초 후 확률 체크

        // ScreenGroup은 라이브러리에서 자동으로 처리됨 (수동 호출 제거)
        Log.i(TAG, "ℹ️ ScreenGroup은 라이브러리에서 자동으로 시작됩니다")

        // UserLogger API 테스트 - onCreate에서 실행 - WhatAp 비활성화
        Log.i(TAG, "📝 UserLogger API 테스트 스킵 - WhatAp SDK 비활성화")
        // try {
        //     // 간단한 문자열 로그
        //     UserLogger.print("MainActivity onCreate - UserLogger test")
        //     
        //     // 구조화된 로그 데이터
        //     val logData = HashMap<Any, Any>()
        //     logData["event_type"] = "lifecycle"
        //     logData["action"] = "activity_created"
        //     logData["activity_name"] = "MainActivity"
        //     logData["screen_group"] = "WebViewFlow"
        //     logData["timestamp"] = System.currentTimeMillis()
        //     logData["device_model"] = android.os.Build.MODEL
        //     logData["os_version"] = android.os.Build.VERSION.RELEASE
        //     logData["app_version"] = "1.0.0"
        //     
        //     UserLogger.print(logData)
        //     Log.i(TAG, "✅ UserLogger 커스텀 로그 전송 완료")
        // } catch (e: Exception) {
        //     Log.e(TAG, "❌ UserLogger 커스텀 로그 전송 실패: ${e.message}")
        // }

        // WebView URL - 실제 디바이스에서 PC의 localhost 접근 (192.168.1.100)
        val defaultUrl = "http://192.168.1.100:18000"
        Log.i(TAG, "📱 Device: ${android.os.Build.MODEL} → URL: $defaultUrl")
        val urlFromIntent = intent.getStringExtra("URL") ?: defaultUrl
        
        // Activity → Fragment → WebView 구조 설정
        Log.i(TAG, "🔧 Activity → Fragment → WebView 구조 준비 중...")

        // 🔗 activityFragmentChainId는 SplashActivity에서 전달받은 값 유지
        // (line 453에서 덮어쓰지 않도록 주석 처리)
        Log.i(TAG, "🔗 ChainId 유지: $activityFragmentChainId (SplashActivity에서 전달됨)")
        
        val fragment = TestFragment()
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(android.R.id.content, fragment)
        fragmentTransaction.commit()
        
        Log.i(TAG, "✅ Activity → Fragment → WebView 구조 설정 완료")
    }
    

    override fun onStart() {
        // onStart 지연 제거 (메인 스레드 블록킹 방지)
        super.onStart()
        Log.i(TAG, "✅ onStart 완료")

        // onStart 시점 네트워크 요청 - 주석 처리 (Thread 생성 방지)
        // lifecycleScope.launch {
        //     performNetworkRequest("https://httpbin.org/get?phase=onStart", "onStart-Request")
        // }
    }

    override fun onResume() {
        // onResume 지연 제거 (메인 스레드 블록킹 방지)
        super.onResume()
        Log.i(TAG, "✅ onResume 완료")

        // onResume 시점 네트워크 요청 - 주석 처리 (Thread 생성 방지)
        // lifecycleScope.launch {
        //     performNetworkRequest("https://httpbin.org/get?phase=onResume", "onResume-Request")
        // }
    }

    override fun onPause() {
        // onPause 지연 제거 (메인 스레드 블록킹 방지)
        super.onPause()
        Log.i(TAG, "✅ onPause 완료")
    }

    override fun onStop() {
        // onStop 지연 제거 (메인 스레드 블록킹 방지)
        super.onStop()
        Log.i(TAG, "✅ onStop 완료")
    }

    override fun onDestroy() {
        // onDestroy 지연 제거 (메인 스레드 블록킹 방지)
        super.onDestroy()
        Log.i(TAG, "🛑 MainActivity 종료")
    }
    
    // 🚨 ANR 트리거 함수
    private fun triggerANR() {
        Log.e(TAG, "🔴 ANR 테스트 시작 - 메인 스레드 10초 블록")
        Thread.sleep(10000) // ANR 유발 (10초 블록 - ANR은 5초 이상 블록 시 발생)
        Log.e(TAG, "🔴 ANR 테스트 완료")
    }
    
    // 💥 크래시 트리거 함수
    private fun triggerCrash() {
        Log.e(TAG, "💥 실제 크래시 발생 - NullPointerException")
        val nullString: String? = null
        val result = nullString!!.length // 실제 크래시 발생 (catch 없음)
        Log.e(TAG, "이 로그는 출력되지 않음: $result")
    }
    
    // 🧪 UserScreenGroupManager 테스트
    private fun startUserScreenGroupTest() {
        Log.e(TAG, "🔍 Step 0: startUserScreenGroupTest() 진입")
        try {
            Log.e(TAG, "🔍 Step 1: try 블록 진입")
            Log.i(TAG, "🧪 UserScreenGroupManager 테스트 시작")
            addExportLog("🧪 UserScreenGroupManager 테스트 시작")

            // Tracer 가져오기
            Log.e(TAG, "🔍 Step 2: OpenTelemetry.getGlobal() 호출 전")
            val telemetrySdk = OpenTelemetry.getGlobal()
            Log.e(TAG, "🔍 Step 3: telemetrySdk = $telemetrySdk")

            if (telemetrySdk == null) {
                Log.e(TAG, "❌ TelemetrySdk가 null - UserScreenGroupManager 테스트 불가")
                addExportLog("❌ TelemetrySdk null - UserScreenGroup 테스트 실패")
                return
            }

            Log.e(TAG, "🔍 Step 4: getTracerProvider() 호출 전")
            val tracerProvider = telemetrySdk.getTracerProvider()
            Log.e(TAG, "🔍 Step 5: tracerProvider = $tracerProvider")

            Log.e(TAG, "🔍 Step 6: getTracer() 호출 전")
            val tracer = tracerProvider.getTracer()
            Log.e(TAG, "🔍 Step 7: tracer = $tracer")

            if (tracer == null) {
                Log.e(TAG, "❌ Tracer가 null - UserScreenGroupManager 테스트 불가")
                addExportLog("❌ Tracer null - UserScreenGroup 테스트 실패")
                return
            }

            // UserScreenGroupManager 생성 (리스너로 Activity 이벤트 자동 수신)
            Log.e(TAG, "🔍 Step 8: UserScreenGroupManager 생성 전")
            val userScreenManager = UserScreenGroupManager(tracer, "TestUserFlow")
            Log.e(TAG, "🔍 Step 9: UserScreenGroupManager 생성 완료")
            Log.i(TAG, "✅ UserScreenGroupManager 생성 완료: TestUserFlow")
            Log.i(TAG, "📡 리스너로 Activity 이벤트 자동 수신 대기중...")
            addExportLog("✅ UserScreenGroup 생성: TestUserFlow (리스너 자동 수신)")

            // 8초 후 종료 (그 사이에 Activity 이벤트가 자동으로 수신됨)
            Handler(Looper.getMainLooper()).postDelayed({
                val taskCount = userScreenManager.activeTaskCount
                Log.i(TAG, "🏁 UserScreenGroup 종료 - 자동 수신된 task 개수: $taskCount")
                userScreenManager.close()
                Log.i(TAG, "🏁 UserScreenGroup 테스트 완료")
                addExportLog("🏁 UserScreenGroup 테스트 완료 (자동 수신)")
            }, 8000)

        } catch (e: Exception) {
            Log.e(TAG, "🔍 Step ERROR: Exception 발생!")
            Log.e(TAG, "❌ Exception 클래스: ${e.javaClass.name}")
            Log.e(TAG, "❌ Exception 메시지: ${e.message}")
            Log.e(TAG, "❌ UserScreenGroupManager 테스트 실패", e)
            e.printStackTrace()
            addExportLog("❌ UserScreenGroup 테스트 실패: ${e.javaClass.name} - ${e.message}")
        }
        Log.e(TAG, "🔍 Step END: startUserScreenGroupTest() 종료")
    }

    // 🔄 UserLogger 30초마다 반복 테스트
    private fun startUserLoggerTest() {
        Thread {
            var testCount = 0
            while (true) {
                testCount++
                try {
                    Log.i(TAG, "🔴 UserLogger 테스트 #$testCount - .c 접미어 및 WhatapExtra 확인")

                    // ✅ 고객사 방식: WhatapExtra.getMap().put() 사용
                    WhatapExtra.getMap().put("extra_test_key", "extra_value_$testCount")
                    WhatapExtra.getMap().put("extra_transaction_id", "txn_${System.currentTimeMillis()}")
                    WhatapExtra.getMap().put("extra_count", testCount)
                    WhatapExtra.getMap().put("extra_device_info", "Android_Test_Device")
                    Log.i(TAG, "🧪 WhatapExtra 데이터 설정 완료: 4개 키")

                    // String 메시지 테스트
                    UserLogger.print("Test message #$testCount with .c suffix and Extras")

                    // Map 데이터 테스트 - 매번 다른 데이터
                    val testData = HashMap<Any, Any>()
                    testData["log_userId"] = "test_user_$testCount"
                    testData["log_action"] = "periodic_test_$testCount"
                    testData["log_timestamp"] = System.currentTimeMillis()
                    testData["log_count"] = testCount
                    testData["log_isActive"] = (testCount % 2 == 0) // true/false 번갈아가며
                    testData["log_testType"] = "30sec_interval"
                    UserLogger.print(testData as Map<Any, Any>)

                    Log.i(TAG, "✅ UserLogger 테스트 #$testCount 데이터 전송 완료 (Extras 포함)")
                    addExportLog("📝 UserLogger 테스트 #$testCount 실행 완료 + Extras")
                    
                } catch (e: Exception) {
                    Log.e(TAG, "❌ UserLogger 테스트 #$testCount 실패: ${e.message}")
                    addExportLog("❌ UserLogger 테스트 #$testCount 실패: ${e.message}")
                }
                
                // 30초 대기
                Thread.sleep(30000)
            }
        }.start()
        
        Log.i(TAG, "🔄 UserLogger 30초 간격 반복 테스트 시작됨")
        addExportLog("🔄 UserLogger 30초 간격 테스트 시작")
    }
    
    // OkHttp 네트워크 요청 수행
    private suspend fun performNetworkRequest(url: String, tag: String) {
        withContext(Dispatchers.IO) {
            try {
                val client = OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .build()

                val request = Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "WhatapAgent-Android-Sample/$tag")
                    .addHeader("X-Request-Tag", tag)
                    .build()

                Log.i(TAG, "🌐 [$tag] 네트워크 요청 시작: $url")
                val startTime = System.currentTimeMillis()

                val response = client.newCall(request).execute()
                val duration = System.currentTimeMillis() - startTime

                response.use {
                    val code = it.code
                    val bodyString = it.body?.string() ?: ""
                    val bodySize = bodyString.length

                    if (code in 200..299) {
                        Log.i(TAG, "✅ [$tag] 성공 ($code) - ${duration}ms, ${bodySize}bytes")
                        addExportLog("✅ [$tag] HTTP $code - ${duration}ms")
                    } else {
                        Log.w(TAG, "⚠️ [$tag] 에러 ($code) - ${duration}ms")
                        addExportLog("⚠️ [$tag] HTTP $code - ${duration}ms")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ [$tag] 네트워크 요청 실패: ${e.message}")
                addExportLog("❌ [$tag] 실패: ${e.message}")
            }
        }
    }

    private fun performOkHttpRequests() {
        Thread {
            try {
                val client = OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .build()
                
                // 여러 URL에 대해 순차적으로 요청 (다양한 HTTP 응답 코드 포함)
                val testUrls = listOf(
                    "https://httpbin.org/get",                    // 200 OK
                    "https://httpbin.org/status/200",            // 200 OK
                    "https://httpbin.org/status/404",            // 404 Not Found
                    "https://httpbin.org/status/500",            // 500 Internal Server Error
                    "https://httpbin.org/status/403",            // 403 Forbidden
                    "https://httpbin.org/delay/2",               // 지연 응답
                    "https://jsonplaceholder.typicode.com/posts/1",
                    "https://api.github.com/zen",
                    "https://httpbin.org/json",
                    "https://httpbin.org/uuid"
                )
                
                testUrls.forEach { url ->
                    try {
                        Log.i(TAG, "🌐 OkHttp 요청 시작: $url")
                        
                        val request = Request.Builder()
                            .url(url)
                            .addHeader("User-Agent", "WhatapAgent-Test/1.0")
                            .addHeader("X-Test-Type", "OkHttp-Auto")
                            .addHeader("X-Device", android.os.Build.MODEL)
                            .build()
                        
                        val response = client.newCall(request).execute()
                        val responseBody = response.body?.string() ?: ""
                        val responseCode = response.code

                        Log.i(TAG, "✅ OkHttp 응답: $responseCode - ${url}")
                        Log.d(TAG, "📦 응답 내용 (첫 100자): ${responseBody.take(100)}")

                        addExportLog("🌐 OkHttp: $responseCode - ${url.substringAfter("://").substringBefore("/")}")

                        response.close()

                        // 확률적 크래시 체크 (10% 확률, 에러 응답 시 20% 확률)
                        val crashProbability = if (responseCode >= 400) 20 else 10
                        val randomValue = (0..99).random()
                        Log.d(TAG, "🎲 크래시 확률 체크: $randomValue (크래시 if < $crashProbability)")

                        if (randomValue < crashProbability) {
                            Log.e(TAG, "💥 크래시 발생! (응답 코드: $responseCode)")
                            throw RuntimeException("Intentional crash after HTTP $responseCode response from $url")
                        }

                        // 다음 요청 전 짧은 대기
                        Thread.sleep(500)
                        
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ OkHttp 요청 실패 ($url): ${e.message}")
                        addExportLog("❌ OkHttp 실패: ${url.substringAfter("://").substringBefore("/")}")
                    }
                }
                
                // POST 요청 테스트도 추가
                try {
                    val postUrl = "https://httpbin.org/post"
                    Log.i(TAG, "🌐 OkHttp POST 요청: $postUrl")
                    
                    val json = """
                        {
                            "test": "WhatAp Android SDK",
                            "timestamp": ${System.currentTimeMillis()},
                            "device": "${android.os.Build.MODEL}",
                            "pCode": ${BuildConfig.WHATAP_PCODE}
                        }
                    """.trimIndent()
                    
                    val requestBody = RequestBody.create(
                        "application/json; charset=utf-8".toMediaType(),
                        json
                    )
                    
                    val postRequest = Request.Builder()
                        .url(postUrl)
                        .post(requestBody)
                        .addHeader("User-Agent", "WhatapAgent-Test/1.0")
                        .addHeader("X-Test-Type", "OkHttp-POST")
                        .build()
                    
                    val postResponse = client.newCall(postRequest).execute()
                    Log.i(TAG, "✅ OkHttp POST 응답: ${postResponse.code}")
                    addExportLog("🌐 OkHttp POST: ${postResponse.code}")
                    postResponse.close()
                    
                } catch (e: Exception) {
                    Log.e(TAG, "❌ OkHttp POST 실패: ${e.message}")
                }
                
                Log.i(TAG, "✅ 모든 OkHttp 요청 완료")
                addExportLog("✅ OkHttp 네트워크 테스트 완료")
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ OkHttp 테스트 실패: ${e.message}")
                addExportLog("❌ OkHttp 테스트 실패: ${e.message}")
            }
        }.start()
    }

    // 🌐 백그라운드 네트워크 자동 테스트 시작
    private fun startAutoNetworkTests() {
        Log.i(TAG, "🚀 백그라운드 네트워크 자동 테스트 시작 (3초 후)")
        addExportLog("🚀 백그라운드 네트워크 자동 테스트 시작")

        lifecycleScope.launch {
            kotlinx.coroutines.delay(3000)
            Log.i(TAG, "🔥 네트워크 테스트 실행 중...")

            // OkHttp 200 테스트
            testOkHttp(true) { Log.i(TAG, it); addExportLog(it) }
            kotlinx.coroutines.delay(1000)

            // OkHttp 404 테스트
            testOkHttp(false) { Log.i(TAG, it); addExportLog(it) }
            kotlinx.coroutines.delay(1000)

            // Volley 200 테스트
            testVolley(true) { Log.i(TAG, it); addExportLog(it) }
            kotlinx.coroutines.delay(2000)

            // Volley 404 테스트
            testVolley(false) { Log.i(TAG, it); addExportLog(it) }
            kotlinx.coroutines.delay(2000)

            // HttpURLConnection 200 테스트
            testHttpURLConnection(true) { Log.i(TAG, it); addExportLog(it) }
            kotlinx.coroutines.delay(1000)

            // HttpURLConnection 404 테스트
            testHttpURLConnection(false) { Log.i(TAG, it); addExportLog(it) }
            kotlinx.coroutines.delay(1000)

            // 🔴 DNS 실패 테스트 - 존재하지 않는 도메인
            testDnsFailure { Log.i(TAG, it); addExportLog(it) }
            kotlinx.coroutines.delay(1000)

            Log.i(TAG, "✅ 모든 자동 네트워크 테스트 완료")
            addExportLog("✅ 모든 자동 네트워크 테스트 완료")

            // 테스트 완료 후 5초마다 반복
            startPeriodicNetworkTests()
        }
    }

    private fun startPeriodicNetworkTests() {
        lifecycleScope.launch {
            var count = 1
            while (true) {
                kotlinx.coroutines.delay(5000) // 5초마다 반복

                Log.i(TAG, "🔄 반복 네트워크 테스트 #$count 시작")

                // 랜덤하게 다른 라이브러리 테스트
                when (count % 3) {
                    0 -> {
                        testOkHttp(true) { Log.i(TAG, it); addExportLog(it) }
                        kotlinx.coroutines.delay(500)
                        testOkHttp(false) { Log.i(TAG, it); addExportLog(it) }
                    }
                    1 -> {
                        testVolley(true) { Log.i(TAG, it); addExportLog(it) }
                        kotlinx.coroutines.delay(1000)
                        testVolley(false) { Log.i(TAG, it); addExportLog(it) }
                    }
                    2 -> {
                        testHttpURLConnection(true) { Log.i(TAG, it); addExportLog(it) }
                        kotlinx.coroutines.delay(500)
                        testHttpURLConnection(false) { Log.i(TAG, it); addExportLog(it) }
                    }
                }

                count++
            }
        }
    }

    // ========== OkHttp 테스트 ==========
    private fun testOkHttp(success: Boolean, onResult: (String) -> Unit) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = if (success) {
                    "https://httpbin.org/status/200"
                } else {
                    "https://httpbin.org/status/404"
                }

                Log.i(TAG, "🔷 [OkHttp] 요청: $url")
                val request = okhttp3.Request.Builder()
                    .url(url)
                    .build()

                val response = okHttpClient.newCall(request).execute()
                val statusCode = response.code
                val statusText = if (success) "200" else "404"
                val message = "✅ OkHttp $statusText: HTTP $statusCode"

                Log.i(TAG, "🔷 [OkHttp] 응답: $statusCode")
                withContext(Dispatchers.Main) {
                    onResult(message)
                }
            } catch (e: Exception) {
                val message = "❌ OkHttp 에러: ${e.message}"
                Log.e(TAG, "🔷 [OkHttp] 에러", e)
                withContext(Dispatchers.Main) {
                    onResult(message)
                }
            }
        }
    }

    // ========== Volley 테스트 ==========
    private fun testVolley(success: Boolean, onResult: (String) -> Unit) {
        val url = if (success) {
            "https://httpbin.org/status/200"
        } else {
            "https://httpbin.org/status/404"
        }

        Log.i(TAG, "🔶 [Volley] 요청: $url")

        val statusText = if (success) "200" else "404"
        val request = StringRequest(
            com.android.volley.Request.Method.GET,
            url,
            { response ->
                val message = "✅ Volley $statusText: 응답 받음"
                Log.i(TAG, "🔶 [Volley] 성공: $message")
                onResult(message)
            },
            { error ->
                val statusCode = error.networkResponse?.statusCode ?: -1
                val message = if (statusCode == 404 && !success) {
                    "✅ Volley 404: HTTP $statusCode (예상된 에러)"
                } else {
                    "❌ Volley 에러: HTTP $statusCode - ${error.message}"
                }
                Log.w(TAG, "🔶 [Volley] 응답: $statusCode")
                onResult(message)
            }
        )

        volleyQueue.add(request)
    }

    // ========== HttpURLConnection 테스트 ==========
    private fun testHttpURLConnection(success: Boolean, onResult: (String) -> Unit) {
        lifecycleScope.launch(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                val urlString = if (success) {
                    "https://httpbin.org/status/200"
                } else {
                    "https://httpbin.org/status/404"
                }

                Log.i(TAG, "🟢 [HttpURLConnection] 요청: $urlString")
                val url = URL(urlString)
                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val statusCode = connection.responseCode
                val statusText = if (success) "200" else "404"
                val message = "✅ HttpURLConnection $statusText: HTTP $statusCode"

                Log.i(TAG, "🟢 [HttpURLConnection] 응답: $statusCode")
                withContext(Dispatchers.Main) {
                    onResult(message)
                }
            } catch (e: Exception) {
                val message = "❌ HttpURLConnection 에러: ${e.message}"
                Log.e(TAG, "🟢 [HttpURLConnection] 에러", e)
                withContext(Dispatchers.Main) {
                    onResult(message)
                }
            } finally {
                connection?.disconnect()
            }
        }
    }

    // ========== DNS 실패 테스트 (존재하지 않는 도메인) ==========
    private fun testDnsFailure(onResult: (String) -> Unit) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 무작위 존재하지 않는 도메인 생성
                val randomDomain = "https://nonexistent-whatap-test-${System.currentTimeMillis()}-${kotlin.random.Random.nextInt(1000, 9999)}.com/api/test"

                Log.i(TAG, "🔴 [DNS Failure] 요청: $randomDomain")
                val request = okhttp3.Request.Builder()
                    .url(randomDomain)
                    .build()

                val response = okHttpClient.newCall(request).execute()
                val statusCode = response.code
                val message = "⚠️ DNS Failure: 예상치 못한 성공 - HTTP $statusCode"

                Log.w(TAG, "🔴 [DNS Failure] 예상치 못한 응답: $statusCode")
                withContext(Dispatchers.Main) {
                    onResult(message)
                }
            } catch (e: Exception) {
                val message = "✅ DNS Failure: 예상된 실패 - ${e.javaClass.simpleName}"
                Log.i(TAG, "🔴 [DNS Failure] 예상된 에러: ${e.message}")
                withContext(Dispatchers.Main) {
                    onResult(message)
                }
            }
        }
    }

    // logcat을 통해 export 로그 수집
    private fun startLogCollection() {
        addExportLog("🔍 로그 수집 시스템 시작...")

        Thread {
            try {
                val process = Runtime.getRuntime().exec("logcat -v brief")
                val reader = BufferedReader(InputStreamReader(process.inputStream))

                addExportLog("✅ logcat 실행 성공")

                var line: String?
                var lineCount = 0
                while (reader.readLine().also { line = it } != null) {
                    lineCount++

                    if (lineCount % 100 == 0) {
                        addExportLog("📊 로그 수집 중... $lineCount 라인 처리됨")
                    }

                    line?.let { logLine ->
                        if (logLine.contains("HttpSpanExporter") ||
                            logLine.contains("실제 전송 데이터") ||
                            logLine.contains("전송 span 개수") ||
                            logLine.contains("JSON payload") ||
                            logLine.contains("export") ||
                            logLine.contains("POST") ||
                            logLine.contains("trace_id") ||
                            logLine.contains("span_id") ||
                            logLine.contains("🔥") ||
                            logLine.contains("Bridge") ||
                            logLine.contains("generateUUID") ||
                            logLine.contains("pageLoad") ||
                            logLine.contains("webVitals") ||
                            logLine.contains("whatap_bridge") ||
                            logLine.contains("OkHttp") ||
                            logLine.contains("okhttp") ||
                            logLine.contains("HttpLog") ||
                            logLine.contains("NetworkTrace") ||
                            logLine.contains("BackgroundRequest") ||
                            logLine.contains("httpbin.org") ||
                            logLine.contains("jsonplaceholder")) {

                            val cleanLog = logLine.substringAfter(": ").take(100)
                            addExportLog("🔴 $cleanLog")
                        }
                    }
                }
            } catch (e: Exception) {
                addExportLog("❌ 로그 수집 오류: ${e.message}")
            }
        }.start()

        Thread {
            var count = 0
            while (true) {
                Thread.sleep(15000)
                addExportLog("⏰ 데이터 수집 대기 중... ${++count}")
            }
        }.start()
    }

}



@Composable
fun ServerUrlEditor() {
    val context = LocalContext.current
    val sharedPrefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    val defaultUrl = BuildConfig.WHATAP_SERVER_URL  // Build variant에 따른 서버 URL
    var text by remember {
        mutableStateOf(TextFieldValue(sharedPrefs.getString("server_url", defaultUrl) ?: defaultUrl))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        OutlinedTextField(
            modifier = Modifier.weight(1f),
            value = text,
            onValueChange = { text = it },
            placeholder = { Text("Server URL", fontSize = 11.sp) },
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
        )
        Button(
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
            onClick = {
            sharedPrefs.edit().putString("server_url", text.text).apply()
            Toast.makeText(context, "서버 주소가 저장되었습니다.\n앱을 재시작해주세요.", Toast.LENGTH_LONG).show()
        }) {
            Text("저장", fontSize = 10.sp)
        }
    }
}

@Composable
fun WebViewWithUrlController(initialUrl: String) {
    val context = LocalContext.current
    var urlState by remember { mutableStateOf(TextFieldValue(initialUrl)) }
    val webViewRef = remember { mutableStateOf<WebView?>(null) }
    val exportLogs by MainActivity.exportLogs.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // 새 로그가 추가될 때마다 스크롤을 맨 아래로
    LaunchedEffect(exportLogs.size) {
        if (exportLogs.isNotEmpty()) {
            listState.animateScrollToItem(exportLogs.size - 1)
        }
    }

    // 자동 리로드 제거됨 (사용자 요청에 따라)

    Column(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.weight(0.55f), // WebView 비율을 55%로 설정
            factory = { ctx ->
                WebView(ctx).apply {
                    // Chrome DevTools 디버깅 활성화
                    WebView.setWebContentsDebuggingEnabled(true)

                    settings.javaScriptEnabled = true
                    settings.cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE

                     val bridge = WhatapWebviewBridge(ctx)
                     bridge.configureWebView(this)

                    bridge.startDataUploadTimer()

                    // 🔥 핵심 수정: WhatapWebViewClient 사용
                    webViewClient = io.whatap.android.agent.webview.WhatapWebViewClient(bridge, object : android.webkit.WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            url?.let {
                                Log.i("WebViewSample", "🌐 WebView 페이지 로드 시작: $it")

                                // ⛓️ ChainView 종료 - Splash → MainActivity → WebView 로드 시작 (안전한 시점)
                                MainActivity.activityFragmentChainId?.let { chainId ->
                                    try {
                                        ChainView.getInstance().endChain(chainId)
                                        Log.i("WebViewSample", "⛓️ ChainView 종료 - Splash → MainActivity → WebView 로드 시작 ($chainId)")
                                        MainActivity.addExportLog("⛓️ ChainView 완료: Splash → MainActivity → WebView (로드 시작)")
                                        MainActivity.activityFragmentChainId = null // 한 번만 종료하도록 초기화
                                    } catch (e: Exception) {
                                        Log.e("WebViewSample", "❌ ChainView 종료 실패: ${e.message}", e)
                                        MainActivity.addExportLog("❌ ChainView 종료 실패: ${e.message}")
                                    }
                                }
                            }
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            url?.let { loadedUrl ->
                                Log.i("WebViewSample", "✅ WebView 페이지 로드 완료: $loadedUrl")
                                Toast.makeText(ctx, "페이지 로드 완료: $loadedUrl", Toast.LENGTH_SHORT).show()

                                // ChainView는 이미 onPageStarted에서 종료됨
                                // (이전 코드는 로드 완료 시점에 종료했으나, 로드 실패 시 chain이 남는 문제가 있었음)
                            }
                        }
                        
                        override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                            super.doUpdateVisitedHistory(view, url, isReload)
                            if (!isReload && url != null) {
                                Log.i("WebViewSample", "🔄 WebView 내부 네비게이션: $url")
                            }
                        }
                    })

                    // 🌐 기존 localhost:18000 서버로 로드 (IP는 동적으로 설정)
                    loadUrl(initialUrl)
                    webViewRef.value = this
                }
            }
        )

        // URL 입력 줄
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            OutlinedTextField(
                modifier = Modifier.weight(1f),
                value = urlState,
                onValueChange = { urlState = it },
                placeholder = { Text("Enter URL", fontSize = 11.sp) },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
            )
            Button(
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                onClick = {
                val urlToLoad = urlState.text
                if (urlToLoad.startsWith("http")) {
                    webViewRef.value?.loadUrl(urlToLoad)
                } else {
                    Toast.makeText(context, "유효한 URL이 아닙니다.", Toast.LENGTH_SHORT).show()
                }
            }) {
                Text("Go", fontSize = 10.sp)
            }
        }

        // 테스트 버튼 줄
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 새로운 외부 도메인 OkHttp 요청 버튼
            Button(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                onClick = {
                    coroutineScope.launch {
                        testExternalDomainRequest()
                    }
                }
            ) {
                Text("Test API", fontSize = 10.sp)
            }

            // 🔴 chrome://crash 테스트 버튼
            Button(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                onClick = {
                    Log.e("WebViewSample", "💥 chrome://crash 테스트 시작")
                    MainActivity.addExportLog("💥 chrome://crash 로드 중...")
                    webViewRef.value?.loadUrl("chrome://crash")
                }
            ) {
                Text("💥 Crash", fontSize = 9.sp)
            }

            // 🧨 WebView OOM 테스트 버튼
            Button(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                onClick = {
                    Log.e("WebViewSample", "🧨 WebView OOM 테스트 시작")
                    MainActivity.addExportLog("🧨 OOM 테스트: 메모리 폭탄 주입 중...")

                    // JavaScript를 사용해서 WebView 렌더러 프로세스의 메모리를 급격하게 소모
                    val oomScript = """
                        (function() {
                            console.log('🧨 OOM 테스트 시작 - 메모리 폭탄');
                            try {
                                var arrays = [];
                                // 100MB씩 메모리 할당 (빠른 OOM 유도)
                                for (var i = 0; i < 100; i++) {
                                    arrays.push(new Array(10 * 1024 * 1024).fill('A'));
                                    console.log('메모리 할당: ' + (i * 100) + 'MB');
                                }
                            } catch (e) {
                                console.error('OOM 예외:', e);
                            }
                        })();
                    """.trimIndent()

                    webViewRef.value?.evaluateJavascript(oomScript, null)
                }
            ) {
                Text("🧨 OOM", fontSize = 9.sp)
            }
        }

        // NDK 크래시 테스트 버튼 줄
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // SIGSEGV 테스트
            Button(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                onClick = {
                    Log.e("WebViewSample", "💥 NDK SIGSEGV 테스트 시작")
                    MainActivity.addExportLog("💥 NDK SIGSEGV 크래시 트리거")
                    try {
                        NativeCrashCollector.triggerTestCrash(0)
                    } catch (e: Exception) {
                        MainActivity.addExportLog("❌ NDK 크래시 실패: ${e.message}")
                    }
                }
            ) {
                Text("SIGSEGV", fontSize = 8.sp)
            }

            // SIGABRT 테스트
            Button(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                onClick = {
                    Log.e("WebViewSample", "💥 NDK SIGABRT 테스트 시작")
                    MainActivity.addExportLog("💥 NDK SIGABRT 크래시 트리거")
                    try {
                        NativeCrashCollector.triggerTestCrash(1)
                    } catch (e: Exception) {
                        MainActivity.addExportLog("❌ NDK 크래시 실패: ${e.message}")
                    }
                }
            ) {
                Text("SIGABRT", fontSize = 8.sp)
            }

            // SIGBUS 테스트
            Button(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                onClick = {
                    Log.e("WebViewSample", "💥 NDK SIGBUS 테스트 시작")
                    MainActivity.addExportLog("💥 NDK SIGBUS 크래시 트리거")
                    try {
                        NativeCrashCollector.triggerTestCrash(2)
                    } catch (e: Exception) {
                        MainActivity.addExportLog("❌ NDK 크래시 실패: ${e.message}")
                    }
                }
            ) {
                Text("SIGBUS", fontSize = 8.sp)
            }

            // SIGILL 테스트
            Button(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                onClick = {
                    Log.e("WebViewSample", "💥 NDK SIGILL 테스트 시작")
                    MainActivity.addExportLog("💥 NDK SIGILL 크래시 트리거")
                    try {
                        NativeCrashCollector.triggerTestCrash(3)
                    } catch (e: Exception) {
                        MainActivity.addExportLog("❌ NDK 크래시 실패: ${e.message}")
                    }
                }
            ) {
                Text("SIGILL", fontSize = 8.sp)
            }

            // SIGFPE 테스트
            Button(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                onClick = {
                    Log.e("WebViewSample", "💥 NDK SIGFPE 테스트 시작")
                    MainActivity.addExportLog("💥 NDK SIGFPE 크래시 트리거")
                    try {
                        NativeCrashCollector.triggerTestCrash(4)
                    } catch (e: Exception) {
                        MainActivity.addExportLog("❌ NDK 크래시 실패: ${e.message}")
                    }
                }
            ) {
                Text("SIGFPE", fontSize = 8.sp)
            }
        }

        // Export 로그 표시 영역
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.45f) // 나머지 45%를 로그 영역으로 사용
                .padding(horizontal = 8.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
            ) {
                Text(
                    text = "📡 Export Log",
                    color = Color.Green,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
                
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(exportLogs) { log ->
                        Text(
                            text = log,
                            color = Color.White,
                            fontSize = 8.sp, // 로그 텍스트 크기 축소
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 10.sp,
                            modifier = Modifier.padding(vertical = 0.dp) // 줄 간격 제거
                        )
                    }
                    
                    // 로그가 없을 때 안내 메시지
                    if (exportLogs.isEmpty()) {
                        item {
                            Text(
                                text = "대기 중... Export 로그와 WebView 브리지 로그가 여기에 표시됩니다.",
                                color = Color.Gray,
                                fontSize = 8.sp, // 안내 메시지 크기 축소
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 백그라운드 HTTP 네트워크 요청 시작
 */
private fun MainActivity.startBackgroundNetworkRequests() {
    lifecycleScope.launch {
        var requestCount = 0
        
        while (true) {
            try {
                delay(MainActivity.NETWORK_REQUEST_INTERVAL_MS)
                
                val urlToRequest = MainActivity.TEST_URLS[requestCount % MainActivity.TEST_URLS.size]
                requestCount++
                
                Log.i(MainActivity.TAG, "🔗 백그라운드 HTTP 요청 #$requestCount: $urlToRequest")
                MainActivity.addExportLog("🔗 HTTP 요청 #$requestCount: ${urlToRequest.substringAfter("//").substringBefore("/")}")
                
                val response = makeHttpRequest(urlToRequest)
                
                Log.i(MainActivity.TAG, "✅ HTTP 응답 #$requestCount: ${response.substring(0, minOf(100, response.length))}...")
                MainActivity.addExportLog("✅ HTTP 응답 #$requestCount 수신 (${response.length} bytes)")
                
            } catch (e: Exception) {
                Log.e(MainActivity.TAG, "❌ 백그라운드 HTTP 요청 실패: ${e.message}")
                MainActivity.addExportLog("❌ HTTP 요청 실패: ${e.message}")
            }
        }
    }
}

/**
 * OkHttp를 사용한 HTTP 요청 실행 (WhatapAgent 네트워크 수집을 위함)
 */
suspend fun makeHttpRequest(urlString: String): String = withContext(Dispatchers.IO) {
    val client = OkHttpClient.Builder()
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    
    val request = Request.Builder()
        .url(urlString)
        .addHeader("User-Agent", "WhatapAgent-Android-WebView-Sample/1.0")
        .addHeader("X-Test-Source", "BackgroundRequest")
        .build()
    
    var response: Response? = null
    try {
        response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        
        "HTTP ${response.code}: ${responseBody.take(200)}..." // 200자로 제한
        
    } catch (e: Exception) {
        "Error: ${e.message}"
    } finally {
        response?.close()
    }
}

/**
 * 외부 도메인으로 OkHttp 테스트 요청 실행
 */
suspend fun testExternalDomainRequest() = withContext(Dispatchers.IO) {
    try {
        // 테스트용 외부 API 도메인들
        val testDomains = listOf(
            "https://api.coindesk.com/v1/bpi/currentprice.json",  // Bitcoin 가격 API
            "https://dog.ceo/api/breeds/image/random",            // Random Dog Image API
            "https://api.quotable.io/random",                     // Random Quote API
            "https://api.ipify.org?format=json",                  // Public IP API
            "https://worldtimeapi.org/api/timezone/Asia/Seoul"    // 세계 시간 API
        )

        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        val randomDomain = testDomains.random()
        Log.i(MainActivity.TAG, "🚀 외부 도메인 테스트: $randomDomain")
        MainActivity.addExportLog("🚀 외부 API 호출: ${randomDomain.substringAfter("//").substringBefore("/")}")

        val request = Request.Builder()
            .url(randomDomain)
            .addHeader("User-Agent", "WhatapAgent-Android-Test/1.0")
            .addHeader("X-Test-Type", "External-Domain-Test")
            .addHeader("X-Device", android.os.Build.MODEL)
            .addHeader("X-Request-Time", System.currentTimeMillis().toString())
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        Log.i(MainActivity.TAG, "✅ 외부 도메인 응답: ${response.code} - $randomDomain")
        Log.d(MainActivity.TAG, "📦 응답 내용: ${responseBody.take(200)}")

        MainActivity.addExportLog("✅ 외부 API 응답: ${response.code} - ${responseBody.take(50)}...")

        response.close()

        // 추가로 실패하는 도메인도 테스트
        if (Math.random() > 0.5) {
            val failDomain = "https://non-existent-domain-${System.currentTimeMillis()}.com/api/test"
            Log.i(MainActivity.TAG, "🔴 실패 도메인 테스트: $failDomain")
            MainActivity.addExportLog("🔴 실패 테스트: $failDomain")

            try {
                val failRequest = Request.Builder()
                    .url(failDomain)
                    .build()

                val failResponse = client.newCall(failRequest).execute()
                failResponse.close()
            } catch (e: Exception) {
                Log.e(MainActivity.TAG, "❌ 예상된 실패: ${e.message}")
                MainActivity.addExportLog("❌ 도메인 해결 실패: ${e.message?.take(50)}")
            }
        }

    } catch (e: Exception) {
        Log.e(MainActivity.TAG, "❌ 외부 도메인 요청 실패: ${e.message}")
        MainActivity.addExportLog("❌ 외부 API 실패: ${e.message}")
    }
}

