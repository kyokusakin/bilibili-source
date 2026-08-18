package dev.lavalink.bilibili

import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BilibiliAudioTrackTest {

    @Test
    fun `detects the HE-AAC ladder that also makes the progressive MP4 unplayable`() {
        assertTrue(BilibiliAudioTrack.hasHeAacStream(heAacLadder))
    }

    @Test
    fun `treats an all AAC-LC ladder as playable through the progressive MP4`() {
        assertFalse(BilibiliAudioTrack.hasHeAacStream(aacLcLadder))
    }

    @Test
    fun `picks the smallest AAC-LC stream and skips codecs the native decoder rejects`() {
        assertEquals("https://cdn/30232.m4s", BilibiliAudioTrack.getDashAudioUrl(heAacLadder))
    }

    @Test
    fun `falls back to the snake case base url`() {
        val data = JsonBrowser.parse(
            """{"dash":{"audio":[{"id":30232,"codecs":"mp4a.40.2","bandwidth":69196,"base_url":"https://cdn/snake.m4s"}]}}"""
        )

        assertEquals("https://cdn/snake.m4s", BilibiliAudioTrack.getDashAudioUrl(data))
    }

    @Test
    fun `reports no DASH stream when none of them is AAC-LC`() {
        val data = JsonBrowser.parse(
            """{"dash":{"audio":[{"id":30216,"codecs":"mp4a.40.5","bandwidth":34551,"baseUrl":"https://cdn/30216.m4s"}]}}"""
        )

        assertNull(BilibiliAudioTrack.getDashAudioUrl(data))
    }

    @Test
    fun `reports no DASH stream when the response carries no dash section`() {
        val data = JsonBrowser.parse("""{"durl":[{"url":"https://cdn/progressive.mp4"}]}""")

        assertFalse(BilibiliAudioTrack.hasHeAacStream(data))
        assertNull(BilibiliAudioTrack.getDashAudioUrl(data))
    }

    private val heAacLadder = JsonBrowser.parse(
        """
        {"dash":{"audio":[
          {"id":30216,"codecs":"mp4a.40.5","bandwidth":34551,"baseUrl":"https://cdn/30216.m4s"},
          {"id":30232,"codecs":"mp4a.40.2","bandwidth":69196,"baseUrl":"https://cdn/30232.m4s"},
          {"id":30280,"codecs":"mp4a.40.2","bandwidth":87549,"baseUrl":"https://cdn/30280.m4s"},
          {"id":30250,"codecs":"ec-3","bandwidth":510000,"baseUrl":"https://cdn/30250.m4s"}
        ]}}
        """.trimIndent()
    )

    private val aacLcLadder = JsonBrowser.parse(
        """
        {"dash":{"audio":[
          {"id":30216,"codecs":"mp4a.40.2","bandwidth":65558,"baseUrl":"https://cdn/30216.m4s"},
          {"id":30232,"codecs":"mp4a.40.2","bandwidth":90501,"baseUrl":"https://cdn/30232.m4s"},
          {"id":30280,"codecs":"mp4a.40.2","bandwidth":115690,"baseUrl":"https://cdn/30280.m4s"}
        ]}}
        """.trimIndent()
    )
}
