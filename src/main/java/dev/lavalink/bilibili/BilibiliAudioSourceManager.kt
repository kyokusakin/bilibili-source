package dev.lavalink.bilibili

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager
import com.sedmelluq.discord.lavaplayer.tools.DataFormatTools
import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterfaceManager
import com.sedmelluq.discord.lavaplayer.track.AudioItem
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioReference
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo
import com.sedmelluq.discord.lavaplayer.track.BasicAudioPlaylist
import org.apache.http.client.methods.HttpGet
import org.apache.http.util.EntityUtils
import org.slf4j.LoggerFactory
import java.io.DataInput
import java.io.DataOutput
import java.io.IOException
import java.util.regex.Pattern

private fun JsonBrowser.textOrDefault(default: String): String = text() ?: default

class BilibiliAudioSourceManager : AudioSourceManager {
    private val log = LoggerFactory.getLogger(BilibiliAudioSourceManager::class.java)
    private val httpInterfaceManager: HttpInterfaceManager = HttpClientTools.createDefaultThreadLocalManager().also {
        it.setHttpContextFilter(BilibiliHttpContextFilter())
    }

    private var playlistPageCount: Int = -1

    override fun getSourceName(): String = SOURCE_NAME

    override fun loadItem(manager: AudioPlayerManager, reference: AudioReference): AudioItem? {
        val resolvedIdentifier = resolveReference(reference.identifier)
        val matcher = URL_PATTERN.matcher(resolvedIdentifier)

        if (!matcher.matches()) {
            return null
        }

        return when (matcher.group("type")) {
            "video" -> loadVideoItem(
                matcher.group("id"),
                matcher.group("audioType") == "av",
                extractPageParameter(resolvedIdentifier)
            )

            "audio" -> loadAudioItem(matcher.group("audioType"), matcher.group("audioId"))
            else -> null
        }
    }

    fun setPlaylistPageCount(count: Int): BilibiliAudioSourceManager {
        playlistPageCount = count
        return this
    }

    fun getHttpInterface(): HttpInterface = httpInterfaceManager.getInterface()

    private fun loadVideoItem(videoId: String, isAid: Boolean, page: Int?): AudioItem {
        val url = if (isAid) {
            "${BASE_URL}x/web-interface/view?aid=${videoId.removePrefix("av")}"
        } else {
            "${BASE_URL}x/web-interface/view?bvid=$videoId"
        }

        val responseJson = fetchJson(url, "bilibili video metadata")
        if (responseJson.get("code").asLong(-1) != 0L) {
            return AudioReference.NO_TRACK
        }

        val trackData = responseJson.get("data")
        val pages = trackData.get("pages").values()

        return if (pages.size > 1) {
            if (page != null) {
                loadVideoFromAnthology(trackData, page - 1)
            } else {
                loadVideoAnthology(trackData, 0)
            }
        } else {
            loadVideo(trackData)
        }
    }

    private fun loadAudioItem(audioType: String?, sid: String?): AudioItem {
        val requestType = when (audioType) {
            "am" -> "menu"
            "au" -> "song"
            else -> return AudioReference.NO_TRACK
        }

        val audioSid = sid ?: return AudioReference.NO_TRACK
        val responseJson = fetchJson("${BASE_URL}audio/music-service-c/web/$requestType/info?sid=$audioSid", "bilibili audio metadata")
        if (responseJson.get("code").asLong(-1) != 0L) {
            return AudioReference.NO_TRACK
        }

        return when (requestType) {
            "song" -> loadAudio(responseJson.get("data"))
            "menu" -> loadAudioPlaylist(responseJson.get("data"))
            else -> AudioReference.NO_TRACK
        }
    }

    private fun loadVideo(trackData: JsonBrowser): AudioTrack {
        val bvid = trackData.get("bvid").text() ?: throw IOException("Bilibili response did not contain a bvid")

        return BilibiliAudioTrack(
            AudioTrackInfo(
                trackData.get("title").textOrDefault("Unknown title"),
                trackData.get("owner").get("name").textOrDefault(UNKNOWN_ARTIST),
                trackData.get("duration").asLong(0) * 1000,
                bvid,
                false,
                getVideoUrl(bvid)
            ),
            BilibiliAudioTrack.TrackType.VIDEO,
            bvid,
            trackData.get("cid").asLong(0).takeIf { it > 0 },
            this
        )
    }

    private fun loadVideoFromAnthology(trackData: JsonBrowser, pageIndex: Int): AudioItem {
        val pages = trackData.get("pages").values()
        if (pageIndex !in pages.indices) {
            return loadVideoAnthology(trackData, 0)
        }

        return createAnthologyTrack(trackData, pages[pageIndex])
    }

    private fun loadVideoAnthology(trackData: JsonBrowser, selectedPage: Int): AudioPlaylist {
        val playlistName = trackData.get("title").textOrDefault("Bilibili playlist")
        val pages = trackData.get("pages").values()
        val tracks = ArrayList<AudioTrack>(pages.size)

        pages.forEach { tracks.add(createAnthologyTrack(trackData, it)) }

        return BasicAudioPlaylist(
            playlistName,
            tracks,
            tracks.getOrNull(selectedPage),
            false
        )
    }

    private fun createAnthologyTrack(trackData: JsonBrowser, pageData: JsonBrowser): AudioTrack {
        val bvid = trackData.get("bvid").text() ?: throw IOException("Bilibili response did not contain a bvid")
        val pageNumber = pageData.get("page").asLong(0).toInt().takeIf { it > 0 }

        return BilibiliAudioTrack(
            AudioTrackInfo(
                pageData.get("part").textOrDefault(trackData.get("title").textOrDefault("Unknown title")),
                trackData.get("owner").get("name").textOrDefault(UNKNOWN_ARTIST),
                pageData.get("duration").asLong(0) * 1000,
                bvid,
                false,
                getVideoUrl(bvid, pageNumber)
            ),
            BilibiliAudioTrack.TrackType.VIDEO,
            bvid,
            pageData.get("cid").asLong(0).takeIf { it > 0 },
            this
        )
    }

    private fun loadAudio(trackData: JsonBrowser): AudioTrack {
        val sid = extractAudioSid(trackData)
        val identifier = "au$sid"

        return BilibiliAudioTrack(
            AudioTrackInfo(
                trackData.get("title").textOrDefault("Unknown title"),
                trackData.get("uname").textOrDefault(UNKNOWN_ARTIST),
                trackData.get("duration").asLong(0) * 1000,
                identifier,
                false,
                getAudioUrl(identifier)
            ),
            BilibiliAudioTrack.TrackType.AUDIO,
            sid,
            null,
            this
        )
    }

    private fun loadAudioPlaylist(playlistData: JsonBrowser): AudioPlaylist {
        val playlistName = playlistData.get("title").textOrDefault("Bilibili audio playlist")
        val sid = extractAudioSid(playlistData)

        val firstPage = fetchJson(
            "${BASE_URL}audio/music-service-c/web/song/of-menu?sid=$sid&pn=1&ps=100",
            "bilibili audio playlist"
        ).get("data")

        val trackItems = ArrayList(firstPage.get("data").values())
        var currentPage = firstPage.get("curPage").asLong(1).toInt()
        val totalPages = firstPage.get("pageCount").asLong(1).toInt()
        val maxPages = when {
            playlistPageCount < 0 -> totalPages
            playlistPageCount < 1 -> 1
            else -> playlistPageCount.coerceAtMost(totalPages)
        }

        while (currentPage < maxPages) {
            currentPage += 1
            val pageData = fetchJson(
                "${BASE_URL}audio/music-service-c/web/song/of-menu?sid=$sid&pn=$currentPage&ps=100",
                "bilibili audio playlist page"
            ).get("data")

            trackItems.addAll(pageData.get("data").values())
        }

        return BasicAudioPlaylist(
            playlistName,
            trackItems.mapTo(ArrayList(trackItems.size)) { loadAudio(it) },
            null,
            false
        )
    }

    override fun isTrackEncodable(track: AudioTrack): Boolean = true

    override fun encodeTrack(track: AudioTrack, output: DataOutput) {
        track as BilibiliAudioTrack
        DataFormatTools.writeNullableText(output, track.type.name)
        DataFormatTools.writeNullableText(output, track.id)
        DataFormatTools.writeNullableText(output, track.cid?.toString())
    }

    override fun decodeTrack(trackInfo: AudioTrackInfo, input: DataInput): AudioTrack {
        val typeName = DataFormatTools.readNullableText(input)
            ?: throw IOException("Missing Bilibili track type")
        val trackId = DataFormatTools.readNullableText(input)
            ?: throw IOException("Missing Bilibili track identifier")
        val cid = DataFormatTools.readNullableText(input)?.toLong()

        val type = when (typeName) {
            BilibiliAudioTrack.TrackType.VIDEO.name -> BilibiliAudioTrack.TrackType.VIDEO
            BilibiliAudioTrack.TrackType.AUDIO.name -> BilibiliAudioTrack.TrackType.AUDIO
            else -> throw IOException("Unknown Bilibili track type: $typeName")
        }

        return BilibiliAudioTrack(trackInfo, type, trackId, cid, this)
    }

    override fun shutdown() {
        httpInterfaceManager.close()
    }

    private fun resolveReference(identifier: String): String {
        if (!SHORT_URL_PATTERN.matcher(identifier).matches()) {
            return identifier
        }

        return try {
            getHttpInterface().use { httpInterface ->
                httpInterface.execute(HttpGet(identifier)).use { response ->
                    if (!HttpClientTools.isSuccessWithContent(response.statusLine.statusCode)) {
                        return identifier
                    }

                    EntityUtils.consume(response.entity)
                    httpInterface.finalLocation?.toString() ?: identifier
                }
            }
        } catch (exception: Exception) {
            log.debug("Failed to resolve Bilibili short URL {}", identifier, exception)
            identifier
        }
    }

    private fun fetchJson(url: String, context: String): JsonBrowser =
        getHttpInterface().use { httpInterface ->
            httpInterface.execute(HttpGet(url)).use { response ->
                HttpClientTools.assertSuccessWithContent(response, context)
                JsonBrowser.parse(response.entity.content)
            }
        }

    private fun extractAudioSid(trackData: JsonBrowser): String =
        trackData.get("sid").text()
            ?: trackData.get("id").text()
            ?: trackData.get("statistic").get("sid").text()
            ?: throw IOException("Bilibili response did not contain an audio sid")

    private fun extractPageParameter(identifier: String): Int? =
        PAGE_PATTERN.find(identifier)?.groupValues?.getOrNull(1)?.toIntOrNull()?.takeIf { it > 0 }

    companion object {
        const val BASE_URL = "https://api.bilibili.com/"
        private const val SOURCE_NAME = "bilibili"
        private const val UNKNOWN_ARTIST = "Unknown artist"

        private val SHORT_URL_PATTERN = Pattern.compile("^https?://(?:www\\.)?b23\\.tv/.+$")
        private val PAGE_PATTERN = Regex("[?&]p=(\\d+)")
        private val URL_PATTERN = Pattern.compile(
            "^https?://(?:(?:www|m)\\.)?bilibili\\.com/(?<type>video|audio)/(?<id>(?:(?<audioType>am|au|av)?(?<audioId>[0-9]+))|[A-Za-z0-9]+)/?(?:\\?.*)?$"
        )

        private fun getVideoUrl(id: String, page: Int? = null): String =
            "https://www.bilibili.com/video/$id${if (page != null) "?p=$page" else ""}"

        private fun getAudioUrl(id: String): String = "https://www.bilibili.com/audio/$id"
    }
}
