package io.whatap.webview.sample

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import io.whatap.android.agent.webview.WhatapWebViewClient
import io.whatap.android.agent.webview.WhatapWebviewBridge
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import android.util.Log
import io.whatap.android.agent.instrumentation.screengroup.ChainView

class TestFragment : Fragment() {
    companion object {
        private const val TAG = "TestFragment"
        private val chainView = ChainView()
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.i(TAG, "🔄 TestFragment onCreateView 시작")
        
        // Fragment 시작 작업
        try {
            chainView.startTask("TestFragment", "fragment-${hashCode()}")
            Log.i(TAG, "✅ Fragment ScreenGroup 작업 시작: fragment-${hashCode()}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Fragment ScreenGroup 작업 시작 실패: ${e.message}")
        }
        
        // 프로그래매틱하게 레이아웃 생성
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }
        
        // URL 입력 필드와 Go 버튼
        val urlLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        
        val urlEditText = android.widget.EditText(requireContext()).apply {
            setText("http://192.168.1.6:18000/")
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            textSize = 12f
            setSingleLine(true)
        }
        urlLayout.addView(urlEditText)
        
        // WebView 변수 먼저 선언
        lateinit var webView: WebView
        
        val goButton = Button(requireContext()).apply {
            text = "Go"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener {
                val url = urlEditText.text.toString()
                if (url.startsWith("http")) {
                    webView.loadUrl(url)
                    Log.i(TAG, "🌐 URL 변경: $url")
                }
            }
        }
        urlLayout.addView(goButton)
        layout.addView(urlLayout)
        
        // WebView 생성
        webView = WebView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                0.55f // 55% 비율 (MainActivity와 동일)
            )
            
            settings.javaScriptEnabled = true
            
            // Bridge 먼저 생성
            val bridge = WhatapWebviewBridge(requireContext())
            bridge.configureWebView(this)
            
            // 🔥 핵심: WhatapWebViewClient 사용 (최신 빌드 버전)
            webViewClient = object : WhatapWebViewClient(bridge) {
                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    url?.let {
                        Log.i(TAG, "🌐 Fragment 내 WebView 페이지 로드 시작: $it")
                        try {
                            // Fragment → WebView Chain 연결
                            chainView.startChain("FragmentWebChain", "frag-web-${it.hashCode()}")
                            chainView.endTask("fragment-${hashCode()}")
                            chainView.startTask(it, "frag-webview-${it.hashCode()}")
                            Log.i(TAG, "✅ Fragment → WebView Chain 연결 성공")
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Fragment → WebView Chain 연결 실패: ${e.message}")
                        }
                    }
                }
                
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    url?.let {
                        Log.i(TAG, "✅ Fragment 내 WebView 페이지 로드 완료: $it")
                        try {
                            chainView.endChain("frag-web-${it.hashCode()}")
                            Log.i(TAG, "✅ Fragment WebView Chain 종료")
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Fragment WebView Chain 종료 실패: ${e.message}")
                        }
                    }
                }
            }
            
            // 🔥 JavaScript Bridge 설정 추가 (이미 위에서 생성됨)
            bridge.startDataUploadTimer()
            
            loadUrl("http://192.168.1.6:18000/")
        }
        layout.addView(webView)
        
        // Export 로그 영역 추가
        val logScrollView = android.widget.ScrollView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                0.45f // 45% 비율 (MainActivity와 동일)
            )
            setBackgroundColor(android.graphics.Color.BLACK)
        }
        
        val logTextView = TextView(requireContext()).apply {
            text = "📡 Export Log\n대기 중...\n"
            textSize = 8f
            setTextColor(android.graphics.Color.parseColor("#00FF00"))
            setPadding(8, 8, 8, 8)
            typeface = android.graphics.Typeface.MONOSPACE
        }
        logScrollView.addView(logTextView)
        layout.addView(logScrollView)
        
        // 로그 업데이트 스레드
        Thread {
            var count = 0
            while (isAdded) {
                Thread.sleep(3000)
                activity?.runOnUiThread {
                    val timestamp = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()).format(java.util.Date())
                    val newLog = "[$timestamp] Fragment WebView 이벤트 #${++count}\n"
                    logTextView.append(newLog)
                    // 스크롤 맨 아래로
                    logScrollView.fullScroll(View.FOCUS_DOWN)
                    
                    // 100줄 이상이면 오래된 로그 제거
                    val lines = logTextView.text.split("\n")
                    if (lines.size > 100) {
                        val recentLines = lines.takeLast(100)
                        logTextView.text = recentLines.joinToString("\n")
                    }
                }
            }
        }.start()
        
        // 닫기 버튼
        val closeButton = Button(requireContext()).apply {
            text = "Fragment 닫기"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            
            setOnClickListener {
                Log.i(TAG, "🔄 Fragment 닫기 버튼 클릭")
                try {
                    chainView.endTask("frag-webview-${webView.url?.hashCode()}")
                    Log.i(TAG, "✅ Fragment 종료 - WebView 작업 정리 완료")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Fragment 종료 시 작업 정리 실패: ${e.message}")
                }
                
                // Fragment 컨테이너에서 숨기기 (MainActivity의 isFragmentVisible을 false로)
                activity?.let { mainActivity ->
                    if (mainActivity is io.whatap.webview.sample.MainActivity) {
                        // MainActivity의 Fragment 상태를 false로 변경하는 방법이 필요
                        // 임시적으로 Fragment Manager를 통해 제거
                        parentFragmentManager.beginTransaction()
                            .remove(this@TestFragment)
                            .commit()
                    }
                }
            }
        }
        layout.addView(closeButton)
        
        return layout
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