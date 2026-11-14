package dev.lavalink.bilibili

import org.apache.http.client.methods.HttpGet
import org.apache.http.client.protocol.HttpClientContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class BilibiliHttpContextFilterTest {

    @Test
    fun `uses a dedicated user agent`() {
        val filter = BilibiliHttpContextFilter()
        val request = HttpGet("https://api.bilibili.com/x/web-interface/view?bvid=BV1Yye36rEHa")

        filter.onRequest(HttpClientContext.create(), request, false)

        assertEquals("bilibili-source/1.0", request.getFirstHeader("User-Agent").value)
        assertEquals("https://www.bilibili.com/", request.getFirstHeader("Referer").value)
        assertFalse(request.containsHeader("Range"))
    }

    @Test
    fun `requests Bilibili CDN media from byte zero on startup`() {
        val request = HttpGet("https://upos-sz-mirrorcosov.bilivideo.com/media.mp4")

        BilibiliHttpContextFilter().onRequest(HttpClientContext.create(), request, false)

        assertEquals("bytes=0-", request.getFirstHeader("Range")?.value)
    }

    @Test
    fun `preserves the requested seek offset on Bilibili CDN media`() {
        val request = HttpGet("https://upos-sz-mirrorcosov.bilivideo.com/media.mp4")
        request.setHeader("Range", "bytes=1048576-")

        BilibiliHttpContextFilter().onRequest(HttpClientContext.create(), request, false)

        assertEquals("bytes=1048576-", request.getFirstHeader("Range").value)
    }
}
