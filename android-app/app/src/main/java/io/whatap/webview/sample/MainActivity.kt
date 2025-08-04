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
import io.whatap.android.agent.instrumentation.screengroup.ScreenGroupManager
import io.whatap.android.agent.instrumentation.screengroup.ChainView
import io.whatap.android.agent.instrumentation.userlog.UserLogger
import android.os.Handler
import android.os.Looper
import java.net.HttpURLConnection
import java.net.URL
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

class MainActivity : FragmentActivity() {
    companion object {
        const val TAG = "WebViewSample"
        val chainView = ChainView() // public으로 변경
        const val RELOAD_INTERVAL_MS = 10000L // 10초
        const val NETWORK_REQUEST_INTERVAL_MS = 5000L // 5초 네트워크 요청 간격
        
        // 백그라운드 네트워크 요청용 URLs
        val TEST_URLS = listOf(
            "https://httpbin.org/get",
            "https://jsonplaceholder.typicode.com/posts/1",
            "https://api.github.com/zen",
            "https://httpbin.org/uuid",
            "https://httpbin.org/delay/1"
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
                        ServerUrlEditor()
                        Spacer(modifier = Modifier.height(8.dp))
                        FragmentTestButton()
                        Spacer(modifier = Modifier.height(8.dp))
                        WebViewWithUrlController(initialUrl = urlFromIntent)
                    }
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "🛑 MainActivity 종료")
        // 실제 ScreenGroup 정리
        try {
            chainView.endTask("main-activity")
            Log.i(TAG, "✅ 실제 ScreenGroup 정리 완료")
        } catch (e: Exception) {
            Log.d(TAG, "MainActivity 작업 이미 종료됨: ${e.message}")
        }
    }
}


@Composable
fun FragmentTestButton() {
    val context = LocalContext.current as FragmentActivity
    
    Button(
        onClick = {
            Log.i("WebViewSample", "🔄 Fragment 테스트 버튼 클릭 - Activity → Fragment Chain 시작")
            
            // UserLogger API를 사용한 커스텀 로그
            try {
                // 간단한 문자열 로그
                UserLogger.print("Fragment test button clicked")
                
                // 구조화된 로그 데이터
                val logData = HashMap<Any, Any>()
                logData["event_type"] = "user_action"
                logData["action"] = "fragment_navigation"
                logData["button_id"] = "fragment_test_button"
                logData["screen_name"] = "MainActivity"
                logData["user_id"] = "test_user_123"
                logData["timestamp"] = System.currentTimeMillis()
                logData["device_model"] = android.os.Build.MODEL
                logData["os_version"] = android.os.Build.VERSION.RELEASE
                
                UserLogger.print(logData)
                Log.i("WebViewSample", "📝 UserLogger 커스텀 로그 전송 완료")
            } catch (e: Exception) {
                Log.e("WebViewSample", "❌ UserLogger 커스텀 로그 전송 실패: ${e.message}")
            }
            
            try {
                // Activity → Fragment Chain 연결
                MainActivity.chainView.startChain("ActivityFragmentChain", "act-frag-${System.currentTimeMillis()}")
                MainActivity.chainView.endTask("main-activity")
                MainActivity.chainView.startTask("TestFragment", "fragment-transition")
                
                context.supportFragmentManager.beginTransaction()
                    .replace(android.R.id.content, TestFragment())
                    .addToBackStack("TestFragment")
                    .commit()
                    
                Log.i("WebViewSample", "✅ Activity → Fragment Chain 연결 및 Fragment 실행 성공")
            } catch (e: Exception) {
                Log.e("WebViewSample", "❌ Fragment 실행 실패: ${e.message}")
                Toast.makeText(context, "Fragment 실행 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        Text("📱 Activity → Fragment → WebView Chain 테스트")
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
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            modifier = Modifier.weight(1f),
            value = text,
            onValueChange = { text = it },
            label = { Text("Server URL") }
        )
        Button(onClick = {
            sharedPrefs.edit().putString("server_url", text.text).apply()
            Toast.makeText(context, "서버 주소가 저장되었습니다.\n앱을 재시작해주세요.", Toast.LENGTH_LONG).show()
        }) {
            Text("저장")
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
    
    // 10초마다 자동 리로드
    LaunchedEffect(webViewRef.value) {
        webViewRef.value?.let {
            while (true) {
                delay(MainActivity.RELOAD_INTERVAL_MS)
                Log.i("WebViewSample", "🔄 자동 리로드 실행 (10초 간격)")
                it.reload()
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.weight(0.6f), // WebView 비율을 60%로 줄임
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true

                    val bridge = WhatapWebviewBridge(ctx)
                    bridge.configureWebView(this)
                    bridge.startDataUploadTimer()
                    
                    // 🔥 핵심 수정: WhatapWebViewClient 사용 (통합 버전 API)
                    webViewClient = object : WhatapWebViewClient(bridge) {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            url?.let {
                                Log.i("WebViewSample", "🌐 WebView 페이지 로드 시작: $it")
                                try {
                                    // 실제 WebView Chain 시작
                                    MainActivity.chainView.startChain("WebLoadChain", "web-chain-${it.hashCode()}")
                                    MainActivity.chainView.endTask("main-activity")
                                    MainActivity.chainView.startTask(it, "webview-${it.hashCode()}")
                                    Log.i("WebViewSample", "✅ 실제 ScreenGroup WebView Chain 시작: $it")
                                } catch (e: Exception) {
                                    Log.e("WebViewSample", "❌ WebView Chain 시작 실패: ${e.message}")
                                }
                            }
                        }
                        
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            url?.let {
                                Log.i("WebViewSample", "✅ WebView 페이지 로드 완료: $it")
                                try {
                                    // 실제 WebView Chain 종료
                                    MainActivity.chainView.endChain("web-chain-${it.hashCode()}")
                                    Log.i("WebViewSample", "✅ 실제 ScreenGroup WebView Chain 종료: $it")
                                } catch (e: Exception) {
                                    Log.e("WebViewSample", "❌ WebView Chain 종료 실패: ${e.message}")
                                }
                                Toast.makeText(ctx, "페이지 로드 완료: $it", Toast.LENGTH_SHORT).show()
                            }
                        }
                        
                        override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                            super.doUpdateVisitedHistory(view, url, isReload)
                            if (!isReload && url != null) {
                                Log.i("WebViewSample", "🔄 WebView 내부 네비게이션: $url")
                                try {
                                    // 실제 페이지 네비게이션 Chain
                                    view?.url?.let { currentUrl ->
                                        if (currentUrl != url) {
                                            MainActivity.chainView.endTask("webview-${currentUrl.hashCode()}")
                                            MainActivity.chainView.startChain("NavigationChain", "nav-chain-${url.hashCode()}")
                                            MainActivity.chainView.startTask(url, "webview-${url.hashCode()}")
                                            MainActivity.chainView.endChain("nav-chain-${url.hashCode()}")
                                            Log.i("WebViewSample", "✅ 실제 페이지 네비게이션: $currentUrl → $url")
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("WebViewSample", "❌ 네비게이션 Chain 실패: ${e.message}")
                                }
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
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                modifier = Modifier.weight(1f),
                value = urlState,
                onValueChange = { urlState = it },
                label = { Text("현재 URL") }
            )
            Button(onClick = {
                val urlToLoad = urlState.text
                if (urlToLoad.startsWith("http")) {
                    webViewRef.value?.loadUrl(urlToLoad)
                } else {
                    Toast.makeText(context, "유효한 URL이 아닙니다.", Toast.LENGTH_SHORT).show()
                }
            }) {
                Text("이동")
            }
        }
        
        // Export 로그 표시 영역 (더 큰 크기)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.4f) // 나머지 40%를 로그 영역으로 사용
                .padding(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                Text(
                    text = "📡 Export 로그 & 🌏 WebView 브리지 (실시간)",
                    color = Color.Green,
                    fontSize = 14.sp, // 제목 크기 증가
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(exportLogs) { log ->
                        Text(
                            text = log,
                            color = Color.White,
                            fontSize = 11.sp, // 로그 텍스트 크기 증가
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(vertical = 1.5.dp) // 줄 간격 증가
                        )
                    }
                    
                    // 로그가 없을 때 안내 메시지
                    if (exportLogs.isEmpty()) {
                        item {
                            Text(
                                text = "대기 중... Export 로그와 WebView 브리지 로그가 여기에 표시됩니다.",
                                color = Color.Gray,
                                fontSize = 11.sp, // 안내 메시지 크기도 증가
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
 * HTTP 요청 실행
 */
suspend fun makeHttpRequest(urlString: String): String = withContext(Dispatchers.IO) {
    var connection: HttpURLConnection? = null
    try {
        val url = URL(urlString)
        connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        connection.setRequestProperty("User-Agent", "WhatapAgent-Android-WebView-Sample/1.0")
        
        val responseCode = connection.responseCode
        
        val inputStream = if (responseCode == HttpURLConnection.HTTP_OK) {
            connection.inputStream
        } else {
            connection.errorStream
        }
        
        val reader = BufferedReader(InputStreamReader(inputStream))
        val response = StringBuilder()
        var line: String?
        
        while (reader.readLine().also { line = it } != null) {
            response.append(line).append("\n")
        }
        
        reader.close()
        
        "HTTP $responseCode: ${response.toString()}"
        
    } catch (e: Exception) {
        throw e
    } finally {
        connection?.disconnect()
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
                    // HttpSpanExporter와 WebView 브리지 관련 로그 필터링
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
                        logLine.contains("WhatapWebviewBridge")) {
                        
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