package com.tps.data.remote

/**
 * 文件说明：网络地址配置中心，负责整理主地址与备用地址列表供 HTTP 和 WebSocket 共用。
 */

import com.tps.BuildConfig
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

object NetworkEndpointConfig {
    // 构建脚本会把主地址和备用地址写进 BuildConfig，这里统一规范成可直接使用的地址列表。
    val apiBaseUrls: List<HttpUrl> = buildList {
        addNormalized(BuildConfig.BASE_URL)
        BuildConfig.FALLBACK_BASE_URLS
            .split(",")
            .forEach { addNormalized(it) }
    }.distinct()

    val websocketUrls: List<String> = buildList {
        addNormalizedWebSocket(BuildConfig.WS_URL)
        BuildConfig.FALLBACK_WS_URLS
            .split(",")
            .forEach { addNormalizedWebSocket(it) }
    }.distinct()

    val primaryApiBaseUrl: HttpUrl = apiBaseUrls.first()

    // 网络层请求成功后会回写该值，用于记录当前这台真机真正打通的是哪一个入口地址。
    @Volatile
    var lastWorkingApiBaseUrl: HttpUrl = primaryApiBaseUrl

    private fun MutableList<HttpUrl>.addNormalized(value: String) {
        val normalized = value.trim()
        if (normalized.isEmpty()) return
        add(normalized.trimEnd('/').plus("/").toHttpUrl())
    }

    private fun MutableList<String>.addNormalizedWebSocket(value: String) {
        val normalized = value.trim()
        if (normalized.isEmpty()) return
        add(normalized)
    }
}
