package com.tps.util

/**
 * 文件说明：URL 工具类，负责兼容不同网络环境下的地址解析与补全。
 */

import com.tps.BuildConfig
import com.tps.data.remote.NetworkEndpointConfig

fun resolveMediaUrl(url: String?): String? {
    val value = url?.trim().orEmpty()
    if (value.isEmpty()) return null
    if (value.startsWith("http://") || value.startsWith("https://") || value.startsWith("content://")) {
        return value
    }
    val base = NetworkEndpointConfig.lastWorkingApiBaseUrl.toString().trimEnd('/')
    return if (value.startsWith("/")) "$base$value" else "$base/$value"
}
