package dev.lavalink.bilibili

import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BilibiliAudioSourceManagerTest {

    @Test
    fun `extracts video metadata from the initial page state`() {
        val html = """
            <html><script>window.__INITIAL_STATE__={"videoData":{"bvid":"BV1Yye36rEHa","title":"Track }; escaped \"text\" \u4e2d\u6587","cid":123,"owner":{"name":"Artist"},"pages":[{"page":1,"cid":123,"part":"Part 1"}]}};(function(){})();</script></html>
        """.trimIndent()

        val data = BilibiliAudioSourceManager.parseVideoPageMetadata(html)

        assertEquals("BV1Yye36rEHa", data.get("bvid").text())
        assertEquals("Track }; escaped \"text\" \u4e2d\u6587", data.get("title").text())
        assertEquals("Artist", data.get("owner").get("name").text())
        assertEquals(123L, data.get("pages").values().first().get("cid").asLong(0))
    }

    @Test
    fun `rejects a page without initial state`() {
        assertFailsWith<IOException> {
            BilibiliAudioSourceManager.parseVideoPageMetadata("<html><body>Access denied</body></html>")
        }
    }

    @Test
    fun `rejects initial state without video metadata`() {
        assertFailsWith<IOException> {
            BilibiliAudioSourceManager.parseVideoPageMetadata("<script>window.__INITIAL_STATE__={\"error\":{}};</script>")
        }
    }
}
