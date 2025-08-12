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
import androidx.compose.runtime.LaunchedEffect
import android.util.Log
import io.whatap.android.agent.instrumentation.userlog.UserLogger
import android.os.Handler
import android.os.Looper
import io.whatap.android.agent.instrumentation.screengroup.ChainView
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

class MainActivity : FragmentActivity() {
    companion object {
        const val TAG = "WebViewSample"
        const val RELOAD_INTERVAL_MS = 10000L // 10초
        const val NETWORK_REQUEST_INTERVAL_MS = 5000L // 5초 네트워크 요청 간격
        
        // Activity→Fragment Chain 관리용 변수
        var activityFragmentChainId: String? = null
        var activityFragmentChainView: ChainView? = null
        
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
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WebView.setWebContentsDebuggingEnabled(true)

        // QAFileLogger는 Application에서 이미 설정됨
        Log.i(TAG, "📄 QAFileLogger가 Application에서 설정됨")
        
        // Export 로그 수집 시작
        startLogCollection()
        
        // 백그라운드 네트워크 요청 시작
        startBackgroundNetworkRequests()
        
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

        // ScreenGroup은 라이브러리에서 자동으로 처리됨 (수동 호출 제거)
        Log.i(TAG, "ℹ️ ScreenGroup은 라이브러리에서 자동으로 시작됩니다")

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

        // Activity → Fragment → WebView 구조 설정
        Log.i(TAG, "🔧 Activity → Fragment → WebView 구조 준비 중...")
        
        // 🔗 Activity→Fragment Chain 시작
        try {
            activityFragmentChainView = ChainView.getInstance()
            activityFragmentChainId = "activity-fragment-${System.currentTimeMillis()}"
            activityFragmentChainView?.startChain("ActivityFragmentChain", activityFragmentChainId!!)
            Log.i(TAG, "🔗 Activity→Fragment Chain 시작: $activityFragmentChainId")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Activity→Fragment Chain 시작 실패: ${e.message}", e)
            // Chain 실패 시에도 Fragment는 생성해야 함
        }
        
        val fragment = TestFragment()
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(android.R.id.content, fragment)
        fragmentTransaction.commit()
        
        Log.i(TAG, "✅ Activity → Fragment → WebView 구조 설정 완료")
    }
    
    
    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "🛑 MainActivity 종료")
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
                    settings.javaScriptEnabled = true

                    val bridge = WhatapWebviewBridge(ctx)
                    bridge.configureWebView(this)
                    bridge.startDataUploadTimer()
                    
                    // 🔥 핵심 수정: WhatapWebViewClient 사용 (최신 빌드 버전)
                    webViewClient = object : WhatapWebViewClient(bridge) {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            url?.let {
                                Log.i("WebViewSample", "🌐 WebView 페이지 로드 시작: $it")
                                // Chain은 이미 시작되었으므로 추가 작업 불필요
                            }
                        }
                        
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            url?.let {
                                Log.i("WebViewSample", "✅ WebView 페이지 로드 완료: $it")
                                Toast.makeText(ctx, "페이지 로드 완료: $it", Toast.LENGTH_SHORT).show()
                            }
                        }
                        
                        override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                            super.doUpdateVisitedHistory(view, url, isReload)
                            if (!isReload && url != null) {
                                Log.i("WebViewSample", "🔄 WebView 내부 네비게이션: $url")
                            }
                        }
                    }

                    // 🌐 기존 localhost:18000 서버로 로드 (IP는 동적으로 설정)
                    loadUrl(initialUrl)
                    webViewRef.value = this
                }
            }
        )

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
 * logcat을 통해 export 로그 수집
 */
private fun MainActivity.startLogCollection() {
    // 디버깅: 로그 수집 시작 알림
    MainActivity.addExportLog("🔍 로그 수집 시스템 시작...")
    
    Thread {
        try {
            // WebView 브리지 로그와 HttpSpanExporter 로그 수집
            val process = Runtime.getRuntime().exec("logcat -v brief")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            
            MainActivity.addExportLog("✅ logcat 실행 성공")
            
            var line: String?
            var lineCount = 0
            while (reader.readLine().also { line = it } != null) {
                lineCount++
                
                // 디버깅: 100번째 라인마다 알림 
                if (lineCount % 100 == 0) {
                    MainActivity.addExportLog("📊 로그 수집 중... $lineCount 라인 처리됨")
                }
                
                line?.let { logLine ->
                    // HttpSpanExporter, WebView 브리지, 네트워크 관련 로그 필터링
                    if (logLine.contains("HttpSpanExporter") || 
                        logLine.contains("실제 전송 데이터") || 
                        logLine.contains("전송 span 개수") ||
                        logLine.contains("JSON payload") ||
                        logLine.contains("export") ||
                        logLine.contains("POST") ||
                        logLine.contains("trace_id") ||
                        logLine.contains("span_id") ||
                        // WebView 브리지 함수 호출 로그
                        logLine.contains("🔥") ||
                        logLine.contains("Bridge") ||
                        logLine.contains("generateUUID") ||
                        logLine.contains("pageLoad") ||
                        logLine.contains("webVitals") ||
                        logLine.contains("whatap_bridge") ||
                        logLine.contains("WhatapWebviewBridge") ||
                        // 네트워크 instrumentation 로그
                        logLine.contains("OkHttp") ||
                        logLine.contains("okhttp") ||
                        logLine.contains("HttpLog") ||
                        logLine.contains("NetworkTrace") ||
                        logLine.contains("BackgroundRequest") ||
                        logLine.contains("httpbin.org") ||
                        logLine.contains("jsonplaceholder")) {
                        
                        // 로그에서 태그와 메시지 부분만 추출
                        val cleanLog = logLine.substringAfter(": ").take(100) // 100자로 제한
                        MainActivity.addExportLog("🔴 $cleanLog")
                    }
                }
            }
        } catch (e: Exception) {
            MainActivity.addExportLog("❌ 로그 수집 오류: ${e.message}")
        }
    }.start()
    
    // 정기적으로 테스트 로그 추가 (개발용)
    Thread {
        var count = 0
        while (true) {
            Thread.sleep(15000) // 15초마다
            MainActivity.addExportLog("⏰ 데이터 수집 대기 중... ${++count}")
        }
    }.start()
}