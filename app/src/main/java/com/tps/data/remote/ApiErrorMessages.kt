package com.tps.data.remote

import com.google.gson.JsonParser
import retrofit2.HttpException

data class FieldErrorDetail(
    val field: String,
    val label: String,
    val message: String
)

data class UserFacingApiError(
    val message: String,
    val fieldErrors: List<FieldErrorDetail> = emptyList()
) {
    val isFieldError: Boolean get() = fieldErrors.isNotEmpty()
}

fun apiErrorMessage(error: Exception): String? {
    return parseApiError(error)?.message
}

fun userFacingApiErrorMessage(error: Exception, fallback: String): String {
    return userFacingApiError(error, fallback).message
}

fun userFacingApiError(error: Exception, fallback: String): UserFacingApiError {
    parseApiError(error)?.let { return it }
    val message = when {
        error is HttpException && error.code() == 401 -> "未登录或登录已过期"
        error is HttpException && error.code() == 403 -> "无权限"
        error is HttpException -> "请求失败：HTTP ${error.code()}"
        !error.message.isNullOrBlank() -> error.message!!
        else -> fallback
    }
    return UserFacingApiError(message)
}

fun sensitiveFieldErrorDialogText(error: UserFacingApiError): String {
    if (error.fieldErrors.isEmpty()) {
        return error.message
    }
    return error.fieldErrors.joinToString("\n") { "• ${it.message}" }
}

private fun parseApiError(error: Exception): UserFacingApiError? {
    if (error !is HttpException) return null
    val body = error.response()?.errorBody()?.string() ?: return null
    return runCatching {
        val json = JsonParser.parseString(body).asJsonObject
        val message = json.get("message")?.asString?.takeIf { it.isNotBlank() } ?: return@runCatching null
        val fields = json.getAsJsonObject("data")
            ?.getAsJsonArray("fields")
            ?.mapNotNull { item ->
                val fieldJson = item.asJsonObject
                FieldErrorDetail(
                    field = fieldJson.get("field")?.asString ?: return@mapNotNull null,
                    label = fieldJson.get("label")?.asString ?: return@mapNotNull null,
                    message = fieldJson.get("message")?.asString ?: return@mapNotNull null
                )
            }
            ?: emptyList()
        UserFacingApiError(message, fields)
    }.getOrNull()
}
