package dev.lavalink.bilibili.plugin

import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager
import dev.lavalink.bilibili.BilibiliAudioSourceManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Conditional
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment

@Configuration(proxyBeanMethods = false)
class BilibiliPluginConfiguration {

    @Bean
    @Conditional(BilibiliEnabledCondition::class)
    fun bilibiliAudioSourceManager(
        pluginConfig: BilibiliPluginConfig,
        environment: Environment
    ): AudioSourceManager {
        val sourceManager = BilibiliAudioSourceManager()
        val legacyPlaylistLoadLimit = environment.getProperty(
            "lavalink.server.bilibiliPlaylistLoadLimit",
            Int::class.javaObjectType
        )

        (pluginConfig.playlistLoadLimit ?: legacyPlaylistLoadLimit)?.let {
            sourceManager.setPlaylistPageCount(it)
        }

        return sourceManager
    }
}
