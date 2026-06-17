package com.tps.data.remote

import com.google.gson.JsonParser
import retrofit2.HttpException

fun apiErrorMessage(error: Exception): String? {
    if (error !is HttpException) return null
    val body = error.response()?.errorBody()?.string() ?: return null
    return runCatching {
        JsonParser.parseString(body).asJsonObject.get("message")?.asString
    }.getOrNull()?.takeIf { it.isNotBlank() }
}

fun userFacingApiErrorMessage(error: Exception, fallback: String): String {
    apiErrorMessage(error)?.let { return it }
    return when {
        error is HttpException && error.code() == 401 -> "未登录或登录已过期"
        error is HttpException && error.code() == 403 -> "无权限"
        error is HttpException -> "请求失败：HTTP ${error.code()}"
        !error.message.isNullOrBlank() -> error.message!!
        else -> fallback
    }
}
