package com.tps.ui.order

/**
 * 文件说明：订单模块状态管理，负责订单查询、筛选与状态操作。
 */

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tps.data.remote.UserFacingApiError
import com.tps.data.remote.userFacingApiError
import com.tps.data.remote.userFacingApiErrorMessage
import com.tps.data.remote.api.ApiService
import com.tps.data.remote.dto.OrderDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OrderUiState(
    val orders: List<OrderDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val fieldError: UserFacingApiError? = null,
    val role: String = "buyer",
    val successMessage: String? = null
)

@HiltViewModel
class OrderViewModel @Inject constructor(
    private val apiService: ApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrderUiState())
    val uiState: StateFlow<OrderUiState> = _uiState

    init { loadOrders() }

    fun loadOrders(role: String = _uiState.value.role) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, role = role)
            try {
                val resp = apiService.getMyOrders(role = role)
                _uiState.value = _uiState.value.copy(orders = resp.data?.content ?: emptyList(), isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = userFacingApiErrorMessage(e, "订单加载失败"))
            }
        }
    }

    fun switchRole(role: String) {
        if (role != _uiState.value.role) loadOrders(role)
    }

    fun pay(orderId: Long) {
        viewModelScope.launch {
            try {
                apiService.payOrder(orderId)
                loadOrders()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = userFacingApiErrorMessage(e, "支付失败"))
            }
        }
    }

    fun confirmReceived(orderId: Long) {
        viewModelScope.launch {
            try {
                apiService.confirmOrder(orderId)
                loadOrders()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = userFacingApiErrorMessage(e, "确认收货失败"))
            }
        }
    }

    fun ship(orderId: Long) {
        viewModelScope.launch {
            try {
                apiService.shipOrder(orderId)
                loadOrders()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = userFacingApiErrorMessage(e, "发货失败"))
            }
        }
    }

    fun cancel(orderId: Long) {
        viewModelScope.launch {
            try {
                apiService.cancelOrder(orderId)
                loadOrders()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = userFacingApiErrorMessage(e, "取消订单失败"))
            }
        }
    }

    fun review(orderId: Long, score: Int, content: String) {
        viewModelScope.launch {
            try {
                apiService.reviewOrder(orderId, score, content.ifBlank { null })
                _uiState.value = _uiState.value.copy(successMessage = "评价已提交")
                loadOrders()
            } catch (e: Exception) {
                val apiError = userFacingApiError(e, "评价提交失败")
                _uiState.value = _uiState.value.copy(
                    error = apiError.message,
                    fieldError = apiError.takeIf { it.isFieldError }
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null, fieldError = null)
    }

    fun clearSuccess() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }
}
