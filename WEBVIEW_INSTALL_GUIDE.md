# WhatAp Android WebView SDK - WebView 연동 가이드

## 📋 개요

기존 앱의 WebView에 WhatAp 모니터링을 연동하는 방법을 안내합니다.

## 🌐 WebView 기본 모니터링 연동

Activity나 Fragment 어디에 있든 동일한 방법으로 연동 가능합니다.

### Kotlin 버전

```kotlin
import io.whatap.android.agent.webview.WhatapWebViewClient
import io.whatap.android.agent.webview.WhatapWebviewBridge

private fun setupWhatapWebView(existingWebView: WebView, context: Context) {
    // 1. WhatAp Bridge 설정
    val bridge = WhatapWebviewBridge(context)
    bridge.configureWebView(existingWebView)
    bridge.startDataUploadTimer()
    
    // 2. 기존 WebViewClient를 WhatapWebViewClient로 교체
    existingWebView.webViewClient = WhatapWebViewClient(bridge)
}
```

### Java 버전

```java
import io.whatap.android.agent.webview.WhatapWebViewClient;
import io.whatap.android.agent.webview.WhatapWebviewBridge;

private void setupWhatapWebView(WebView existingWebView, Context context) {
    // 1. WhatAp Bridge 설정
    WhatapWebviewBridge bridge = new WhatapWebviewBridge(context);
    bridge.configureWebView(existingWebView);
    bridge.startDataUploadTimer();
    
    // 2. 기존 WebViewClient를 WhatapWebViewClient로 교체
    existingWebView.setWebViewClient(new WhatapWebViewClient(bridge));
}
```

## 🔗 ScreenGroup Chain 연동

**Chain 동작 원리:**
1. 상위 화면(Activity/Fragment) 라이프사이클에서 Chain Task 시작
2. WebView 페이지 로드 완료 시점에서 Task 종료
3. 연속된 사용자 플로우가 하나의 ScreenGroup으로 연결됨

**예시 시나리오:** MainActivity → ProductDetailFragment(onCreate에서 Chain 시작) → WebView(상품 상세 정보) → onPageFinished에서 Chain 종료

### Chain 연동 - Kotlin

```kotlin
import io.whatap.android.agent.instrumentation.screengroup.ChainView

// Activity/Fragment에서 사용
class ProductDetailFragment : Fragment() {
    private lateinit var chainView: ChainView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Chain Task 시작 (완전 자동)
        chainView = ChainView()
        chainView.startTask()
    }
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_product_detail, container, false)
        val webView = view.findViewById<WebView>(R.id.webview)
        
        setupWhatapWebViewWithChain(webView, requireContext())
        webView.loadUrl("https://your-product-detail-page.com")
        return view
    }
    
    private fun setupWhatapWebViewWithChain(existingWebView: WebView, context: Context) {
        // WhatAp Bridge 설정
        val bridge = WhatapWebviewBridge(context)
        bridge.configureWebView(existingWebView)
        bridge.startDataUploadTimer()
        
        // WebView 로드 완료 시 Chain 종료
        existingWebView.webViewClient = object : WhatapWebViewClient(bridge) {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                chainView.endTask()
            }
        }
    }
}
```

### Chain 연동 - Java

```java
import io.whatap.android.agent.instrumentation.screengroup.ChainView;

// Activity/Fragment에서 사용
public class ProductDetailFragment extends Fragment {
    private ChainView chainView;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Chain Task 시작 (완전 자동)
        chainView = new ChainView();
        chainView.startTask();
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_product_detail, container, false);
        WebView webView = view.findViewById(R.id.webview);
        
        setupWhatapWebViewWithChain(webView, requireContext());
        webView.loadUrl("https://your-product-detail-page.com");
        return view;
    }
    
    private void setupWhatapWebViewWithChain(WebView existingWebView, Context context) {
        // WhatAp Bridge 설정
        WhatapWebviewBridge bridge = new WhatapWebviewBridge(context);
        bridge.configureWebView(existingWebView);
        bridge.startDataUploadTimer();
        
        // WebView 로드 완료 시 Chain 종료
        existingWebView.setWebViewClient(new WhatapWebViewClient(bridge) {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                chainView.endTask();
            }
        });
    }
}
