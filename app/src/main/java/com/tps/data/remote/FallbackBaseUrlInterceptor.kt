package com.tps.data.remote

/**
 * 文件说明：网络兜底拦截器，负责在多个 API 地址之间自动重试并记录最后可用地址。
 */

import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class FallbackBaseUrlInterceptor(
    private val baseUrls: List<HttpUrl>
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        var lastException: IOException? = null

        // 第一个地址沿用原请求，后续地址只替换协议、主机和端口，避免破坏已有路径与查询参数。
        baseUrls.forEachIndexed { index, baseUrl ->
            val request = if (index == 0) {
                originalRequest
            } else {
                originalRequest.newBuilder()
                    .url(originalRequest.url.rewriteBaseUrl(baseUrl))
                    .build()
            }

            try {
                val response = chain.proceed(request)
                if (response.isSuccessful) {
                    // 一旦某个地址通了，就把它记为最近可用地址，方便其他地方优先展示当前可联通的入口。
                    NetworkEndpointConfig.lastWorkingApiBaseUrl = baseUrl
                }
                return response
            } catch (exception: IOException) {
                lastException = exception
            }
        }

        throw lastException ?: IOException("No API base URL configured")
    }

    private fun HttpUrl.rewriteBaseUrl(baseUrl: HttpUrl): HttpUrl =
        newBuilder()
            .scheme(baseUrl.scheme)
            .host(baseUrl.host)
            .port(baseUrl.port)
            .build()
}
