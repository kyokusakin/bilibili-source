package dev.lavalink.bilibili

import org.apache.http.client.methods.HttpGet
import org.apache.http.client.protocol.HttpClientContext
import kotlin.test.Test
import kotlin.test.assertEquals

class BilibiliHttpContextFilterTest {

    @Test
    fun `uses a dedicated user agent`() {
        val filter = BilibiliHttpContextFilter()
        val request = HttpGet("https://api.bilibili.com/x/web-interface/view?bvid=BV1Yye36rEHa")

        filter.onRequest(HttpClientContext.create(), request, false)

        assertEquals("bilibili-source/1.0", request.getFirstHeader("User-Agent").value)
        assertEquals("https://www.bilibili.com/", request.getFirstHeader("Referer").value)
    }
}
