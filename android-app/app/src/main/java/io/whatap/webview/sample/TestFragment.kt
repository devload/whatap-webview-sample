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
            setPadding(32, 32, 32, 32)
        }
        
        // 제목 텍스트
        val title = TextView(requireContext()).apply {
            text = "Fragment 내 WebView 테스트"
            textSize = 18f
            setPadding(0, 0, 0, 32)
        }
        layout.addView(title)
        
        // WebView 생성
        val webView = WebView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f // weight
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
            
            loadUrl("https://www.google.com")
        }
        layout.addView(webView)
        
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
                parentFragmentManager.popBackStack()
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