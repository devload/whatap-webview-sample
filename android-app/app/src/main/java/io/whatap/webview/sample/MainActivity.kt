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
import io.whatap.android.agent.webview.WhatapWebViewClient
import io.whatap.android.agent.webview.WhatapWebviewBridge
import io.whatap.webview.sample.ui.theme.WebviewTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import androidx.compose.runtime.LaunchedEffect
import android.util.Log
import io.whatap.android.agent.instrumentation.screengroup.ScreenGroupManager
import io.whatap.android.agent.instrumentation.screengroup.ChainView
import io.whatap.android.agent.instrumentation.userlog.UserLogger
import android.os.Handler
import android.os.Looper
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
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
import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment

class MainActivity : FragmentActivity() {
    companion object {
        const val TAG = "WebViewSample"
        val chainView = ChainView() // public으로 변경
        const val RELOAD_INTERVAL_MS = 10000L // 10초
        const val NETWORK_REQUEST_INTERVAL_MS = 5000L // 5초 네트워크 요청 간격
        
        // logcat 프로세스 관리 (메모리 누수 방지)
        @Volatile
        internal var logcatProcess: Process? = null
        @Volatile
        internal var logcatThread: Thread? = null
        
        // 백그라운드 네트워크 요청용 URLs (WhatapAgent 네트워크 수집 테스트)
        val TEST_URLS = listOf(
            "https://httpbin.org/get",                           // GET 요청 테스트
            "https://jsonplaceholder.typicode.com/posts/1",      // JSON API 테스트  
            "https://api.github.com/zen",                        // GitHub API 테스트
            "https://httpbin.org/uuid",                          // UUID 생성 테스트
            "https://httpbin.org/delay/2",                       // 지연 응답 테스트 (2초)
            "https://httpbin.org/status/200",                    // 정상 응답 테스트
            "https://httpbin.org/status/404",                    // 에러 응답 테스트
            "https://jsonplaceholder.typicode.com/users/1"       // 사용자 정보 API 테스트
        )
        
        // Export 로그를 위한 StateFlow
        private val _exportLogs = MutableStateFlow<List<String>>(emptyList())
        val exportLogs = _exportLogs.asStateFlow()
        
        // 로그 추가 함수
        fun addExportLog(message: String) {
            val timestamp = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()).format(java.util.Date())
            val logEntry = "[$timestamp] $message"
            
            // LogRepository에 저장
            LogRepository.addLog(logEntry)
            
            // StateFlow 업데이트
            _exportLogs.value = LogRepository.getAllLogs()
        }
        
        // logcat 프로세스 안전하게 정리
        fun stopLogCollection() {
            try {
                logcatThread?.interrupt()
                logcatThread = null
                
                logcatProcess?.destroyForcibly()
                logcatProcess = null
                
                Log.i(TAG, "✅ logcat 프로세스 정리 완료")
            } catch (e: Exception) {
                Log.e(TAG, "❌ logcat 프로세스 정리 실패: ${e.message}")
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WebView.setWebContentsDebuggingEnabled(true)

        // QAFileLogger는 Application에서 이미 설정됨
        Log.i(TAG, "📄 QAFileLogger가 Application에서 설정됨")
        
        // 기존 로그 복원
        _exportLogs.value = LogRepository.getAllLogs()
        
        // Intent에서 재시작 여부 확인
        val isRestart = intent.getBooleanExtra("IS_RESTART", false)
        
        // 첫 실행일 때만 로그 수집과 네트워크 요청 시작
        if (!isRestart) {
            // Export 로그 수집 시작
            startLogCollection()
            
            // 백그라운드 네트워크 요청 시작
            startBackgroundNetworkRequests()
            
            Log.i(TAG, "🚀 첫 실행 - 모든 백그라운드 작업 시작")
            addExportLog("🚀 첫 실행 - 백그라운드 작업 시작")
        } else {
            Log.i(TAG, "🔄 재시작된 Activity - 백그라운드 작업 스킵")
            addExportLog("🔄 재시작된 Activity")
        }
        
        // 10초마다 Activity 재시작
        startActivityRestart()
        
        // 테스트용 초기 로그 추가
        addExportLog("🚀 WhatapAgent 모니터링 시작")
        addExportLog("📱 디바이스: ${android.os.Build.MODEL}")
        addExportLog("🌐 프록시 서버: http://192.168.1.73:8080")
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

        // 실제 ScreenGroup 시작
        Log.i(TAG, "🔄 실제 ScreenGroup 시작: WebViewFlow")
        try {
            ScreenGroupManager.getInstance().startGroup("WebViewFlow")
            chainView.startTask("MainActivity", "main-activity")
            Log.i(TAG, "✅ 실제 ScreenGroup 및 Chain 시작 성공")
        } catch (e: Exception) {
            Log.e(TAG, "❌ ScreenGroup 시작 실패: ${e.message}")
        }

        // UserLogger API 테스트 - onCreate에서 실행
        Log.i(TAG, "📝 UserLogger API 테스트 시작")
        try {
            // 간단한 문자열 로그
            UserLogger.print("MainActivity onCreate - UserLogger test")
            
            // 구조화된 로그 데이터
            val logData = HashMap<Any, Any>()
            logData["event_type"] = "lifecycle"
            logData["action"] = "activity_created"
            logData["activity_name"] = "MainActivity"
            logData["screen_group"] = "WebViewFlow"
            logData["timestamp"] = System.currentTimeMillis()
            logData["device_model"] = android.os.Build.MODEL
            logData["os_version"] = android.os.Build.VERSION.RELEASE
            logData["app_version"] = "1.0.0"
            
            UserLogger.print(logData)
            Log.i(TAG, "✅ UserLogger 커스텀 로그 전송 완료")
        } catch (e: Exception) {
            Log.e(TAG, "❌ UserLogger 커스텀 로그 전송 실패: ${e.message}")
        }

        val defaultUrl = "http://192.168.1.6:18000/"
        val urlFromIntent = intent.getStringExtra("URL") ?: defaultUrl

        setContent {
            WebviewTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)) {
                        // 컴팩트한 레이아웃: 높이와 패딩 축소
                        ServerUrlEditor()
                        
                        // Fragment 영역 (WebView를 포함) - 전체 영역 사용
                        FragmentContainer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "🛑 MainActivity 종료")
        
        // logcat 프로세스 정리 (메모리 누수 방지)
        val isRestart = intent.getBooleanExtra("IS_RESTART", false)
        if (!isRestart) {
            // 마지막 Activity 종료 시에만 정리
            stopLogCollection()
        }
        
        // 실제 ScreenGroup 정리
        try {
            chainView.endTask("main-activity")
            Log.i(TAG, "✅ 실제 ScreenGroup 정리 완료")
        } catch (e: Exception) {
            Log.d(TAG, "MainActivity 작업 이미 종료됨: ${e.message}")
        }
    }
    
    /**
     * 10초마다 Activity 재시작
     */
    private fun startActivityRestart() {
        lifecycleScope.launch(Dispatchers.IO) { // IO 스레드에서 실행
            // 10초 대기 후 Activity 재시작
            delay(10000L)
            
            Log.i(TAG, "🔄 Activity 재시작 (10초 타이머)")
            
            withContext(Dispatchers.Main) { // UI 작업은 메인 스레드에서
                addExportLog("🔄 Activity 재시작 중...")
                
                // 새로운 MainActivity 시작 (IS_RESTART 플래그 추가)
                val intent = Intent(this@MainActivity, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("URL", "http://192.168.1.6:18000/")
                    putExtra("IS_RESTART", true)  // 재시작 플래그
                }
                startActivity(intent)
                
                // 현재 Activity 종료
                finish()
            }
        }
    }
    
}



@Composable
fun ServerUrlEditor() {
    val context = LocalContext.current
    val sharedPrefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    val defaultUrl = "https://rumote.whatap-mobile-agent.io/m"
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

// WebViewWithUrlController 함수 제거됨 - Fragment 내부에서 WebView 처리

/**
 * 백그라운드 HTTP 네트워크 요청 시작
 */
private fun MainActivity.startBackgroundNetworkRequests() {
    lifecycleScope.launch(Dispatchers.IO) { // IO 디스패처 사용
        var requestCount = 0
        
        while (isActive) { // isActive로 코루틴 상태 체크
            try {
                delay(MainActivity.NETWORK_REQUEST_INTERVAL_MS)
                
                // 코루틴이 취소되었는지 확인
                if (!isActive) break
                
                val urlToRequest = MainActivity.TEST_URLS[requestCount % MainActivity.TEST_URLS.size]
                requestCount++
                
                Log.i(MainActivity.TAG, "🔗 백그라운드 HTTP 요청 #$requestCount: $urlToRequest")
                MainActivity.addExportLog("🔗 HTTP 요청 #$requestCount: ${urlToRequest.substringAfter("//").substringBefore("/")}")
                
                // 네트워크 요청 실행
                try {
                    val response = makeHttpRequest(urlToRequest)
                    Log.i(MainActivity.TAG, "✅ HTTP 응답 #$requestCount: ${response.substring(0, minOf(100, response.length))}...")
                    MainActivity.addExportLog("✅ HTTP 응답 #$requestCount 수신 (${response.length} bytes)")
                } catch (e: java.util.concurrent.CancellationException) {
                    // 코루틴 취소는 정상 동작이므로 무시
                    Log.d(MainActivity.TAG, "⏸️ 백그라운드 HTTP 요청 취소됨")
                    break
                } catch (e: Exception) {
                    // Job was cancelled 에러는 무시
                    if (e.message?.contains("Job was cancelled") == true || 
                        e.message?.contains("cancelled", ignoreCase = true) == true) {
                        Log.d(MainActivity.TAG, "⏸️ 백그라운드 작업 종료")
                        break
                    }
                    Log.e(MainActivity.TAG, "❌ 백그라운드 HTTP 요청 실패: ${e.message}")
                    MainActivity.addExportLog("❌ HTTP 요청 실패: ${e.message?.take(30)}")
                }
                
            } catch (e: kotlinx.coroutines.CancellationException) {
                // 코루틴 취소는 정상 동작
                Log.d(MainActivity.TAG, "⏸️ 백그라운드 작업 정상 종료")
                break
            } catch (e: Exception) {
                // Job was cancelled 에러는 로그에 표시하지 않음
                if (!e.message.orEmpty().contains("cancelled", ignoreCase = true)) {
                    Log.e(MainActivity.TAG, "❌ 백그라운드 오류: ${e.message}")
                }
            }
        }
        
        Log.i(MainActivity.TAG, "🛑 백그라운드 HTTP 요청 루프 종료")
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
 * logcat을 통해 export 로그 수집 (안전한 프로세스 관리)
 */
private fun MainActivity.startLogCollection() {
    // 이미 실행 중인 프로세스가 있으면 정리
    MainActivity.stopLogCollection()
    
    // 디버깅: 로그 수집 시작 알림
    MainActivity.addExportLog("🔍 로그 수집 시스템 시작...")
    
    val thread = Thread {
        var process: Process? = null
        try {
            // WebView 브리지 로그와 HttpSpanExporter 로그 수집
            process = Runtime.getRuntime().exec("logcat -v brief")
            MainActivity.logcatProcess = process
            val reader = BufferedReader(InputStreamReader(process?.inputStream))
            
            MainActivity.addExportLog("✅ logcat 실행 성공")
            
            var line: String?
            var lineCount = 0
            while (!Thread.currentThread().isInterrupted) {
                try {
                    line = reader.readLine()
                    if (line == null) break
                    
                    lineCount++
                    
                    // 디버깅: 500번째 라인마다만 알림 (빈도 감소)
                    if (lineCount % 500 == 0) {
                        MainActivity.addExportLog("📊 로그 수집 중... $lineCount")
                    }
                    
                    // 너무 많은 로그 필터링으로 인한 부하 감소
                    if (lineCount % 10 == 0) { // 10개 중 1개만 검사
                        line?.let { logLine ->
                            // 주요 로그만 필터링 (조건 단순화)
                            if (logLine.contains("HttpSpanExporter") || 
                                logLine.contains("chain-group") ||
                                logLine.contains("screen-group") ||
                                logLine.contains("Created") ||
                                logLine.contains("Fragment")) {
                                
                                // 로그에서 태그와 메시지 부분만 추출
                                val cleanLog = logLine.substringAfter(": ").take(50) // 50자로 제한
                                MainActivity.addExportLog("🔴 $cleanLog")
                            }
                        }
                    }
                    
                    // CPU 부하 감소를 위한 짧은 대기
                    Thread.sleep(1)
                    
                } catch (e: InterruptedException) {
                    break // 스레드 인터럽트 시 즉시 종료
                } catch (e: Exception) {
                    // 개별 라인 오류는 무시하고 계속
                }
            }
        } catch (e: Exception) {
            MainActivity.addExportLog("❌ 로그 수집 오류: ${e.message}")
        } finally {
            // 프로세스 정리
            process?.destroy()
            MainActivity.logcatProcess = null
        }
    }
    MainActivity.logcatThread = thread
    thread.start()
    
    // 정기적으로 테스트 로그 추가 (개발용) - 첫 실행에만
    Thread {
        var count = 0
        while (!Thread.currentThread().isInterrupted) {
            try {
                Thread.sleep(15000) // 15초마다
                MainActivity.addExportLog("⏰ 데이터 수집 대기 중... ${++count}")
            } catch (e: InterruptedException) {
                // 스레드 인터럽트 시 종료
                break
            }
        }
    }.start()
}

// Fragment 표시를 위한 상태 관리
private val isFragmentVisible = mutableStateOf(false)
private var fragmentContainer: android.widget.FrameLayout? = null
private var fragmentContainerId: Int = 0

@Composable 
fun FragmentContainer(modifier: Modifier = Modifier) {
    val context = LocalActivity.current as FragmentActivity
    
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            // Fragment를 위한 컨테이너 생성
            fragmentContainer = android.widget.FrameLayout(ctx).apply {
                id = android.view.View.generateViewId()
                setBackgroundColor(android.graphics.Color.parseColor("#F5F5F5"))
            }
            fragmentContainerId = fragmentContainer!!.id
            
            // 초기 상태: Fragment를 지연 실행으로 표시
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                try {
                    context.supportFragmentManager.beginTransaction()
                        .replace(fragmentContainerId, TestFragment())
                        .commit()
                } catch (e: Exception) {
                    android.util.Log.e("WebViewSample", "Fragment 추가 실패: ${e.message}")
                }
            }
            
            fragmentContainer!!
        }
    )
}