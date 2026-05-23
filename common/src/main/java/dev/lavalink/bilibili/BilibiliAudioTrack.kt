package dev.lavalink.bilibili

import com.sedmelluq.discord.lavaplayer.container.mpeg.MpegAudioTrack
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager
import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface
import com.sedmelluq.discord.lavaplayer.tools.io.PersistentHttpStream
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo
import com.sedmelluq.discord.lavaplayer.track.DelegatedAudioTrack
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor
import org.apache.http.client.methods.HttpGet
import java.io.IOException
import java.net.URI

class BilibiliAudioTrack(
    audioTrackInfo: AudioTrackInfo,
    val type: TrackType,
    val id: String,
    val cid: Long?,
    private val sourceManager: BilibiliAudioSourceManager
) : DelegatedAudioTrack(audioTrackInfo) {

    override fun process(executor: LocalAudioTrackExecutor) {
        sourceManager.getHttpInterface().use { httpInterface ->
            val playbackUrl = getPlaybackUrl(httpInterface)

            PersistentHttpStream(httpInterface, URI(playbackUrl), null).use { stream ->
                processDelegate(MpegAudioTrack(trackInfo, stream), executor)
            }
        }
    }

    private fun getPlaybackUrl(httpInterface: HttpInterface): String = when (type) {
        TrackType.AUDIO -> {
            val request = HttpGet("${BilibiliAudioSourceManager.BASE_URL}audio/music-service-c/web/url?sid=$id&privilege=2&quality=2")
            httpInterface.execute(request).use { response ->
                HttpClientTools.assertSuccessWithContent(response, "bilibili audio playback URL")

                val data = JsonBrowser.parse(response.entity.content).get("data")
                data.get("cdns").values().firstOrNull()?.text()
                    ?: data.get("cdn").text()
                    ?: throw IOException("No playable Bilibili audio CDN was returned for sid=$id")
            }
        }

        TrackType.VIDEO -> {
            val videoCid = cid ?: throw IOException("Missing cid for Bilibili video track $id")

            getProgressiveVideoUrl(fetchVideoPlaybackData(httpInterface, videoCid, "fnval=0&qn=16"))
                ?: getDashAudioUrl(fetchVideoPlaybackData(httpInterface, videoCid, "fnval=16"))
                ?: throw IOException("No playable Bilibili audio stream was returned for bvid=$id")
        }
    }

    private fun fetchVideoPlaybackData(httpInterface: HttpInterface, videoCid: Long, options: String): JsonBrowser {
        val request = HttpGet("${BilibiliAudioSourceManager.BASE_URL}x/player/playurl?bvid=$id&cid=$videoCid&$options")

        httpInterface.execute(request).use { response ->
            HttpClientTools.assertSuccessWithContent(response, "bilibili video playback URL")
            return JsonBrowser.parse(response.entity.content).get("data")
        }
    }

    private fun getProgressiveVideoUrl(data: JsonBrowser): String? =
        data.get("durl").values().firstOrNull()?.get("url")?.text()

    private fun getDashAudioUrl(data: JsonBrowser): String? =
        data.get("dash").get("audio").values()
            .filter { item: JsonBrowser -> item.get("codecs").text()?.startsWith("mp4a.") != false }
            .sortedBy { item: JsonBrowser -> item.get("bandwidth").asLong(Long.MAX_VALUE) }
            .mapNotNull { item: JsonBrowser -> item.get("baseUrl").text() ?: item.get("base_url").text() }
            .firstOrNull()

    override fun makeShallowClone(): AudioTrack = BilibiliAudioTrack(trackInfo, type, id, cid, sourceManager)

    override fun getSourceManager(): AudioSourceManager = sourceManager

    enum class TrackType {
        VIDEO,
        AUDIO
    }
}
