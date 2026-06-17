package com.tps.ui.message

/**
 * 文件说明：消息模块状态管理，负责会话列表、聊天消息与已读状态编排。
 */

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tps.data.remote.userFacingApiErrorMessage
import com.tps.data.remote.api.ApiService
import com.tps.data.remote.dto.ConversationDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MessageListUiState(
    val conversations: List<ConversationDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class MessageViewModel @Inject constructor(
    private val apiService: ApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(MessageListUiState())
    val uiState: StateFlow<MessageListUiState> = _uiState

    init { loadConversations() }

    fun loadConversations() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val resp = apiService.getConversations()
                _uiState.value = _uiState.value.copy(conversations = resp.data?.content ?: emptyList(), isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = userFacingApiErrorMessage(e, "会话加载失败"))
            }
        }
    }
}
