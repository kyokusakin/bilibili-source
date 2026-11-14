package dev.lavalink.bilibili

import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo
import com.sun.net.httpserver.HttpServer
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.impl.client.HttpClients
import java.net.InetSocketAddress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BilibiliAudioTrackTest {

    @Test
    fun `opens the Bilibili backup stream when the primary CDN returns HTTP 567`() {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        val origin = "http://127.0.0.1:${server.address.port}"
        server.createContext("/x/player/playurl") { exchange ->
            val data = if (exchange.requestURI.query.contains("fnval=16")) {
                """{"dash":{"audio":[{"codecs":"mp4a.40.2"}]}}"""
            } else {
                """{"durl":[{"url":"$origin/primary","backup_url":["$origin/backup"]}]}"""
            }
            val body = """{"code":0,"data":$data}""".toByteArray()
            exchange.sendResponseHeaders(200, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }
        server.createContext("/primary") { exchange ->
            exchange.sendResponseHeaders(567, -1)
            exchange.close()
        }
        server.createContext("/backup") { exchange ->
            val body = byteArrayOf(0, 0, 0, 24, 102, 116, 121, 112)
            exchange.sendResponseHeaders(200, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }
        val source = BilibiliAudioSourceManager()
        server.start()
        try {
            val info = AudioTrackInfo("Track", "Artist", 1000L, "BV1T14z6FEuK", false, null)
            val track = BilibiliAudioTrack(info, BilibiliAudioTrack.TrackType.VIDEO, info.identifier, 123L, source)
            HttpClients.createDefault().use { client ->
                val http = object : HttpInterface(client, HttpClientContext.create(), false, BilibiliHttpContextFilter()) {
                    override fun execute(request: HttpUriRequest) =
                        super.execute(if (request.uri.host == "api.bilibili.com") {
                            HttpGet("$origin${request.uri.rawPath}?${request.uri.rawQuery}")
                        } else {
                            request
                        })
                }
                track.openPlaybackStream(http).use { stream ->
                    assertEquals("ftyp", stream.readNBytes(8).drop(4).toByteArray().decodeToString())
                }
            }
        } finally {
            server.stop(0)
            source.shutdown()
        }
    }

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
        assertEquals(listOf("https://cdn/30232.m4s"), BilibiliAudioTrack.getDashAudioUrls(heAacLadder))
    }

    @Test
    fun `keeps DASH mirrors after the primary and ignores duplicate aliases`() {
        val data = JsonBrowser.parse(
            """
            {"dash":{"audio":[{"id":30232,"codecs":"mp4a.40.2","bandwidth":69196,"base_url":"https://cdn/snake.m4s",
              "backupUrl":["https://mirror/audio.m4s"],"backup_url":["https://mirror/audio.m4s","https://other/audio.m4s"]
            }]}}
            """.trimIndent()
        )

        assertEquals(
            listOf("https://cdn/snake.m4s", "https://mirror/audio.m4s", "https://other/audio.m4s"),
            BilibiliAudioTrack.getDashAudioUrls(data)
        )
    }

    @Test
    fun `reports no DASH stream when none of them is AAC-LC`() {
        val data = JsonBrowser.parse(
            """{"dash":{"audio":[{"id":30216,"codecs":"mp4a.40.5","bandwidth":34551,"baseUrl":"https://cdn/30216.m4s"}]}}"""
        )

        assertTrue(BilibiliAudioTrack.getDashAudioUrls(data).isEmpty())
    }

    @Test
    fun `reports no DASH stream when the response carries no dash section`() {
        val data = JsonBrowser.parse("""{"durl":[{"url":"https://cdn/progressive.mp4"}]}""")

        assertFalse(BilibiliAudioTrack.hasHeAacStream(data))
        assertTrue(BilibiliAudioTrack.getDashAudioUrls(data).isEmpty())
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
