package com.tps.ui.message

/**
 * 文件说明：消息模块状态管理，负责会话列表、聊天消息与已读状态编排。
 */

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tps.data.remote.UserFacingApiError
import com.tps.data.remote.userFacingApiError
import com.tps.data.remote.api.ApiService
import com.tps.data.remote.websocket.ChatMessage
import com.tps.data.remote.websocket.StompClient
import com.tps.util.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val apiService: ApiService,
    private val stompClient: StompClient,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _title = MutableStateFlow("聊天")
    val title: StateFlow<String> = _title

    private val _product = MutableStateFlow<com.tps.data.remote.dto.ProductDto?>(null)
    val product: StateFlow<com.tps.data.remote.dto.ProductDto?> = _product

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _fieldError = MutableStateFlow<UserFacingApiError?>(null)
    val fieldError: StateFlow<UserFacingApiError?> = _fieldError

    val myUserId: Long get() = tokenManager.getUserId()

    private var conversationId: Long = -1

    fun init(conversationId: Long) {
        this.conversationId = conversationId
        loadHistory()
        loadTitle(conversationId)
        subscribeWebSocket()
    }

    private fun loadTitle(convId: Long) {
        viewModelScope.launch {
            try {
                val resp = apiService.getConversations()
                val conv = resp.data?.content?.find { it.id == convId }
                if (conv != null) {
                    val myId = tokenManager.getUserId()
                    val otherRole = if (conv.buyerId == myId) "卖家" else "买家"
                    _title.value = "商品 #${conv.productId} · $otherRole"

                    val prodResp = apiService.getProduct(conv.productId)
                    _product.value = prodResp.data
                }
            } catch (_: Exception) {}
        }
    }

    private fun loadHistory() {
        viewModelScope.launch {
            try {
                val resp = apiService.getMessages(conversationId)
                val history = resp.data?.map {
                    ChatMessage(
                        id = it.id,
                        conversationId = conversationId,
                        senderId = it.senderId,
                        content = it.content,
                        type = it.type,
                        createdAt = it.createdAt
                    )
                } ?: emptyList()
                _messages.value = history

                // 聊天页打开后立即把当前会话标记为已读，保证会话列表未读数能及时回落。
                try {
                    apiService.markConversationRead(conversationId)
                } catch (_: Exception) {}
            } catch (_: Exception) {}
        }
    }

    private fun subscribeWebSocket() {
        stompClient.subscribeConversation(conversationId)
        viewModelScope.launch {
            stompClient.messages
                .filter { it.conversationId == conversationId }
                .collect { msg ->
                    _messages.value = _messages.value + msg
                }
        }
    }

    fun sendMessage(content: String) {
        viewModelScope.launch {
            try {
                // WebSocket 可用时优先走实时链路；断线时自动退回 REST，避免“按钮点了没反应”。
                val sentByWs = stompClient.isConnected() && stompClient.sendMessage(conversationId, myUserId, content)
                if (!sentByWs) {
                    val resp = apiService.sendMessage(conversationId, content)
                    resp.data?.let {
                        _messages.value = _messages.value + ChatMessage(
                            id = it.id,
                            conversationId = it.conversationId ?: conversationId,
                            senderId = it.senderId,
                            content = it.content,
                            type = it.type,
                            createdAt = it.createdAt
                        )
                    }
                }
            } catch (e: Exception) {
                val apiError = userFacingApiError(e, "发送消息失败")
                _error.value = apiError.message
                _fieldError.value = apiError.takeIf { it.isFieldError }
            }
        }
    }

    fun clearError() {
        _error.value = null
        _fieldError.value = null
    }

    override fun onCleared() {
        super.onCleared()
    }
}
