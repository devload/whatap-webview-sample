package io.whatap.webview.sample

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import io.whatap.android.agent.webview.WhatapWebViewClient
import io.whatap.android.agent.webview.WhatapWebviewBridge
import io.whatap.webview.sample.ui.theme.WebviewTheme
import android.util.Log
import kotlinx.coroutines.delay
import androidx.compose.runtime.LaunchedEffect
import io.whatap.android.agent.instrumentation.screengroup.ChainView

class TestFragment : Fragment() {
    companion object {
        private const val TAG = "TestFragment"
        
        // Fragment→WebView Chain 관리용 변수
        private var fragmentWebViewChainId: String? = null
        private var fragmentWebViewChainView: ChainView? = null
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.i(TAG, "🔄 TestFragment onCreateView 시작 - Fragment instrumentation 테스트 중")
        Log.i(TAG, "📊 Fragment 구조: Activity → Fragment → WebView")
        
        // 🔗 Activity→Fragment Chain 종료 (MainActivity에서 시작된 Chain)
        try {
            MainActivity.activityFragmentChainView?.let { chainView ->
                MainActivity.activityFragmentChainId?.let { chainId ->
                    chainView.endChain(chainId)
                    Log.i(TAG, "🔗 Activity→Fragment Chain 종료: $chainId")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Activity→Fragment Chain 종료 실패: ${e.message}", e)
        }
        
        // 🔗 Fragment→WebView Chain 시작 (onCreateView에서 미리 시작)
        try {
            fragmentWebViewChainView = ChainView.getInstance()
            fragmentWebViewChainId = "fragment-webview-${System.currentTimeMillis()}"
            fragmentWebViewChainView?.startChain("FragmentWebViewChain", fragmentWebViewChainId!!)
            Log.i(TAG, "🔗 Fragment→WebView Chain 시작 (onCreateView): $fragmentWebViewChainId")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Fragment→WebView Chain 시작 실패: ${e.message}", e)
        }
        
        // Fragment 시작 작업은 MainActivity에서 이미 수행되므로 생략
        Log.i(TAG, "✅ Fragment ScreenGroup 작업은 MainActivity에서 이미 시작됨: fragment-${hashCode()}")
        Log.i(TAG, "🎯 WhatapAndroidPlugin Fragment instrumentation 감지 대기 중...")
        
        
        return ComposeView(requireContext()).apply {
            setContent {
                WebviewTheme {
                    FragmentContent()
                }
            }
        }
    }
    
    @Composable
    private fun FragmentContent() {
        val defaultUrl = "http://192.168.1.6:18000/#whatap_debug_mode#android_test#bridge_debug"
        val urlFromIntent = activity?.intent?.getStringExtra("URL") ?: defaultUrl
        
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                // 컴팩트한 레이아웃: 높이와 패딩 축소
                ServerUrlEditor()
                WebViewWithUrlController(initialUrl = urlFromIntent)
            }
        }
    }
    
    @Composable
    private fun ServerUrlEditor() {
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
    private fun WebViewWithUrlController(initialUrl: String) {
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
        
        // 자동 리로드 비활성화
        // LaunchedEffect(webViewRef.value) {
        //     webViewRef.value?.let {
        //         while (true) {
        //             delay(MainActivity.RELOAD_INTERVAL_MS)
        //             Log.i(TAG, "🔄 자동 리로드 실행 (10초 간격)")
        //             it.reload()
        //         }
        //     }
        // }

        Column(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                modifier = Modifier.weight(0.55f), // WebView 비율을 55%로 설정
                factory = { ctx ->
                    // Fragment→WebView Chain은 이미 onCreateView에서 시작됨
                    Log.i(TAG, "🌐 WebView factory 실행 - Chain은 이미 활성화됨")
                    
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
                                    Log.i(TAG, "🌐 Fragment WebView 페이지 로드 시작: $it")
                                }
                            }
                            
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                url?.let {
                                    Log.i(TAG, "✅ Fragment WebView 페이지 로드 완료: $it")
                                    Toast.makeText(ctx, "페이지 로드 완료: $it", Toast.LENGTH_SHORT).show()
                                    
                                    // Fragment→WebView Chain 종료는 WhatapWebViewClient.onPageStarted에서 자동 처리됨
                                    Log.i(TAG, "🔗 Chain 자동 종료는 WhatapWebViewClient에서 처리됨")
                                    
                                    // pageLoad 이벤트를 JavaScript에서 직접 발생시켜 테스트
                                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                        try {
                                            val testData = """{
                                                "meta": {
                                                    "pageLocation": "$it"
                                                },
                                                "pageLoad": {
                                                    "totalDuration": 2574,
                                                    "navigationTiming": {
                                                        "data": {
                                                            "loadTime": 2452,
                                                            "backendTime": 106,
                                                            "renderTime": {
                                                                "duration": 2063
                                                            }
                                                        }
                                                    }
                                                }
                                            }"""
                                            val testTaskId = "test_task_" + System.currentTimeMillis()
                                            val testTraceId = "trace_" + System.currentTimeMillis()
                                            
                                            view?.evaluateJavascript("""
                                                if (window.whatapBridge) {
                                                    console.log('🔥 Testing pageLoad with data: $testData');
                                                    window.whatapBridge.pageLoad('$testData', '$testTaskId', '$testTraceId');
                                                    console.log('✅ pageLoad call completed');
                                                } else {
                                                    console.log('❌ whatapBridge not available');
                                                }
                                            """.trimIndent()) { result ->
                                                Log.i(TAG, "🔥 JavaScript pageLoad test result: $result")
                                            }
                                        } catch (e: Exception) {
                                            Log.e(TAG, "❌ JavaScript pageLoad test failed: ${e.message}")
                                        }
                                    }, 2000) // 2초 후 실행
                                }
                            }
                            
                            override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                                super.doUpdateVisitedHistory(view, url, isReload)
                                if (!isReload && url != null) {
                                    Log.i(TAG, "🔄 Fragment WebView 내부 네비게이션: $url")
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
    
    override fun onDestroyView() {
        super.onDestroyView()
        Log.i(TAG, "🛑 TestFragment onDestroyView")
    }
}