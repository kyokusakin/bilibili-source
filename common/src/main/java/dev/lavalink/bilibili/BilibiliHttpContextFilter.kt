package dev.lavalink.bilibili

import com.sedmelluq.discord.lavaplayer.tools.http.HttpContextFilter
import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.client.protocol.HttpClientContext

class BilibiliHttpContextFilter : HttpContextFilter {

    override fun onContextOpen(context: HttpClientContext) = Unit

    override fun onContextClose(context: HttpClientContext) = Unit

    override fun onRequest(context: HttpClientContext, request: HttpUriRequest, isRepetition: Boolean) {
        request.setHeader("Referer", "https://www.bilibili.com/")
        request.setHeader("Origin", "https://www.bilibili.com")
        request.setHeader("User-Agent", USER_AGENT)
        request.setHeader("Accept", "application/json, text/plain, */*")
        request.setHeader("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
    }

    override fun onRequestResponse(
        context: HttpClientContext,
        request: HttpUriRequest,
        response: HttpResponse
    ): Boolean = false

    override fun onRequestException(
        context: HttpClientContext?,
        request: HttpUriRequest,
        error: Throwable
    ): Boolean = false

    companion object {
        private const val USER_AGENT = "bilibili-source/1.0"
    }
}
