package com.tps.data.remote.websocket

/**
 * 文件说明：WebSocket STOMP 客户端封装，负责聊天连接、订阅和实时消息发送接收。
 */

import com.google.gson.Gson
import com.tps.data.remote.NetworkEndpointConfig
import com.tps.util.TokenManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import javax.inject.Inject
import javax.inject.Singleton

data class ChatMessage(
    val id: Long = 0,
    val conversationId: Long = 0,
    val senderId: Long = 0,
    val content: String = "",
    val type: String = "TEXT",
    val createdAt: String = ""
)

@Singleton
class StompClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val tokenManager: TokenManager,
    private val gson: Gson
) {
    private var webSocket: WebSocket? = null
    private val _messages = MutableSharedFlow<ChatMessage>(extraBufferCapacity = 64)
    val messages: SharedFlow<ChatMessage> = _messages

    private var subscribedConversationId: Long = -1
    private var connected = false
    private var pendingSubscription: Long? = null
    private var nextEndpointIndex = 0

    fun connect() {
        if (connected) return
        val token = tokenManager.getToken() ?: return
        val endpoints = NetworkEndpointConfig.websocketUrls
        if (endpoints.isEmpty()) return

        // 局域网、USB 反向代理和模拟器地址会一起下发，这里按顺序轮询连接。
        val endpointIndex = nextEndpointIndex.coerceIn(0, endpoints.lastIndex)
        val request = Request.Builder()
            .url(endpoints[endpointIndex])
            .addHeader("Authorization", "Bearer $token")
            .build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: okhttp3.Response) {
                connected = true
                // 后端是在 STOMP CONNECT 帧里取 Authorization，而不是只依赖握手阶段的请求头。
                ws.send("CONNECT\naccept-version:1.2\nheart-beat:0,0\nAuthorization:Bearer $token\n\n\u0000")
            }

            override fun onMessage(ws: WebSocket, text: String) {
                when {
                    text.startsWith("CONNECTED") -> {
                        // 只有 STOMP 会话真正建立后再订阅，避免 CONNECT 尚未完成时丢掉订阅请求。
                        pendingSubscription?.let { subscribeConversation(it) }
                    }
                    text.startsWith("MESSAGE") -> {
                        val body = text.substringAfterLast("\n\n").trimEnd('\u0000')
                        try {
                            val msg = gson.fromJson(body, ChatMessage::class.java)
                            _messages.tryEmit(msg)
                        } catch (_: Exception) {}
                    }
                }
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: okhttp3.Response?) {
                connected = false
                webSocket = null
                if (endpointIndex < endpoints.lastIndex) {
                    nextEndpointIndex = endpointIndex + 1
                    connect()
                } else {
                    nextEndpointIndex = 0
                }
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                connected = false
            }
        })
    }

    fun subscribeConversation(conversationId: Long) {
        subscribedConversationId = conversationId
        if (!connected) {
            pendingSubscription = conversationId
            connect()
            return
        }

        webSocket?.send(
            "SUBSCRIBE\nid:sub-$conversationId\ndestination:/topic/conversation/$conversationId\n\n\u0000"
        )
    }

    fun isConnected(): Boolean = connected

    fun sendMessage(conversationId: Long, senderId: Long, content: String): Boolean {
        // 真机联调时优先走 WebSocket；如果发送失败，上层 ViewModel 会退回到 REST 接口补发。
        val payload = gson.toJson(mapOf(
            "conversationId" to conversationId,
            "senderId" to senderId,
            "content" to content,
            "type" to "TEXT"
        ))
        val frame = "SEND\ndestination:/app/chat.send\ncontent-type:application/json\n\n$payload\u0000"
        return webSocket?.send(frame) == true
    }

    fun disconnect() {
        webSocket?.send("DISCONNECT\n\n\u0000")
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        connected = false
    }
}
