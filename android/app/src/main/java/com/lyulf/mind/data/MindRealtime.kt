package com.lyulf.mind.data

import android.util.Log
import com.lyulf.mind.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.atomic.AtomicBoolean

/**
 * WebSocket：有 reminder 变更就发一记 tick。
 * 断线指数退避重连；忽略旧 socket 的失败回调，避免互相 cancel 打成重连风暴。
 */
class MindRealtime(
    private val client: OkHttpClient,
) {
    private companion object {
        const val TAG = "MindWs"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _events = MutableSharedFlow<Unit>(extraBufferCapacity = 16)
    val events: SharedFlow<Unit> = _events.asSharedFlow()

    private val lock = Any()
    private var token: String? = null
    private var socket: WebSocket? = null
    private var reconnectJob: Job? = null
    private val wantOpen = AtomicBoolean(false)
    private var attempt = 0

    fun connect(deviceToken: String) {
        synchronized(lock) {
            if (wantOpen.get() && token == deviceToken && socket != null) {
                Log.d(TAG, "connect skip (already open)")
                return
            }
            token = deviceToken
            wantOpen.set(true)
            Log.i(TAG, "connect wantOpen=true")
            openNowLocked()
        }
    }

    fun disconnect() {
        synchronized(lock) {
            wantOpen.set(false)
            reconnectJob?.cancel()
            reconnectJob = null
            socket?.close(1000, "bye")
            socket = null
            token = null
            attempt = 0
            Log.i(TAG, "disconnect")
        }
    }

    private fun wsUrl(deviceToken: String): String {
        val http = BuildConfig.API_BASE_URL.trimEnd('/')
        val ws = when {
            http.startsWith("https://") -> "wss://" + http.removePrefix("https://")
            http.startsWith("http://") -> "ws://" + http.removePrefix("http://")
            else -> "ws://$http"
        }
        return "$ws/ws?token=${deviceToken}"
    }

    /** 调用方须已持有 [lock] */
    private fun openNowLocked() {
        val t = token ?: return
        if (!wantOpen.get()) return
        if (socket != null) {
            Log.d(TAG, "openNow skip (socket alive)")
            return
        }
        val req = Request.Builder().url(wsUrl(t)).build()
        socket = client.newWebSocket(req, listener)
        Log.d(TAG, "openNow new socket")
    }

    private fun scheduleReconnect() {
        if (!wantOpen.get()) return
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            attempt += 1
            val wait = (800L * (1 shl attempt.coerceAtMost(4))).coerceAtMost(8_000L)
            delay(wait)
            if (!isActive || !wantOpen.get()) return@launch
            synchronized(lock) {
                if (socket == null && wantOpen.get()) {
                    Log.i(TAG, "reconnect attempt=$attempt")
                    openNowLocked()
                }
            }
        }
    }

    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            attempt = 0
            Log.i(TAG, "open ok")
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            val tick = text.contains("\"reminder\"") || text.contains("\"hello\"")
            Log.d(TAG, "msg tick=$tick raw=${text.take(120)}")
            if (tick) {
                _events.tryEmit(Unit)
            }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            webSocket.close(code, reason)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            synchronized(lock) {
                if (socket !== webSocket) {
                    Log.d(TAG, "ignore stale closed")
                    return
                }
                socket = null
                Log.w(TAG, "closed code=$code reason=$reason → reconnect")
            }
            scheduleReconnect()
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            synchronized(lock) {
                if (socket !== webSocket) {
                    Log.d(TAG, "ignore stale failure: ${t.message}")
                    return
                }
                socket = null
                Log.w(TAG, "failure ${t.message} → reconnect")
            }
            scheduleReconnect()
        }
    }
}
