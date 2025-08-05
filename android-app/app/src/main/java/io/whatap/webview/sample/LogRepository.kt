package io.whatap.webview.sample

import java.util.concurrent.ConcurrentLinkedQueue

object LogRepository {
    private const val MAX_LOG_LINES = 100
    private val logQueue = ConcurrentLinkedQueue<String>()
    
    fun addLog(log: String) {
        logQueue.offer(log)
        
        // 100줄 초과시 오래된 로그 제거
        while (logQueue.size > MAX_LOG_LINES) {
            logQueue.poll()
        }
    }
    
    fun getAllLogs(): List<String> {
        return logQueue.toList()
    }
    
    fun clear() {
        logQueue.clear()
    }
}