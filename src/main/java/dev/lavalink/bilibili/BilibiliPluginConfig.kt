package dev.lavalink.bilibili

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "plugins.bilibili")
class BilibiliPluginConfig {
    var enabled: Boolean? = null
    var playlistLoadLimit: Int? = null
}
