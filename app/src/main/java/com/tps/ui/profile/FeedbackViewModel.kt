package com.tps.ui.profile

/**
 * 文件说明：个人中心状态管理，负责资料、收藏、历史、反馈等数据编排。
 */

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tps.data.remote.api.ApiService
import com.tps.data.remote.dto.FeedbackDto
import com.tps.data.remote.dto.FeedbackRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FeedbackUiState(
    val feedbackList: List<FeedbackDto> = emptyList(),
    val isLoading: Boolean = false,
    val submitSuccess: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class FeedbackViewModel @Inject constructor(
    private val apiService: ApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedbackUiState())
    val uiState: StateFlow<FeedbackUiState> = _uiState

    fun loadMyFeedback() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val resp = apiService.getMyFeedback()
                _uiState.value = _uiState.value.copy(
                    feedbackList = resp.data?.content ?: emptyList(),
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun submitFeedback(type: String, content: String, contact: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, submitSuccess = false)
            try {
                apiService.submitFeedback(FeedbackRequest(type, content, contact.takeIf { it.isNotBlank() }))
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    submitSuccess = true
                )
                loadMyFeedback()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun resetSuccess() {
        _uiState.value = _uiState.value.copy(submitSuccess = false)
    }
}
