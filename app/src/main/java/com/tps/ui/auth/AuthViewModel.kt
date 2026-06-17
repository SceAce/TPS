package com.tps.ui.auth

/**
 * 文件说明：认证页面状态管理，负责登录注册流程与接口交互。
 */

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tps.data.remote.UserFacingApiError
import com.tps.data.remote.api.ApiService
import com.tps.data.remote.apiErrorMessage
import com.tps.data.remote.dto.LoginRequest
import com.tps.data.remote.dto.RegisterRequest
import com.tps.data.remote.userFacingApiError
import com.tps.data.remote.userFacingApiErrorMessage
import com.tps.util.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val fieldError: UserFacingApiError? = null,
    val isSuccess: Boolean = false,
    val isAdmin: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState

    fun sendCode(phone: String) {
        viewModelScope.launch {
            try {
                apiService.sendCode(phone)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = userFacingApiErrorMessage(e, "验证码发送失败"))
            }
        }
    }

    fun login(phone: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, fieldError = null)
            try {
                tokenManager.clear()
                val resp = apiService.login(LoginRequest(phone = phone.trim(), password = password.trim()))
                if (resp.code == 200 && resp.data != null) {
                    tokenManager.saveToken(resp.data.token)
                    tokenManager.saveRefreshToken(resp.data.refreshToken)
                    tokenManager.saveUserId(resp.data.userId)
                    tokenManager.saveRole(resp.data.role)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSuccess = true,
                        isAdmin = resp.data.role == "ADMIN"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = resp.message)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = loginErrorMessage(e))
            }
        }
    }

    fun register(phone: String, password: String, code: String, studentId: String, nickname: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, fieldError = null)
            try {
                val resp = apiService.register(RegisterRequest(phone, password, code, studentId, nickname))
                if (resp.code == 200 && resp.data != null) {
                    tokenManager.saveToken(resp.data.token)
                    tokenManager.saveRefreshToken(resp.data.refreshToken)
                    tokenManager.saveUserId(resp.data.userId)
                    tokenManager.saveRole(resp.data.role)
                    _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = resp.message)
                }
            } catch (e: Exception) {
                val apiError = userFacingApiError(e, "注册失败，请检查后重试")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = apiError.message,
                    fieldError = apiError.takeIf { it.isFieldError }
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null, fieldError = null)
    }

    fun resetState() {
        _uiState.value = AuthUiState()
    }
}

internal fun loginErrorMessage(error: Exception): String {
    apiErrorMessage(error)?.let { return it }
    return when {
        error is HttpException && error.code() == 400 -> "账号被封禁，请联系管理员"
        error is HttpException && error.code() == 401 -> "登录状态已过期，请重新登录"
        error is HttpException && error.code() == 403 -> "当前账号无权登录该入口"
        error is HttpException -> "登录失败：HTTP ${error.code()}"
        !error.message.isNullOrBlank() -> error.message!!
        else -> "登录失败，请检查网络后重试"
    }
}
