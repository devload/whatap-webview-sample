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
import io.whatap.android.agent.instrumentation.screengroup.ChainView
import kotlinx.coroutines.delay
import androidx.compose.runtime.LaunchedEffect

class TestFragment : Fragment() {
    companion object {
        private const val TAG = "TestFragment"
        private val chainView = ChainView()
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.i(TAG, "🔄 TestFragment onCreateView 시작")
        
        // Fragment 시작 작업은 MainActivity에서 이미 수행되므로 생략
        Log.i(TAG, "✅ Fragment ScreenGroup 작업은 MainActivity에서 이미 시작됨: fragment-${hashCode()}")
        
        
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
        val defaultUrl = "http://192.168.1.6:18000/"
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
        val defaultUrl = "http://192.168.1.73:8080/m"
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
                                    try {
                                        // Fragment → WebView Chain 연결 (WhatapWebViewClient에서 이미 WebView task 생성)
                                        chainView.startChain("FragmentWebChain", "frag-web-${it.hashCode()}")
                                        chainView.endTask("fragment-${hashCode()}")
                                        // WebView task는 WhatapWebViewClient에서 자동 생성되므로 별도 생성하지 않음
                                        Log.i(TAG, "✅ Fragment → WebView Chain 연결 성공 (WebView task는 자동 생성)")
                                    } catch (e: Exception) {
                                        Log.e(TAG, "❌ Fragment → WebView Chain 연결 실패: ${e.message}")
                                    }
                                }
                            }
                            
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                url?.let {
                                    Log.i(TAG, "✅ Fragment WebView 페이지 로드 완료: $it")
                                    try {
                                        chainView.endChain("frag-web-${it.hashCode()}")
                                        Log.i(TAG, "✅ Fragment WebView Chain 종료")
                                    } catch (e: Exception) {
                                        Log.e(TAG, "❌ Fragment WebView Chain 종료 실패: ${e.message}")
                                    }
                                    Toast.makeText(ctx, "페이지 로드 완료: $it", Toast.LENGTH_SHORT).show()
                                }
                            }
                            
                            override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                                super.doUpdateVisitedHistory(view, url, isReload)
                                if (!isReload && url != null) {
                                    Log.i(TAG, "🔄 Fragment WebView 내부 네비게이션: $url")
                                    try {
                                        // 실제 페이지 네비게이션 Chain
                                        view?.url?.let { currentUrl ->
                                            if (currentUrl != url) {
                                                chainView.endTask("frag-webview-${currentUrl.hashCode()}")
                                                chainView.startChain("NavigationChain", "nav-chain-${url.hashCode()}")
                                                chainView.startTask(url, "frag-webview-${url.hashCode()}")
                                                chainView.endChain("nav-chain-${url.hashCode()}")
                                                Log.i(TAG, "✅ Fragment 페이지 네비게이션: $currentUrl → $url")
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Log.e(TAG, "❌ Fragment 네비게이션 Chain 실패: ${e.message}")
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
        try {
            chainView.endTask("fragment-${hashCode()}")
            Log.i(TAG, "✅ Fragment ScreenGroup 정리 완료")
        } catch (e: Exception) {
            Log.d(TAG, "Fragment 작업 이미 종료됨: ${e.message}")
        }
    }
}