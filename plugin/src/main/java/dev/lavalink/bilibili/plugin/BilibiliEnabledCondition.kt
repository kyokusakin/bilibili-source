package dev.lavalink.bilibili.plugin

import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.core.type.AnnotatedTypeMetadata

class BilibiliEnabledCondition : Condition {

    override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean {
        val environment = context.environment

        return parseBoolean(environment.getProperty("plugins.bilibili.enabled"))
            ?: parseBoolean(environment.getProperty("lavalink.server.sources.bilibili"))
            ?: true
    }

    private fun parseBoolean(value: String?): Boolean? = when (value?.trim()?.lowercase()) {
        "true" -> true
        "false" -> false
        else -> null
    }
}
